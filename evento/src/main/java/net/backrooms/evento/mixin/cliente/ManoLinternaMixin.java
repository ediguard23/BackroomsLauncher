package net.backrooms.evento.mixin.cliente;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.backrooms.evento.objetos.Objetos;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * En primera persona Minecraft no dibuja el brazo cuando llevas algo: la linterna parecia flotar.
 * Con la linterna se dibuja el brazo (como con la mano vacia, con el traje si lo llevas:
 * BrazoTrajeMixin) y la linterna agarrada en el puño, siguiendo al brazo.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ManoLinternaMixin {
	/** El puño en el brazo (x, y), cuanto sale la linterna por delante, cabeceo, giro, alabeo, escala y un
	 * desplazamiento respecto a la vista (x, y, z), todo en 1/16 de bloque y grados. */
	private static float[] backrooms$ajuste = {-1.0F, 8.0F, -5.0F, 2.0F, -5.0F, 0.0F, 0.5F, -0.5F, 3.2F, 1.0F};
	private static long backrooms$leido;

	@Shadow
	@Final
	private EntityRenderDispatcher entityRenderDispatcher;

	@Shadow
	private void renderPlayerArm(PoseStack pose, SubmitNodeCollector cola, int luz, float equipar, float golpe, HumanoidArm brazo) {
		throw new AssertionError();
	}

	@Shadow
	public abstract void renderItem(LivingEntity e, ItemStack s, ItemDisplayContext ctx, PoseStack pose, SubmitNodeCollector cola, int luz);

	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void backrooms$linterna(AbstractClientPlayer j, float pt, float pitch, InteractionHand mano, float golpe, ItemStack s, float equipar,
		PoseStack pose, SubmitNodeCollector cola, int luz, CallbackInfo ci) {
		if (!s.is(Objetos.LINTERNA) || j.isScoping() || j.isInvisible()) {
			return;
		}
		float[] a = backrooms$ajuste();
		HumanoidArm brazo = mano == InteractionHand.MAIN_HAND ? j.getMainArm() : j.getMainArm().getOpposite();
		boolean derecho = brazo == HumanoidArm.RIGHT;
		int k = derecho ? 1 : -1;
		// el brazo quieto (sin el golpe al pulsar: la linterna se enciende con la F); de su cadena solo se
		// toma donde queda el puño: la linterna va ahi, pero orientada respecto a la vista (hacia delante)
		pose.pushPose();
		org.joml.Matrix4f base = new org.joml.Matrix4f(pose.last().pose());
		pose.pushPose();
		this.renderPlayerArm(pose, cola, luz, equipar, 0.0F, brazo);
		PlayerModel modelo = this.entityRenderDispatcher.getPlayerRenderer(j).getModel();
		ModelPart parte = derecho ? modelo.rightArm : modelo.leftArm;
		parte.translateAndRotate(pose);
		pose.translate(k * a[0] / 16.0F, a[1] / 16.0F, 0.0F);
		org.joml.Vector3f puno = pose.last().pose().transformPosition(new org.joml.Vector3f());
		pose.popPose();
		org.joml.Vector3f p = base.invert(new org.joml.Matrix4f()).transformPosition(puno);
		pose.translate(p.x + k * a[7] / 16.0F, p.y + a[8] / 16.0F, p.z + a[9] / 16.0F);
		pose.mulPose(Axis.YP.rotationDegrees(k * a[4]));
		pose.mulPose(Axis.XP.rotationDegrees(a[3]));
		pose.mulPose(Axis.ZP.rotationDegrees(k * a[5]));
		pose.scale(a[6], a[6], a[6]);
		// se agarra por detras: la linterna sale hacia delante del puño
		pose.translate(0.0F, 0.0F, a[2] / 16.0F);
		this.renderItem(j, s, ItemDisplayContext.NONE, pose, cola, luz);
		pose.popPose();
		ci.cancel();
	}

	/** Para afinar en pruebas sin recompilar: %TEMP%/backrooms-mano.txt con los diez numeros. */
	private static float[] backrooms$ajuste() {
		if (System.getenv("BACKROOMS_PRUEBAS") == null) {
			return backrooms$ajuste;
		}
		long ahora = System.currentTimeMillis();
		if (ahora - backrooms$leido > 500) {
			backrooms$leido = ahora;
			try {
				java.nio.file.Path f = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "backrooms-mano.txt");
				if (java.nio.file.Files.exists(f)) {
					String[] p = java.nio.file.Files.readString(f).trim().split("\s+");
					float[] n = new float[10];
					for (int i = 0; i < 10; i++) {
						n[i] = Float.parseFloat(p[i]);
					}
					backrooms$ajuste = n;
				}
			} catch (Exception e) {
				// se queda como estaba
			}
		}
		return backrooms$ajuste;
	}
}
