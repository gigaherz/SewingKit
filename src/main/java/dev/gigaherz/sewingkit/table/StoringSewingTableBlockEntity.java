package dev.gigaherz.sewingkit.table;

import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

public class StoringSewingTableBlockEntity extends BlockEntity implements InventoryProvider
{
    private final ItemStackHandler inventory = new ItemStackHandler(6)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            super.onContentsChanged(slot);
            setChanged();
            listenable.doCallbacks();
        }
    };

    protected StoringSewingTableBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state)
    {
        super(tileEntityTypeIn, pos, state);
    }

    public StoringSewingTableBlockEntity(BlockPos pos, BlockState state)
    {
        this(SewingKitMod.STORING_SEWING_STATION_BLOCK_ENTITY.get(), pos, state);
    }

    public IItemHandlerModifiable getInventory()
    {
        return inventory;
    }

    @Override
    protected void saveAdditional(CompoundTag compound, HolderLookup.Provider provider)
    {
        super.saveAdditional(compound, provider);
        compound.put("Items", inventory.serializeNBT(provider));
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        super.loadAdditional(nbt, provider);
        inventory.deserializeNBT(provider, nbt.getCompoundOrEmpty("Items"));
    }

    private final ListenableHolder listenable = new ListenableHolder();

    @Override
    public void addWeakListener(SewingTableMenu e)
    {
        listenable.addWeakListener(e);
    }

    @Override
    public void preRemoveSideEffects(BlockPos p_394577_, BlockState p_394161_)
    {
        dropContents();
    }

    public void dropContents()
    {
        for (int i = 0; i < inventory.getSlots(); i++)
        {
            var pos = getBlockPos();
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(i));
        }
    }
}
