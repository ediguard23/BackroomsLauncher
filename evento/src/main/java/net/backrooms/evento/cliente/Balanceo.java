package net.backrooms.evento.cliente;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.backrooms.evento.cliente.efectos.Cordura;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * El balanceo de la vista del evento, en lugar del de Minecraft (que es
 * brusco): al andar la vista se mece suave y con peso; al girar se inclina un
 * poco hacia el giro; parado, respira. Sin aliento jadea, y con la cordura
 * baja la vista se tambalea.
 *
 * Respeta la opcion «Balanceo de la vista»: si el jugador la quita (mareos),
 * no se mueve nada (ver BalanceoMixin).
 */
public final class Balanceo {
	private static final float TAU = (float) (Math.PI * 2.0);

	/** Inclinacion por girar, en grados, suavizada. */
	private static float giro;
	private static float giroAntes;
	private static float yawAntes;
	private static boolean primera = true;
	/** Fase y fuerza de la respiracion (la frecuencia cambia, asi que la fase se acumula). */
	private static float respiro;
	private static float respiroAntes;
	private static float fuerza = 0.22F;
	/** Tiempo en ticks para el tambaleo de la cordura. */
	private static int ticks;

	private Balanceo() {
	}

	public static void tick(Minecraft mc) {
		LocalPlayer j = mc.player;
		if (j == null) {
			primera = true;
			return;
		}
		ticks++;
		float yaw = j.getYRot();
		float d = primera ? 0.0F : Mth.wrapDegrees(yaw - yawAntes);
		primera = false;
		yawAntes = yaw;
		giroAntes = giro;
		giro += (Mth.clamp(d * 0.1F, -2.0F, 2.0F) - giro) * 0.2F;

		// respira despacio; cansado, mas deprisa y mas hondo; sin aliento, jadea
		float cansancio = 1.0F - SupervivenciaCliente.estamina();
		boolean agotado = SupervivenciaCliente.agotado();
		float frecuencia = agotado ? 0.7F : 0.22F + 0.25F * cansancio;
		float objetivo = agotado ? 1.1F : 0.22F + 0.35F * cansancio;
		fuerza += (objetivo - fuerza) * 0.05F;
		respiroAntes = respiro;
		respiro += frecuencia * TAU / 20.0F;
		if (respiro > TAU * 1000) {
			respiro -= TAU * 1000;
			respiroAntes -= TAU * 1000;
		}
	}

	public static void aplicar(PoseStack p, float parcial) {
		if (!(Minecraft.getInstance().getCameraEntity() instanceof AbstractClientPlayer j)) {
			return;
		}
		ClientAvatarState s = j.avatarState();
		float paso = s.getBackwardsInterpolatedWalkDistance(parcial) * (float) Math.PI;
		float bob = s.getInterpolatedBob(parcial);

		// andar: como el de Minecraft pero mas suave (2/3) y con mas peso abajo
		float lado = Mth.sin(paso);
		p.translate(lado * bob * 0.32F, -Math.abs(Mth.cos(paso)) * bob * 0.75F, 0.0F);
		float alabeo = lado * bob * 2.0F;
		float cabeceo = Math.abs(Mth.cos(paso - 0.25F)) * bob * 3.2F;

		// girar: se inclina hacia el giro
		alabeo += Mth.lerp(parcial, giroAntes, giro);

		// respirar
		cabeceo += Mth.sin(Mth.lerp(parcial, respiroAntes, respiro)) * fuerza;

		// cordura baja: la vista se tambalea
		float loco = Cordura.efecto();
		float guinada = 0.0F;
		if (loco > 0.0F) {
			float t = (ticks + parcial) / 20.0F;
			alabeo += (Mth.sin(t * 0.63F) * 2.2F + Mth.sin(t * 1.71F) * 0.6F) * loco;
			guinada = Mth.sin(t * 0.41F) * 1.2F * loco;
		}

		p.mulPose(Axis.ZP.rotationDegrees(alabeo));
		p.mulPose(Axis.XP.rotationDegrees(cabeceo));
		if (guinada != 0.0F) {
			p.mulPose(Axis.YP.rotationDegrees(guinada));
		}
	}
}
