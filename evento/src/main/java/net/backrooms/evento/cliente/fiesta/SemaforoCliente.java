package net.backrooms.evento.cliente.fiesta;

import net.backrooms.evento.Sonidos;
import net.backrooms.evento.fiesta.PlanoFiesta;
import net.backrooms.evento.red.Semaforo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * El semaforo de la Fiesta en el cliente: la musiquilla de la luz verde suena desde el semaforo
 * mientras dura la verde y se corta en seco con la roja (la voz que canta cada luz la pone el
 * servidor). JuegoSemaforo manda la luz a quien esta en el pasillo.
 */
public final class SemaforoCliente {
	private static int luz;
	private static Musica musica;

	private SemaforoCliente() {
	}

	public static int luz() {
		return luz;
	}

	public static void recibir(Semaforo s) {
		luz = s.luz();
		Minecraft mc = Minecraft.getInstance();
		if (luz == 1) {
			if (musica != null) {
				musica.cortar();
			}
			musica = new Musica();
			mc.getSoundManager().play(musica);
		} else if (musica != null) {
			musica.cortar();
			musica = null;
		}
	}

	public static void olvidar() {
		luz = 0;
		if (musica != null) {
			musica.cortar();
			musica = null;
		}
	}

	private static final class Musica extends AbstractTickableSoundInstance {
		Musica() {
			super(Sonidos.SEMAFORO_MUSICA, SoundSource.AMBIENT, RandomSource.create());
			this.looping = true;
			this.delay = 0;
			this.volume = 1.4F;
			this.x = PlanoFiesta.SEM_X1 + 0.5;
			this.y = PlanoFiesta.SUELO + 7;
			this.z = -0.5;
			this.attenuation = SoundInstance.Attenuation.LINEAR;
		}

		void cortar() {
			this.stop();
		}

		@Override
		public void tick() {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || !net.backrooms.evento.fiesta.Fiesta.es(mc.level) || luz != 1) {
				this.stop();
			}
		}
	}
}
