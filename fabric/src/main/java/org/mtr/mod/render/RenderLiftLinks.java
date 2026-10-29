package org.mtr.mod.render;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.mapper.PlayerHelper;
import org.mtr.mod.block.BlockLiftButtons;
import org.mtr.mod.block.BlockLiftPanelBase;
import org.mtr.mod.block.BlockLiftTrackFloor;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.block.TripleHorizontalBlock;
import org.mtr.mod.data.IGui;
import org.mtr.mod.item.ItemLiftButtonsLinkModifier;

public final class RenderLiftLinks implements IGui, DirectionHelper {

	private static final float SEGMENT_LENGTH = 2.0F;

	private RenderLiftLinks() {
	}

	public static void render() {
		final MinecraftClient minecraftClient = MinecraftClient.getInstance();
		final ClientWorld clientWorld = minecraftClient.getWorldMapped();
		final ClientPlayerEntity clientPlayerEntity = minecraftClient.getPlayerMapped();
		if (clientWorld != null && clientPlayerEntity != null && isHoldingLinker(clientPlayerEntity)) {
			final World world = new World(clientWorld.data);
			final BlockPos playerPos = clientPlayerEntity.getBlockPos();
			final int viewChunks = Math.max(4, minecraftClient.getOptionsMapped().getEffectiveRenderDistance());
			final ClientChunkManager chunkManager = clientWorld.getChunkSource();
			final int chunkX = playerPos.getX() >> 4;
			final int chunkZ = playerPos.getZ() >> 4;

			for (int dx = -viewChunks; dx <= viewChunks; dx++) {
				for (int dz = -viewChunks; dz <= viewChunks; dz++) {
					final WorldChunk chunk = chunkManager.getChunkNow(chunkX + dx, chunkZ + dz);
					if (chunk != null && chunk.data != null) {
						for (final Map.Entry<net.minecraft.core.BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
							final BlockEntity raw = entry.getValue();
							if (raw != null) {
								final BlockPos blockPos = new BlockPos(entry.getKey());
								scheduleFromBlockEntity(world, blockPos, raw);
							}
						}
					}
				}
			}
		}
	}

	private static void scheduleFromBlockEntity(World world, BlockPos blockPos, BlockEntity raw) {
		if (raw instanceof BlockLiftButtons.BlockEntity buttons) {
			final BlockState blockState = world.getBlockState(blockPos);
			if (blockState.getBlock().data instanceof BlockLiftButtons) {
				final Direction facing = IBlock.getStatePropertySafe(blockState, FACING);
				buttons.forEachTrackPosition(
						trackPosition -> scheduleLink(world, blockPos, facing, facing.getOffsetX() / 2.0F, 0.5F, facing.getOffsetZ() / 2.0F, trackPosition, -1)
				);
			}
		} else if (raw instanceof BlockLiftPanelBase.BlockEntityBase panel) {
			final BlockPos trackPosition = panel.getTrackPosition();
			if (trackPosition != null) {
				final BlockState blockState = world.getBlockState(blockPos);
				if (blockState.getBlock().data instanceof BlockLiftPanelBase) {
					final Direction facing = IBlock.getStatePropertySafe(blockState, FACING);
					final float side = blockState.contains(new Property<>(TripleHorizontalBlock.CENTER.data)) ? 1.0F : 0.5F;
					scheduleLink(
							world,
							blockPos,
							facing,
							facing.getOffsetX() / 2.0F + facing.rotateYClockwise().getOffsetX() * side,
							0.5F,
							facing.getOffsetZ() / 2.0F + facing.rotateYClockwise().getOffsetZ() * side,
							trackPosition,
							-1
					);
				}
			}
		} else {
			invokeForEachTrackPosition(raw, trackPosition -> {
				final BlockState blockState = world.getBlockState(blockPos);
				final Direction facing = IBlock.getStatePropertySafe(blockState, FACING);
				scheduleLink(world, blockPos, facing, facing.getOffsetX() / 2.0F, 0.5F, facing.getOffsetZ() / 2.0F, trackPosition, -16711936);
			});
		}
	}

	private static void invokeForEachTrackPosition(Object raw, Consumer<BlockPos> consumer) {
		try {
			Method method = null;
			for (Class<?> clazz = raw.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
				try {
					method = clazz.getDeclaredMethod("forEachTrackPosition", Consumer.class);
					break;
				} catch (NoSuchMethodException ignored) {
				}
			}
			if (method == null) {
				return;
			}
			method.setAccessible(true);
			method.invoke(raw, consumer);
		} catch (Throwable ignored) {
		}
	}

	private static void scheduleLink(World world, BlockPos blockPos, Direction facing, float localX, float localY, float localZ, BlockPos trackPosition, int color) {
		if (world.getBlockState(trackPosition).getBlock().data instanceof BlockLiftTrackFloor) {
			final Direction trackFacing = IBlock.getStatePropertySafe(world, trackPosition, FACING);
			final double x1 = blockPos.getX() + 0.5 + localX;
			final double y1 = blockPos.getY() + localY;
			final double z1 = blockPos.getZ() + 0.5 + localZ;
			final double x2 = trackPosition.getX() + 0.5 + trackFacing.getOffsetX() / 2.0F;
			final double y2 = trackPosition.getY() + 0.5;
			final double z2 = trackPosition.getZ() + 0.5 + trackFacing.getOffsetZ() / 2.0F;
			scheduleSegmentedLine(x1, y1, z1, x2, y2, z2, color);
		}
	}

	public static void scheduleSegmentedLine(double x1, double y1, double z1, double x2, double y2, double z2, int color) {
		final double dx = x2 - x1;
		final double dy = y2 - y1;
		final double dz = z2 - z1;
		final double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (!(length < 0.001)) {
			final int segments = Math.max(1, (int) Math.ceil(length / 2.0));
			MainRenderer.scheduleRender(QueuedRenderLayer.LINES, (graphicsHolder, offset) -> {
				final double ox = offset.getXMapped();
				final double oy = offset.getYMapped();
				final double oz = offset.getZMapped();
				for (int i = 0; i < segments; i++) {
					final double t0 = (double) i / segments;
					final double t1 = (double) (i + 1) / segments;
					graphicsHolder.drawLineInWorld(
							(float) (x1 + dx * t0 - ox),
							(float) (y1 + dy * t0 - oy),
							(float) (z1 + dz * t0 - oz),
							(float) (x1 + dx * t1 - ox),
							(float) (y1 + dy * t1 - oy),
							(float) (z1 + dz * t1 - oz),
							color
					);
				}
			});
		}
	}

	public static boolean isHoldingLinker(ClientPlayerEntity clientPlayerEntity) {
		return PlayerHelper.isHolding(PlayerEntity.cast(clientPlayerEntity), item -> {
			if (item.data instanceof ItemLiftButtonsLinkModifier) {
				return true;
			} else {
				final Object blockData = Block.getBlockFromItem(item).data;
				if (!(blockData instanceof BlockLiftButtons) && !(blockData instanceof BlockLiftPanelBase)) {
					final String name = item.data.getClass().getName();
					return name.contains("YteLiftButtonsLinker") || name.contains("YteGroupLiftButtonsLinker");
				} else {
					return true;
				}
			}
		});
	}
}
