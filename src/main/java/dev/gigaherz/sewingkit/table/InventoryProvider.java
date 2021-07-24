package dev.gigaherz.sewingkit.table;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

public interface InventoryProvider
{
    void addWeakListener(SewingTableContainer listener);

    IItemHandlerModifiable getInventory();

    default boolean isDummy() { return false; }
}
