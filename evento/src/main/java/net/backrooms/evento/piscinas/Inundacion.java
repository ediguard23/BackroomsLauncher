package net.backrooms.evento.piscinas;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.ambiente.Ambiente;
import net.backrooms.evento.red.EstadoInundacion;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * La inundacion de las Piscinas: suena la alarma de la megafonia y el agua empieza a subir
 * por toda la planta baja hasta casi el techo de las salas bajas (MAXIMO, por encima de la
 * cabeza de quien este en el suelo); se queda un rato y luego baja. Solo se salva quien sube
 * a una planta alta (los caracoles y las terrazas: se pisa en PlanoPiscinas.PLANTA + 1) o a
 * lo alto de una escalera. Quien se queda debajo se queda sin aire y se ahoga.
 *
 * No se llena el mapa de bloques de agua (serian millones con todos repartidos): el agua es
 * una altura que el servidor lleva y manda (EstadoInundacion) y el cliente dibuja
 * (InundacionCliente, efectos.fsh). El aire y el ahogo los lleva esta clase.
 *
 * Sale sola cada CADA minutos (nunca con la alarma) mientras hay alguien en las Piscinas;
 * el staff la fuerza o la para con /backrooms inundacion.
 */
public final class Inundacion {
	private static final Inundacion INSTANCIA = new Inundacion();
	/** Altura del agua: desde el suelo seco hasta por encima de la cabeza en la planta baja. */
	public static final float BAJO = PlanoPiscinas.PISO - 0.05F;
	public static final float MAXIMO = PlanoPiscinas.PISO + 5.5F;
	/** Minutos entre inundaciones. */
	private static final int[] CADA = {14, 22};
	/** Segundos: aviso (la megafonia, sin agua aun), subida, lleno y bajada. */
	private static final float AVISO = 8.0F;
	private static final float SUBE = 80.0F;
	private static final float LLENO = 50.0F;
	private static final float BAJA = 40.0F;
	/** Aire como el de Minecraft (300 ticks, 15 s) y lo que se recupera fuera del agua. */
	private static final int AIRE = 300;

	public static Inundacion get() {
		return INSTANCIA;
	}

	private MinecraftServer servidor;
	private final Random azar = new Random();
	private long ticks;
	/** Ticks desde que empezo (-1 sin inundacion). */
	private long desde = -1;
	private float duracion = SUBE + LLENO + BAJA;
	private long proxima = -1;
	private final Map<UUID, Integer> aire = new HashMap<>();
	private boolean habia;

	private Inundacion() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> INSTANCIA.servidor = s);
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
		ServerPlayConnectionEvents.JOIN.register((h, e, s) -> INSTANCIA.enviar(h.player));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((j, de, a) -> INSTANCIA.enviar(j));
	}

	public boolean activa() {
		return this.desde >= 0;
	}

	/** La altura del agua ahora (o un valor bajo el suelo si no hay). */
	public float nivel() {
		if (this.desde < 0) {
			return -1.0F;
		}
		float t = (this.ticks - this.desde) / 20.0F - AVISO;
		float sube = this.duracion - LLENO - BAJA;
		if (t < 0) {
			return BAJO;
		}
		if (t < sube) {
			// sube deprisa al principio y despacio al final (como un deposito que se llena)
			float k = t / sube;
			return BAJO + (MAXIMO - BAJO) * (1.0F - (1.0F - k) * (1.0F - k) * 0.6F - (1.0F - k) * 0.4F);
		}
		if (t < sube + LLENO) {
			return MAXIMO;
		}
		float k = Math.min(1.0F, (t - sube - LLENO) / BAJA);
		return MAXIMO - (MAXIMO - BAJO) * k;
	}

	private float velocidad() {
		if (this.desde < 0) {
			return 0.0F;
		}
		float t = (this.ticks - this.desde) / 20.0F;
		float n0 = this.nivel();
		this.ticks += 10;
		float n1 = this.nivel();
		this.ticks -= 10;
		return t < AVISO ? 0.0F : (n1 - n0) * 2.0F;
	}

	/** Empieza ya (staff o sola); `segundos`: lo que tarda en subir. */
	public void empezar(float segundos) {
		ServerLevel nivel = this.nivelPiscinas();
		if (nivel == null) {
			return;
		}
		this.desde = this.ticks;
		this.duracion = Math.max(10.0F, segundos) + LLENO + BAJA;
		this.aire.clear();
		for (ServerPlayer j : nivel.players()) {
			Sonidos.aJugador(j, Sonidos.MEGAFONIA_ALERTA, 1.0F);
			j.displayClientMessage(Component.literal("¡INUNDACIÓN! Sube a una planta alta.").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
		}
		this.enviarATodos();
		BackroomsEvento.LOG.info("Inundacion en las Piscinas (sube en {} s)", segundos);
	}

	public void parar() {
		if (this.desde < 0) {
			return;
		}
		this.desde = -1;
		this.proxima = this.ticks + minutos();
		this.enviarATodos();
		for (ServerPlayer j : this.jugadores()) {
			j.setAirSupply(j.getMaxAirSupply());
		}
		this.aire.clear();
	}

	public String resumen() {
		if (this.desde >= 0) {
			return String.format(java.util.Locale.ROOT, "inundacion: agua a y=%.1f (%d s de %d)", this.nivel(), (this.ticks - this.desde) / 20,
				(int) (AVISO + this.duracion));
		}
		return this.proxima < 0 ? "sin inundacion" : "proxima inundacion en " + Math.max(0, this.proxima - this.ticks) / 20 + " s";
	}

	private long minutos() {
		return (long) ((CADA[0] + this.azar.nextDouble() * (CADA[1] - CADA[0])) * 60 * 20);
	}

	private ServerLevel nivelPiscinas() {
		return this.servidor == null ? null : this.servidor.getLevel(Piscinas.DIMENSION);
	}

	private Iterable<ServerPlayer> jugadores() {
		ServerLevel n = this.nivelPiscinas();
		return n == null ? java.util.List.of() : new java.util.ArrayList<>(n.players());
	}

	private void tick() {
		if (this.servidor == null) {
			return;
		}
		this.ticks++;
		ServerLevel nivel = this.nivelPiscinas();
		if (nivel == null || nivel.players().isEmpty()) {
			// sin nadie no corre el reloj
			if (this.desde >= 0) {
				this.desde = -1;
			}
			this.proxima = -1;
			return;
		}
		if (this.desde < 0) {
			if (this.proxima < 0) {
				this.proxima = this.ticks + minutos();
			}
			if (Ambiente.get().automatico() && this.ticks >= this.proxima) {
				if (Ambiente.get().alarma(nivel)) {
					this.proxima = this.ticks + 20 * 60;
				} else {
					this.empezar(SUBE);
				}
			}
			return;
		}
		if ((this.ticks - this.desde) / 20.0F > AVISO + this.duracion) {
			this.parar();
			return;
		}
		if (this.ticks % 10 == 0) {
			this.enviarATodos();
		}
		this.ahogar(nivel);
	}

	/** Quien tiene la cabeza bajo el agua se queda sin aire y, sin aire, se ahoga. */
	private void ahogar(ServerLevel nivel) {
		float agua = this.nivel();
		// sobre una copia: el que se ahoga puede salir de la lista
		for (ServerPlayer j : new java.util.ArrayList<>(nivel.players())) {
			if (j.isSpectator() || j.isCreative() || !j.isAlive()) {
				continue;
			}
			boolean cabeza = j.getEyeY() < agua;
			if (j.getY() < agua - 0.4F) {
				// andar con el agua por la cintura cuesta
				j.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25, 0, false, false, false));
			}
			int a = this.aire.getOrDefault(j.getUUID(), AIRE);
			if (cabeza) {
				a--;
				if (a <= -20) {
					a = 0;
					j.hurtServer(nivel, nivel.damageSources().drown(), 2.0F);
				}
			} else {
				a = Math.min(AIRE, a + 4);
			}
			this.aire.put(j.getUUID(), a);
			if (cabeza || a < AIRE) {
				j.setAirSupply(Math.max(a, 0));
			}
		}
	}

	private void enviarATodos() {
		for (ServerPlayer j : this.jugadores()) {
			this.enviar(j);
		}
		this.habia = this.desde >= 0;
	}

	public void enviar(ServerPlayer j) {
		boolean aqui = Piscinas.es(j.level());
		EstadoInundacion e = aqui && this.desde >= 0 ? new EstadoInundacion(this.nivel(), this.velocidad()) : EstadoInundacion.NADA;
		if (ServerPlayNetworking.canSend(j, EstadoInundacion.TYPE)) {
			ServerPlayNetworking.send(j, e);
		}
	}
}
