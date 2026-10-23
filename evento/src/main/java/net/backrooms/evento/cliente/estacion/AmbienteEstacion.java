package net.backrooms.evento.cliente.estacion;

import net.backrooms.evento.Sonidos;
import net.backrooms.evento.cliente.cinematica.CinematicaCliente;
import net.backrooms.evento.estacion.Estacion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Lo que se oye en la Estacion, en el cliente (sonidos en tools/sonidos/estacion.js):
 *
 *   la maquinaria          en bucle, sin posicion: zumbido de transformadores y ventilacion
 *   un estruendo metalico  cada 50-150 s, lejos: algo que cae, una tuberia que cede...
 *   las chispas            de las centrales sin activar (las pone el propio bloque)
 *
 * Cada cliente sortea lo suyo.
 */
public final class AmbienteEstacion {
	private static final RandomSource AZAR = RandomSource.create();
	private static Bucle maquinas;
	private static long ticks;
	private static long estruendo;

	private AmbienteEstacion() {
	}

	public static void tick(Minecraft mc) {
		LocalPlayer j = mc.player;
		if (mc.level == null || j == null || !Estacion.es(mc.level)) {
			return;
		}
		if (mc.isPaused() || CinematicaCliente.activa() || !j.isAlive()) {
			return;
		}
		ticks++;
		if (maquinas == null || maquinas.isStopped() || !mc.getSoundManager().isActive(maquinas)) {
			maquinas = new Bucle(Sonidos.ESTACION_AMBIENTE, 0.5F);
			mc.getSoundManager().play(maquinas);
		}
		if (estruendo == 0) {
			estruendo = ticks + 20L * (20 + AZAR.nextInt(60));
		}
		if (ticks >= estruendo) {
			estruendo = ticks + 20L * (50 + AZAR.nextInt(100));
			double ang = AZAR.nextDouble() * Math.PI * 2;
			double d = 25 + AZAR.nextDouble() * 30;
			Vec3 p = j.getEyePosition().add(Math.cos(ang) * d, AZAR.nextDouble() * 3, Math.sin(ang) * d);
			mc.getSoundManager().play(new SimpleSoundInstance(Sonidos.ESTACION_ESTRUENDO.location(), SoundSource.AMBIENT, 1.6F,
				0.7F + AZAR.nextFloat() * 0.4F, RandomSource.create(AZAR.nextLong()), false, 0, SoundInstance.Attenuation.LINEAR, p.x, p.y, p.z, false));
		}
	}

	/** Un bucle sin posicion que sube poco a poco al entrar y se apaga al salir del nivel. */
	private static final class Bucle extends AbstractTickableSoundInstance {
		private final float con;

		Bucle(SoundEvent sonido, float con) {
			super(sonido, SoundSource.AMBIENT, RandomSource.create());
			this.con = con;
			this.looping = true;
			this.delay = 0;
			this.volume = 0.0F;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
		}

		@Override
		public void tick() {
			Minecraft mc = Minecraft.getInstance();
			boolean aqui = mc.level != null && Estacion.es(mc.level) && mc.player != null;
			float quiero = aqui ? this.con : 0.0F;
			this.volume += (quiero - this.volume) * (quiero > this.volume ? 0.03F : 0.08F);
			if (!aqui && this.volume < 0.01F) {
				this.stop();
			}
		}
	}
}
