package net.backrooms.evento.mision;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.mundo.GeneradorNivel0;
import net.backrooms.evento.mundo.Plano;
import net.backrooms.evento.red.SyncMisiones;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Las misiones de cada explorador y sus casetes.
 *
 * Cada jugador tiene 3 misiones: recoger 10 casetes y dos de grabar al azar
 * (TipoMision.deGrabar).
 * Sus casetes son posiciones fijas repartidas en anillos alrededor de donde
 * empezo (de ~60 a ~600 bloques), siempre en suelo libre: con 200 jugadores
 * repartidos por un mapa de 10k x 10k todos tienen los suyos y nadie puede
 * quitarle los suyos a otro. Es dificil a proposito (el organizador quiere
 * que acaben pocos): la senal solo aparece a menos de ALCANCE_SENAL bloques.
 *
 * Las entidades de los casetes solo existen mientras su dueno esta a menos de
 * RADIO_VISTA bloques: nunca hay cientos cargadas a la vez. El estado se
 * guarda en <mundo>/backrooms/misiones.json.
 */
public final class Misiones {
	public static final int CASETES = 10;
	/** La senal del casete solo llega de cerca: mas alla no hay pista. */
	public static final int ALCANCE_SENAL = 70;
	private static final int RADIO_VISTA = 48;
	private static final int RADIO_QUITAR = 96;

	private static final Misiones INSTANCIA = new Misiones();

	public static Misiones get() {
		return INSTANCIA;
	}

	/** Estado guardado de un jugador. */
	public static final class Estado {
		public List<TipoMision> misiones = new ArrayList<>();
		public int actual;
		public int casetes;
		public List<int[]> pendientes = new ArrayList<>(); // {x, z, recogido 0/1}
	}

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final Map<UUID, Estado> estados = new HashMap<>();
	/** Casetes ya creados en el mundo: "uuid#indice" -> entidad. */
	private final Map<String, CaseteEntidad> vivos = new HashMap<>();
	private final Random azar = new Random();
	private MinecraftServer servidor;
	private boolean sucio;
	private int ticks;

	private Misiones() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> INSTANCIA.cargar(s));
		ServerLifecycleEvents.SERVER_STOPPING.register(s -> INSTANCIA.guardar());
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
	}

	/* ------------------------------------------------------ guardado */

	private Path archivo() {
		return this.servidor.getWorldPath(LevelResource.ROOT).resolve("backrooms").resolve("misiones.json");
	}

	private void cargar(MinecraftServer s) {
		this.servidor = s;
		this.estados.clear();
		try {
			Path f = this.archivo();
			if (Files.exists(f)) {
				Map<UUID, Estado> leidos = this.gson.fromJson(Files.readString(f, StandardCharsets.UTF_8),
					new TypeToken<Map<UUID, Estado>>() { }.getType());
				if (leidos != null) {
					this.estados.putAll(leidos);
				}
			}
			BackroomsEvento.LOG.info("Misiones cargadas: {} exploradores", this.estados.size());
		} catch (Exception e) {
			BackroomsEvento.LOG.error("No se pudieron leer las misiones", e);
		}
	}

	public void guardar() {
		if (this.servidor == null) {
			return;
		}
		try {
			Path f = this.archivo();
			Files.createDirectories(f.getParent());
			Files.writeString(f, this.gson.toJson(this.estados), StandardCharsets.UTF_8);
			this.sucio = false;
		} catch (IOException e) {
			BackroomsEvento.LOG.error("No se pudieron guardar las misiones", e);
		}
	}

	/* ----------------------------------------------------- asignar */

	public Estado estado(ServerPlayer jugador) {
		return this.estados.get(jugador.getUUID());
	}

	/**
	 * Da sus 3 misiones a `jugador` y reparte sus casetes alrededor de
	 * `origen` (donde empieza tras el /start). Sustituye las que tuviera.
	 */
	public void asignar(ServerPlayer jugador, BlockPos origen) {
		this.quitarCasetes(jugador.getUUID());
		Estado e = new Estado();
		e.misiones.add(TipoMision.CASETES);
		List<TipoMision> grabar = TipoMision.deGrabar(this.azar);
		java.util.Collections.shuffle(grabar, this.azar);
		e.misiones.add(grabar.get(0));
		e.misiones.add(grabar.get(1));
		ServerLevel nivel = jugador.level();
		if (nivel.getChunkSource().getGenerator() instanceof GeneradorNivel0 gen) {
			Plano p = gen.plano(nivel.getChunkSource().randomState());
			double base = this.azar.nextDouble() * Math.PI * 2;
			for (int i = 0; i < CASETES; i++) {
				// anillos cada vez mas lejos y repartidos alrededor: siempre hay uno cerca
				double ang = base + i * (Math.PI * 2 / CASETES) + (this.azar.nextDouble() - 0.5) * 0.9;
				double dist = 60 + i * 55 + this.azar.nextDouble() * 40;
				int x = origen.getX() + (int) Math.round(Math.cos(ang) * dist);
				int z = origen.getZ() + (int) Math.round(Math.sin(ang) * dist);
				int[] libre = sueloLibre(p, x, z);
				e.pendientes.add(new int[] {libre[0], libre[1], 0});
			}
		}
		this.estados.put(jugador.getUUID(), e);
		this.sucio = true;
		this.sincronizar(jugador);
	}

	/** Columna sin pared ni decoracion mas cercana a (x, z). */
	private static int[] sueloLibre(Plano p, int x, int z) {
		for (int r = 0; r <= Plano.G * 2; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) == r && !p.pared(x + dx, z + dz)
						&& p.decoracion(x + dx, z + dz).tipo() == Plano.D_NADA) {
						return new int[] {x + dx, z + dz};
					}
				}
			}
		}
		return new int[] {x, z};
	}

	/* --------------------------------------------------- progreso */

	public void recoger(ServerPlayer jugador, CaseteEntidad casete) {
		Estado e = this.estado(jugador);
		int i = casete.indice();
		casete.discard();
		this.vivos.remove(jugador.getUUID() + "#" + i);
		if (e == null || i < 0 || i >= e.pendientes.size() || e.pendientes.get(i)[2] == 1) {
			return;
		}
		e.pendientes.get(i)[2] = 1;
		e.casetes++;
		this.sucio = true;
		jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 0.7F);
		jugador.displayClientMessage(Component.literal("CASETE " + e.casetes + "/" + necesarios(e)).withStyle(ChatFormatting.YELLOW), true);
		if (e.actual < e.misiones.size() && e.misiones.get(e.actual) == TipoMision.CASETES && e.casetes >= necesarios(e)) {
			this.completar(jugador, TipoMision.CASETES);
		}
		this.sincronizar(jugador);
	}

	/** Casetes que le tocan (los de su reparto: una partida vieja puede tener otro numero). */
	private static int necesarios(Estado e) {
		return e.pendientes.isEmpty() ? CASETES : e.pendientes.size();
	}

	/** Marca hecha la mision en curso si es de ese tipo. Devuelve true si avanzo. */
	public boolean completar(ServerPlayer jugador, TipoMision tipo) {
		Estado e = this.estado(jugador);
		if (e == null || e.actual >= e.misiones.size() || e.misiones.get(e.actual) != tipo) {
			return false;
		}
		e.actual++;
		this.sucio = true;
		jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5F, 0.8F);
		if (e.actual >= e.misiones.size()) {
			jugador.displayClientMessage(Component.literal("Has completado tus misiones. La cinta sigue grabando...").withStyle(ChatFormatting.GOLD), false);
		} else {
			TipoMision sig = e.misiones.get(e.actual);
			jugador.displayClientMessage(Component.literal("Misión completada. Siguiente: " + sig.titulo).withStyle(ChatFormatting.YELLOW), false);
		}
		this.sincronizar(jugador);
		return true;
	}

	/* --------------------------------------------- casetes y envio */

	private void tick() {
		if (this.servidor == null || ++this.ticks % 20 != 0) {
			return;
		}
		for (ServerPlayer j : this.servidor.getPlayerList().getPlayers()) {
			Estado e = this.estado(j);
			if (e == null) {
				continue;
			}
			this.casetesCerca(j, e);
			this.sincronizar(j);
		}
		if (this.sucio && this.ticks % 1200 == 0) {
			this.guardar();
		}
	}

	/** Crea los casetes pendientes cerca del jugador y quita los que dejo atras. */
	private void casetesCerca(ServerPlayer j, Estado e) {
		ServerLevel nivel = j.level();
		for (int i = 0; i < e.pendientes.size(); i++) {
			int[] c = e.pendientes.get(i);
			String clave = j.getUUID() + "#" + i;
			CaseteEntidad vivo = this.vivos.get(clave);
			if (vivo != null && (vivo.isRemoved() || vivo.level() != nivel)) {
				this.vivos.remove(clave);
				vivo = null;
			}
			double dx = c[0] + 0.5 - j.getX();
			double dz = c[1] + 0.5 - j.getZ();
			double d2 = dx * dx + dz * dz;
			if (c[2] == 1) {
				if (vivo != null) {
					vivo.discard();
					this.vivos.remove(clave);
				}
				continue;
			}
			if (vivo == null && d2 < RADIO_VISTA * RADIO_VISTA) {
				CaseteEntidad nuevo = new CaseteEntidad(Entidades.CASETE, nivel);
				nuevo.preparar(j.getUUID(), i, (float) (Math.floorMod(c[0] * 31 + c[1] * 17, 360)));
				nuevo.setPos(c[0] + 0.5, GeneradorNivel0.SUELO + 1.0, c[1] + 0.5);
				nivel.addFreshEntity(nuevo);
				this.vivos.put(clave, nuevo);
			} else if (vivo != null && d2 > RADIO_QUITAR * RADIO_QUITAR) {
				vivo.discard();
				this.vivos.remove(clave);
			}
		}
	}

	private void quitarCasetes(UUID jugador) {
		this.vivos.entrySet().removeIf(en -> {
			if (en.getKey().startsWith(jugador + "#")) {
				en.getValue().discard();
				return true;
			}
			return false;
		});
	}

	/** Manda al cliente su estado (y la senal del casete mas cercano). */
	public void sincronizar(ServerPlayer j) {
		Estado e = this.estado(j);
		if (e == null) {
			ServerPlayNetworking.send(j, SyncMisiones.VACIO);
			return;
		}
		int distancia = -1;
		float rumbo = 0;
		if (e.actual < e.misiones.size() && e.misiones.get(e.actual) == TipoMision.CASETES) {
			double mejor = Double.MAX_VALUE;
			for (int[] c : e.pendientes) {
				if (c[2] == 1) {
					continue;
				}
				double dx = c[0] + 0.5 - j.getX();
				double dz = c[1] + 0.5 - j.getZ();
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d < mejor) {
					mejor = d;
					rumbo = (float) Math.toDegrees(Math.atan2(-dx, dz));
				}
			}
			if (mejor <= ALCANCE_SENAL) {
				distancia = (int) Math.round(mejor);
			} else if (mejor < Double.MAX_VALUE) {
				distancia = -2; // buscando, pero sin senal
				rumbo = 0;
			}
		}
		List<String> nombres = e.misiones.stream().map(Enum::name).toList();
		ServerPlayNetworking.send(j, new SyncMisiones(nombres, e.actual, e.casetes, necesarios(e), distancia, rumbo));
	}

	public void olvidar(ServerPlayer j) {
		this.quitarCasetes(j.getUUID());
		this.estados.remove(j.getUUID());
		this.sucio = true;
		this.sincronizar(j);
	}
}
