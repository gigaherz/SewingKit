package dev.gigaherz.sewingkit.table;

import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class StoringSewingTableBlockEntity extends BlockEntity implements InventoryProvider
{
    private final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler (6)
    {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents)
        {
            super.onContentsChanged(index, previousContents);
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

    public ItemStacksResourceHandler getInventory()
    {
        return inventory;
    }

    @Override
    protected void saveAdditional(ValueOutput output)
    {
        super.saveAdditional(output);
        inventory.serialize(output.child("Items"));
    }

    @Override
    public void loadAdditional(ValueInput nbt)
    {
        super.loadAdditional(nbt);
        inventory.deserialize(nbt.childOrEmpty("Items"));
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
        for (int i = 0; i < inventory.size(); i++)
        {
            var pos = getBlockPos();
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), SewingInput.stackAt(inventory, i));
        }
    }
}
