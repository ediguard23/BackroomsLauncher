package net.backrooms.menu.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.backrooms.menu.render.Logos;
import net.backrooms.menu.render.PantallaCarga;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * La pantalla de carga (la roja de Mojang Studios) pasa a ser la cinta del
 * evento (PantallaCarga). Se conservan los tiempos de Minecraft: fundido de
 * entrada en las recargas en partida, barra que se va primero y fundido de
 * salida de un segundo antes de quitar la pantalla.
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
	@Shadow
	@Final
	private Minecraft minecraft;
	@Shadow
	@Final
	private ReloadInstance reload;
	@Shadow
	@Final
	private boolean fadeIn;
	@Shadow
	private float currentProgress;
	@Shadow
	private long fadeOutStart;
	@Shadow
	private long fadeInStart;

	/** Los logos se registran junto al de Mojang: leidos del jar, ya valen en la primera carga. */
	@Inject(method = "registerTextures", at = @At("TAIL"))
	private static void backrooms$texturas(TextureManager tm, CallbackInfo ci) {
		Logos.registrar(tm);
	}

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void backrooms$cinta(GuiGraphics g, int mx, int my, float parcial, CallbackInfo ci) {
		ci.cancel();
		long ahora = Util.getMillis();
		if (this.fadeIn && this.fadeInStart == -1L) {
			this.fadeInStart = ahora;
		}
		float salida = this.fadeOutStart > -1L ? (ahora - this.fadeOutStart) / 1000.0F : -1.0F;
		float entrada = this.fadeInStart > -1L ? (ahora - this.fadeInStart) / 500.0F : -1.0F;
		float fondo;
		float contenido;
		if (salida >= 1.0F) {
			if (this.minecraft.screen != null) {
				this.minecraft.screen.renderWithTooltipAndSubtitles(g, 0, 0, parcial);
			} else {
				this.minecraft.gui.renderDeferredSubtitles();
			}
			fondo = 1.0F - Mth.clamp(salida - 1.0F, 0.0F, 1.0F);
			contenido = fondo;
			g.nextStratum();
		} else if (this.fadeIn) {
			if (this.minecraft.screen != null && entrada < 1.0F) {
				this.minecraft.screen.renderWithTooltipAndSubtitles(g, mx, my, parcial);
			} else {
				this.minecraft.gui.renderDeferredSubtitles();
			}
			fondo = (float) Mth.clamp(entrada, 0.15, 1.0);
			contenido = Mth.clamp(entrada, 0.0F, 1.0F);
			g.nextStratum();
		} else {
			RenderSystem.getDevice().createCommandEncoder()
				.clearColorTexture(this.minecraft.getMainRenderTarget().getColorTexture(), PantallaCarga.NEGRO);
			fondo = 1.0F;
			contenido = 1.0F;
		}

		float real = this.reload.getActualProgress();
		this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + real * 0.050000012F, 0.0F, 1.0F);
		float barra = salida < 1.0F ? 1.0F - Mth.clamp(salida, 0.0F, 1.0F) : 0.0F;
		PantallaCarga.dibujar(g, fondo, contenido, barra, this.currentProgress);

		if (salida >= 2.0F) {
			this.minecraft.setOverlay(null);
		}
	}
}
