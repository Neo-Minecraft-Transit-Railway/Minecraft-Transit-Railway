package org.mtr.mod.packet;

import org.mtr.core.data.Lift;
import org.mtr.core.data.TransportMode;
import org.mtr.core.serializer.JsonReader;
import org.mtr.core.tool.Utilities;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mod.Init;
import org.mtr.mod.block.*;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.screen.*;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Since packets are registered serverside, they will fail if any client classes are used (e.g. screens).
 */
public final class ClientPacketHelper {

	public static void openBlockEntityScreen(BlockPos blockPos) {
		getBlockEntity(blockPos, blockEntity -> {
			if (blockEntity.data instanceof BlockTrainAnnouncer.BlockEntity) {
				openScreen(new TrainAnnouncerScreen(blockPos, (BlockTrainAnnouncer.BlockEntity) blockEntity.data), screenExtension -> screenExtension instanceof TrainAnnouncerScreen);
			} else if (blockEntity.data instanceof BlockTrainScheduleSensor.BlockEntity) {
				openScreen(new TrainScheduleSensorScreen(blockPos, (BlockTrainScheduleSensor.BlockEntity) blockEntity.data), screenExtension -> screenExtension instanceof TrainScheduleSensorScreen);
			} else if (blockEntity.data instanceof BlockTrainSensorBase.BlockEntityBase) {
				openScreen(new TrainBasicSensorScreen(blockPos), screenExtension -> screenExtension instanceof TrainBasicSensorScreen);
			} else if (blockEntity.data instanceof BlockRailwaySign.BlockEntity || blockEntity.data instanceof BlockRouteSignBase.BlockEntityBase) {
				openScreen(new RailwaySignScreen(blockPos), screenExtension -> screenExtension instanceof RailwaySignScreen);
			} else if (blockEntity.data instanceof BlockLiftTrackFloor.BlockEntity) {
				openScreen(new LiftTrackFloorScreen(blockPos, (BlockLiftTrackFloor.BlockEntity) blockEntity.data), screenExtension -> screenExtension instanceof LiftTrackFloorScreen);
			} else if (blockEntity.data instanceof BlockSignalBase.BlockEntityBase) {
				openScreen(new SignalColorScreen(blockPos, (BlockSignalBase.BlockEntityBase) blockEntity.data), screenExtension -> screenExtension instanceof SignalColorScreen);
			} else if (blockEntity.data instanceof BlockDriverKeyDispenser.BlockEntity) {
				openScreen(new DriverKeyDispenserScreen(blockPos, (BlockDriverKeyDispenser.BlockEntity) blockEntity.data), screenExtension -> screenExtension instanceof DriverKeyDispenserScreen);
			} else if (blockEntity.data instanceof BlockEyeCandy.BlockEntity) {
				openScreen(new EyeCandyScreen(blockPos, (BlockEyeCandy.BlockEntity) blockEntity.data), screenExtension -> screenExtension instanceof EyeCandyScreen);
			}
		});
	}

	public static void openDashboardScreen(TransportMode transportMode, PacketOpenDashboardScreen.ScreenType screenType, long id) {
		switch (screenType) {
			case STATION:
				openScreen(new EditStationScreen(MinecraftClientData.getDashboardInstance().stationIdMap.get(id), new DashboardScreen(transportMode)), screenExtension -> screenExtension instanceof EditStationScreen);
				break;
			case DEPOT:
				openScreen(new EditDepotScreen(MinecraftClientData.getDashboardInstance().depotIdMap.get(id), transportMode, new DashboardScreen(transportMode)), screenExtension -> screenExtension instanceof EditDepotScreen);
				break;
			case PLATFORM:
				openScreen(new PlatformScreen(MinecraftClientData.getDashboardInstance().platformIdMap.get(id), transportMode, new DashboardScreen(transportMode)), screenExtension -> screenExtension instanceof PlatformScreen);
				break;
			case SIDING:
				openScreen(new SidingScreen(MinecraftClientData.getDashboardInstance().sidingIdMap.get(id), transportMode, new DashboardScreen(transportMode)), screenExtension -> screenExtension instanceof SidingScreen);
				break;
			default:
				openScreen(new DashboardScreen(transportMode), screenExtension -> screenExtension instanceof DashboardScreen);
				break;
		}
	}

	private static BlockPos pendingLiftCustomizationPos;
	private static int pendingLiftCustomizationAttempts;

	public static void openLiftCustomizationScreen(BlockPos blockPos) {
		openLiftCustomizationScreen(blockPos, null);
	}

	public static void openLiftCustomizationScreen(BlockPos blockPos, String liftJson) {
		if (liftJson != null && !liftJson.isEmpty() && tryOpenLiftCustomizationScreenFromJson(liftJson)) {
			pendingLiftCustomizationPos = null;
			return;
		}
		pendingLiftCustomizationPos = blockPos;
		pendingLiftCustomizationAttempts = 0;
		tryOpenPendingLiftCustomizationScreen();
	}

	/**
	 * Called each client tick so lift data synced after GENERATE_BY_LIFT can be found.
	 */
	public static void tick() {
		if (pendingLiftCustomizationPos != null) {
			tryOpenPendingLiftCustomizationScreen();
		}
	}

	private static boolean tryOpenLiftCustomizationScreenFromJson(String liftJson) {
		try {
			final MinecraftClientData minecraftClientData = MinecraftClientData.getInstance();
			final Lift lift = new Lift(new JsonReader(Utilities.parseJson(liftJson)), minecraftClientData);
			minecraftClientData.lifts.removeIf(existing -> existing.getId() == lift.getId() || existing.overlappingFloors(lift));
			minecraftClientData.lifts.add(lift);
			minecraftClientData.sync();
			openScreen(new LiftCustomizationScreen(lift), screenExtension -> screenExtension instanceof LiftCustomizationScreen);
			return true;
		} catch (Exception e) {
			Init.LOGGER.error("Failed to open lift customization screen from packet", e);
			return false;
		}
	}

	private static void tryOpenPendingLiftCustomizationScreen() {
		final BlockPos blockPos = pendingLiftCustomizationPos;
		if (blockPos == null) {
			return;
		}
		for (final Lift lift : MinecraftClientData.getInstance().lifts) {
			if (lift.getFloorIndex(Init.blockPosToPosition(blockPos)) >= 0) {
				pendingLiftCustomizationPos = null;
				openScreen(new LiftCustomizationScreen(lift), screenExtension -> screenExtension instanceof LiftCustomizationScreen);
				return;
			}
		}
		if (++pendingLiftCustomizationAttempts > 200) {
			pendingLiftCustomizationPos = null;
		}
	}

	public static void openPIDSConfigScreen(BlockPos blockPos, int maxArrivals) {
		getBlockEntity(blockPos, blockEntity -> {
			if (blockEntity.data instanceof BlockPIDSBase.BlockEntityBase) {
				openScreen(new PIDSConfigScreen(blockPos, maxArrivals), screenExtension -> screenExtension instanceof PIDSConfigScreen);
			}
		});
	}

	public static void openRailShapeModifierScreen(String railId) {
		openScreen(new RailModifierScreen(railId), screenExtension -> screenExtension instanceof RailModifierScreen);
	}

	public static void openTicketMachineScreen(int balance) {
		openScreen(new TicketMachineScreen(balance), screenExtension -> screenExtension instanceof TicketMachineScreen);
	}

	private static void openScreen(ScreenExtension screenExtension, Predicate<ScreenExtension> isInstance) {
		final MinecraftClient minecraftClient = MinecraftClient.getInstance();
		final Runnable open = () -> {
			final Screen screen = minecraftClient.getCurrentScreenMapped();
			// Always replace vanilla/non-MTR screens. Skip only if the same MTR screen is already open.
			if (screen != null && screen.data instanceof ScreenExtension && isInstance.test((ScreenExtension) screen.data)) {
				return;
			}
			minecraftClient.openScreen(new Screen(screenExtension));
		};
		if (minecraftClient.isSameThread()) {
			open.run();
		} else {
			minecraftClient.execute(open);
		}
	}

	private static void getBlockEntity(BlockPos blockPos, Consumer<BlockEntity> consumer) {
		final ClientWorld clientWorld = MinecraftClient.getInstance().getWorldMapped();
		if (clientWorld != null) {
			final BlockEntity blockEntity = clientWorld.getBlockEntity(blockPos);
			if (blockEntity != null) {
				consumer.accept(blockEntity);
			}
		}
	}
}
