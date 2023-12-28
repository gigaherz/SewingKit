package dev.gigaherz.sewingkit.table;

import net.neoforged.neoforge.items.IItemHandlerModifiable;

public interface InventoryProvider
{
    void addWeakListener(SewingTableMenu e);

    IItemHandlerModifiable getInventory();

    default boolean isDummy()
    {
        return false;
    }
}
