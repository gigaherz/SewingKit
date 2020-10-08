package dev.gigaherz.sewingkit.table;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

public class SewingTableBlock extends Block
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SewingTableBlock(Properties properties)
    {
        super(properties);
    }

    public VoxelShape cuboidWithRotation(Direction facing, double x1, double y1, double z1, double x2, double y2, double z2)
    {
        switch (facing)
        {
            case NORTH:
                return makeCuboidShape(x1, y1, z1, x2, y2, z2);
            case EAST:
                return makeCuboidShape(16 - z2, y1, x1, 16 - z1, y2, x2);
            case SOUTH:
                return makeCuboidShape(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
            case WEST:
                return makeCuboidShape(z1, y1, 16 - x2, z2, y2, 16 - x1);
        }
        LOGGER.warn("Sewing Table voxel shape requested for an invalid rotation " + facing + ". This can't happen. The selection/collision shape will be wrong.");
        return makeCuboidShape(x1, y1, z1, x2, y2, z2);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
    {
        Direction facing = state.get(FACING);
        return VoxelShapes.or(
                cuboidWithRotation(facing, 0, 14, 0, 16, 16, 16),
                cuboidWithRotation(facing, 11, 6, 1, 15, 14, 15),
                cuboidWithRotation(facing, 12, 0, 2, 14, 6, 14),
                cuboidWithRotation(facing, 1, 6, 1, 5, 14, 15),
                cuboidWithRotation(facing, 2, 0, 2, 4, 6, 14)
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {
        return getDefaultState().with(FACING, context.getPlacementHorizontalFacing().getOpposite());
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit)
    {
        if (worldIn.isRemote)
            return ActionResultType.SUCCESS;

        player.openContainer(new SimpleNamedContainerProvider(
                (id,playerInv,p) -> new SewingTableContainer(id,playerInv, IWorldPosCallable.of(worldIn, pos)),
                new TranslationTextComponent("container.sewingkit.sewing_station")
        ));

        return ActionResultType.SUCCESS;
    }
}
