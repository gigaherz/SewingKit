package dev.gigaherz.sewingkit.table;

import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public interface InventoryProvider
{
    void addWeakListener(SewingTableMenu e);

    ItemStacksResourceHandler getInventory();

    default boolean isDummy()
    {
        return false;
    }
}
