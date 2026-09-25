package org.mtr.mod.item;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mod.Blocks;
import org.mtr.mod.block.BlockEscalatorBase;
import org.mtr.mod.block.BlockEscalatorSide;
import org.mtr.mod.block.BlockEscalatorStep;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nonnull;

public class ItemEscalator extends ItemExtension implements IBlock {

	private static final int MAX_CHAIN_LENGTH = 64;

	public ItemEscalator(ItemSettings itemSettings) {
		super(itemSettings);
	}

	@Nonnull
	@Override
	public ActionResult useOnBlock2(ItemUsageContext context) {
		if (ItemPSDAPGBase.blocksNotReplaceable(context, 2, 2, null)) {
			return ActionResult.FAIL;
		}

		final World world = context.getWorld();
		Direction playerFacing = context.getPlayerFacing();
		BlockPos pos1 = context.getBlockPos().offset(context.getSide());
		BlockPos pos2 = pos1.offset(playerFacing.rotateYClockwise());

		final BlockState frontState = world.getBlockState(pos1.offset(playerFacing));
		if (frontState.getBlock().data instanceof BlockEscalatorBase) {
			if (IBlock.getStatePropertySafe(frontState, BlockEscalatorBase.FACING) == playerFacing.getOpposite()) {
				playerFacing = playerFacing.getOpposite();
				final BlockPos pos3 = pos1;
				pos1 = pos2;
				pos2 = pos3;
			}
		}

		final BlockState stepState = Blocks.ESCALATOR_STEP.get().getDefaultState().with(new Property<>(BlockEscalatorStep.FACING.data), playerFacing.data);
		world.setBlockState(pos1, stepState.with(new Property<>(SIDE.data), EnumSide.LEFT));
		world.setBlockState(pos2, stepState.with(new Property<>(SIDE.data), EnumSide.RIGHT));

		final BlockState sideState = Blocks.ESCALATOR_SIDE.get().getDefaultState().with(new Property<>(BlockEscalatorSide.FACING.data), playerFacing.data);
		world.setBlockState(pos1.up(), sideState.with(new Property<>(SIDE.data), EnumSide.LEFT));
		world.setBlockState(pos2.up(), sideState.with(new Property<>(SIDE.data), EnumSide.RIGHT));

		// Walk the whole escalator run: from the newly placed segment forward, then reverse.
		refreshEscalatorChain(world, pos1, playerFacing);
		refreshEscalatorChain(world, pos2, playerFacing);

		context.getStack().decrement(1);
		return ActionResult.SUCCESS;
	}

	/**
	 * Refresh orientation starting at {@code start}, along {@code facing}, then the opposite way.
	 * Covers slope / landing / transition variants for the entire connected run.
	 */
	private static void refreshEscalatorChain(World world, BlockPos start, Direction facing) {
		refreshAlong(world, start, facing);
		refreshAlong(world, start, facing.getOpposite());
	}

	private static void refreshAlong(World world, BlockPos start, Direction direction) {
		BlockPos pos = start;
		for (int i = 0; i < MAX_CHAIN_LENGTH; i++) {
			if (!refreshOrientationAt(world, pos) && !refreshOrientationAt(world, pos.up()) && !refreshOrientationAt(world, pos.down())) {
				// No escalator at this column (or adjacent vertical) — end of run.
				if (i > 0) {
					break;
				}
			}
			// Also refresh the vertical pair at this column.
			refreshOrientationAt(world, pos);
			refreshOrientationAt(world, pos.up());
			refreshOrientationAt(world, pos.down());

			final BlockPos ahead = pos.offset(direction);
			final BlockPos aheadUp = ahead.up();
			final BlockPos aheadDown = ahead.down();
			if (isEscalator(world, ahead)) {
				pos = ahead;
			} else if (isEscalator(world, aheadUp)) {
				pos = aheadUp;
			} else if (isEscalator(world, aheadDown)) {
				pos = aheadDown;
			} else {
				break;
			}
		}
	}

	private static boolean isEscalator(World world, BlockPos pos) {
		return world.getBlockState(pos).getBlock().data instanceof BlockEscalatorBase;
	}

	private static boolean refreshOrientationAt(World world, BlockPos pos) {
		final BlockState state = world.getBlockState(pos);
		if (!(state.getBlock().data instanceof BlockEscalatorBase)) {
			return false;
		}
		final BlockEscalatorBase.EnumEscalatorOrientation orientation = BlockEscalatorBase.computeOrientation(BlockView.cast(world), pos, state);
		world.setBlockState(pos, state.with(new Property<>(BlockEscalatorBase.ORIENTATION.data), orientation), 3);
		return true;
	}
}
