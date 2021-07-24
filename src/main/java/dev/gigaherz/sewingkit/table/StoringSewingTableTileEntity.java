package dev.gigaherz.sewingkit.table;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ObjectHolder;

public class StoringSewingTableTileEntity extends BlockEntity implements InventoryProvider
{
    @ObjectHolder("sewingkit:storing_sewing_station")
    public static BlockEntityType<?> TYPE;

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

    protected StoringSewingTableTileEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state)
    {
        super(tileEntityTypeIn, pos, state);
    }

    public StoringSewingTableTileEntity(BlockPos pos, BlockState state)
    {
        super(TYPE, pos, state);
    }

    public IItemHandlerModifiable getInventory()
    {
        return inventory;
    }

    @Override
    public CompoundTag save(CompoundTag compound)
    {
        compound = super.save(compound);
        compound.put("Items", inventory.serializeNBT());
        return compound;
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
    public void addWeakListener(SewingTableContainer e)
    {
        listenable.addWeakListener(e);
    }
}
