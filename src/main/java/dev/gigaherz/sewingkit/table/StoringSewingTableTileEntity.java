package dev.gigaherz.sewingkit.table;

import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ObjectHolder;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

public class StoringSewingTableTileEntity extends TileEntity implements InventoryProvider
{
    @ObjectHolder("sewingkit:storing_sewing_station")
    public static TileEntityType<?> TYPE;

    private final ItemStackHandler inventory = new ItemStackHandler(6)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            super.onContentsChanged(slot);
            markDirty();
            listenable.doCallbacks();
        }
    };

    protected StoringSewingTableTileEntity(TileEntityType<?> tileEntityTypeIn)
    {
        super(tileEntityTypeIn);
    }

    public StoringSewingTableTileEntity()
    {
        super(TYPE);
    }

    public IItemHandlerModifiable getInventory()
    {
        return inventory;
    }

    @Override
    public CompoundNBT write(CompoundNBT compound)
    {
        compound = super.write(compound);
        compound.put("Items", inventory.serializeNBT());
        return compound;
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt)
    {
        super.read(state, nbt);
        inventory.deserializeNBT(nbt.getCompound("Items"));
    }

    private final ListenableHolder listenable = new ListenableHolder();

    public void addWeakListener(SewingTableContainer e)
    {
        listenable.addWeakListener(e);
    }
}
