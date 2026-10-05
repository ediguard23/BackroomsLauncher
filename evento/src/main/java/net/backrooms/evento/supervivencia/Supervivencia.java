package net.backrooms.evento.supervivencia;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.ambiente.Ambiente;
import net.backrooms.evento.entidad.Bacteria;
import net.backrooms.evento.entidad.Smiler;
import net.backrooms.evento.fase.Fase;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.mundo.GeneradorNivel0;
import net.backrooms.evento.red.EstadoJugador;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.AABB;

/**
 * Cordura y estamina de los exploradores (el hambre es la del juego).
 *
 * Estamina (0-100): baja al correr y se recupera al dejar de correr. Si se
 * acaba, el jugador queda sin aliento y no puede correr hasta llegar al 35 %.
 *
 * Cordura (0-100 %): baja poco a poco y controlada (~50 min de 100 a 0 en la
 * fase 1, sin agua de almendras); mas deprisa a oscuras, en un apagon, con
 * un Smiler cerca y, sobre todo, si la Bacteria te persigue. Los sustos
 * (flashbang, golpes) la bajan de golpe. Solo el agua de almendras la sube.
 * Baja, el cliente oye cosas y ve alucinaciones; a 0, el jugador muere.
 *
 * Solo cuenta dentro de una fase y para quien esta en la expedicion; la
 * cordura se guarda con el jugador (vuelve a 100 al reaparecer).
 */
public final class Supervivencia {
	public static final AttachmentType<Float> CORDURA = AttachmentRegistry.create(BackroomsEvento.id("cordura"),
		b -> b.persistent(Codec.FLOAT).initializer(() -> 100.0F));
	public static final ResourceKey<DamageType> LOCURA = ResourceKey.create(Registries.DAMAGE_TYPE, BackroomsEvento.id("cordura"));

	/** Minutos de 100 a 0 sin nada que la acelere, en la fase 1. */
	private static final float MINUTOS_CORDURA = 50.0F;
	// ~23 s corriendo hasta agotarse y ~12 s en recargarse entera (a 20 ticks por segundo)
	private static final float GASTO_CORRER = 0.22F;
	private static final float RECARGA = 0.4F;
	private static final float RECUPERADO = 35.0F;

	private static final Supervivencia INSTANCIA = new Supervivencia();

	public static Supervivencia get() {
		return INSTANCIA;
	}

	private static final class Aliento {
		float estamina = 100.0F;
		boolean agotado;
		int sinCorrer;
		String enviado = "";
	}

	private final Map<UUID, Aliento> alientos = new HashMap<>();
	private MinecraftServer servidor;
	private long ticks;

	private Supervivencia() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> INSTANCIA.servidor = s);
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
		ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> INSTANCIA.alientos.remove(h.player.getUUID()));
	}

	private Aliento aliento(ServerPlayer j) {
		return this.alientos.computeIfAbsent(j.getUUID(), k -> new Aliento());
	}

	/** true si a este jugador le cuentan la cordura y la estamina. */
	public static boolean cuenta(ServerPlayer j) {
		return !j.isCreative() && !j.isSpectator() && Fase.de(j.level()) != null && Misiones.get().enExpedicion(j);
	}

	public float cordura(ServerPlayer j) {
		return j.getAttachedOrCreate(CORDURA);
	}

	public void cordura(ServerPlayer j, float valor) {
		j.setAttached(CORDURA, Math.max(0.0F, Math.min(100.0F, valor)));
	}

	/** Un susto: baja la cordura de golpe. */
	public void asustar(ServerPlayer j, float cuanto) {
		if (cuenta(j)) {
			this.cordura(j, this.cordura(j) - cuanto);
		}
	}

	/** El agua de almendras: lo unico que la sube. */
	public void beber(ServerPlayer j, float cuanto) {
		this.cordura(j, this.cordura(j) + cuanto);
		this.enviar(j, true);
	}

	public boolean agotado(ServerPlayer j) {
		return this.aliento(j).agotado;
	}

	private void tick() {
		if (this.servidor == null) {
			return;
		}
		this.ticks++;
		for (ServerPlayer j : this.servidor.getPlayerList().getPlayers()) {
			boolean cuenta = cuenta(j);
			Aliento a = this.aliento(j);
			if (cuenta) {
				this.estamina(j, a);
				if (this.ticks % 20 == 0) {
					this.locura(j);
				}
			} else {
				a.estamina = 100.0F;
				a.agotado = false;
			}
			if (this.ticks % 5 == 0) {
				this.enviar(j, false);
			}
		}
	}

	private void estamina(ServerPlayer j, Aliento a) {
		if (j.isSprinting()) {
			a.sinCorrer = 0;
			a.estamina = Math.max(0.0F, a.estamina - GASTO_CORRER);
			if (a.estamina <= 0.0F) {
				a.agotado = true;
			}
		} else if (++a.sinCorrer > 20) {
			a.estamina = Math.min(100.0F, a.estamina + RECARGA * (a.agotado ? 0.7F : 1.0F));
		}
		if (a.agotado) {
			j.setSprinting(false);
			if (a.estamina >= RECUPERADO) {
				a.agotado = false;
			}
		}
	}

	/** Cada segundo: cuanto baja la cordura ahora mismo. */
	private void locura(ServerPlayer j) {
		ServerLevel nivel = j.level();
		Fase f = Fase.de(nivel);
		if (f == null) {
			return;
		}
		float ritmo = 1.0F;
		if (Ambiente.get().apagon(nivel)) {
			ritmo *= 2.0F;
		} else if (nivel.getChunkSource().getGenerator() instanceof GeneradorNivel0 gen
			&& gen.plano(nivel.getChunkSource().randomState()).oscuridad(j.getBlockX(), j.getBlockZ()) == 2) {
			ritmo *= 1.5F;
		}
		AABB cerca = j.getBoundingBox().inflate(24);
		for (Bacteria b : nivel.getEntitiesOfClass(Bacteria.class, cerca)) {
			if (b.getTarget() == j) {
				ritmo *= 4.0F;
				break;
			}
		}
		if (!nivel.getEntitiesOfClass(Smiler.class, j.getBoundingBox().inflate(16)).isEmpty()) {
			ritmo *= 2.0F;
		}
		float porSegundo = 100.0F / (MINUTOS_CORDURA * 60.0F) * (float) f.cordura() * ritmo;
		float nueva = this.cordura(j) - porSegundo;
		this.cordura(j, nueva);
		if (nueva <= 0.0F && j.isAlive()) {
			DamageSource fuente = new DamageSource(nivel.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LOCURA));
			j.hurtServer(nivel, fuente, Float.MAX_VALUE);
		}
	}

	private void enviar(ServerPlayer j, boolean forzar) {
		if (!ServerPlayNetworking.canSend(j, EstadoJugador.TYPE)) {
			return;
		}
		Aliento a = this.aliento(j);
		boolean cuenta = cuenta(j);
		float cordura = this.cordura(j);
		String firma = Math.round(cordura * 10) + "," + Math.round(a.estamina) + "," + a.agotado + "," + cuenta;
		if (forzar || !firma.equals(a.enviado)) {
			a.enviado = firma;
			ServerPlayNetworking.send(j, new EstadoJugador(cordura, a.estamina, a.agotado, cuenta));
		}
	}
}
