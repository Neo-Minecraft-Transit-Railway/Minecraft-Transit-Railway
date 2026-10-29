package org.mtr.mod.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.render.CapturedRenderMatrices;
import org.mtr.mod.client.MinecraftClientData;

public final class RailVanishProbe {
   private static final Logger LOGGER = LogManager.getLogger("MTR-RailVanishProbe");
   private static final float YAW_STEP = 15.0F;
   private static final float PITCH_STEP = 15.0F;
   private static final float YAW_MIN = -180.0F;
   private static final float YAW_MAX = 165.0F;
   private static final float PITCH_MIN = -45.0F;
   private static final float PITCH_MAX = 45.0F;
   private static final int HOLD_FRAMES = 4;
   private static final int MAX_RAILS = 6;
   private static final int VISUAL_THRESHOLD = 80;
   private static boolean running;
   private static boolean finished;
   private static boolean waitingForRails;
   private static float yaw;
   private static float pitch;
   private static int framesAtAngle;
   private static int waitFrames;
   private static Path csvPath;
   private static int sampleCount;
   private static int anomalyCount;
   private static int visualVanishCount;
   private static float savedYaw;
   private static float savedPitch;

   private RailVanishProbe() {
   }

   public static void tickApplyCamera() {
      MinecraftClient client = MinecraftClient.getInstance();
      Path gameDir = client.getRunDirectoryMapped().toPath();
      Path startFile = gameDir.resolve("mtr-rail-vanish-probe.start");
      Path stopFile = gameDir.resolve("mtr-rail-vanish-probe.stop");
      if (Files.exists(stopFile)) {
         try {
            Files.deleteIfExists(stopFile);
         } catch (IOException var5) {
         }

         if (running) {
            finish(client, "stopped");
         }

         return;
      }

      if (!running && Files.exists(startFile)) {
         finished = false;
         start(client, gameDir, startFile);
      }

      if (!running) {
         return;
      }

      if (client.getCurrentScreenMapped() != null) {
         client.openScreen(null);
      }

      ClientPlayerEntity player = client.getPlayerMapped();
      if (player == null) {
         return;
      }

      player.setYRot(yaw);
      player.setXRot(pitch);
      player.setYRotOMapped(yaw);
      player.setXRotOMapped(pitch);
   }

   public static void onWorldRendered() {
      if (!running) {
         return;
      }

      MinecraftClient client = MinecraftClient.getInstance();
      ClientPlayerEntity player = client.getPlayerMapped();
      if (player == null) {
         return;
      }

      if (waitingForRails) {
         int n = MinecraftClientData.getInstance().railWrapperList.size();
         waitFrames++;
         if (n <= 0) {
            if (waitFrames % 40 == 0) {
               LOGGER.info("[MTR-RailVanishProbe] waiting for rails\u2026 ({} frames)", waitFrames);
            }

            if (waitFrames > 1200) {
               finish(client, "timeout-no-rails");
            }
         } else {
            waitingForRails = false;
            framesAtAngle = 0;
            aimAtNearestRail(client, player);
            LOGGER.info("[MTR-RailVanishProbe] rails ready count={} \u2014 starting sweep from yaw={} pitch={}", n, yaw, pitch);
            player.sendMessage(new Text((net.minecraft.network.chat.Component) TextHelper.literal("[MTR] Probe: " + n + " rails, sweeping").data), false);
         }
      } else {
         framesAtAngle++;
         if (framesAtAngle >= 4) {
            sample(client, player);
            sampleCount++;
            framesAtAngle = 0;
            pitch += 15.0F;
            if (pitch > 45.01F) {
               pitch = -45.0F;
               yaw += 15.0F;
               if (yaw > 165.01F) {
                  finish(client, "complete");
               }
            }
         }
      }
   }

   private static void start(MinecraftClient client, Path gameDir, Path startFile) {
      ClientPlayerEntity player = client.getPlayerMapped();
      if (player != null) {
         if (client.getCurrentScreenMapped() != null) {
            client.openScreen(null);
         }

         savedYaw = player.getYRot();
         savedPitch = player.getXRot();
         yaw = -180.0F;
         pitch = -45.0F;
         framesAtAngle = 0;
         waitFrames = 0;
         sampleCount = 0;
         anomalyCount = 0;
         visualVanishCount = 0;
         running = true;
         finished = false;
         waitingForRails = true;

         try {
            Files.createDirectories(gameDir.resolve("logs"));
            csvPath = gameDir.resolve("logs/mtr-rail-vanish-probe.csv");
            Files.writeString(
               csvPath,
               "yaw,pitch,railIdx,rx,ry,rz,lookDot,ndcCapX,ndcCapY,ndcCapZ,inCap,sleeperPx,visualVanish,viewDiff,transLen,capValid\n",
               StandardCharsets.UTF_8
            );
            Files.deleteIfExists(startFile);
            Files.deleteIfExists(gameDir.resolve("mtr-rail-vanish-probe.done"));
            Files.writeString(gameDir.resolve("mtr-rail-vanish-probe.running"), "1", StandardCharsets.UTF_8);
         } catch (IOException var5) {
            LOGGER.error("[MTR-RailVanishProbe] failed to init", var5);
            running = false;
            return;
         }

         LOGGER.info("[MTR-RailVanishProbe] START");
         player.sendMessage(new Text((net.minecraft.network.chat.Component)TextHelper.literal("[MTR] Rail vanish probe START").data), false);
      }
   }

   private static void finish(MinecraftClient client, String reason) {
      running = false;
      finished = true;
      ClientPlayerEntity player = client.getPlayerMapped();
      if (player != null) {
         player.setYRot(savedYaw);
         player.setXRot(savedPitch);
         player.sendMessage(
            new Text(
               (net.minecraft.network.chat.Component)TextHelper.literal(
                     "[MTR] Probe " + reason + " samples=" + sampleCount + " matrixAnom=" + anomalyCount + " visualVanish=" + visualVanishCount
                  )
                  .data
            ),
            false
         );
      }

      Path gameDir = client.getRunDirectoryMapped().toPath();

      try {
         Files.deleteIfExists(gameDir.resolve("mtr-rail-vanish-probe.running"));
         Files.writeString(
            gameDir.resolve("mtr-rail-vanish-probe.done"),
            "reason=" + reason + " samples=" + sampleCount + " anomalies=" + anomalyCount + " visualVanish=" + visualVanishCount + "\n",
            StandardCharsets.UTF_8
         );
      } catch (IOException var5) {
      }

      LOGGER.info("[MTR-RailVanishProbe] DONE reason={} samples={} matrixAnom={} visualVanish={}", reason, sampleCount, anomalyCount, visualVanishCount);
   }

   private static void aimAtNearestRail(MinecraftClient client, ClientPlayerEntity player) {
      Vector3d cam = client.getGameRendererMapped().getCamera().getPos();
      List<float[]> rails = nearestRails(cam, 1);
      if (!rails.isEmpty()) {
         float[] r = rails.get(0);
         float dx = r[0] - (float)cam.getXMapped();
         float dy = r[1] - (float)cam.getYMapped();
         float dz = r[2] - (float)cam.getZMapped();
         float horiz = (float)Math.sqrt(dx * dx + dz * dz);
         yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
         pitch = (float)Math.toDegrees(-Math.atan2(dy, Math.max(horiz, 0.001F)));
         pitch = Math.max(-45.0F, Math.min(45.0F, pitch));
         yaw = Math.round(yaw / 15.0F) * 15.0F;
         pitch = Math.round(pitch / 15.0F) * 15.0F;
         player.setYRot(yaw);
         player.setXRot(pitch);
      }
   }

   private static void sample(MinecraftClient client, ClientPlayerEntity player) {
      Vector3d cam = client.getGameRendererMapped().getCamera().getPos();
      float yawRad = (float)Math.toRadians(yaw);
      float pitchRad = (float)Math.toRadians(pitch);
      Vector3f look = new Vector3f(
            -((float)Math.sin(yawRad)) * (float)Math.cos(pitchRad), -((float)Math.sin(pitchRad)), (float)Math.cos(yawRad) * (float)Math.cos(pitchRad)
         )
         .normalize();
      Matrix4f capView = CapturedRenderMatrices.copyViewOrNull();
      Matrix4f capProj = CapturedRenderMatrices.copyProjectionOrNull();
      Matrix4f sysView = new Matrix4f(RenderSystem.getModelViewMatrix());
      boolean capValid = capView != null && capProj != null;
      float viewDiff = capValid ? frobeniusDiff(capView, sysView) : -1.0F;
      float rotTrans = capValid ? translationLength(capView) : -1.0F;
      int sleeperPx = countSleeperPixels();
      List<float[]> rails = nearestRails(cam, 6);
      if (rails.isEmpty()) {
         appendCsv(String.format(Locale.ROOT, "%.1f,%.1f,-1,0,0,0,0,0,0,0,0,%d,0,%.5f,%.5f,%s\n", yaw, pitch, sleeperPx, viewDiff, rotTrans, capValid));
      } else {
         StringBuilder batch = new StringBuilder();
         int lookingIn = 0;
         int outCap = 0;
         boolean anyLooking = false;

         for (int i = 0; i < rails.size(); i++) {
            float[] r = rails.get(i);
            float dx = r[0] - (float)cam.getXMapped();
            float dy = r[1] - (float)cam.getYMapped();
            float dz = r[2] - (float)cam.getZMapped();
            Vector3f toRail = new Vector3f(dx, dy, dz);
            float len = toRail.length();
            float lookDot = len < 0.001F ? 0.0F : new Vector3f(toRail).normalize().dot(look);
            Matrix4f modelCamRel = new Matrix4f().translation(dx, dy, dz);
            Vector3f ndcCap = capValid ? project(capProj, capView, modelCamRel) : nan();
            boolean inCap = inNdc(ndcCap);
            int visualVanish = 0;
            if (lookDot > 0.85F) {
               anyLooking = true;
               lookingIn++;
               if (!inCap) {
                  outCap++;
                  anomalyCount++;
               }

               if (inCap && sleeperPx < 80) {
                  visualVanish = 1;
                  visualVanishCount++;
                  LOGGER.warn(
                     "[MTR-RailVanishProbe] VISUAL-VANISH yaw={} pitch={} lookDot={} sleeperPx={} ndc={}",
                     yaw,
                     pitch,
                     String.format(Locale.ROOT, "%.3f", lookDot),
                     sleeperPx,
                     fmt(ndcCap)
                  );
               }
            }

            batch.append(
               String.format(
                  Locale.ROOT,
                  "%.1f,%.1f,%d,%.3f,%.3f,%.3f,%.4f,%.4f,%.4f,%.4f,%d,%d,%d,%.5f,%.5f,%s\n",
                  yaw,
                  pitch,
                  i,
                  r[0],
                  r[1],
                  r[2],
                  lookDot,
                  ndcCap.x,
                  ndcCap.y,
                  ndcCap.z,
                  inCap ? 1 : 0,
                  sleeperPx,
                  visualVanish,
                  viewDiff,
                  rotTrans,
                  capValid
               )
            );
         }

         appendCsv(batch.toString());
         if (sampleCount % 5 == 0 || outCap > 0 || anyLooking && sleeperPx < 80) {
            LOGGER.info(
               "[MTR-RailVanishProbe] yaw={} pitch={} hits={} outCap={} sleeperPx={} viewDiff={}",
               yaw,
               pitch,
               lookingIn,
               outCap,
               sleeperPx,
               String.format(Locale.ROOT, "%.5f", viewDiff)
            );
         }
      }
   }

   private static int countSleeperPixels() {
      try {
         int[] viewport = new int[4];
         GL11.glGetIntegerv(2978, viewport);
         int vw = Math.max(1, viewport[2]);
         int vh = Math.max(1, viewport[3]);
         int x0 = vw / 4;
         int y0 = vh / 5;
         int w = Math.max(1, vw / 2);
         int h = Math.max(1, vh * 2 / 5);
         ByteBuffer buf = ByteBuffer.allocateDirect(w * h * 3);
         GL11.glPixelStorei(3333, 1);
         GL11.glReadPixels(x0, y0, w, h, 6407, 5121, buf);
         int count = 0;

         for (int i = 0; i < w * h; i++) {
            int r = buf.get(i * 3) & 255;
            int g = buf.get(i * 3 + 1) & 255;
            int b = buf.get(i * 3 + 2) & 255;
            int max = Math.max(r, Math.max(g, b));
            int min = Math.min(r, Math.min(g, b));
            if (max >= 160 && max - min <= 40) {
               count++;
            }
         }

         return count;
      } catch (Exception e) {
         LOGGER.warn("[MTR-RailVanishProbe] glReadPixels failed: {}", e.toString());
         return -1;
      }
   }

   private static List<float[]> nearestRails(Vector3d cam, int limit) {
      List<float[]> list = new ArrayList<>();
      int wrappers = MinecraftClientData.getInstance().railWrapperList.size();

      try {
         ObjectIterator e = MinecraftClientData.getInstance().railWrapperList.values().iterator();

         while (e.hasNext()) {
            MinecraftClientData.RailWrapper wrapper = (MinecraftClientData.RailWrapper)e.next();
            if (wrapper != null) {
               float x = (float)((wrapper.startVector.x + wrapper.endVector.x) * 0.5);
               float y = (float)((wrapper.startVector.y + wrapper.endVector.y) * 0.5);
               float z = (float)((wrapper.startVector.z + wrapper.endVector.z) * 0.5);
               list.add(new float[]{x, y, z});
               if (wrapper.getRail() != null) {
                  try {
                     wrapper.getRail()
                        .railMath
                        .render(
                           (x1, z1, x2, z2, x3, z3, x4, z4, y1, y2) -> list.add(
                              new float[]{(float)((x1 + x3) * 0.5), (float)((y1 + y2) * 0.5), (float)((z1 + z3) * 0.5)}
                           ),
                           8.0,
                           0.0F,
                           0.0F
                        );
                  } catch (Exception var10) {
                  }
               }
            }
         }
      } catch (Exception var11) {
         LOGGER.error("[MTR-RailVanishProbe] rail sample failed wrappers={}", wrappers, var11);
      }

      list.sort(Comparator.comparingDouble(a -> {
         double dx = a[0] - cam.getXMapped();
         double dy = a[1] - cam.getYMapped();
         double dz = a[2] - cam.getZMapped();
         return dx * dx + dy * dy + dz * dz;
      }));
      if (list.size() > limit) {
         return new ArrayList<>(list.subList(0, limit));
      }
      return list;
   }

   private static float translationLength(Matrix4f view) {
      float x = view.m30();
      float y = view.m31();
      float z = view.m32();
      return (float)Math.sqrt(x * x + y * y + z * z);
   }

   private static Vector3f nan() {
      return new Vector3f(Float.NaN, Float.NaN, Float.NaN);
   }

   private static Vector3f project(Matrix4f proj, Matrix4f view, Matrix4f model) {
      Vector4f clip = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
      model.transform(clip);
      view.transform(clip);
      proj.transform(clip);
      return Math.abs(clip.w) < 1.0E-8F ? new Vector3f(Float.NaN, Float.NaN, Float.NaN) : new Vector3f(clip.x / clip.w, clip.y / clip.w, clip.z / clip.w);
   }

   private static boolean inNdc(Vector3f ndc) {
      return ndc != null && !Float.isNaN(ndc.x)
         ? ndc.x >= -1.05F && ndc.x <= 1.05F && ndc.y >= -1.05F && ndc.y <= 1.05F && ndc.z >= -1.05F && ndc.z <= 1.05F
         : false;
   }

   private static float frobeniusDiff(Matrix4f a, Matrix4f b) {
      float sum = 0.0F;
      float[] aa = new float[16];
      float[] bb = new float[16];
      a.get(aa);
      b.get(bb);

      for (int i = 0; i < 16; i++) {
         float d = aa[i] - bb[i];
         sum += d * d;
      }

      return (float)Math.sqrt(sum);
   }

   private static String fmt(Vector3f v) {
      return String.format(Locale.ROOT, "(%.3f,%.3f,%.3f)", v.x, v.y, v.z);
   }

   private static void appendCsv(String line) {
      if (csvPath != null) {
         try {
            Files.writeString(csvPath, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
         } catch (IOException var2) {
            LOGGER.error("[MTR-RailVanishProbe] write failed", var2);
         }
      }
   }
}
