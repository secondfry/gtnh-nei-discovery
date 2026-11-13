package com.secondfry.mc.neidiscovery;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import codechicken.nei.NEIServerUtils;

public class DiscoveryRecipeTest {

    @Test
    public void specificHammerMaterialIsRequiredAndDoesNotBecomeAnObservedDust() {
        Item hammerItem = new Item().setHasSubtypes(true);
        ItemStack ironHammer = new ItemStack(hammerItem, 1, 5);
        ItemStack woodHammer = ironHammer.copy();
        NBTTagCompound ironTag = new NBTTagCompound();
        NBTTagCompound ironStats = new NBTTagCompound();
        ironStats.setString("PrimaryMaterial", "Iron");
        ironTag.setTag("GT.ToolStats", ironStats);
        ironHammer.setTagCompound(ironTag);
        NBTTagCompound woodTag = (NBTTagCompound) ironTag.copy();
        woodTag.getCompoundTag("GT.ToolStats")
            .setString("PrimaryMaterial", "Wood");
        woodHammer.setTagCompound(woodTag);
        StackKey ore = new StackKey(new ItemStack(new Item()));
        StackKey dust = new StackKey(new ItemStack(new Item()));
        DiscoveryGraph.Recipe<StackKey> recipe = new DiscoveryGraph.Recipe<>(
            java.util.Arrays.asList(Set.of(ore), Set.of(new StackKey(ironHammer))),
            Set.of(dust));
        assertFalse(
            DiscoveryGraph
                .visible(
                    Set.of(ore, new StackKey(woodHammer)),
                    Collections.singletonList(recipe),
                    true,
                    StackKey::ingredientIdentity)
                .contains(dust));
        assertTrue(
            DiscoveryGraph
                .visible(
                    Set.of(ore, new StackKey(ironHammer)),
                    Collections.singletonList(recipe),
                    false,
                    StackKey::ingredientIdentity)
                .contains(dust));
    }

    @Test
    public void helmetAcceptedByCraftablesAlsoPassesDiscoveryWithTaggedMallet() {
        ItemStack recipeMallet = new ItemStack(new Item().setHasSubtypes(true), 1, 3);
        ItemStack inventoryMallet = recipeMallet.copy();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound stats = new NBTTagCompound();
        stats.setString("PrimaryMaterial", "Wood");
        stats.setLong("Damage", 17);
        tag.setTag("GT.ToolStats", stats);
        inventoryMallet.setTagCompound(tag);
        StackKey helmet = new StackKey(new ItemStack(new Item()));
        DiscoveryGraph.Recipe<StackKey> recipe = new DiscoveryGraph.Recipe<>(
            Collections.singletonList(Set.of(new StackKey(recipeMallet))),
            Set.of(helmet));
        assertTrue(NEIServerUtils.areStacksSameTypeCrafting(recipeMallet, inventoryMallet));
        assertTrue(
            DiscoveryGraph
                .visible(
                    Set.of(new StackKey(inventoryMallet)),
                    Collections.singletonList(recipe),
                    false,
                    StackKey::ingredientIdentity)
                .contains(helmet));
    }
}
