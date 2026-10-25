package org.mtr.mod.render;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.render.RenderDiagnosticsFlag;

/**
 * Optional on-screen / log diagnostics for rail and optimized rendering (toggle with F8).
 */
public final class RenderDiagnostics {

	private static final Logger LOGGER = LogManager.getLogger("MTR-RenderDiag");
	private static int lastRailsTotal;
	private static int lastRailsVisible;

	private RenderDiagnostics() {
	}

	public static boolean isEnabled() {
		return RenderDiagnosticsFlag.isEnabled();
	}

	public static void setEnabled(boolean enabled) {
		RenderDiagnosticsFlag.setEnabled(enabled);
		final String state = enabled ? "ON" : "OFF";
		LOGGER.info("[MTR-RenderDiag] diagnostics {}", state);
		final ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
		if (player != null) {
			player.sendMessage(new Text(TextHelper.literal("[MTR] Render diagnostics " + state).data), false);
		}
	}

	public static void toggle() {
		setEnabled(!isEnabled());
	}

	public static void noteRails(int total, int visible) {
		lastRailsTotal = total;
		lastRailsVisible = visible;
	}

	public static void noteVehiclesAndLifts(int vehicles, int lifts) {
		// kept for parity with the released jar diagnostics hooks
	}

	public static void noteQueuedDraws(int count) {
	}

	public static void logFrame(org.mtr.mapping.holder.Vector3d offset, org.mtr.mapping.holder.Vector3d cameraShakeOffset) {
		if (!isEnabled()) {
			return;
		}
		LOGGER.debug("[MTR-RenderDiag] rails {}/{} offset=({}, {}, {})", lastRailsVisible, lastRailsTotal, offset.getXMapped(), offset.getYMapped(), offset.getZMapped());
	}
}
