package dev.gigaherz.sewingkit.file;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class FileItem extends Item
{
    public FileItem(Item.Properties properties)
    {
        super(properties);
    }

    @Override
    public ItemStack getCraftingRemainder(ItemStack itemStack)
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
