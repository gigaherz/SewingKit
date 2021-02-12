package dev.gigaherz.sewingkit.file;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class FileItem extends Item
{
    public FileItem(Item.Properties properties)
    {
        super(properties);
    }

    @Override
    public boolean hasContainerItem()
    {
        return true;
    }

    @Override
    public boolean hasContainerItem(ItemStack stack)
    {
        return stack.getDamage() < stack.getMaxDamage();
    }

    @Override
    public ItemStack getContainerItem(ItemStack itemStack)
    {
        if (itemStack.getDamage() < itemStack.getMaxDamage())
        {
            ItemStack stack = itemStack.copy();
            stack.setDamage(itemStack.getDamage()+1);
            return stack;
        }
        return ItemStack.EMPTY;
    }
}
