package org.mtr.mod.item;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ItemExtension;
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
		if (clickCondition(context)) {
			final ItemStack stack = context.getStack();
			final CompoundTag compoundTag = stack.getOrCreateTag();

			if (compoundTag.contains(TAG_POS)) {
				if (!context.getWorld().isClient()) {
					final BlockPos posEnd = BlockPos.fromLong(compoundTag.getLong(TAG_POS));
					onEndClick(context, posEnd, compoundTag);
				}
				compoundTag.remove(TAG_POS);
			} else {
				if (!context.getWorld().isClient()) {
					onStartClick(context, compoundTag);
				}
				compoundTag.putLong(TAG_POS, context.getBlockPos().asLong());
			}
			org.mtr.mapping.mapper.ItemStackNbtHelper.saveTag(stack, compoundTag);
			return ActionResult.SUCCESS;
		}
		return context.getWorld().isClient() ? super.useOnBlock2(context) : ActionResult.FAIL;
	}

	@Override
	public void addTooltips(ItemStack stack, @Nullable World world, List<MutableText> tooltip, TooltipContext options) {
		final CompoundTag compoundTag = org.mtr.mapping.mapper.ItemStackNbtHelper.getTag(stack);
		final long posLong = compoundTag.getLong(TAG_POS);
		if (posLong != 0) {
			tooltip.add(TranslationProvider.TOOLTIP_MTR_SELECTED_BLOCK.getMutableText(BlockPos.fromLong(posLong).toShortString()).formatted(TextFormatting.GOLD));
		}
	}

	protected abstract void onStartClick(ItemUsageContext context, CompoundTag compoundTag);

	protected abstract void onEndClick(ItemUsageContext context, BlockPos posEnd, CompoundTag compoundTag);

	protected abstract boolean clickCondition(ItemUsageContext context);
}
