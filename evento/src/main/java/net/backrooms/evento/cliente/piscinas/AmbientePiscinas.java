package net.backrooms.evento.cliente.piscinas;

import net.backrooms.evento.Sonidos;
import net.backrooms.evento.cliente.cinematica.CinematicaCliente;
import net.backrooms.evento.piscinas.Piscinas;
import net.backrooms.evento.piscinas.PlanoPiscinas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Lo que se oye en las Piscinas, en el cliente. Nada en bucle: cada cosa suena de vez en
 * cuando, a intervalos al azar y cada vez en otro sitio (en 3D), para que nunca se sepa del
 * todo que se esta oyendo (sonidos en tools/sonidos/nivel37.js):
 *
 *   gotas                     cada 5-16 s, a 4-18 bloques, desde el techo
 *   agua que se mueve, baja   cada 25-70 s, en el agua mas cercana (o lejos)
 *   el agua que lame el borde cada 7-18 s, solo con una piscina a menos de 7 bloques
 *   un chapoteo suave         cada 50-140 s, en un agua a 14-34 bloques
 *   tuberias                  cada 45-150 s, dentro de las paredes, a 8-26 bloques
 *   algo grave                cada 2-5 min, a 40-70 bloques
 *   el grunido de la Serpiente cada 1-2 min, a 55-95 bloques: no quiere decir que este cerca
 *
 * Y rarisimos: una voz humana lejana e irreconocible (cada 12-40 min) y, alguna vez en toda
 * una partida, un susurro con el nombre de quien lo oye, justo detras (cada 25-75 min). Cada
 * jugador oye los suyos: nunca suenan para todos a la vez.
 */
public final class AmbientePiscinas {
	private enum Tipo {
		GOTA(5, 16), AGUA(25, 70), ORILLA(7, 18), CHAPOTEO(50, 140), TUBERIA(45, 150), GRAVE(120, 300), GRUNIDO(60, 120),
		VOZ(12 * 60, 40 * 60), SUSURRO(25 * 60, 75 * 60);

		final int min;
		final int max;

		Tipo(int min, int max) {
			this.min = min;
			this.max = max;
		}
	}

	private static final RandomSource AZAR = RandomSource.create();
	private static final long[] SIGUIENTE = new long[Tipo.values().length];
	private static long ticks;
	private static boolean dentro;

	private AmbientePiscinas() {
	}

	public static void tick(Minecraft mc) {
		LocalPlayer j = mc.player;
		ClientLevel nivel = mc.level;
		if (nivel == null || j == null || !Piscinas.es(nivel)) {
			dentro = false;
			return;
		}
		if (mc.isPaused() || CinematicaCliente.activa() || !j.isAlive()) {
			return;
		}
		ticks++;
		if (!dentro) {
			// al llegar: cada cosa en su momento, nada de todo a la vez
			dentro = true;
			for (Tipo t : Tipo.values()) {
				SIGUIENTE[t.ordinal()] = ticks + Math.round(20 * (t.min + AZAR.nextFloat() * (t.max - t.min)) * (t.ordinal() >= Tipo.VOZ.ordinal() ? 1.0F : 0.6F));
			}
		}
		for (Tipo t : Tipo.values()) {
			if (ticks < SIGUIENTE[t.ordinal()]) {
				continue;
			}
			if (t == Tipo.ORILLA) {
				// solo cerca de una piscina; si no la hay, se mira otra vez en un segundo
				BlockPos agua = agua(nivel, j.blockPosition(), 2, 7, 24);
				if (agua == null) {
					SIGUIENTE[t.ordinal()] = ticks + 20;
					continue;
				}
				sonar(mc, Sonidos.PISCINAS_ORILLA, Vec3.atCenterOf(agua), SoundSource.AMBIENT, 0.55F + AZAR.nextFloat() * 0.25F);
			} else {
				sonar(mc, j, t);
			}
			programar(t);
		}
	}

	private static void programar(Tipo t) {
		SIGUIENTE[t.ordinal()] = ticks + Math.round(20 * (t.min + AZAR.nextFloat() * (t.max - t.min)));
	}

	/** Suena ya (para el staff y las pruebas): voz, susurro o cualquiera de los de ambiente. */
	public static void forzar(Minecraft mc, String que) {
		if (mc.player == null) {
			return;
		}
		for (Tipo t : Tipo.values()) {
			if (t.name().equalsIgnoreCase(que)) {
				if (t == Tipo.ORILLA) {
					BlockPos agua = mc.level == null ? null : agua(mc.level, mc.player.blockPosition(), 2, 7, 48);
					if (agua != null) {
						sonar(mc, Sonidos.PISCINAS_ORILLA, Vec3.atCenterOf(agua), SoundSource.AMBIENT, 0.7F);
					}
				} else {
					sonar(mc, mc.player, t);
				}
			}
		}
	}

	private static void sonar(Minecraft mc, LocalPlayer j, Tipo t) {
		Vec3 ojo = j.getEyePosition();
		switch (t) {
			case GOTA -> sonar(mc, Sonidos.PISCINAS_GOTA, alrededor(ojo, 4, 18, 1.5, 4.0), SoundSource.AMBIENT, 0.45F + AZAR.nextFloat() * 0.4F);
			case AGUA -> {
				BlockPos agua = mc.level == null ? null : agua(mc.level, j.blockPosition(), 3, 24, 30);
				Vec3 p = agua != null ? Vec3.atCenterOf(agua) : alrededor(ojo, 12, 24, -1.0, 0.0);
				sonar(mc, Sonidos.PISCINAS_AGUA, p, SoundSource.AMBIENT, 0.6F);
			}
			case CHAPOTEO -> {
				BlockPos agua = mc.level == null ? null : agua(mc.level, j.blockPosition(), 14, 34, 40);
				Vec3 p = agua != null ? Vec3.atCenterOf(agua) : alrededor(ojo, 20, 34, -1.0, 0.0);
				sonar(mc, Sonidos.PISCINAS_CHAPOTEO, p, SoundSource.AMBIENT, 0.7F + AZAR.nextFloat() * 0.3F);
			}
			case TUBERIA -> sonar(mc, Sonidos.PISCINAS_TUBERIA, alrededor(ojo, 8, 26, -1.0, 3.0), SoundSource.AMBIENT, 0.7F + AZAR.nextFloat() * 0.3F);
			case GRAVE -> sonar(mc, Sonidos.PISCINAS_GRAVE, alrededor(ojo, 40, 70, -2.0, 4.0), SoundSource.AMBIENT, 1.3F + AZAR.nextFloat() * 0.5F);
			case GRUNIDO -> sonar(mc, Sonidos.SERPIENTE_GRUNIDO, alrededor(ojo, 55, 95, 0.0, 3.0), SoundSource.HOSTILE, 1.0F);
			case VOZ -> sonar(mc, Sonidos.PISCINAS_VOZ, alrededor(ojo, 40, 65, 0.0, 2.0), SoundSource.AMBIENT, 1.0F);
			case SUSURRO -> SusurroNombre.sonar(mc, j);
			default -> {
			}
		}
	}

	private static void sonar(Minecraft mc, SoundEvent sonido, Vec3 p, SoundSource fuente, float volumen) {
		mc.getSoundManager().play(new SimpleSoundInstance(sonido.location(), fuente, volumen, 0.88F + AZAR.nextFloat() * 0.24F,
			RandomSource.create(AZAR.nextLong()), false, 0, SoundInstance.Attenuation.LINEAR, p.x, p.y, p.z, false));
	}

	/** Un punto al azar a entre min y max bloques en horizontal, y entre dy0 y dy1 de altura. */
	private static Vec3 alrededor(Vec3 o, double min, double max, double dy0, double dy1) {
		double ang = AZAR.nextDouble() * Math.PI * 2;
		double d = min + AZAR.nextDouble() * (max - min);
		return new Vec3(o.x + Math.cos(ang) * d, o.y + dy0 + AZAR.nextDouble() * (dy1 - dy0), o.z + Math.sin(ang) * d);
	}

	/**
	 * La superficie de algun agua a entre min y max bloques (en horizontal) de p, o null: se
	 * prueban unas cuantas columnas al azar (barato, para hacerlo cada pocos segundos).
	 */
	private static @Nullable BlockPos agua(ClientLevel nivel, BlockPos p, int min, int max, int intentos) {
		BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
		for (int k = 0; k < intentos; k++) {
			double ang = AZAR.nextDouble() * Math.PI * 2;
			double d = min + AZAR.nextDouble() * (max - min);
			int x = p.getX() + (int) Math.round(Math.cos(ang) * d);
			int z = p.getZ() + (int) Math.round(Math.sin(ang) * d);
			q.set(x, PlanoPiscinas.SUELO, z);
			if (nivel.isLoaded(q) && nivel.getFluidState(q).is(FluidTags.WATER)) {
				return q.immutable();
			}
		}
		return null;
	}
}
