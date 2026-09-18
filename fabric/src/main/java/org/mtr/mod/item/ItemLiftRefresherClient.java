package org.mtr.mod.item;

import org.mtr.core.data.Lift;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;
import org.mtr.mod.Init;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.screen.LiftCustomizationScreen;

/**
 * Client-only GUI open for {@link ItemLiftRefresher}. Referenced only from the
 * {@code world.isClient()} branch so dedicated servers never load this class.
 */
public final class ItemLiftRefresherClient {

	private ItemLiftRefresherClient() {
	}

	public static void open(Lift lift) {
		final MinecraftClient minecraftClient = MinecraftClient.getInstance();
		final Runnable open = () -> {
			try {
				final MinecraftClientData minecraftClientData = MinecraftClientData.getInstance();
				minecraftClientData.lifts.removeIf(existing -> existing.getId() == lift.getId() || existing.overlappingFloors(lift));
				minecraftClientData.lifts.add(lift);
				minecraftClientData.sync();
				Init.LOGGER.info("Opening lift customization screen for lift {}", lift.getId());
				minecraftClient.openScreen(new Screen(new LiftCustomizationScreen(lift)));
			} catch (Exception e) {
				Init.LOGGER.error("Failed to open lift customization screen", e);
			}
		};
		if (minecraftClient.isSameThread()) {
			open.run();
		} else {
			minecraftClient.execute(open);
		}
	}
}
