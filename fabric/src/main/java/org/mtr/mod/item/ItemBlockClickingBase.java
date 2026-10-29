package org.mtr.mod.item;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mapping.mapper.ItemStackNbtHelper;
import org.mtr.mod.generated.lang.TranslationProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class ItemBlockClickingBase extends ItemExtension {

	public static final String TAG_POS = "pos";

	public ItemBlockClickingBase(ItemSettings itemSettings) {
		super(itemSettings);
	}

	@Nonnull
	@Override
	public ActionResult useOnBlock2(ItemUsageContext context) {
		final ItemStack stack = context.getStack();
		final CompoundTag compoundTag = ItemStackNbtHelper.getOrCreateTag(stack);
		final boolean hasSelection = compoundTag.contains(TAG_POS);
		if (hasSelection) {
			final BlockPos posEnd = BlockPos.fromLong(compoundTag.getLong(TAG_POS));
			if (clickCondition(context)) {
				if (!context.getWorld().isClient()) {
					onEndClick(context, posEnd, compoundTag);
				}
				compoundTag.remove(TAG_POS);
				ItemStackNbtHelper.saveTag(stack, compoundTag);
				return ActionResult.SUCCESS;
			} else {
				compoundTag.remove(TAG_POS);
				compoundTag.remove("transport_mode");
				ItemStackNbtHelper.saveTag(stack, compoundTag);
				return ActionResult.FAIL;
			}
		} else if (clickCondition(context)) {
			if (!context.getWorld().isClient()) {
				onStartClick(context, compoundTag);
			}
			compoundTag.putLong(TAG_POS, context.getBlockPos().asLong());
			ItemStackNbtHelper.saveTag(stack, compoundTag);
			return ActionResult.SUCCESS;
		} else {
			return context.getWorld().isClient() ? super.useOnBlock2(context) : ActionResult.FAIL;
		}
	}

	@Override
	public void addTooltips(ItemStack stack, @Nullable World world, List<MutableText> tooltip, TooltipContext options) {
		final CompoundTag compoundTag = ItemStackNbtHelper.getTag(stack);
		final long posLong = compoundTag.getLong(TAG_POS);
		if (posLong != 0) {
			tooltip.add(TranslationProvider.TOOLTIP_MTR_SELECTED_BLOCK.getMutableText(BlockPos.fromLong(posLong).toShortString()).formatted(TextFormatting.GOLD));
		}
	}

	protected abstract void onStartClick(ItemUsageContext context, CompoundTag compoundTag);

	protected abstract void onEndClick(ItemUsageContext context, BlockPos posEnd, CompoundTag compoundTag);

	protected abstract boolean clickCondition(ItemUsageContext context);
}
