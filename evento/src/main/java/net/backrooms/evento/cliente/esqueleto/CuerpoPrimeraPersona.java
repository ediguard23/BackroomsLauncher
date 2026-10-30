package net.backrooms.evento.cliente.esqueleto;

import java.util.EnumSet;
import java.util.Set;
import net.backrooms.evento.esqueleto.Figura;
import net.backrooms.evento.esqueleto.Hueso;
import net.backrooms.evento.esqueleto.Pose;
import net.backrooms.evento.mision.Entidades;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import org.jspecify.annotations.Nullable;

/**
 * El cuerpo del jugador visto desde sus propios ojos en las cinematicas (la camara va sola y el
 * jugador de verdad no se dibuja): una Figura con el esqueleto articulado colocada con los pies
 * bajo la camara y mirando hacia donde mira ella, de la que solo se dibujan los huesos que se piden
 * (un brazo que pulsa una botonera; los brazos y el cuerpo al mirarse las manos). La cabeza nunca:
 * estaria en la camara.
 */
public final class CuerpoPrimeraPersona {
	private CuerpoPrimeraPersona() {
	}

	/** Una figura (solo de este cliente) con esa piel de la que solo se ven esos huesos. */
	public static @Nullable Figura crear(Minecraft mc, Identifier piel, int id, Hueso... visibles) {
		Figura f = Entidades.FIGURA.create(mc.level, EntitySpawnReason.EVENT);
		if (f == null) {
			return null;
		}
		Set<Hueso> ver = EnumSet.noneOf(Hueso.class);
		java.util.Collections.addAll(ver, visibles);
		for (Hueso h : Hueso.TODOS) {
			if (!ver.contains(h) || h == Hueso.CABEZA) {
				f.ocultar(h);
			}
		}
		f.setId(id);
		f.piel = piel;
		mc.level.addEntity(f);
		return f;
	}

	/**
	 * Pone el cuerpo bajo unos ojos (x, y, z de la camara, giro en grados): los pies 1,62 por debajo y
	 * `adelante` bloques hacia donde mira (para que lo que esta pegado a la cara no tape la imagen).
	 */
	public static void colocar(Figura f, double x, double y, double z, float giro, double adelante) {
		f.xo = f.getX();
		f.yo = f.getY();
		f.zo = f.getZ();
		f.yRotO = f.getYRot();
		double g = Math.toRadians(giro);
		f.setPos(x - Math.sin(g) * adelante, y - 1.62, z + Math.cos(g) * adelante);
		f.setYRot(giro);
	}

	public static void quitar(Minecraft mc, @Nullable Figura f) {
		if (f != null && mc.level != null) {
			mc.level.removeEntity(f.getId(), Entity.RemovalReason.DISCARDED);
		}
	}

	/**
	 * El brazo derecho pulsando una botonera que tiene delante, a la altura del pecho: sube a la vez
	 * que empieza `desde`, en cada pulsacion estira el codo de golpe y lo recoge, y baja al acabar
	 * `hasta`. `dentro`: cuanto se cierra el brazo hacia el centro (grados, giro en vertical): el hombro
	 * esta a la derecha de los ojos y la botonera, delante de ellos.
	 */
	public static Pose pulsar(float t, float desde, float hasta, float[] pulsa, float dentro) {
		float sube = suave((t - desde) / 0.35F) * (1.0F - suave((t - hasta) / 0.4F));
		float empuja = 0.0F;
		for (float p : pulsa) {
			float d = t - p;
			if (d > -0.07F && d < 0.16F) {
				empuja = Math.max(empuja, d < 0.0F ? 1.0F + d / 0.07F : 1.0F - d / 0.16F);
			}
		}
		Pose abajo = new Pose().gira(Hueso.BRAZO_D, 15, 0, 5).gira(Hueso.ANTEBRAZO_D, -10, 0, 0);
		Pose alcanza = new Pose()
			.gira(Hueso.BRAZO_D, -80.0F - 8.0F * empuja, -dentro, 0)
			.gira(Hueso.ANTEBRAZO_D, -28.0F + 26.0F * empuja, 0, 0)
			.gira(Hueso.MANO_D, -10.0F + 8.0F * empuja, 0, 0);
		return Pose.mezcla(abajo, alcanza, sube);
	}

	private static float suave(float k) {
		k = Math.max(0.0F, Math.min(1.0F, k));
		return k * k * (3.0F - 2.0F * k);
	}
}
