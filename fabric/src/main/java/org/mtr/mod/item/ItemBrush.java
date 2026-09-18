package org.mtr.mod.item;

import org.mtr.core.data.Rail;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mod.block.BlockNode;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.packet.PacketUpdateLastRailStyles;

import javax.annotation.Nonnull;

public class ItemBrush extends ItemExtension {

	public ItemBrush(ItemSettings itemSettings) {
		super(itemSettings.maxCount(1));
	}

	/**
	 * Shift-click on a rail node applies last styles (cannot be done in {@link Block#onUse}).
	 * Non-shift must {@link ActionResult#PASS} so {@code BlockNode.onUse2} can open the rail editor.
	 */
	@Nonnull
	@Override
	public ActionResult useOnBlock2(ItemUsageContext context) {
		final World world = context.getWorld();
		final PlayerEntity playerEntity = context.getPlayer();
		if (world.isClient() && playerEntity != null && playerEntity.isSneaking() && world.getBlockState(context.getBlockPos()).getBlock().data instanceof BlockNode) {
			final ObjectObjectImmutablePair<Rail, BlockPos> railAndBlockPos = MinecraftClientData.getInstance().getFacingRailAndBlockPos(false);
			if (railAndBlockPos != null) {
				return PacketUpdateLastRailStyles.CLIENT_CACHE.canApplyStylesToRail(playerEntity.getUuid(), railAndBlockPos.left(), true) ? ActionResult.SUCCESS : ActionResult.FAIL;
			}
		}
		return ActionResult.getPassMapped();
	}
}
