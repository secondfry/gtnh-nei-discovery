package com.secondfry.mc.neidiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class StackKeyTest {

    @Test
    public void changingTheObservedStackDoesNotMutateTheFilterSnapshot() {
        Item item = new Item().setHasSubtypes(true);
        ItemStack original = tool(item, 2, "Iron", 20);
        StackKey key = new StackKey(original);
        original.getTagCompound()
            .getCompoundTag("GT.ToolStats")
            .setString("PrimaryMaterial", "Steel");
        assertEquals(key, new StackKey(tool(item, 2, "Iron", 0)));
        assertNotEquals(key, new StackKey(original));
    }

    @Test
    public void vanillaToolDurabilityAndNamesDoNotHidePreviouslySeenTools() {
        Item item = new Item().setMaxDamage(100);
        ItemStack fresh = new ItemStack(item, 1, 0);
        ItemStack worn = new ItemStack(item, 1, 17);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("display", "Named tool");
        worn.setTagCompound(tag);
        assertEquals(new StackKey(fresh), new StackKey(worn));
    }

    @Test
    public void gregTechToolWearIsIgnoredButMaterialAndToolTypeAreRetained() {
        Item item = new Item().setHasSubtypes(true);
        ItemStack iron = tool(item, 2, "Iron", 0);
        ItemStack wornIron = tool(item, 2, "Iron", 900);
        wornIron.stackSize = 7;
        wornIron.getTagCompound()
            .setLong("GT.ItemCharge", 1000);
        assertEquals(new StackKey(iron), new StackKey(wornIron));
        assertEquals(new StackKey(iron).hashCode(), new StackKey(wornIron).hashCode());
        assertNotEquals(new StackKey(iron), new StackKey(tool(item, 2, "Steel", 0)));
        assertNotEquals(new StackKey(iron), new StackKey(tool(item, 4, "Iron", 0)));
    }

    @Test
    public void metadataItemsAndNbtVariantsStayDistinct() {
        Item item = new Item().setHasSubtypes(true);
        ItemStack one = new ItemStack(item, 1, 1);
        ItemStack two = new ItemStack(item, 1, 2);
        assertNotEquals(new StackKey(one), new StackKey(two));
        ItemStack tagged = one.copy();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("material", "steel");
        tagged.setTagCompound(tag);
        assertNotEquals(new StackKey(one), new StackKey(tagged));
    }

    private ItemStack tool(Item item, int type, String material, long wear) {
        ItemStack stack = new ItemStack(item, 1, type);
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound stats = new NBTTagCompound();
        stats.setString("PrimaryMaterial", material);
        stats.setString("SecondaryMaterial", "Wood");
        stats.setLong("Damage", wear);
        stats.setByte("Mode", (byte) (wear == 0 ? 0 : 1));
        tag.setTag("GT.ToolStats", stats);
        stack.setTagCompound(tag);
        return stack;
    }
}
