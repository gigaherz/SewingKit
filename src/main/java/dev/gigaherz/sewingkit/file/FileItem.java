package dev.gigaherz.sewingkit.file;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class FileItem extends Item
{
    public FileItem(Item.Properties properties)
    {
        super(properties);
    }

    @Deprecated
    @Override
    public boolean hasCraftingRemainingItem()
    {
        return true;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack)
    {
        return stack.getDamageValue() < stack.getMaxDamage();
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack itemStack)
    {
        if (itemStack.getDamageValue() < itemStack.getMaxDamage())
        {
            ItemStack stack = itemStack.copy();
            stack.setDamageValue(itemStack.getDamageValue() + 1);
            return stack;
        }
        return ItemStack.EMPTY;
    }
}
