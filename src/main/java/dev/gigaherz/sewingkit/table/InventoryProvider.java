package dev.gigaherz.sewingkit.table;

import net.minecraftforge.items.IItemHandlerModifiable;

public interface InventoryProvider
{
    void addWeakListener(SewingTableContainer e);

    IItemHandlerModifiable getInventory();

    default boolean isDummy()
    {
        return false;
    }
}
