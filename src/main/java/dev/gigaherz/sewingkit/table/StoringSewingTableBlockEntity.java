package dev.gigaherz.sewingkit.table;

import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

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
    protected void saveAdditional(CompoundTag compound)
    {
        super.saveAdditional(compound);
        compound.put("Items", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag nbt)
    {
        super.load(nbt);
        inventory.deserializeNBT(nbt.getCompound("Items"));
    }

    private final ListenableHolder listenable = new ListenableHolder();

    @Override
    public void setChanged()
    {
        super.setChanged();
    }

    @Override
    public void addWeakListener(SewingTableMenu e)
    {
        listenable.addWeakListener(e);
    }
}
