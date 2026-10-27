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
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Lo de jugar en la Fiesta, por jugador: monedas (abren puertas y el localizador), tickets (el
 * progreso: la puerta del final pide ConfigFiesta.objetivoTickets), las fichas que ya cogio, las
 * puertas que ya pago y su vuelta. Se guarda en <mundo>/backrooms/fiesta.json.
 *
 * Una VUELTA empieza en una habitacion de inicio (al llegar, al caer por un noclip de la sala
 * central o al reaparecer): las monedas de las mesas vuelven a estar y las puertas se vuelven a
 * pagar. Se acaba bajando por el tobogan del castillo, que da el BOTE: ConfigFiesta.jackpotInicial
 * menos jackpotPerdidaMinuto por cada minuto de vuelta (nunca menos de jackpotMinimo). Las tiras de
 * tickets, en cambio, se cogen una vez y ya.
 *
 * Las fichas (PlanoFiesta.MONEDAS y TICKETS) estan en sitios fijos y se cogen al pasar por encima.
 * Se ven como entidades (FichaEntidad, una por sitio y para todos, puestas solo cerca de alguien);
 * quien ya la cogio no la ve (EstadoFiesta.cogidas).
 *
 * Las puertas (Torno) no se abren: al pagar (o al volver desde detras) se cruza al otro lado.
 */
public final class JuegoFiesta {
	private static final JuegoFiesta INSTANCIA = new JuegoFiesta();
	/** Fichas puestas (entidades) a esta distancia de alguien; se quitan a QUITAR. */
	private static final double VER = 40.0;
	private static final double QUITAR = 56.0;

	public static JuegoFiesta get() {
		return INSTANCIA;
	}

	/** Lo que se guarda de cada uno. */
	public static final class Datos {
		public int monedas;
		public int tickets;
		public Set<Integer> cogidas = new HashSet<>();
		public Set<Integer> pagados = new HashSet<>();
		public int memoria;
		public int regalos;
		/** Vueltas empezadas y acabadas, cuando empezo la de ahora (tiempo del nivel) y si ya cobro su bote. */
		public int vuelta;
		public int vueltas;
		public long vueltaDesde;
		public boolean cobrada;
		public boolean premio;
	}

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private Map<UUID, Datos> datos = new HashMap<>();
	private boolean sucio;
	private MinecraftServer servidor;
	private long ticks;
	private final Map<Integer, FichaEntidad> vivas = new HashMap<>();
	private final Map<UUID, Integer> enTobogan = new HashMap<>();
	private final Map<UUID, Long> ultimoTorno = new HashMap<>();
	/** Rastros del localizador que quedan por ensenar (jugador -> veces). */
	private final Map<UUID, Integer> rastros = new HashMap<>();

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

	/**
	 * Empieza otra vuelta: las monedas de las mesas vuelven a estar para el, las puertas se vuelven a
	 * pagar y el bote empieza de nuevo. Las monedas que lleve encima se las queda.
	 */
	public void nuevaVuelta(ServerPlayer j) {
		Datos d = this.datos(j);
		for (PlanoFiesta.Punto p : PlanoFiesta.MONEDAS) {
			d.cogidas.remove(p.id());
		}
		d.pagados.clear();
		d.vuelta++;
		d.cobrada = false;
		ServerLevel nivel = Fiesta.nivel();
		d.vueltaDesde = nivel == null ? 0 : nivel.getGameTime();
		this.sucio = true;
		this.enviar(j);
	}

	/** El bote que le daria la vuelta si la acabara ahora. */
	public int bote(ServerPlayer j) {
		Datos d = this.datos(j);
		ConfigFiesta c = ConfigFiesta.get();
		ServerLevel nivel = Fiesta.nivel();
		long t = nivel == null ? 0 : Math.max(0, nivel.getGameTime() - d.vueltaDesde);
		return bote(c.jackpotInicial, c.jackpotMinimo, c.jackpotPerdidaMinuto, t);
	}

	/** El bote tras `ticks` de vuelta (lo mismo en el cliente, MarcadorRenderer). */
	public static int bote(int inicial, int minimo, int porMinuto, long ticks) {
		return Math.max(minimo, inicial - (int) (ticks * porMinuto / 1200L));
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
			j.displayClientMessage(Component.literal("¡Tienes los " + objetivo + " tickets! La puerta del norte de la sala central ya se abre para ti.")
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

	/** La moneda o tira de tickets sin coger mas cercana (x, z) para el radar, o null. */
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

	/** La puerta del final (x, z), para el radar. */
	public int[] puertaPremio() {
		return new int[] {PlanoFiesta.PUERTA_FINAL_X, PlanoFiesta.PUERTA_FINAL_Z};
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
			this.saltos(nivel, j);
			this.rastro(nivel, j);
		}
		if (this.ticks % 20 == 0) {
			this.fichas(nivel);
		}
	}

	/**
	 * Clic derecho en una moneda (FichaEntidad): si no la tiene ya y esta al alcance, se la queda.
	 * Las monedas no se cogen al pasar: hay que buscarlas en las mesas.
	 */
	public void cogerFicha(ServerPlayer j, FichaEntidad f) {
		Datos d = this.datos(j);
		int id = f.id();
		if (d.cogidas.contains(id) || j.distanceToSqr(f) > 4.5 * 4.5 || !Fiesta.es(j.level()) || j.isSpectator()) {
			return;
		}
		d.cogidas.add(id);
		this.sucio = true;
		if (f.tipo() == 0) {
			d.monedas++;
			Sonidos.aJugador(j, Sonidos.FIESTA_MONEDA, 0.9F);
			j.level().sendParticles(j, new DustParticleOptions(0xFFD23A, 1.2F), true, false, f.getX(), f.getY() + 0.2, f.getZ(), 10, 0.2, 0.1, 0.2, 0.0);
			j.displayClientMessage(Component.literal("+1 MONEDA (" + d.monedas + ")").withStyle(ChatFormatting.GOLD), true);
			this.enviar(j);
		} else {
			Sonidos.aJugador(j, Sonidos.FIESTA_TICKETS, 0.9F);
			this.sumarTickets(j, f.valor(), "tira de tickets");
		}
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

	/* --------------------------------------------------------------- puertas */

	/**
	 * Alguien usa una puerta (la casilla de abajo en `pos`, mirando a `mira`). Desde delante se
	 * paga (o se ensenan los tickets, la del final); desde detras se vuelve gratis.
	 */
	public void torno(ServerPlayer j, BlockPos pos, Direction mira, int coste, int tipo) {
		Long antes = this.ultimoTorno.get(j.getUUID());
		if (antes != null && this.ticks - antes < 10) {
			return;
		}
		this.ultimoTorno.put(j.getUUID(), this.ticks);
		PlanoFiesta.Puerta puerta = PlanoFiesta.puerta(pos.getX(), pos.getY(), pos.getZ());
		int id = puerta == null ? -1 : puerta.id();
		String nombre = puerta == null ? "esta puerta" : puerta.nombre();
		double lado = (j.getX() - (pos.getX() + 0.5)) * mira.getStepX() + (j.getZ() - (pos.getZ() + 0.5)) * mira.getStepZ();
		ServerLevel nivel = j.level();
		Datos d = this.datos(j);
		if (lado <= 0) {
			if (tipo == 2) {
				// la puerta del final no tiene vuelta atras
				nivel.playSound(null, pos, Sonidos.FIESTA_TORNO_NO, SoundSource.BLOCKS, 1.0F, 0.8F);
				j.displayClientMessage(Component.literal("No hay vuelta atrás.").withStyle(ChatFormatting.DARK_RED), true);
				return;
			}
			// desde detras: de vuelta
			this.cruzar(j, pos, mira);
			return;
		}
		if (tipo == 2) {
			int objetivo = ConfigFiesta.get().objetivoTickets;
			if (d.tickets < objetivo) {
				nivel.playSound(null, pos, Sonidos.FIESTA_TORNO_NO, SoundSource.BLOCKS, 1.0F, 1.0F);
				j.displayClientMessage(Component.literal("Esta puerta pide " + objetivo + " tickets. Te faltan " + (objetivo - d.tickets) + ".")
					.withStyle(ChatFormatting.RED), true);
				return;
			}
			if (!d.premio) {
				d.premio = true;
				this.sucio = true;
				Sonidos.aJugador(j, Sonidos.FIESTA_PREMIO, 1.0F);
				j.displayClientMessage(Component.literal("¡Felicidades! La fiesta te espera.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
			}
			this.cruzar(j, pos, mira);
			return;
		}
		if (tipo == 1) {
			this.cruzar(j, pos, mira);
			j.displayClientMessage(Component.literal("Al fondo del pasillo, el castillo de bolas. Muévete solo con la luz verde.")
				.withStyle(ChatFormatting.LIGHT_PURPLE), true);
			return;
		}
		if (coste > 0 && !d.pagados.contains(id)) {
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

	/** Lo pasa al otro lado de la puerta (el contrario de donde esta), de pie junto a la hoja. */
	private void cruzar(ServerPlayer j, BlockPos pos, Direction mira) {
		double lado = (j.getX() - (pos.getX() + 0.5)) * mira.getStepX() + (j.getZ() - (pos.getZ() + 0.5)) * mira.getStepZ();
		Direction d = lado > 0 ? mira.getOpposite() : mira;
		double x = pos.getX() + 0.5 + d.getStepX() * 1.3;
		double z = pos.getZ() + 0.5 + d.getStepZ() * 1.3;
		float giro = j.getYRot();
		// las puertas que unen zonas lejanas llevan a su pareja (PlanoFiesta.destino)
		double[] lejos = PlanoFiesta.destino(pos.getX(), pos.getZ(), d);
		if (lejos != null) {
			x = lejos[0];
			z = lejos[1];
			giro = (float) lejos[2];
		}
		j.teleportTo(j.level(), x, pos.getY(), z, Set.of(), giro, j.getXRot(), true);
		j.level().playSound(null, pos, Sonidos.FIESTA_TORNO, SoundSource.BLOCKS, 0.9F, 0.95F + j.getRandom().nextFloat() * 0.1F);
	}

	/* ----------------------------------------------------------- localizador */

	/**
	 * El localizador de una sala segura: por ConfigFiesta.localizadorCoste monedas, la puerta de la
	 * salida suena solo para quien lo pulsa (desde donde esta la puerta, lejos y a traves de las
	 * paredes) y unas chispas suben de ella un momento.
	 */
	public void localizador(ServerPlayer j, BlockPos pos) {
		Long antes = this.ultimoTorno.get(j.getUUID());
		if (antes != null && this.ticks - antes < 20) {
			return;
		}
		this.ultimoTorno.put(j.getUUID(), this.ticks);
		Datos d = this.datos(j);
		int coste = ConfigFiesta.get().localizadorCoste;
		if (d.monedas < coste) {
			j.level().playSound(null, pos, Sonidos.FIESTA_TORNO_NO, SoundSource.BLOCKS, 1.0F, 1.0F);
			j.displayClientMessage(Component.literal("LOCALIZADOR · Hacen falta " + coste + " monedas (tienes " + d.monedas + ")").withStyle(ChatFormatting.RED), true);
			return;
		}
		d.monedas -= coste;
		this.sucio = true;
		this.enviar(j);
		j.level().playSound(null, pos, Sonidos.FIESTA_MONEDA, SoundSource.BLOCKS, 1.0F, 0.8F);
		j.connection.send(new ClientboundSoundPacket(net.minecraft.core.Holder.direct(Sonidos.FIESTA_LOCALIZADOR), SoundSource.BLOCKS,
			PlanoFiesta.SALIDA_X + 0.5, PlanoFiesta.SUELO + 2.0, PlanoFiesta.SALIDA_Z + 0.5, 6.0F, 1.0F, j.getRandom().nextLong()));
		this.rastros.put(j.getUUID(), 10);
		j.displayClientMessage(Component.literal("-" + coste + " MONEDAS · Escucha: la salida está sonando. Ve hacia el sonido.").withStyle(ChatFormatting.AQUA), true);
	}

	/** Las chispas del localizador sobre la puerta de la salida, solo para quien lo pulso. */
	private void rastro(ServerLevel nivel, ServerPlayer j) {
		Integer n = this.rastros.get(j.getUUID());
		if (n == null) {
			return;
		}
		if (n <= 0) {
			this.rastros.remove(j.getUUID());
			return;
		}
		this.rastros.put(j.getUUID(), n - 1);
		nivel.sendParticles(j, ParticleTypes.END_ROD, true, true, PlanoFiesta.SALIDA_X - 0.5, PlanoFiesta.SUELO + 2.0, PlanoFiesta.SALIDA_Z + 0.5,
			8, 0.2, 2.5, 0.2, 0.02);
	}

	/* ------------------------------------------------- noclip y toboganes */

	/**
	 * Los saltos de un sitio a otro: el noclip de la sala central (vuelve a empezar en una habitacion
	 * de inicio), el tobogan del castillo (a la sala central, con el bote de la vuelta) y el tobogan
	 * del final (se sale del nivel).
	 */
	private void saltos(ServerLevel nivel, ServerPlayer j) {
		double x = j.getX();
		double z = j.getZ();
		if (PlanoFiesta.enCentral(x, z) && j.getY() < PlanoFiesta.SUELO - 0.3) {
			nivel.playSound(null, j.blockPosition(), Sonidos.FIESTA_NOCLIP, SoundSource.PLAYERS, 1.0F, 1.0F);
			this.nuevaVuelta(j);
			Fiesta.aUnInicio(j);
			j.displayClientMessage(Component.literal("VUELTA " + this.datos(j).vuelta + " · Otra vez dentro. Las monedas han vuelto a las mesas.")
				.withStyle(ChatFormatting.LIGHT_PURPLE), true);
			return;
		}
		if (x >= PlanoFiesta.TOB_X0 - 0.7 && x < PlanoFiesta.TOB_X1 + 1 && z >= PlanoFiesta.TOB_Z0 && z < PlanoFiesta.TOB_Z1 + 1
			&& j.getY() > PlanoFiesta.CAS_P1) {
			nivel.playSound(null, j.blockPosition(), Sonidos.FIESTA_TOBOGAN, SoundSource.PLAYERS, 1.2F, 1.0F);
			j.teleportTo(nivel, PlanoFiesta.CENTRAL_LLEGADA_X, PlanoFiesta.SUELO + 1, PlanoFiesta.CENTRAL_LLEGADA_Z, Set.of(), -90.0F, 0.0F, true);
			j.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 2, false, false));
			nivel.playSound(null, PlanoFiesta.CENTRAL_LLEGADA_X, PlanoFiesta.SUELO + 1.5, PlanoFiesta.CENTRAL_LLEGADA_Z, Sonidos.FIESTA_TOBOGAN,
				SoundSource.PLAYERS, 0.8F, 1.3F);
			Datos d = this.datos(j);
			if (!d.cobrada) {
				int bote = this.bote(j);
				d.cobrada = true;
				d.vueltas++;
				this.sumarTickets(j, bote, "¡vuelta " + d.vuelta + " completada! (bote)");
				Sonidos.aJugador(j, Sonidos.SEMAFORO_GANA, 1.0F);
			} else {
				j.displayClientMessage(Component.literal("Esta vuelta ya está cobrada: para otro bote, salta a un cuadrado de noclip.")
					.withStyle(ChatFormatting.GRAY), true);
			}
			return;
		}
		boolean boca = x >= PlanoFiesta.FINAL_TOB_X0 && x < PlanoFiesta.FINAL_TOB_X1 + 1 && z < PlanoFiesta.FIN_Z0 - 0.3 && z >= PlanoFiesta.FINAL_TOB_Z0;
		if (!boca) {
			this.enTobogan.remove(j.getUUID());
			return;
		}
		// dentro del ascensor del salon: la cinematica de la salida (SalidaFiesta) y, al acabar, al Nivel 3
		this.enTobogan.remove(j.getUUID());
		SalidaFiesta.get().empezar(j);
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
		ConfigFiesta c = ConfigFiesta.get();
		int[] cogidas = d.cogidas.stream().mapToInt(Integer::intValue).toArray();
		ServerPlayNetworking.send(j, new EstadoFiesta(false, d.monedas, d.tickets, c.objetivoTickets, cogidas,
			JuegoSemaforo.get().jugando(j) ? 1 : 0, d.cobrada ? 0 : c.jackpotInicial, c.jackpotMinimo, c.jackpotPerdidaMinuto, d.vueltaDesde));
	}
}
