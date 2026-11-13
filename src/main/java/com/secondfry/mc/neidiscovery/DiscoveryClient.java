package com.secondfry.mc.neidiscovery;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import codechicken.nei.ItemList;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.RecipeCatalysts;
import codechicken.nei.recipe.StackInfo;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class DiscoveryClient {

    static final DiscoveryClient INSTANCE = new DiscoveryClient();
    private static final Logger LOG = LogManager.getLogger("NEI Discovery");

    volatile Set<StackKey> visible = Collections.emptySet();
    boolean ready;
    private final Set<StackKey> seen = new HashSet<>();
    private final List<DiscoveryGraph.Recipe<StackKey>> recipes = new ArrayList<>();
    private final Map<ICraftingHandler, Set<StackKey>> workstationCache = new HashMap<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "NEI Discovery reachability");
        thread.setDaemon(true);
        return thread;
    });
    private List<ICraftingHandler> handlers;
    private int handlerIndex;
    private ICraftingHandler handler;
    private int recipeIndex;
    private boolean indexed;
    private String session;
    private File historyFile;
    private boolean writable;
    private boolean unsaved;
    private long revision;
    private long computedRevision = -1;
    private long calculationRevision;
    private Future<Set<StackKey>> calculation;
    private boolean chains;
    private int ticks;

    void invalidate() {
        revision++;
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ready) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) {
            if (session != null) {
                save();
                session = null;
                seen.clear();
                visible = Collections.emptySet();
                invalidate();
            }
            return;
        }
        String worldPath = NEIClientConfig.getWorldPath();
        if (worldPath == null) return;
        String current = worldPath + "\n" + mc.thePlayer.getUniqueID();
        if (!current.equals(session)) {
            save();
            session = current;
            load(mc, current);
            visible = Collections.unmodifiableSet(new HashSet<>(seen));
            invalidate();
            ItemList.updateFilter.restart();
        }
        ticks++;
        int before = seen.size();
        for (ItemStack stack : mc.thePlayer.inventory.mainInventory) observe(stack);
        for (ItemStack stack : mc.thePlayer.inventory.armorInventory) observe(stack);
        observe(mc.thePlayer.inventory.getItemStack());
        if (mc.thePlayer.openContainer != null) {
            for (Object value : mc.thePlayer.openContainer.inventorySlots) observe(((Slot) value).getStack());
        }
        if (before != seen.size()) {
            unsaved = true;
            invalidate();
        }
        if (ItemList.loadFinished && !indexed) indexRecipes();
        boolean setting = NEIClientConfig.getBooleanSetting(NEIDiscoveryConfig.CHAINS);
        if (chains != setting) {
            chains = setting;
            invalidate();
        }
        if (calculation != null && calculation.isDone()) {
            try {
                Set<StackKey> result = calculation.get();
                if (calculationRevision == revision) {
                    visible = result;
                    computedRevision = revision;
                    ItemList.updateFilter.restart();
                }
            } catch (Exception e) {
                LOG.error("Could not compute discoveries", e);
            }
            calculation = null;
        }
        if (calculation == null && computedRevision != revision && ticks % 20 == 0) {
            final Set<StackKey> snapshot = new HashSet<>(seen);
            final List<DiscoveryGraph.Recipe<StackKey>> catalog = new ArrayList<>(recipes);
            final boolean includeChains = chains;
            calculationRevision = revision;
            calculation = worker
                .submit(() -> DiscoveryGraph.visible(snapshot, catalog, includeChains, StackKey::ingredientIdentity));
        }
        if (ticks % 100 == 0) save();
    }

    private void observe(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) return;
        seen.add(StackKey.of(stack));
        FluidStack fluid = StackInfo.getFluid(stack);
        if (fluid != null && fluid.getFluid() != null && fluid.amount > 0) seen.add(new StackKey(fluid));
    }

    private void indexRecipes() {
        if (handlers == null) {
            handlers = new ArrayList<>(GuiCraftingRecipe.craftinghandlers);
            handlers.addAll(GuiCraftingRecipe.serialCraftingHandlers);
        }
        long deadline = System.nanoTime() + 3_000_000;
        while (System.nanoTime() < deadline) {
            if (handler == null) {
                if (handlerIndex == handlers.size()) {
                    indexed = true;
                    invalidate();
                    LOG.info("Indexed {} recipes from {} NEI handlers", recipes.size(), handlers.size());
                    return;
                }
                ICraftingHandler prototype = handlers.get(handlerIndex++);
                try {
                    // Use NEI's own all-recipes protocol, including machine handler transfer identifiers.
                    handler = prototype instanceof TemplateRecipeHandler
                        ? ((TemplateRecipeHandler) prototype).getAllRecipeHandler()
                        : prototype.getRecipeHandler("all");
                    recipeIndex = 0;
                } catch (RuntimeException e) {
                    LOG.warn("Skipping unsupported NEI handler " + prototype.getHandlerId(), e);
                    continue;
                }
            }
            try {
                if (handler == null) continue;
                if (recipeIndex >= handler.numRecipes()) {
                    handler = null;
                    continue;
                }
                addRecipe(handler, recipeIndex++);
            } catch (RuntimeException e) {
                LOG.warn("Skipping remaining recipes of " + handler.getHandlerId(), e);
                handler = null;
            }
        }
    }

    private void addRecipe(ICraftingHandler source, int index) {
        List<Set<StackKey>> inputs = new ArrayList<>();
        for (PositionedStack ingredient : source.getIngredientStacks(index)) inputs.add(keys(ingredient));
        if (inputs.isEmpty()) return;
        Set<StackKey> outputs = new HashSet<>();
        PositionedStack result = source.getResultStack(index);
        if (result != null) {
            outputs.addAll(keys(result));
        } else {
            // GregTech exposes its item/fluid outputs here; with a primary result this can instead be fuel.
            for (PositionedStack output : source.getOtherStacks(index)) outputs.addAll(keys(output));
        }
        Set<StackKey> workstations = workstationCache.computeIfAbsent(source, handler -> {
            String overlay = handler.getOverlayIdentifier();
            if ("crafting".equals(overlay) || "crafting2x2".equals(overlay)) return null;
            Set<StackKey> catalysts = new HashSet<>();
            for (PositionedStack catalyst : RecipeCatalysts.getRecipeCatalysts(handler)) {
                catalysts.addAll(keys(catalyst));
            }
            // shortcut: missing workstation metadata hides inferred outputs; add handler support if needed.
            return Collections.unmodifiableSet(catalysts);
        });
        if (!outputs.isEmpty()) recipes.add(new DiscoveryGraph.Recipe<>(inputs, outputs, workstations));
    }

    private Set<StackKey> keys(PositionedStack positioned) {
        Set<StackKey> result = new HashSet<>();
        if (positioned == null) return result;
        positioned.generatePermutations();
        for (ItemStack stack : positioned.items) {
            if (stack != null && stack.getItem() != null) result.add(StackKey.of(stack));
        }
        return result;
    }

    private void load(Minecraft mc, String identity) {
        seen.clear();
        unsaved = false;
        writable = false;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(identity.getBytes(StandardCharsets.UTF_8));
            StringBuilder name = new StringBuilder();
            for (byte value : digest) name.append(String.format("%02x", value & 255));
            historyFile = new File(mc.mcDataDir, "saves/NEIDiscovery/" + name + ".dat");
            if (historyFile.exists()) {
                try (FileInputStream stream = new FileInputStream(historyFile)) {
                    NBTTagCompound root = CompressedStreamTools.readCompressed(stream);
                    if (root.getInteger("version") != 1) throw new java.io.IOException("Unknown history version");
                    NBTTagList entries = root.getTagList("seen", 10);
                    for (int i = 0; i < entries.tagCount(); i++) {
                        StackKey key = StackKey.read(entries.getCompoundTagAt(i));
                        if (key != null) seen.add(key);
                    }
                }
            }
            writable = true;
        } catch (Exception e) {
            // Preserve unreadable history rather than replacing it with a fresh partial history.
            LOG.error("Could not load discoveries; history writes disabled for this session", e);
        }
    }

    private void save() {
        if (!unsaved || !writable || historyFile == null) return;
        try {
            Files.createDirectories(
                historyFile.toPath()
                    .getParent());
            NBTTagCompound root = new NBTTagCompound();
            root.setInteger("version", 1);
            NBTTagList entries = new NBTTagList();
            for (StackKey key : seen) entries.appendTag(key.write());
            root.setTag("seen", entries);
            File temporary = new File(historyFile.getPath() + ".tmp");
            try (FileOutputStream stream = new FileOutputStream(temporary)) {
                CompressedStreamTools.writeCompressed(root, stream);
            }
            try {
                Files.move(
                    temporary.toPath(),
                    historyFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temporary.toPath(), historyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            unsaved = false;
        } catch (Exception e) {
            LOG.error("Could not save discoveries; retaining previous history", e);
        }
    }
}
