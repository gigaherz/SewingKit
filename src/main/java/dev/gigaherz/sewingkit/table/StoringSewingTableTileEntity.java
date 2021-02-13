package dev.gigaherz.sewingkit.table;

import com.google.common.collect.Lists;
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

public class StoringSewingTableTileEntity extends TileEntity
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
    public void read(CompoundNBT nbt)
    {
        super.read(nbt);
        inventory.deserializeNBT(nbt.getCompound("Items"));
    }

    private final List<Reference<? extends SewingTableContainer>> listeners = Lists.newArrayList();
    private final ReferenceQueue<SewingTableContainer> pendingRemovals = new ReferenceQueue<>();

    public void addWeakListener(SewingTableContainer e)
    {
        listeners.add(new WeakReference<>(e, pendingRemovals));
    }

    @Override
    public void markDirty()
    {
        super.markDirty();

        for (Reference<? extends SewingTableContainer>
             ref = pendingRemovals.poll();
             ref != null;
             ref = pendingRemovals.poll())
        {
            listeners.remove(ref);
        }

        for (Iterator<Reference<? extends SewingTableContainer>> iterator = listeners.iterator(); iterator.hasNext(); )
        {
            Reference<? extends SewingTableContainer> reference = iterator.next();
            SewingTableContainer listener = reference.get();
            if (listener == null)
                iterator.remove();
            else
                listener.onInventoryChanged();
        }
    }
}
