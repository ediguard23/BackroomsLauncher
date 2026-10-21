package net.backrooms.evento.cliente.estacion;

import net.backrooms.evento.Sonidos;
import net.backrooms.evento.cliente.cinematica.CinematicaCliente;
import net.backrooms.evento.estacion.Estacion;
import net.backrooms.evento.estacion.PlanoEstacion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Lo que se oye en la Estacion, en el cliente (sonidos en tools/sonidos/estacion.js):
 *
 *   la maquinaria          en bucle, sin posicion: zumbido de transformadores y ventilacion;
 *                          sin corriente baja casi del todo (y se oye mas todo lo demas)
 *   el generador           en bucle desde el generador de la nave, solo con corriente
 *   un estruendo metalico  cada 50-150 s, lejos: algo que cae, una tuberia que cede...
 *   las chispas            de las centrales sin activar (las pone el propio bloque)
 *
 * Cada cliente sortea lo suyo.
 */
public final class AmbienteEstacion {
	private static final RandomSource AZAR = RandomSource.create();
	private static Bucle maquinas;
	private static Bucle generador;
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
			maquinas = new Bucle(Sonidos.ESTACION_AMBIENTE, null, 0.5F, 0.12F);
			mc.getSoundManager().play(maquinas);
		}
		if (generador == null || generador.isStopped() || !mc.getSoundManager().isActive(generador)) {
			double[] gp = PlanoEstacion.GENERADOR;
			generador = new Bucle(Sonidos.ESTACION_GENERADOR, new Vec3(gp[0], gp[1], gp[2]), 2.6F, 0.0F);
			mc.getSoundManager().play(generador);
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

	/**
	 * Un bucle de la Estacion que sube con corriente y baja sin ella (`con` y `sin`); con `donde`
	 * suena desde alli (el generador), si no, sin posicion. Al salir del nivel se apaga.
	 */
	private static final class Bucle extends AbstractTickableSoundInstance {
		private final float con;
		private final float sin;

		Bucle(SoundEvent sonido, @Nullable Vec3 donde, float con, float sin) {
			super(sonido, SoundSource.AMBIENT, RandomSource.create());
			this.con = con;
			this.sin = sin;
			this.looping = true;
			this.delay = 0;
			this.volume = 0.0F;
			if (donde == null) {
				this.relative = true;
				this.attenuation = SoundInstance.Attenuation.NONE;
			} else {
				this.x = donde.x;
				this.y = donde.y;
				this.z = donde.z;
				this.attenuation = SoundInstance.Attenuation.LINEAR;
			}
		}

		@Override
		public void tick() {
			Minecraft mc = Minecraft.getInstance();
			boolean aqui = mc.level != null && Estacion.es(mc.level) && mc.player != null;
			float quiero = !aqui ? 0.0F : EstacionCliente.estado().corriente() ? this.con : this.sin;
			this.volume += (quiero - this.volume) * (quiero > this.volume ? 0.03F : 0.08F);
			if (!aqui && this.volume < 0.01F) {
				this.stop();
			}
		}
	}
}
