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
import net.backrooms.evento.fase.Fase;
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
 * Cada jugador tiene sus misiones de la fase: recoger casetes y dos o tres
 * de grabar al azar (TipoMision.deGrabar). Al completarlas, el radar apunta
 * al ascensor de salida mas cercano (Fases).
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
		/** Fase en la que esta (1..4). */
		public int fase = 1;
		/** Segundos grabados de la mision de grabar en curso. */
		public float grabado;
		/** Ha muerto y esta fuera de la expedicion. */
		public boolean eliminado;
		/** Puesto con el que escapo (0 si no ha escapado). */
		public int escapado;
		/** Si en el ultimo segundo estaba grabando lo que pide su mision (no se guarda). */
		public transient boolean grabando;
	}

	/** Segundos de grabacion que pide cada mision de grabar. */
	public static float segundosGrabar(TipoMision m) {
		return switch (m) {
			case LUCES_ROJAS -> 5.0F;
			case ENTIDAD, ENTIDAD_ALARMA -> 4.0F;
			case SMILER -> 3.0F;
			default -> 0.0F;
		};
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
	 * Da sus misiones de la fase `f` a `jugador` (recoger casetes y las de
	 * grabar que toquen) y reparte sus casetes alrededor de `origen` (donde
	 * empieza la fase). Sustituye las que tuviera.
	 */
	public void asignar(ServerPlayer jugador, BlockPos origen, Fase f) {
		this.quitarCasetes(jugador.getUUID());
		Estado e = new Estado();
		e.fase = f.numero();
		e.misiones.add(TipoMision.CASETES);
		List<TipoMision> grabar = TipoMision.deGrabar(this.azar);
		java.util.Collections.shuffle(grabar, this.azar);
		for (int i = 0; i < Math.min(f.grabar(), grabar.size()); i++) {
			e.misiones.add(grabar.get(i));
		}
		ServerLevel nivel = jugador.level();
		if (nivel.getChunkSource().getGenerator() instanceof GeneradorNivel0 gen) {
			Plano p = gen.plano(nivel.getChunkSource().randomState());
			double base = this.azar.nextDouble() * Math.PI * 2;
			int n = f.casetes();
			for (int i = 0; i < n; i++) {
				// anillos cada vez mas lejos y repartidos alrededor: siempre hay uno cerca
				double ang = base + i * (Math.PI * 2 / n) + (this.azar.nextDouble() - 0.5) * 0.9;
				double dist = (60 + i * 55 + this.azar.nextDouble() * 40) * f.repartoCasetes();
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
		e.grabado = 0;
		this.sucio = true;
		jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5F, 0.8F);
		if (e.actual >= e.misiones.size()) {
			jugador.displayClientMessage(Component.literal("Misiones completadas. El radar ya marca el ascensor de salida: síguelo.").withStyle(ChatFormatting.GOLD), false);
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
			Fase f = Fase.de(j.level());
			if (f != null && f.numero() == e.fase && !e.eliminado && e.escapado == 0) {
				this.casetesCerca(j, e);
			}
			this.sincronizar(j);
			e.grabando = false;
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
		boolean salida = false;
		Fase f = Fase.de(j.level());
		int alcance = f == null ? ALCANCE_SENAL : f.alcanceSenal();
		if (f != null && e.actual >= e.misiones.size() && !e.eliminado && e.escapado == 0
			&& j.level().getChunkSource().getGenerator() instanceof GeneradorNivel0 gen) {
			// misiones hechas: el radar lleva al ascensor de salida mas cercano (sin limite de alcance)
			Plano.Ascensor a = gen.plano(j.level().getChunkSource().randomState()).ascensorCercano(j.getBlockX(), j.getBlockZ());
			double dx = a.centroX() + 0.5 - j.getX();
			double dz = a.centroZ() + 0.5 - j.getZ();
			distancia = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
			rumbo = (float) Math.toDegrees(Math.atan2(-dx, dz));
			salida = true;
		} else if (e.actual < e.misiones.size() && e.misiones.get(e.actual) == TipoMision.CASETES) {
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
			if (mejor <= alcance) {
				distancia = (int) Math.round(mejor);
			} else if (mejor < Double.MAX_VALUE) {
				distancia = -2; // buscando, pero sin senal
				rumbo = 0;
			}
		}
		List<String> nombres = e.misiones.stream().map(Enum::name).toList();
		float grabado = 0;
		if (e.actual < e.misiones.size()) {
			float seg = segundosGrabar(e.misiones.get(e.actual));
			grabado = seg > 0 ? Math.min(1.0F, e.grabado / seg) : 0;
		}
		ServerPlayNetworking.send(j, new SyncMisiones(nombres, e.actual, e.casetes, necesarios(e), distancia, rumbo,
			salida, grabado, e.grabando, f == null ? 0 : f.numero()));
	}

	/**
	 * Grabacion de la mision en curso: suma `segundos` si lo que graba es lo
	 * que pide (`tipo`) y la completa al llegar a lo necesario. La llama
	 * Grabacion cada pocos ticks.
	 */
	public void grabar(ServerPlayer j, TipoMision tipo, float segundos) {
		Estado e = this.estado(j);
		if (e == null || e.actual >= e.misiones.size() || e.misiones.get(e.actual) != tipo) {
			return;
		}
		e.grabado += segundos;
		e.grabando = true;
		this.sucio = true;
		if (e.grabado >= segundosGrabar(tipo)) {
			this.completar(j, tipo);
		}
	}

	/** La mision de grabar en curso, o null. */
	public TipoMision grabacionEnCurso(ServerPlayer j) {
		Estado e = this.estado(j);
		if (e == null || e.actual >= e.misiones.size()) {
			return null;
		}
		TipoMision m = e.misiones.get(e.actual);
		return segundosGrabar(m) > 0 ? m : null;
	}

	public void dejarDeGrabar(ServerPlayer j) {
		Estado e = this.estado(j);
		if (e != null) {
			e.grabando = false;
		}
	}

	/** Ha escapado por el ultimo ascensor con ese puesto. */
	public void escapado(ServerPlayer j, int puesto) {
		this.quitarCasetes(j.getUUID());
		Estado e = this.estado(j);
		if (e == null) {
			e = new Estado();
			this.estados.put(j.getUUID(), e);
		}
		e.escapado = puesto;
		e.pendientes.clear();
		this.sucio = true;
		this.sincronizar(j);
	}

	/** Ha muerto: fuera de la expedicion. */
	public void eliminar(ServerPlayer j) {
		this.quitarCasetes(j.getUUID());
		Estado e = this.estado(j);
		if (e != null) {
			e.eliminado = true;
			this.sucio = true;
			this.sincronizar(j);
		}
	}

	/** true si esta jugando una fase (ni eliminado ni escapado). */
	public boolean enExpedicion(ServerPlayer j) {
		Estado e = this.estado(j);
		return e != null && !e.eliminado && e.escapado == 0;
	}

	public void olvidar(ServerPlayer j) {
		this.quitarCasetes(j.getUUID());
		this.estados.remove(j.getUUID());
		this.sucio = true;
		this.sincronizar(j);
	}
}
