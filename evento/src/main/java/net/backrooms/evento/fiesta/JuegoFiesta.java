package net.backrooms.evento.fiesta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.mision.Entidades;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.mision.TipoMision;
import net.backrooms.evento.red.EstadoFiesta;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Lo de jugar en la Fiesta, por jugador: monedas (abren tornos), tickets (el progreso: el Gran
 * Premio pide ConfigFiesta.objetivoTickets), las fichas que ya cogio y los tornos que ya pago.
 * Se guarda en <mundo>/backrooms/fiesta.json.
 *
 * Las fichas (PlanoFiesta.MONEDAS y TICKETS) estan en sitios fijos y cada uno coge cada una
 * una vez: se cogen al pasar por encima. Se ven como entidades (FichaEntidad, una por sitio y
 * para todos, puestas solo cerca de alguien, como los cadaveres); quien ya la cogio no la ve
 * (EstadoFiesta.cogidas).
 *
 * Los tornos (Torno) no se abren: al pagar (o al volver desde detras) se cruza al otro lado.
 * Quien canjea el Gran Premio entra en su sala; si se queda en el regalo gigante, sigue
 * adelante (de momento, como no hay mas niveles, escapa: Fases#escapar).
 */
public final class JuegoFiesta {
	private static final JuegoFiesta INSTANCIA = new JuegoFiesta();
	/** A esta distancia de una ficha se coge. */
	private static final double COGER = 1.35;
	/** Fichas puestas (entidades) a esta distancia de alguien; se quitan a QUITAR. */
	private static final double VER = 40.0;
	private static final double QUITAR = 56.0;
	/** Ticks dentro del regalo del Gran Premio hasta pasar. */
	private static final int REGALO = 40;

	public static JuegoFiesta get() {
		return INSTANCIA;
	}

	/** Lo que se guarda de cada uno. */
	public static final class Datos {
		public int monedas;
		public int tickets;
		public Set<Integer> cogidas = new HashSet<>();
		public Set<Integer> pagados = new HashSet<>();
		public int semaforo;
		public int memoria;
		public int regalos;
		/** Los parkours ya completados (su premio es solo la primera vez). */
		public Set<Integer> parkours = new HashSet<>();
		public boolean premio;
	}

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private Map<UUID, Datos> datos = new HashMap<>();
	private boolean sucio;
	private MinecraftServer servidor;
	private long ticks;
	private final Map<Integer, FichaEntidad> vivas = new HashMap<>();
	private final Map<UUID, Integer> enRegalo = new HashMap<>();
	private final Map<UUID, Long> ultimoTorno = new HashMap<>();
	/** Ticks (de 4 en 4) que lleva cada uno en la trampilla del final de un parkour. */
	private final Map<UUID, Integer> enTrampilla = new HashMap<>();

	private JuegoFiesta() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			INSTANCIA.servidor = s;
			INSTANCIA.cargar();
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(s -> INSTANCIA.guardar());
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			INSTANCIA.servidor = null;
			INSTANCIA.vivas.clear();
		});
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
		ServerPlayConnectionEvents.JOIN.register((h, e, s) -> INSTANCIA.enviar(h.player));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((j, de, a) -> INSTANCIA.enviar(j));
	}

	/* ---------------------------------------------------------- guardado */

	private Path archivo() {
		return this.servidor.getWorldPath(LevelResource.ROOT).resolve("backrooms").resolve("fiesta.json");
	}

	private void cargar() {
		this.datos = new HashMap<>();
		try {
			Path f = this.archivo();
			if (Files.exists(f)) {
				Map<UUID, Datos> d = this.gson.fromJson(Files.readString(f, StandardCharsets.UTF_8), new TypeToken<Map<UUID, Datos>>() { }.getType());
				if (d != null) {
					this.datos = d;
				}
			}
		} catch (Exception e) {
			BackroomsEvento.LOG.error("No se pudo leer el estado de la Fiesta", e);
		}
	}

	private void guardar() {
		if (this.servidor == null || !this.sucio) {
			return;
		}
		try {
			Path f = this.archivo();
			Files.createDirectories(f.getParent());
			Files.writeString(f, this.gson.toJson(this.datos), StandardCharsets.UTF_8);
			this.sucio = false;
		} catch (IOException e) {
			BackroomsEvento.LOG.error("No se pudo guardar el estado de la Fiesta", e);
		}
	}

	/* ----------------------------------------------------------- consultas */

	public Datos datos(ServerPlayer j) {
		return this.datos.computeIfAbsent(j.getUUID(), k -> new Datos());
	}

	public int tickets(ServerPlayer j) {
		Datos d = this.datos.get(j.getUUID());
		return d == null ? 0 : d.tickets;
	}

	/** Llega a la Fiesta por primera vez (o vuelve a empezarla): todo de cero. */
	public void llega(ServerPlayer j) {
		this.datos.put(j.getUUID(), new Datos());
		this.sucio = true;
	}

	/** Le olvida (staff, pruebas). */
	public void olvidar(ServerPlayer j) {
		this.datos.remove(j.getUUID());
		this.sucio = true;
		this.enviar(j);
	}

	/** Suma (o resta, con n < 0) tickets; avisa con el motivo y completa la mision al llegar. */
	public void sumarTickets(ServerPlayer j, int n, String motivo) {
		Datos d = this.datos(j);
		int antes = d.tickets;
		d.tickets = Math.max(0, d.tickets + n);
		this.sucio = true;
		if (motivo != null) {
			String signo = n >= 0 ? "+" : "";
			j.displayClientMessage(Component.literal(signo + n + " TICKETS · " + motivo).withStyle(n >= 0 ? ChatFormatting.YELLOW : ChatFormatting.RED), true);
		}
		int objetivo = ConfigFiesta.get().objetivoTickets;
		if (antes < objetivo && d.tickets >= objetivo) {
			Misiones.get().completar(j, TipoMision.TICKETS);
			Sonidos.aJugador(j, Sonidos.FIESTA_OBJETIVO, 1.0F);
			j.displayClientMessage(Component.literal("¡Tienes los " + objetivo + " tickets! El Gran Premio te espera al norte del salón.")
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
		}
		this.enviar(j);
		Misiones.get().sincronizar(j);
	}

	public void sumarMonedas(ServerPlayer j, int n) {
		Datos d = this.datos(j);
		d.monedas = Math.max(0, d.monedas + n);
		this.sucio = true;
		this.enviar(j);
	}

	/** La ficha sin coger mas cercana (x, z) para el radar, o null. */
	public int[] fichaCercana(ServerPlayer j) {
		Datos d = this.datos(j);
		double mejor = Double.MAX_VALUE;
		int[] r = null;
		for (List<PlanoFiesta.Punto> lista : List.of(PlanoFiesta.MONEDAS, PlanoFiesta.TICKETS)) {
			for (PlanoFiesta.Punto p : lista) {
				if (d.cogidas.contains(p.id())) {
					continue;
				}
				double dd = j.distanceToSqr(p.x(), p.y(), p.z());
				if (dd < mejor) {
					mejor = dd;
					r = new int[] {(int) Math.floor(p.x()), (int) Math.floor(p.z())};
				}
			}
		}
		return r;
	}

	/** La puerta del Gran Premio (x, z), para el radar. */
	public int[] puertaPremio() {
		for (PlanoFiesta.Puerta p : PlanoFiesta.TORNOS) {
			if (p.coste() == 0) {
				return new int[] {p.x(), p.z()};
			}
		}
		return null;
	}

	/* ---------------------------------------------------------------- tick */

	private void tick() {
		if (this.servidor == null) {
			return;
		}
		this.ticks++;
		if (this.ticks % 1200 == 0) {
			this.guardar();
		}
		ServerLevel nivel = Fiesta.nivel();
		if (nivel == null || this.ticks % 4 != 0) {
			return;
		}
		for (ServerPlayer j : new ArrayList<>(nivel.players())) {
			if (j.isSpectator() || !j.isAlive()) {
				continue;
			}
			this.coger(j);
			this.regalo(nivel, j);
			this.parkour(nivel, j);
		}
		if (this.ticks % 20 == 0) {
			this.fichas(nivel);
		}
	}

	/** Coge las fichas que tiene a los pies. */
	private void coger(ServerPlayer j) {
		Datos d = this.datos(j);
		for (PlanoFiesta.Punto p : PlanoFiesta.MONEDAS) {
			if (!d.cogidas.contains(p.id()) && cerca(j, p)) {
				d.cogidas.add(p.id());
				d.monedas++;
				this.sucio = true;
				Sonidos.aJugador(j, Sonidos.FIESTA_MONEDA, 0.9F);
				j.level().sendParticles(j, new DustParticleOptions(0xFFD23A, 1.2F), true, false, p.x(), p.y() + 0.4, p.z(), 10, 0.2, 0.2, 0.2, 0.0);
				j.displayClientMessage(Component.literal("+1 MONEDA (" + d.monedas + ")").withStyle(ChatFormatting.GOLD), true);
				this.enviar(j);
			}
		}
		for (PlanoFiesta.Punto p : PlanoFiesta.TICKETS) {
			if (!d.cogidas.contains(p.id()) && cerca(j, p)) {
				d.cogidas.add(p.id());
				this.sucio = true;
				Sonidos.aJugador(j, Sonidos.FIESTA_TICKETS, 0.9F);
				j.level().sendParticles(j, ParticleTypes.HAPPY_VILLAGER, true, false, p.x(), p.y() + 0.4, p.z(), 6, 0.25, 0.2, 0.25, 0.0);
				this.sumarTickets(j, p.valor(), "tira de tickets");
			}
		}
	}

	private static boolean cerca(ServerPlayer j, PlanoFiesta.Punto p) {
		double dx = j.getX() - p.x();
		double dz = j.getZ() - p.z();
		double dy = j.getY() - p.y();
		return dx * dx + dz * dz <= COGER * COGER && dy > -1.5 && dy < 1.2;
	}

	/** Pone las fichas que tiene alguien cerca y quita las que ya no. */
	private void fichas(ServerLevel nivel) {
		List<ServerPlayer> jugadores = nivel.players();
		for (int tipo = 0; tipo < 2; tipo++) {
			for (PlanoFiesta.Punto p : tipo == 0 ? PlanoFiesta.MONEDAS : PlanoFiesta.TICKETS) {
				boolean alguien = false;
				boolean lejos = true;
				for (ServerPlayer j : jugadores) {
					double d = j.distanceToSqr(p.x(), p.y(), p.z());
					alguien |= d < VER * VER;
					lejos &= d > QUITAR * QUITAR;
				}
				FichaEntidad f = this.vivas.get(p.id());
				if (f != null && (f.isRemoved() || nivel.getEntity(f.getId()) != f)) {
					f = null;
					this.vivas.remove(p.id());
				}
				if (f == null && alguien && nivel.isPositionEntityTicking(BlockPos.containing(p.x(), p.y(), p.z()))) {
					f = new FichaEntidad(Entidades.FICHA, nivel);
					f.preparar(p.id(), tipo, p.valor());
					f.setPos(p.x(), p.y(), p.z());
					nivel.addFreshEntity(f);
					this.vivas.put(p.id(), f);
				} else if (f != null && lejos) {
					f.discard();
					this.vivas.remove(p.id());
				}
			}
		}
	}

	/* --------------------------------------------------------------- tornos */

	/**
	 * Alguien usa un torno (la casilla de abajo en `pos`, mirando a `mira`). Desde delante se
	 * paga (o se ensenan los tickets, el del premio); desde detras se vuelve gratis.
	 */
	public void torno(ServerPlayer j, BlockPos pos, Direction mira, int coste) {
		Long antes = this.ultimoTorno.get(j.getUUID());
		if (antes != null && this.ticks - antes < 10) {
			return;
		}
		this.ultimoTorno.put(j.getUUID(), this.ticks);
		PlanoFiesta.Puerta puerta = PlanoFiesta.torno(pos.getX(), pos.getY(), pos.getZ());
		int id = puerta == null ? -1 : puerta.id();
		String nombre = puerta == null ? "esta puerta" : puerta.nombre();
		double lado = (j.getX() - (pos.getX() + 0.5)) * mira.getStepX() + (j.getZ() - (pos.getZ() + 0.5)) * mira.getStepZ();
		ServerLevel nivel = j.level();
		Datos d = this.datos(j);
		if (lado <= 0) {
			// desde detras: de vuelta
			this.cruzar(j, pos, mira);
			return;
		}
		if (coste == 0) {
			int objetivo = ConfigFiesta.get().objetivoTickets;
			if (d.tickets < objetivo) {
				nivel.playSound(null, pos, Sonidos.FIESTA_TORNO_NO, SoundSource.BLOCKS, 1.0F, 1.0F);
				j.displayClientMessage(Component.literal("GRAN PREMIO · Te faltan " + (objetivo - d.tickets) + " tickets (" + d.tickets + "/" + objetivo + ")")
					.withStyle(ChatFormatting.RED), true);
				return;
			}
			if (!d.premio) {
				d.premio = true;
				this.sucio = true;
				Sonidos.aJugador(j, Sonidos.FIESTA_PREMIO, 1.0F);
				j.displayClientMessage(Component.literal("¡Felicidades! Entra en el regalo.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
			}
			this.cruzar(j, pos, mira);
			return;
		}
		if (!d.pagados.contains(id)) {
			if (d.monedas < coste) {
				nivel.playSound(null, pos, Sonidos.FIESTA_TORNO_NO, SoundSource.BLOCKS, 1.0F, 1.0F);
				j.displayClientMessage(Component.literal("Para pasar a " + nombre + " hacen falta " + coste + (coste == 1 ? " moneda" : " monedas")
					+ " (tienes " + d.monedas + ")").withStyle(ChatFormatting.RED), true);
				return;
			}
			d.monedas -= coste;
			d.pagados.add(id);
			this.sucio = true;
			j.displayClientMessage(Component.literal("-" + coste + (coste == 1 ? " MONEDA" : " MONEDAS") + " · " + nombre).withStyle(ChatFormatting.GOLD), true);
			this.enviar(j);
		}
		this.cruzar(j, pos, mira);
	}

	/** Lo pasa al otro lado del torno (el contrario de donde esta), de pie junto a la hoja. */
	private void cruzar(ServerPlayer j, BlockPos pos, Direction mira) {
		double lado = (j.getX() - (pos.getX() + 0.5)) * mira.getStepX() + (j.getZ() - (pos.getZ() + 0.5)) * mira.getStepZ();
		Direction d = lado > 0 ? mira.getOpposite() : mira;
		double x = pos.getX() + 0.5 + d.getStepX() * 1.3;
		double z = pos.getZ() + 0.5 + d.getStepZ() * 1.3;
		j.teleportTo(j.level(), x, pos.getY(), z, Set.of(), j.getYRot(), j.getXRot(), true);
		j.level().playSound(null, pos, Sonidos.FIESTA_TORNO, SoundSource.BLOCKS, 0.9F, 0.95F + j.getRandom().nextFloat() * 0.1F);
	}

	/* ------------------------------------------------------------ parkour */

	/**
	 * En una zona de parkour: quien cae a la piscina de bolas lejos de donde se llega vuelve al
	 * principio; quien se queda en la trampilla del final sube (y la primera vez se lleva su premio).
	 */
	private void parkour(ServerLevel nivel, ServerPlayer j) {
		for (PlanoFiesta.Parkour p : PlanoFiesta.PARKOURS) {
			if (!p.dentro(j.getX(), j.getY(), j.getZ())) {
				continue;
			}
			double dl = Math.hypot(j.getX() - p.llegadaX(), j.getZ() - p.llegadaZ());
			if (j.getY() < PlanoFiesta.SUB + 1.9 && dl > 1.8 && j.onGround()) {
				nivel.playSound(null, j.blockPosition(), Sonidos.PARKOUR_FALLO, SoundSource.PLAYERS, 1.0F, 1.0F);
				j.teleportTo(nivel, p.llegadaX(), PlanoFiesta.SUB + 2, p.llegadaZ(), Set.of(), j.getYRot(), 0.0F, true);
				j.displayClientMessage(Component.literal("¡Al agua de bolas! Otra vez desde el principio.").withStyle(ChatFormatting.GRAY), true);
				return;
			}
			if (p.enFin(j.getX(), j.getZ()) && j.getY() >= PlanoFiesta.SUB + 3.9) {
				int n = this.enTrampilla.merge(j.getUUID(), 4, Integer::sum);
				if (n == 4) {
					j.displayClientMessage(Component.literal("La trampilla vibra...").withStyle(ChatFormatting.YELLOW), true);
				}
				if (n >= 24) {
					this.enTrampilla.remove(j.getUUID());
					nivel.playSound(null, j.blockPosition(), Sonidos.PARKOUR_SUBE, SoundSource.PLAYERS, 1.2F, 1.0F);
					j.teleportTo(nivel, p.salidaX(), PlanoFiesta.SUELO + 1, p.salidaZ(), Set.of(), p.salidaGiro(), 0.0F, true);
					Datos d = this.datos(j);
					if (d.parkours.add(p.id())) {
						this.sumarTickets(j, p.premio(), "¡atajo secreto!");
					}
				}
			} else {
				this.enTrampilla.remove(j.getUUID());
			}
			return;
		}
	}

	/* --------------------------------------------------------- el regalo */

	/** Quien se queda dentro del regalo del Gran Premio (con los tickets) sigue adelante. */
	private void regalo(ServerLevel nivel, ServerPlayer j) {
		boolean dentro = j.getX() >= PlanoFiesta.PREMIO_X0 && j.getX() < PlanoFiesta.PREMIO_X1 + 1
			&& j.getZ() >= PlanoFiesta.PREMIO_Z0 && j.getZ() < PlanoFiesta.PREMIO_Z1 + 1 && j.getY() < PlanoFiesta.SUELO + 3;
		if (!dentro) {
			this.enRegalo.remove(j.getUUID());
			return;
		}
		Datos d = this.datos(j);
		if (d.tickets < ConfigFiesta.get().objetivoTickets) {
			return;
		}
		int n = this.enRegalo.merge(j.getUUID(), 4, Integer::sum);
		if (n == 4) {
			nivel.playSound(null, j.blockPosition(), Sonidos.FIESTA_REGALO, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
		if (n >= REGALO) {
			this.enRegalo.remove(j.getUUID());
			Misiones.get().completar(j, TipoMision.PREMIO);
			BackroomsEvento.LOG.info("{} canjea el Gran Premio de la Fiesta", j.getGameProfile().name());
			// aqui empezara el siguiente tramo; mientras no exista, el Gran Premio es escapar
			net.backrooms.evento.fase.Fases.get().escapar(j);
		}
	}

	/* ---------------------------------------------------------------- envio */

	public void enviar(ServerPlayer j) {
		if (!ServerPlayNetworking.canSend(j, EstadoFiesta.TYPE)) {
			return;
		}
		if (!Fiesta.es(j.level())) {
			ServerPlayNetworking.send(j, EstadoFiesta.FUERA);
			return;
		}
		Datos d = this.datos(j);
		int[] cogidas = d.cogidas.stream().mapToInt(Integer::intValue).toArray();
		ServerPlayNetworking.send(j, new EstadoFiesta(false, d.monedas, d.tickets, ConfigFiesta.get().objetivoTickets, cogidas,
			JuegoSemaforo.get().jugando(j) ? 1 : 0));
	}
}
