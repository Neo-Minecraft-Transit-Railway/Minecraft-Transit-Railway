package org.mtr.mod.packet;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public final class PacketOpenLiftCustomizationScreen extends PacketHandler {

	private final BlockPos blockPos;
	private final String liftJson;

	public PacketOpenLiftCustomizationScreen(PacketBufferReceiver packetBufferReceiver) {
		blockPos = BlockPos.fromLong(packetBufferReceiver.readLong());
		liftJson = packetBufferReceiver.readString();
	}

	public PacketOpenLiftCustomizationScreen(BlockPos blockPos, String liftJson) {
		this.blockPos = blockPos;
		this.liftJson = liftJson;
	}

	@Override
	public void write(PacketBufferSender packetBufferSender) {
		packetBufferSender.writeLong(blockPos.asLong());
		packetBufferSender.writeString(liftJson);
	}

	@Override
	public void runClient() {
		ClientPacketHelper.openLiftCustomizationScreen(blockPos, liftJson);
	}
}
