package org.mtr.mod.packet;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.mod.Init;

public final class PacketOpenBlockEntityScreen extends PacketHandler {

	private final BlockPos blockPos;

	public PacketOpenBlockEntityScreen(PacketBufferReceiver packetBufferReceiver) {
		blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
	}

	public PacketOpenBlockEntityScreen(BlockPos blockPos) {
		this.blockPos = blockPos;
	}

	@Override
	public void write(PacketBufferSender packetBufferSender) {
		packetBufferSender.writeLong(blockPos.asLong());
	}

	@Override
	public void runClient() {
		ClientPacketHelper.openBlockEntityScreen(blockPos);
	}

	/**
	 * Open on the client immediately; still send the packet from the server as backup.
	 * Referencing {@link ClientPacketHelper} only from the client branch keeps dedicated servers safe.
	 */
	public static void sendOrOpen(World world, PlayerEntity player, BlockPos blockPos) {
		if (world.isClient()) {
			ClientPacketHelper.openBlockEntityScreen(blockPos);
		} else {
			Init.REGISTRY.sendPacketToClient(ServerPlayerEntity.cast(player), new PacketOpenBlockEntityScreen(blockPos));
		}
	}
}
