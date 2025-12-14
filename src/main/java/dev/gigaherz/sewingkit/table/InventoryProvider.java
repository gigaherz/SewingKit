package dev.gigaherz.sewingkit.table;

import net.minecraft.world.Container;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public interface InventoryProvider
{
    void addWeakListener(SewingTableMenu e);

    Container getInventory();

    default boolean isDummy()
    {
        return false;
    }
}
