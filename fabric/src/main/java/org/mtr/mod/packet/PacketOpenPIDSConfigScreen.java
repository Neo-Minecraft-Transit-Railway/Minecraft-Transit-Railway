package org.mtr.mod.packet;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.mod.Init;

public final class PacketOpenPIDSConfigScreen extends PacketHandler {

	private final BlockPos blockPos;
	private final int maxArrivals;

	public PacketOpenPIDSConfigScreen(PacketBufferReceiver packetBufferReceiver) {
		blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
		maxArrivals = packetBufferReceiver.readInt();
	}

	public PacketOpenPIDSConfigScreen(BlockPos blockPos, int maxArrivals) {
		this.blockPos = blockPos;
		this.maxArrivals = maxArrivals;
	}

	@Override
	public void write(PacketBufferSender packetBufferSender) {
		packetBufferSender.writeLong(blockPos.asLong());
		packetBufferSender.writeInt(maxArrivals);
	}

	@Override
	public void runClient() {
		ClientPacketHelper.openPIDSConfigScreen(blockPos, maxArrivals);
	}

	public static void sendOrOpen(World world, PlayerEntity player, BlockPos blockPos, int maxArrivals) {
		if (world.isClient()) {
			ClientPacketHelper.openPIDSConfigScreen(blockPos, maxArrivals);
		} else {
			Init.REGISTRY.sendPacketToClient(ServerPlayerEntity.cast(player), new PacketOpenPIDSConfigScreen(blockPos, maxArrivals));
		}
	}
}
