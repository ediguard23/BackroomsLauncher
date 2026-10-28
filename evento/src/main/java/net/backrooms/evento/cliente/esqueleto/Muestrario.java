package net.backrooms.evento.cliente.esqueleto;

import java.util.ArrayList;
import java.util.List;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.esqueleto.Animacion;
import net.backrooms.evento.esqueleto.Ciclos;
import net.backrooms.evento.esqueleto.Figura;
import net.backrooms.evento.esqueleto.Pose;
import net.backrooms.evento.mision.Entidades;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntitySpawnReason;
import org.jspecify.annotations.Nullable;

/**
 * Para probar y disenar animaciones del esqueleto (orden de prueba "figura <animacion> [lado]", ver
 * OrdenesPrueba): pone delante del jugador una Figura que hace esa animacion durante 12 s, de perfil
 * ("lado") o de frente. Animaciones: quieto, caminar, correr, huir (correr desesperado mirando atras),
 * agachado, susto, caer (tropieza y cae), forcejeo, manos y todo (una tras otra).
 */
public final class Muestrario {
	private static final List<Figura> FIGURAS = new ArrayList<>();
	private static int hasta;

	private Muestrario() {
	}

	/** La animacion de ese nombre (segundos -> pose) o null. */
	public static java.util.function.@Nullable Function<Float, Pose> animacion(String nombre) {
		return switch (nombre) {
			case "quieto" -> Ciclos::quieto;
			case "caminar" -> t -> Ciclos.caminar(t * 0.9F);
			case "correr" -> t -> Ciclos.correr(t * 1.4F, 0.5F);
			case "huir" -> t -> Ciclos.correr(t * 1.5F, 1.0F).mas(Ciclos.mirarAtras((float) Math.max(0, Math.sin(t * 1.3)), 1.0F), 1.0F);
			case "agachado" -> t -> Pose.mezcla(Ciclos.quieto(t), Ciclos.agachado(), (float) (0.5 - 0.5 * Math.cos(t * 1.6)));
			case "susto" -> t -> Ciclos.quieto(t).mas(Ciclos.susto((float) (0.5 - 0.5 * Math.cos(t * 2.0))), 1.0F);
			case "caer" -> t -> caida().en(t % 4.0F);
			case "forcejeo" -> Ciclos::forcejeo;
			case "manos" -> Ciclos::manos;
			case "todo" -> t -> todo().en(t % todo().duracion());
			default -> null;
		};
	}

	/** Corre, tropieza, cae de bruces y se da la vuelta. */
	static Animacion caida() {
		Pose tropieza = Ciclos.correr(0.25F, 1.0F).suma(net.backrooms.evento.esqueleto.Hueso.TORSO, 25, 0, 0)
			.suma(net.backrooms.evento.esqueleto.Hueso.BRAZO_D, -60, 0, 20).suma(net.backrooms.evento.esqueleto.Hueso.BRAZO_I, -70, 0, -20);
		Pose bruces = new Pose().gira(net.backrooms.evento.esqueleto.Hueso.BRAZO_D, -160, 0, 25).gira(net.backrooms.evento.esqueleto.Hueso.BRAZO_I, -165, 0, -25)
			.gira(net.backrooms.evento.esqueleto.Hueso.CABEZA, -40, 0, 0).gira(net.backrooms.evento.esqueleto.Hueso.PIERNA_D, 30, 0, 0)
			.cuerpo(88, 0, 0, 0, 0.12F, 0);
		return new Animacion()
			.clave(0.0F, t -> Ciclos.correr(t * 1.5F, 1.0F))
			.clave(1.2F, t -> Ciclos.correr(t * 1.5F, 1.0F))
			.claveLineal(1.45F, tropieza)
			.claveLineal(1.8F, bruces)
			.clave(2.6F, bruces)
			.clave(3.4F, Ciclos.bocaArriba())
			.clave(4.0F, Ciclos.bocaArriba());
	}

	static Animacion todo() {
		Animacion a = new Animacion();
		String[] orden = {"quieto", "caminar", "correr", "huir", "agachado", "susto", "forcejeo", "manos"};
		float t = 0.0F;
		for (String n : orden) {
			java.util.function.Function<Float, Pose> f = animacion(n);
			a.clave(t + 0.4F, f).clave(t + 3.0F, f);
			t += 3.4F;
		}
		return a;
	}

	public static void mostrar(Minecraft mc, String nombre, boolean deLado) {
		java.util.function.Function<Float, Pose> anim = animacion(nombre);
		if (anim == null || mc.level == null || mc.player == null) {
			BackroomsEvento.LOG.warn("figura: no hay animacion {}", nombre);
			return;
		}
		quitar(mc);
		Figura f = Entidades.FIGURA.create(mc.level, EntitySpawnReason.EVENT);
		if (f == null) {
			return;
		}
		double giro = Math.toRadians(mc.player.getYRot());
		double x = mc.player.getX() - Math.sin(giro) * 3.2;
		double z = mc.player.getZ() + Math.cos(giro) * 3.2;
		f.setId(-7900);
		f.snapTo(x, mc.player.getY(), z, mc.player.getYRot() + (deLado ? 90.0F : 180.0F), 0.0F);
		int t0 = f.tickCount;
		f.animador = (fig, p) -> anim.apply((fig.tickCount - t0 + p) / 20.0F);
		mc.level.addEntity(f);
		FIGURAS.add(f);
		hasta = mc.player.tickCount + 240;
		BackroomsEvento.LOG.info("figura: {} ({})", nombre, deLado ? "de lado" : "de frente");
	}

	public static void tick(Minecraft mc) {
		if (!FIGURAS.isEmpty() && mc.player != null && mc.player.tickCount > hasta) {
			quitar(mc);
		}
	}

	private static void quitar(Minecraft mc) {
		if (mc.level != null) {
			for (Figura f : FIGURAS) {
				mc.level.removeEntity(f.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
			}
		}
		FIGURAS.clear();
	}
}
