package dev.gigaherz.sewingkit.table;

import com.mojang.logging.LogUtils;
import dev.gigaherz.sewingkit.SewingKitMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.slf4j.Logger;

public class StoringSewingTableBlockEntity extends BlockEntity implements InventoryProvider
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private final SewingTableContainer inventory = new SewingTableContainer();

    protected StoringSewingTableBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state)
    {
        super(tileEntityTypeIn, pos, state);
    }

    public StoringSewingTableBlockEntity(BlockPos pos, BlockState state)
    {
        this(SewingKitMod.STORING_SEWING_STATION_BLOCK_ENTITY.get(), pos, state);
    }

    public Container getInventory()
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
        Containers.dropContents(getLevel(), getBlockPos(), inventory);
    }

    private class SewingTableContainer extends SimpleContainer implements ValueIOSerializable
    {
        {
            addListener(this::onContentsChanged);
        }

        public SewingTableContainer()
        {
            super(6);
        }

        private void onContentsChanged(Container itemStacks)
        {
            StoringSewingTableBlockEntity.this.setChanged();
            StoringSewingTableBlockEntity.this.listenable.doCallbacks();
        }

        @Override
        public void serialize(ValueOutput output)
        {
            var children = output.childrenList("Items");
            for(var i=0;i<getContainerSize();i++)
            {
                var stack = getItem(i);
                if (!stack.isEmpty())
                {
                    var child = children.addChild();
                    child.putInt("Slot", i);
                    child.store(ItemStack.MAP_CODEC, stack);
                }
            }
        }

        @Override
        public void deserialize(ValueInput input)
        {
            clearContent();
            var childrenOpt = input.childrenList("Items");
            if (childrenOpt.isEmpty()) return;
            for(var child : childrenOpt.get())
            {
                try
                {
                    int slot = child.getInt("Slot").orElseThrow();
                    var stack = child.read(ItemStack.MAP_CODEC).orElseThrow();
                    setItem(slot, stack);
                }
                catch(Exception e)
                {
                    LOGGER.error("Could not deserialize stack", e);
                }
            }
        }
    }
}
