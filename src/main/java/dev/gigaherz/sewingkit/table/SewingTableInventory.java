package dev.gigaherz.sewingkit.table;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

class SewingTableInventory extends ItemStacksResourceHandler implements InventoryProvider
{
    private final ListenableHolder listenable = new ListenableHolder();

    public SewingTableInventory()
    {
        super(6);
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents)
    {
        super.onContentsChanged(index, previousContents);
        listenable.doCallbacks();
    }

    @Override
    public void addWeakListener(SewingTableMenu e)
    {
        listenable.addWeakListener(e);
    }

    @Override
    public ItemStacksResourceHandler getInventory()
    {
        return this;
    }

    @Override
    public boolean isDummy()
    {
        return true;
    }
}
