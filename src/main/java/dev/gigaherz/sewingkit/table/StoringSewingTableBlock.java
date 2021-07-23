package dev.gigaherz.sewingkit.table;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class StoringSewingTableBlock extends Block implements EntityBlock
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public StoringSewingTableBlock(Properties properties)
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
                cuboidWithRotation(facing, 11, 6, 1, 15, 14, 15),
                cuboidWithRotation(facing, 12, 0, 2, 14, 6, 14),
                cuboidWithRotation(facing, 1, 6, 1, 5, 14, 15),
                cuboidWithRotation(facing, 2, 0, 2, 4, 6, 14)
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
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit)
    {
        BlockEntity te = worldIn.getBlockEntity(pos);
        if (!(te instanceof StoringSewingTableTileEntity table))
            return InteractionResult.FAIL;

        if (worldIn.isClientSide)
            return InteractionResult.SUCCESS;

        player.openMenu(new SimpleMenuProvider(
                (id, playerInv, p) -> new SewingTableContainer(id, playerInv, ContainerLevelAccess.create(worldIn, pos), table),
                new TranslatableComponent("container.sewingkit.sewing_station")
        ));

        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new StoringSewingTableTileEntity(pos, state);
    }
}
