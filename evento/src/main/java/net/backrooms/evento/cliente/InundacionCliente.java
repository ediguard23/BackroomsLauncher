package net.backrooms.evento.cliente;

import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.piscinas.Inundacion;
import net.backrooms.evento.red.EstadoInundacion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * La inundacion de las Piscinas en el cliente: la altura del agua (la manda el servidor cada
 * medio segundo con su velocidad, y aqui se sigue moviendo entre paquete y paquete), el
 * rugido del agua entrando (en bucle, mas fuerte mientras sube y si te pilla dentro) y lo que
 * EfectosMundo necesita para dibujarla.
 */
public final class InundacionCliente {
	private static final SoundEvent AGUA = SoundEvent.createVariableRangeEvent(BackroomsEvento.id("piscinas.inundacion"));
	private static float nivel = -1.0F;
	private static float velocidad;
	private static long recibido;
	/** 0..1: entra y sale suave (para el dibujo). */
	private static float fuerza;
	private static Rugido rugido;

	private InundacionCliente() {
	}

	public static void recibir(EstadoInundacion e) {
		nivel = e.nivel();
		velocidad = e.velocidad();
		recibido = net.minecraft.util.Util.getMillis();
	}

	public static void olvidar() {
		nivel = -1.0F;
		velocidad = 0.0F;
		fuerza = 0.0F;
	}

	public static boolean activa() {
		return fuerza() > 0.001F;
	}

	/** La altura del agua ahora mismo (la de la cinematica del tobogan, si la hay: solo se ve). */
	public static float nivel() {
		float cine = net.backrooms.evento.cliente.cinematica.ToboganCliente.agua();
		if (cine >= 0) {
			return cine;
		}
		return nivelReal();
	}

	private static float nivelReal() {
		if (nivel < 0) {
			return Inundacion.BAJO;
		}
		float s = (net.minecraft.util.Util.getMillis() - recibido) / 1000.0F;
		return Mth.clamp(nivel + velocidad * Math.min(s, 1.0F), Inundacion.BAJO, Inundacion.MAXIMO);
	}

	/** La altura del agua que cuenta como agua de verdad (Inundacion.nivelCliente), o -1 sin inundacion. */
	public static double agua() {
		return nivel < 0 ? -1.0 : nivelReal();
	}

	public static float fuerza() {
		return net.backrooms.evento.cliente.cinematica.ToboganCliente.agua() >= 0 ? 1.0F : fuerza;
	}

	public static void tick(Minecraft mc) {
		fuerza = Mth.clamp(fuerza + (nivel >= 0 ? 0.05F : -0.05F), 0.0F, 1.0F);
		if (nivel >= 0 && mc.player != null && (rugido == null || rugido.isStopped())) {
			rugido = new Rugido();
			mc.getSoundManager().play(rugido);
		}
	}

	private static final class Rugido extends AbstractTickableSoundInstance {
		Rugido() {
			super(AGUA, SoundSource.AMBIENT, RandomSource.create());
			this.looping = true;
			this.delay = 0;
			this.volume = 0.0F;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
		}

		@Override
		public void tick() {
			Minecraft mc = Minecraft.getInstance();
			if (nivel < 0 || mc.player == null) {
				this.volume = Math.max(0.0F, this.volume - 0.03F);
				if (this.volume <= 0.0F) {
					this.stop();
				}
				return;
			}
			// mientras sube, fuerte; lleno, un rumor; debajo del agua, apagado y grave
			float sube = Mth.clamp(Math.abs(velocidad) * 6.0F, 0.25F, 1.0F);
			boolean dentro = mc.player.getEyeY() < nivel();
			// durante la alarma (antes de que entre el agua) aun no suena
			boolean seco = nivel() < Inundacion.BAJO + 0.02F && velocidad <= 0.0F;
			float quiero = seco ? 0.0F : dentro ? 0.35F : 0.8F * sube;
			this.volume += (quiero - this.volume) * 0.05F;
			this.pitch = dentro ? 0.6F : 1.0F;
		}
	}
}
