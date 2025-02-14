package net.bmjo.brewery.block.multiblockparts;

import net.bmjo.brewery.block.property.Liquid;
import net.bmjo.brewery.entity.BrewKettleEntity;
import net.bmjo.brewery.registry.BlockStateRegistry;
import net.bmjo.brewery.registry.ObjectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WaterBasinBlock extends BrewKettleBlock implements EntityBlock {
    public static final EnumProperty<Liquid> LIQUID;

    public WaterBasinBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(LIQUID, Liquid.EMPTY));
    }

    @Override
    public @NotNull InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        ItemStack itemStack = player.getItemInHand(interactionHand);
        if (level.getBlockEntity(blockPos) instanceof BrewKettleEntity brewKettleEntity) {
            if (itemStack.isEmpty()) { //EMPTY
                ItemStack returnStack = brewKettleEntity.removeIngredient();
                if (returnStack != null) {
                    player.addItem(returnStack);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.CONSUME;
            }
            if (itemStack.getItem() == Items.WATER_BUCKET) { //WATER_BUCKET
                if (blockState.getValue(LIQUID) != Liquid.FILLED || blockState.getValue(LIQUID) != Liquid.OVERFLOWING) {
                    level.setBlock(blockPos, blockState.setValue(LIQUID, Liquid.FILLED), 3);
                    level.playSound(null, blockPos, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
                    if (!player.isCreative()) {
                        player.setItemInHand(interactionHand, new ItemStack(Items.BUCKET));
                    }
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.CONSUME;
            }
            if (itemStack.getItem() == Items.BUCKET) { //BUCKET
                Liquid liquid = blockState.getValue(LIQUID);
                if (liquid == Liquid.FILLED || liquid == Liquid.OVERFLOWING) {
                    level.setBlock(blockPos, blockState.setValue(LIQUID, liquid == Liquid.OVERFLOWING ? Liquid.FILLED : Liquid.EMPTY), 3);
                    level.playSound(null, blockPos, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
                    if (!player.isCreative()) {
                        player.setItemInHand(interactionHand, new ItemStack(Items.WATER_BUCKET));
                    }
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.CONSUME;
            }
            //ITEM
            brewKettleEntity.addIngredient(itemStack);
            return InteractionResult.SUCCESS;
        }
        return super.use(blockState, level, blockPos, player, interactionHand, blockHitResult);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        Level level = blockPlaceContext.getLevel();
        BlockPos mainPos = blockPlaceContext.getClickedPos();
        BlockState blockState = super.getStateForPlacement(blockPlaceContext);
        if (blockState == null) return null;
        Direction facing = blockState.getValue(FACING);
        BlockPos backPos = mainPos.relative(facing.getOpposite());
        boolean rightFree = level.getBlockState(mainPos.relative(facing.getClockWise())).isAir() && level.getBlockState(backPos.relative(facing.getClockWise())).isAir();
        BlockPos sidePos = mainPos.relative(rightFree ? facing.getClockWise() : facing.getCounterClockWise());
        BlockPos diagonalPos = sidePos.relative(facing.getOpposite());
        return canPlace(level, backPos, sidePos, diagonalPos) ? blockState : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos mainPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
        super.setPlacedBy(level, mainPos, blockState, livingEntity, itemStack);
        if (level.isClientSide) return;
        Direction facing = blockState.getValue(FACING);
        BlockPos backPos = mainPos.relative(facing.getOpposite());
        boolean rightFree = level.getBlockState(mainPos.relative(facing.getClockWise())).isAir() && level.getBlockState(backPos.relative(facing.getClockWise())).isAir();
        BlockPos sidePos = mainPos.relative(rightFree ? facing.getClockWise() : facing.getCounterClockWise());
        BlockPos diagonalPos = sidePos.relative(facing.getOpposite());
        if (!canPlace(level, backPos, sidePos, diagonalPos)) return;
        level.setBlock(backPos, ObjectRegistry.TIMER.get().defaultBlockState().setValue(FACING, facing), 3);
        level.setBlock(sidePos, ObjectRegistry.OVEN.get().defaultBlockState().setValue(FACING, facing), 3);
        level.setBlock(diagonalPos, ObjectRegistry.STEAM_WHISTLE.get().defaultBlockState().setValue(FACING, facing), 3);
        //setPos to Entity
    }

    private boolean canPlace(Level level, BlockPos... blockPoses) {
        for (BlockPos blockPos : blockPoses) {
            if (!level.getBlockState(blockPos).isAir()) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BrewKettleEntity(blockPos, blockState);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker<?>) {
                ((BlockEntityTicker<T>) blockEntity).tick(world, pos, state1, blockEntity);
            }
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIQUID);
    }

    static {
        LIQUID = BlockStateRegistry.LIQUID;
    }
}
