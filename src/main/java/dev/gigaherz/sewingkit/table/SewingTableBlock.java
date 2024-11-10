package dev.gigaherz.sewingkit.table;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;

public class SewingTableBlock extends Block
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SewingTableBlock(Properties properties)
    {
        super(properties);
    }

    public VoxelShape cuboidWithRotation(Direction facing, double x1, double y1, double z1, double x2, double y2, double z2)
    {
        switch (facing)
        {
            case NORTH:
                return box(x1, y1, z1, x2, y2, z2);
            case EAST:
                return box(16 - z2, y1, x1, 16 - z1, y2, x2);
            case SOUTH:
                return box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
            case WEST:
                return box(z1, y1, 16 - x2, z2, y2, 16 - x1);
        }
        LOGGER.warn("Sewing Table voxel shape requested for an invalid rotation " + facing + ". This can't happen. The selection/collision shape will be wrong.");
        return box(x1, y1, z1, x2, y2, z2);
    }

    @Nonnull
    private VoxelShape makeTableShape(Direction facing)
    {
        return Shapes.or(
                cuboidWithRotation(facing, 0, 14, 0, 16, 16, 16),
                cuboidWithRotation(facing, 2, 0, 2, 4, 6, 4),
                cuboidWithRotation(facing, 2, 0, 12, 4, 6, 14),
                cuboidWithRotation(facing, 12, 0, 2, 14, 6, 4),
                cuboidWithRotation(facing, 12, 0, 12, 14, 6, 14),
                cuboidWithRotation(facing, 2, 6, 2, 4, 14, 14),
                cuboidWithRotation(facing, 12, 6, 2, 14, 14, 14),
                cuboidWithRotation(facing, 4, 10, 10, 12, 14, 12)
        );
    }

    private final EnumMap<Direction, VoxelShape> cache = new EnumMap<>(Direction.class);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
    {
        Direction facing = state.getValue(FACING);
        return cache.computeIfAbsent(facing, this::makeTableShape);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult p_60508_)
    {
        return use(level, pos, player);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        return use(level, pos, player);
    }

    public InteractionResult use(Level level, BlockPos pos, Player player)
    {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        player.openMenu(new SimpleMenuProvider(
                (id, playerInv, p) -> new SewingTableMenu(id, playerInv, ContainerLevelAccess.create(level, pos)),
                Component.translatable("container.sewingkit.sewing_station")
        ));

        return InteractionResult.SUCCESS;
    }
}
