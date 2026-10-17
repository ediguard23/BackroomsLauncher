package net.backrooms.evento.cliente;

import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.cliente.cinematica.CinematicaCliente;
import net.backrooms.evento.vestibulo.PlanoVestibulo;
import net.backrooms.evento.vestibulo.Vestibulo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * La musica tenue de la recepcion (tools/sonidos/recepcion.js), en bucle mientras se esta
 * dentro: entra y sale con un fundido. Durante la cinematica se calla (la del /start empieza
 * en la recepcion).
 */
public final class MusicaRecepcion {
	private static final SoundEvent MUSICA = SoundEvent.createVariableRangeEvent(BackroomsEvento.id("recepcion.musica"));
	private static final float VOLUMEN = 0.55F;
	private static Bucle sonando;

	private MusicaRecepcion() {
	}

	private static boolean dentro(Minecraft mc) {
		return mc.player != null && mc.level != null && mc.level.dimension().equals(Vestibulo.DIMENSION)
			&& PlanoVestibulo.enRecepcion(mc.player.getX(), mc.player.getY(), mc.player.getZ()) && !CinematicaCliente.activa();
	}

	/** Cada tick: si esta en la recepcion y no suena, que empiece. */
	public static void tick(Minecraft mc) {
		if (dentro(mc) && (sonando == null || sonando.isStopped())) {
			sonando = new Bucle();
			mc.getSoundManager().play(sonando);
		}
	}

	private static final class Bucle extends AbstractTickableSoundInstance {
		private float nivel;

		Bucle() {
			super(MUSICA, SoundSource.AMBIENT, RandomSource.create());
			this.looping = true;
			this.delay = 0;
			this.volume = 0.0F;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
		}

		@Override
		public void tick() {
			boolean si = dentro(Minecraft.getInstance());
			this.nivel = Math.max(0.0F, Math.min(1.0F, this.nivel + (si ? 0.02F : -0.04F)));
			this.volume = this.nivel * VOLUMEN;
			if (!si && this.nivel <= 0.0F) {
				this.stop();
			}
		}
	}
}
