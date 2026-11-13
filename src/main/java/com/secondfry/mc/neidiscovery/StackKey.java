package com.secondfry.mc.neidiscovery;

import java.util.Objects;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import codechicken.nei.recipe.StackInfo;

final class StackKey {

    final ItemStack stack;
    private final String fluidName;
    private final NBTTagCompound fluidTag;
    private final int hash;

    StackKey(ItemStack original) {
        stack = original.copy();
        stack.stackSize = 1;
        fluidName = null;
        fluidTag = null;
        if (stack.isItemStackDamageable() && !stack.getHasSubtypes()) stack.setItemDamage(0);
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("GT.ToolStats", 10)) {
            // Tool material determines the pickaxe variant; wear, mode and charge do not.
            NBTTagCompound stats = (NBTTagCompound) tag.getCompoundTag("GT.ToolStats")
                .copy();
            stats.removeTag("Damage");
            stats.removeTag("Mode");
            NBTTagCompound identity = new NBTTagCompound();
            identity.setTag("GT.ToolStats", stats);
            stack.setTagCompound(identity);
        } else if (stack.isItemStackDamageable() && !stack.getHasSubtypes()) {
            // Names and enchantments don't make a newly seen vanilla tool undiscovered.
            stack.setTagCompound(null);
        }
        hash = Objects.hash(stack.getItem(), stack.getItemDamage(), stack.getTagCompound());
    }

    StackKey(FluidStack fluid) {
        stack = null;
        fluidName = fluid.getFluid()
            .getName();
        fluidTag = fluid.tag == null ? null : (NBTTagCompound) fluid.tag.copy();
        hash = Objects.hash(fluidName, fluidTag);
    }

    static StackKey of(ItemStack stack) {
        FluidStack fluid = StackInfo.isFluidDisplayItem(stack) ? StackInfo.getFluid(stack) : null;
        return fluid == null ? new StackKey(stack) : new StackKey(fluid);
    }

    StackKey ingredientIdentity() {
        if (stack == null) return this;
        ItemStack ingredient = stack.copy();
        // Craftables matches item/metadata without NBT, and ignores damage on damageable items.
        ingredient.setTagCompound(null);
        if (ingredient.getItem()
            .isDamageable()) ingredient.setItemDamage(0);
        return new StackKey(ingredient);
    }

    NBTTagCompound write() {
        if (fluidName != null) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("fluidName", fluidName);
            if (fluidTag != null) tag.setTag("fluidTag", fluidTag.copy());
            return tag;
        }
        NBTTagCompound tag = stack.copy()
            .writeToNBT(new NBTTagCompound());
        // Registry names survive numeric item-ID remapping between launches.
        tag.setString("registryName", Item.itemRegistry.getNameForObject(stack.getItem()));
        return tag;
    }

    static StackKey read(NBTTagCompound tag) {
        if (tag.hasKey("fluidName")) {
            if (FluidRegistry.getFluid(tag.getString("fluidName")) == null) return null;
            FluidStack fluid = new FluidStack(FluidRegistry.getFluid(tag.getString("fluidName")), 1);
            if (tag.hasKey("fluidTag", 10)) fluid.tag = tag.getCompoundTag("fluidTag");
            return new StackKey(fluid);
        }
        Item item = (Item) Item.itemRegistry.getObject(tag.getString("registryName"));
        if (item == null) return null;
        ItemStack stack = new ItemStack(item, 1, tag.getShort("Damage"));
        if (tag.hasKey("tag", 10)) stack.setTagCompound(
            (NBTTagCompound) tag.getCompoundTag("tag")
                .copy());
        return new StackKey(stack);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof StackKey)) return false;
        StackKey key = (StackKey) other;
        if (fluidName != null || key.fluidName != null) {
            return Objects.equals(fluidName, key.fluidName) && Objects.equals(fluidTag, key.fluidTag);
        }
        ItemStack that = key.stack;
        return stack.getItem() == that.getItem() && stack.getItemDamage() == that.getItemDamage()
            && Objects.equals(stack.getTagCompound(), that.getTagCompound());
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
