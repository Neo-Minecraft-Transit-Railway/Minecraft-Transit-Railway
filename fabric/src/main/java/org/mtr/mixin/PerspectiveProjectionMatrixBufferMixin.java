package org.mtr.mixin;

import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import org.joml.Matrix4f;
import org.mtr.mapping.render.CapturedProjection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PerspectiveProjectionMatrixBuffer.class)
public abstract class PerspectiveProjectionMatrixBufferMixin {

	@Inject(method = "getBuffer", at = @At("HEAD"))
	private void mtr$captureProjection(Matrix4f matrix, CallbackInfoReturnable<?> cir) {
		CapturedProjection.capture(matrix);
	}
}
