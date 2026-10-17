package net.backrooms.evento.piscinas;

import java.util.Random;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.ambiente.Ambiente;
import net.backrooms.evento.red.EstadoInundacion;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * La inundacion de las Piscinas, cada CADA minutos (o cuando el staff quiera):
 *
 *   0 s      alarma roja (las luces en rojo y la sirena, Ambiente) y la megafonia: fallo en el
 *            sistema de drenaje, busquen refugio en las plantas altas
 *   20 s     el agua empieza a subir por toda la planta baja; en SUBE segundos esta a MAXIMO
 *            (7 bloques sobre el suelo: tapa a quien se quede abajo; la planta alta, que se
 *            pisa en PlanoPiscinas.PLANTA + 1, queda seca)
 *   +40 s    lleno; la megafonia avisa de que la averia esta contenida, se apaga la alarma
 *            y el agua baja en BAJA segundos
 *
 * El agua no son bloques (serian millones): es una altura que el servidor lleva y manda
 * (EstadoInundacion) y que cuenta como agua de Minecraft para los jugadores
 * (InundacionAguaMixin: nadan, se ahogan, no se hacen dano al caer; InundacionCamaraMixin:
 * la niebla de debajo del agua). El cliente dibuja la superficie (efectos.fsh). Mientras
 * dura, las Serpientes no cazan (Serpiente): nadie puede correr dentro del agua.
 *
 * `/backrooms inundacion [segundos|parar]` la fuerza o la para.
 */
public final class Inundacion {
	private static final Inundacion INSTANCIA = new Inundacion();
	/** Altura del agua: desde el suelo seco hasta 7 bloques por encima. */
	public static final float BAJO = PlanoPiscinas.PISO - 0.05F;
	public static final float MAXIMO = PlanoPiscinas.PISO + 7.0F;
	/** Minutos entre inundaciones. */
	private static final int[] CADA = {10, 15};
	/** Segundos: alarma (sin agua aun), subida, lleno y bajada. */
	private static final float AVISO = 20.0F;
	private static final float SUBE = 40.0F;
	private static final float LLENO = 40.0F;
	private static final float BAJA = 40.0F;

	/** En el cliente, la altura del agua que dibuja (la pone InundacionCliente); -1 sin agua. */
	public static java.util.function.DoubleSupplier nivelCliente = () -> -1.0;

	public static Inundacion get() {
		return INSTANCIA;
	}

	private MinecraftServer servidor;
	private final Random azar = new Random();
	private long ticks;
	/** Ticks desde que empezo (-1 sin inundacion). */
	private long desde = -1;
	private float sube = SUBE;
	private long proxima = -1;
	private boolean avisoFin;

	private Inundacion() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> INSTANCIA.servidor = s);
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
		ServerPlayConnectionEvents.JOIN.register((h, e, s) -> INSTANCIA.enviar(h.player));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((j, de, a) -> INSTANCIA.enviar(j));
	}

	/**
	 * La altura del agua de la inundacion en ese nivel, o -1 si no hay (fuera de las Piscinas,
	 * sin inundacion o aun sonando la alarma). En el cliente, la que se dibuja.
	 */
	public static double nivelPara(Level nivel) {
		if (!Piscinas.es(nivel)) {
			return -1.0;
		}
		if (nivel.isClientSide()) {
			return nivelCliente.getAsDouble();
		}
		return INSTANCIA.activa() ? INSTANCIA.nivel() : -1.0;
	}

	public boolean activa() {
		return this.desde >= 0;
	}

	private float segundos() {
		return (this.ticks - this.desde) / 20.0F;
	}

	private float duracion() {
		return AVISO + this.sube + LLENO + BAJA;
	}

	/** La altura del agua ahora (BAJO durante la alarma). */
	public float nivel() {
		if (this.desde < 0) {
			return -1.0F;
		}
		return nivelEn(this.segundos());
	}

	private float nivelEn(float s) {
		float t = s - AVISO;
		if (t < 0) {
			return BAJO;
		}
		if (t < this.sube) {
			// sube deprisa al principio y mas despacio al final (como un deposito que se llena)
			float k = t / this.sube;
			return BAJO + (MAXIMO - BAJO) * (1.0F - (1.0F - k) * (1.0F - k) * 0.6F - (1.0F - k) * 0.4F);
		}
		if (t < this.sube + LLENO) {
			return MAXIMO;
		}
		float k = Math.min(1.0F, (t - this.sube - LLENO) / BAJA);
		return MAXIMO - (MAXIMO - BAJO) * k;
	}

	/** Bloques por segundo a los que sube (o baja) ahora: el cliente lo sigue moviendo entre paquetes. */
	private float velocidad() {
		if (this.desde < 0) {
			return 0.0F;
		}
		float s = this.segundos();
		return (nivelEn(s + 0.5F) - nivelEn(s)) * 2.0F;
	}

	/** Empieza ya: la alarma y, en AVISO segundos, el agua (que sube en `segundos`). */
	public void empezar(float segundos) {
		ServerLevel nivel = this.nivelPiscinas();
		if (nivel == null) {
			return;
		}
		this.desde = this.ticks;
		this.sube = Math.max(10.0F, segundos);
		this.avisoFin = false;
		// la alarma roja dura hasta que el agua empieza a bajar
		Ambiente.get().alarma(nivel, Math.round(AVISO + this.sube + LLENO));
		for (ServerPlayer j : nivel.players()) {
			Sonidos.aJugador(j, Sonidos.MEGAFONIA_INUNDACION, 1.0F);
			j.displayClientMessage(Component.literal("¡INUNDACIÓN! Busca una planta alta.").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
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
		ServerLevel nivel = this.nivelPiscinas();
		if (nivel != null && Ambiente.get().alarma(nivel)) {
			Ambiente.get().terminar(nivel);
		}
		this.enviarATodos();
	}

	public String resumen() {
		if (this.desde >= 0) {
			return String.format(java.util.Locale.ROOT, "inundacion: agua a y=%.1f (%d s de %d)", this.nivel(), (this.ticks - this.desde) / 20,
				(int) this.duracion());
		}
		return this.proxima < 0 ? "sin inundacion" : "proxima inundacion en " + Math.max(0, this.proxima - this.ticks) / 20 + " s";
	}

	private long minutos() {
		return (long) ((CADA[0] + this.azar.nextDouble() * (CADA[1] - CADA[0])) * 60 * 20);
	}

	private ServerLevel nivelPiscinas() {
		return this.servidor == null ? null : this.servidor.getLevel(Piscinas.DIMENSION);
	}

	private java.util.List<ServerPlayer> jugadores() {
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
			this.desde = -1;
			this.proxima = -1;
			return;
		}
		if (this.desde < 0) {
			if (this.proxima < 0) {
				this.proxima = this.ticks + minutos();
			}
			if (Ambiente.get().automatico() && this.ticks >= this.proxima) {
				this.empezar(SUBE);
			}
			return;
		}
		float s = this.segundos();
		if (!this.avisoFin && s >= AVISO + this.sube + LLENO) {
			// averia contenida: se va la alarma y el agua empieza a bajar
			this.avisoFin = true;
			for (ServerPlayer j : this.jugadores()) {
				Sonidos.aJugador(j, Sonidos.MEGAFONIA_DRENAJE, 1.0F);
			}
			if (Ambiente.get().alarma(nivel)) {
				Ambiente.get().terminar(nivel);
			}
		}
		if (s > this.duracion()) {
			this.parar();
			return;
		}
		if (this.ticks % 10 == 0) {
			this.enviarATodos();
		}
	}

	private void enviarATodos() {
		for (ServerPlayer j : this.jugadores()) {
			this.enviar(j);
		}
	}

	public void enviar(ServerPlayer j) {
		boolean aqui = Piscinas.es(j.level());
		EstadoInundacion e = aqui && this.desde >= 0 ? new EstadoInundacion(this.nivel(), this.velocidad()) : EstadoInundacion.NADA;
		if (ServerPlayNetworking.canSend(j, EstadoInundacion.TYPE)) {
			ServerPlayNetworking.send(j, e);
		}
	}
}
