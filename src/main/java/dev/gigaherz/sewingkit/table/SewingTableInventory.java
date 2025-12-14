package dev.gigaherz.sewingkit.table;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;

class SewingTableInventory extends SimpleContainer implements InventoryProvider
{
    private final ListenableHolder listenable = new ListenableHolder();

    public SewingTableInventory()
    {
        super(6);
        addListener(this::onContentsChanged);
    }

    private void onContentsChanged(Container itemStacks)
    {
        listenable.doCallbacks();
    }

    @Override
    public void addWeakListener(SewingTableMenu e)
    {
        listenable.addWeakListener(e);
    }

    @Override
    public Container getInventory()
    {
        return this;
    }

    @Override
    public boolean isDummy()
    {
        return true;
    }
}
