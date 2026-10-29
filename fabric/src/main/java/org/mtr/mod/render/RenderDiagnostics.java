package org.mtr.mod.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.mtr.mapping.holder.Camera;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.render.RenderDiagnosticsFlag;
import org.mtr.mod.Init;
import org.mtr.mod.client.MinecraftClientData;

public final class RenderDiagnostics {

	private static final Logger LOGGER = LogManager.getLogger("MTR-RenderDiag");
	private static final long INTERVAL_MS = 250L;
	private static long lastFrameLogMillis;
	private static double lastEventCamX = Double.NaN;
	private static double lastEventCamY = Double.NaN;
	private static double lastEventCamZ = Double.NaN;
	private static Path logFile;
	private static int lastRailsTotal = -1;
	private static int lastRailsVisible = -1;
	private static int lastVehicles = -1;
	private static int lastLifts = -1;
	private static int lastQueuedDraws = -1;

	private RenderDiagnostics() {
	}

	public static boolean isEnabled() {
		return RenderDiagnosticsFlag.isEnabled();
	}

	public static void setEnabled(boolean enabled) {
		RenderDiagnosticsFlag.setEnabled(enabled);
		final String state = enabled ? "ON" : "OFF";
		LOGGER.info("[MTR-RenderDiag] diagnostics {}", state);
		appendFile("diagnostics " + state);
		final ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
		if (player != null) {
			player.sendMessage(new Text(TextHelper.literal("[MTR] Render diagnostics " + state + " (logs/mtr-render-diag.log)").data), false);
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
		lastVehicles = vehicles;
		lastLifts = lifts;
	}

	public static void noteQueuedDraws(int count) {
		lastQueuedDraws = count;
	}

	public static void logFrame(Vector3d eventCameraOffset, Vector3d cameraShakeOffset) {
		if (isEnabled()) {
			final long now = System.currentTimeMillis();
			if (now - lastFrameLogMillis >= INTERVAL_MS) {
				lastFrameLogMillis = now;
				final MinecraftClient minecraftClient = MinecraftClient.getInstance();
				final ClientPlayerEntity player = minecraftClient.getPlayerMapped();
				if (player != null) {
					final Camera camera = minecraftClient.getGameRendererMapped().getCamera();
					final Vector3d liveCam = camera.getPos();
					final Vector3d feet = player.getPos();
					final Vector3d eye = player.getCameraPosVec2(1.0F);
					final double dEventLiveX = eventCameraOffset.getXMapped() - liveCam.getXMapped();
					final double dEventLiveY = eventCameraOffset.getYMapped() - liveCam.getYMapped();
					final double dEventLiveZ = eventCameraOffset.getZMapped() - liveCam.getZMapped();
					final double dEventLive = Math.sqrt(dEventLiveX * dEventLiveX + dEventLiveY * dEventLiveY + dEventLiveZ * dEventLiveZ);
					final double dEyeLiveX = eye.getXMapped() - liveCam.getXMapped();
					final double dEyeLiveY = eye.getYMapped() - liveCam.getYMapped();
					final double dEyeLiveZ = eye.getZMapped() - liveCam.getZMapped();
					final double dEyeLive = Math.sqrt(dEyeLiveX * dEyeLiveX + dEyeLiveY * dEyeLiveY + dEyeLiveZ * dEyeLiveZ);
					double eventDelta = Double.NaN;
					if (!Double.isNaN(lastEventCamX)) {
						final double dx = eventCameraOffset.getXMapped() - lastEventCamX;
						final double dy = eventCameraOffset.getYMapped() - lastEventCamY;
						final double dz = eventCameraOffset.getZMapped() - lastEventCamZ;
						eventDelta = Math.sqrt(dx * dx + dy * dy + dz * dz);
					}

					lastEventCamX = eventCameraOffset.getXMapped();
					lastEventCamY = eventCameraOffset.getYMapped();
					lastEventCamZ = eventCameraOffset.getZMapped();
					final LocalPlayer raw = (LocalPlayer) player.data;
					final boolean sprinting = raw.isSprinting();
					final boolean speedEffect = raw.hasEffect(MobEffects.SPEED);
					final Vec3 vel = raw.getDeltaMovement();
					final double speed = Math.sqrt(vel.x * vel.x + vel.y * vel.y + vel.z * vel.z);
					final CameraType cameraType = ((Minecraft) minecraftClient.data).options.getCameraType();
					final float optionsFov = ((Integer) ((Minecraft) minecraftClient.data).options.fov().get()).intValue();
					final float partialTick = ((Minecraft) minecraftClient.data).getDeltaTracker().getGameTimeDeltaPartialTick(true);
					final float dynamicFov = ((Minecraft) minecraftClient.data).gameRenderer.getFov(((Minecraft) minecraftClient.data).gameRenderer.getMainCamera(), partialTick, true);
					final double shakeLen = Math.sqrt(
							cameraShakeOffset.getXMapped() * cameraShakeOffset.getXMapped()
									+ cameraShakeOffset.getYMapped() * cameraShakeOffset.getYMapped()
									+ cameraShakeOffset.getZMapped() * cameraShakeOffset.getZMapped()
					);
					if (lastRailsTotal < 0) {
						final MinecraftClientData data = MinecraftClientData.getInstance();
						int total = 0;
						int visible = 0;
						final ObjectIterator var43 = data.railWrapperList.values().iterator();
						while (var43.hasNext()) {
							final MinecraftClientData.RailWrapper wrapper = (MinecraftClientData.RailWrapper) var43.next();
							total++;
							if (wrapper.shouldRender) {
								visible++;
							}
						}
						lastRailsTotal = total;
						lastRailsVisible = visible;
						lastVehicles = data.vehicles.size();
						lastLifts = data.lifts.size();
					}

					final String line = String.format(
							Locale.ROOT,
							"cam event=(%.4f,%.4f,%.4f) live=(%.4f,%.4f,%.4f) eye=(%.4f,%.4f,%.4f) feet=(%.4f,%.4f,%.4f) | d(event-live)=%.5f d(eye-live)=%.5f eventDelta=%.5f shakeLen=%.5f | yaw=%.2f pitch=%.2f persp=%s third=%s | fov options=%.3f dynamic=%.3f shaderDyn=%.3f | sprint=%s speedFx=%s speed=%.4f shadowPass=%s | rails %d/%d vehicles=%d lifts=%d queued=%d",
							eventCameraOffset.getXMapped(),
							eventCameraOffset.getYMapped(),
							eventCameraOffset.getZMapped(),
							liveCam.getXMapped(),
							liveCam.getYMapped(),
							liveCam.getZMapped(),
							eye.getXMapped(),
							eye.getYMapped(),
							eye.getZMapped(),
							feet.getXMapped(),
							feet.getYMapped(),
							feet.getZMapped(),
							dEventLive,
							dEyeLive,
							eventDelta,
							shakeLen,
							camera.getYaw(),
							camera.getPitch(),
							cameraType,
							camera.isThirdPerson(),
							optionsFov,
							dynamicFov,
							RenderDiagnosticsFlag.lastDynamicFov,
							sprinting,
							speedEffect,
							speed,
							OptimizedRenderer.renderingShadows(),
							lastRailsVisible,
							lastRailsTotal,
							lastVehicles,
							lastLifts,
							lastQueuedDraws
					);
					LOGGER.info("[MTR-RenderDiag] {}", line);
					appendFile(line);
				}
			}
		}
	}

	private static void appendFile(String line) {
		try {
			if (logFile == null) {
				final Path gameDir = ((Minecraft) MinecraftClient.getInstance().data).gameDirectory.toPath();
				final Path logs = gameDir.resolve("logs");
				Files.createDirectories(logs);
				logFile = logs.resolve("mtr-render-diag.log");
			}
			final String stamped = String.format(Locale.ROOT, "%tFT%<tT.%<tL %s%n", System.currentTimeMillis(), line);
			Files.writeString(logFile, stamped, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			Init.LOGGER.warn("[MTR-RenderDiag] failed to write log file", e);
		}
	}
}
