package net.backrooms.evento.piscinas;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.entidad.Serpiente;
import net.backrooms.evento.fase.Fases;
import net.backrooms.evento.mision.Entidades;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.mision.TipoMision;
import net.backrooms.evento.objetos.Objetos;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Las tres tarjetas de la salida de emergencia de las Piscinas (roja, amarilla y verde) y la
 * puerta. Hay una de cada para todo el evento: los jugadores tienen que coordinarse para
 * encontrarlas (PlanoPiscinas#tarjeta: cada vez mas lejos de la puerta y en sitios peores; la
 * roja, en el fondo de una piscina honda y con una Serpiente que la guarda) y meterlas en los
 * tres lectores de la puerta. Con las tres dentro, la puerta se abre para siempre y la mision
 * SALIDA queda hecha para todos; cruza quien ademas haya hecho las suyas (cruzar).
 *
 * Cada tarjeta esta en su escondite, la lleva alguien (es un objeto en su inventario), esta
 * tirada donde se le cayo (al morir o al soltarla con Q: lo que Minecraft suelta se cambia por
 * la tarjeta del suelo) o ya esta metida. Si quien la lleva se desconecta, vuelve a su
 * escondite. Todo sale en el chat. El estado se guarda en <mundo>/backrooms/tarjetas.json.
 */
public final class Tarjetas {
	private static final Tarjetas INSTANCIA = new Tarjetas();
	public static final String[] NOMBRES = {"roja", "amarilla", "verde"};
	private static final ChatFormatting[] TINTAS = {ChatFormatting.RED, ChatFormatting.YELLOW, ChatFormatting.GREEN};
	public static final int EN_SITIO = 0;
	public static final int LLEVADA = 1;
	public static final int SUELTA = 2;
	public static final int METIDA = 3;
	/** A esta distancia de alguien aparece la tarjeta tirada (y a QUITAR se quita). */
	private static final double VER = 48.0;
	private static final double QUITAR = 64.0;
	/** Hasta donde se ve el brillo de una tarjeta (para quien tiene en curso la mision SALIDA). */
	private static final double BRILLO = 160.0;
	/** El color de cada tarjeta, para su brillo. */
	private static final int[] COLORES = {0xFF2A1E, 0xFFD21E, 0x2EE84A};
	/** Con alguien a esta distancia de la roja en su sitio, aparece su guardiana. */
	private static final double GUARDIANA = 90.0;

	/** Lo que se guarda. */
	private static final class Estado {
		int[] estado = new int[3];
		String[] quien = new String[3];
		String[] nombre = new String[3];
		double[][] suelta = new double[3][];
	}

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private Estado e = new Estado();
	private final TarjetaEntidad[] vivas = new TarjetaEntidad[3];
	private @Nullable Serpiente guardiana;
	private final Map<UUID, Long> cruzando = new HashMap<>();
	/** Cuando murio cada uno (para decir si la tarjeta se le cayo al morir o la solto). */
	private final Map<UUID, Long> muertes = new HashMap<>();
	private MinecraftServer servidor;
	private long ticks;

	private Tarjetas() {
	}

	public static Tarjetas get() {
		return INSTANCIA;
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			INSTANCIA.servidor = s;
			INSTANCIA.cargar();
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(s -> INSTANCIA.guardar());
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			INSTANCIA.servidor = null;
			java.util.Arrays.fill(INSTANCIA.vivas, null);
			INSTANCIA.guardiana = null;
		});
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((ent, fuente) -> {
			if (ent instanceof ServerPlayer j) {
				INSTANCIA.muertes.put(j.getUUID(), INSTANCIA.ticks);
				INSTANCIA.alMorir(j);
			}
		});
	}

	/**
	 * El evento juega con keep_inventory: al morir no se cae nada, pero la tarjeta si (se le
	 * quita y queda tirada donde murio, para que otro pueda ir a por ella).
	 */
	private void alMorir(ServerPlayer j) {
		if (this.servidor == null || !Piscinas.es(j.level())) {
			return;
		}
		for (int c = 0; c < 3; c++) {
			if (this.e.estado[c] != LLEVADA || !j.getUUID().toString().equals(this.e.quien[c])) {
				continue;
			}
			this.sacar(j, c);
			this.e.estado[c] = SUELTA;
			this.e.suelta[c] = new double[] {j.getX(), Math.floor(j.getY()), j.getZ()};
			this.e.quien[c] = null;
			this.guardar();
			this.anunciar(Component.literal("La tarjeta " + NOMBRES[c] + " ha caído donde murió " + j.getGameProfile().name() + ".").withStyle(TINTAS[c]));
		}
	}

	/* ---------------------------------------------------------- guardado */

	private Path archivo() {
		return this.servidor.getWorldPath(LevelResource.ROOT).resolve("backrooms").resolve("tarjetas.json");
	}

	private void cargar() {
		this.e = new Estado();
		try {
			Path f = this.archivo();
			if (Files.exists(f)) {
				Estado l = this.gson.fromJson(Files.readString(f, StandardCharsets.UTF_8), Estado.class);
				if (l != null && l.estado != null && l.estado.length == 3) {
					this.e = l;
				}
			}
		} catch (Exception ex) {
			BackroomsEvento.LOG.error("No se pudo leer tarjetas.json", ex);
		}
	}

	private void guardar() {
		if (this.servidor == null) {
			return;
		}
		try {
			Path f = this.archivo();
			Files.createDirectories(f.getParent());
			Files.writeString(f, this.gson.toJson(this.e), StandardCharsets.UTF_8);
		} catch (Exception ex) {
			BackroomsEvento.LOG.error("No se pudo guardar tarjetas.json", ex);
		}
	}

	/* ----------------------------------------------------------- consultas */

	private @Nullable ServerLevel nivel() {
		return this.servidor == null ? null : this.servidor.getLevel(Piscinas.DIMENSION);
	}

	public int metidas() {
		int n = 0;
		for (int c = 0; c < 3; c++) {
			if (this.e.estado[c] == METIDA) {
				n++;
			}
		}
		return n;
	}

	public boolean abierta() {
		return this.metidas() == 3;
	}

	/** {x, z} de la puerta (para el radar), o null si no estamos en las Piscinas. */
	public int @Nullable [] puerta(ServerLevel nivel) {
		PlanoPiscinas p = Valvulas.plano(nivel);
		if (p == null) {
			return null;
		}
		int[] c = p.salida().columna(1);
		return new int[] {c[0], c[1]};
	}

	/** Una linea por tarjeta, para el staff. */
	public List<String> resumen() {
		List<String> l = new java.util.ArrayList<>();
		for (int c = 0; c < 3; c++) {
			String s = switch (this.e.estado[c]) {
				case LLEVADA -> "la lleva " + this.e.nombre[c];
				case SUELTA -> String.format("tirada en %.0f %.0f %.0f", this.e.suelta[c][0], this.e.suelta[c][1], this.e.suelta[c][2]);
				case METIDA -> "metida (por " + this.e.nombre[c] + ")";
				default -> "en su escondite";
			};
			l.add("Tarjeta " + NOMBRES[c] + ": " + s);
		}
		l.add(this.abierta() ? "La salida esta ABIERTA" : "La salida esta cerrada (" + this.metidas() + "/3)");
		return l;
	}

	/** El escondite de esa tarjeta: {x, y, z}. */
	public int @Nullable [] escondite(int color) {
		ServerLevel nivel = this.nivel();
		PlanoPiscinas p = nivel == null ? null : Valvulas.plano(nivel);
		return p == null ? null : p.tarjeta(color);
	}

	/* ------------------------------------------------------------- el tick */

	private void tick() {
		if (this.servidor == null || ++this.ticks % 10 != 0) {
			return;
		}
		ServerLevel nivel = this.nivel();
		PlanoPiscinas p = nivel == null ? null : Valvulas.plano(nivel);
		if (p == null) {
			return;
		}
		for (int c = 0; c < 3; c++) {
			switch (this.e.estado[c]) {
				case LLEVADA -> this.vigilar(nivel, c);
				case EN_SITIO -> {
					int[] t = p.tarjeta(c);
					this.mostrar(nivel, c, t[0] + 0.5, t[1], t[2] + 0.5);
					this.brillo(nivel, c, t[0] + 0.5, t[1], t[2] + 0.5);
				}
				case SUELTA -> {
					this.mostrar(nivel, c, this.e.suelta[c][0], this.e.suelta[c][1], this.e.suelta[c][2]);
					this.brillo(nivel, c, this.e.suelta[c][0], this.e.suelta[c][1], this.e.suelta[c][2]);
				}
				default -> this.quitar(c);
			}
		}
		if (this.ticks % 20 == 0) {
			this.equipos();
			this.ajustarPuerta(nivel, p.salida());
			this.guardiana(nivel, p.tarjeta(PlanoPiscinas.T_ROJA));
		}
		this.cabina(nivel, p.salida());
		this.cruzando.values().removeIf(t -> this.ticks - t > 40);
		this.muertes.values().removeIf(t -> this.ticks - t > 400);
	}

	/**
	 * Quien lleva una tarjeta sale en el TAB (y en el chat) con el color de esa tarjeta: va en un
	 * equipo del marcador de Minecraft por color (TablaJugadores lo lee del equipo).
	 */
	private void equipos() {
		net.minecraft.server.ServerScoreboard marcador = this.servidor.getScoreboard();
		for (int c = 0; c < 3; c++) {
			String nombre = "br_tarjeta_" + NOMBRES[c];
			net.minecraft.world.scores.PlayerTeam equipo = marcador.getPlayerTeam(nombre);
			if (equipo == null) {
				equipo = marcador.addPlayerTeam(nombre);
				equipo.setColor(TINTAS[c]);
			}
			String lleva = this.e.estado[c] == LLEVADA ? this.e.nombre[c] : null;
			for (String n : new java.util.ArrayList<>(equipo.getPlayers())) {
				if (!n.equals(lleva)) {
					marcador.removePlayerFromTeam(n, equipo);
				}
			}
			if (lleva != null && !equipo.getPlayers().contains(lleva)) {
				marcador.addPlayerToTeam(lleva, equipo);
			}
		}
	}

	/**
	 * El brillo de una tarjeta que esta por ahi (en su escondite o tirada): un halo de su color y
	 * una columna de luz que sube, que se ven de lejos (por encima de los bordes de las piscinas).
	 * Solo lo ve quien esta buscandolas (la mision SALIDA en curso): son particulas mandadas a
	 * cada uno, no un efecto de la entidad.
	 */
	private void brillo(ServerLevel nivel, int c, double x, double y, double z) {
		net.minecraft.core.particles.DustParticleOptions halo = new net.minecraft.core.particles.DustParticleOptions(COLORES[c], 2.6F);
		net.minecraft.core.particles.DustParticleOptions haz = new net.minecraft.core.particles.DustParticleOptions(COLORES[c], 1.6F);
		for (ServerPlayer j : nivel.players()) {
			if (j.distanceToSqr(x, y, z) > BRILLO * BRILLO || Misiones.get().enCurso(j) != TipoMision.SALIDA) {
				continue;
			}
			nivel.sendParticles(j, halo, true, true, x, y + 0.35, z, 6, 0.35, 0.2, 0.35, 0.0);
			nivel.sendParticles(j, haz, true, true, x, y + 2.5, z, 6, 0.08, 2.0, 0.08, 0.0);
		}
	}

	/** La tarjeta en el suelo, solo con alguien cerca y su chunk cargado. */
	private void mostrar(ServerLevel nivel, int c, double x, double y, double z) {
		TarjetaEntidad t = this.vivas[c];
		if (t != null && (t.isRemoved() || t.level() != nivel)) {
			t = null;
			this.vivas[c] = null;
		}
		boolean cerca = nivel.getNearestPlayer(x, y, z, VER, false) != null;
		if (t == null) {
			if (cerca && nivel.isPositionEntityTicking(BlockPos.containing(x, y, z))) {
				t = new TarjetaEntidad(Entidades.TARJETA, nivel);
				t.color(c);
				t.setPos(x, y, z);
				nivel.addFreshEntity(t);
				this.vivas[c] = t;
			}
		} else if (nivel.getNearestPlayer(x, y, z, QUITAR, false) == null) {
			this.quitar(c);
		}
	}

	private void quitar(int c) {
		if (this.vivas[c] != null) {
			this.vivas[c].discard();
			this.vivas[c] = null;
		}
	}

	/**
	 * La lleva alguien: si sigue en su inventario, nada. Si se ha desconectado o ya no esta en
	 * las Piscinas, vuelve a su escondite. Si la ha soltado (al morir o con Q), lo que solto
	 * Minecraft se cambia por la tarjeta tirada.
	 */
	private void vigilar(ServerLevel nivel, int c) {
		UUID id = UUID.fromString(this.e.quien[c]);
		ServerPlayer j = this.servidor.getPlayerList().getPlayer(id);
		if (j == null) {
			this.volver(c, this.e.nombre[c] + " se ha ido: la tarjeta " + NOMBRES[c] + " vuelve a su escondite.");
			return;
		}
		if (j.level() != nivel) {
			this.sacar(j, c);
			this.volver(c, "La tarjeta " + NOMBRES[c] + " vuelve a su escondite.");
			return;
		}
		if (this.tiene(j, c)) {
			return;
		}
		// lo que solto Minecraft (al morir o con Q), este donde este
		ItemEntity suelto = null;
		for (ItemEntity ie : nivel.getEntities(net.minecraft.world.entity.EntityType.ITEM, ie -> ie.getItem().is(Objetos.TARJETAS[c]))) {
			suelto = ie;
			break;
		}
		if (suelto == null) {
			this.volver(c, "La tarjeta " + NOMBRES[c] + " vuelve a su escondite.");
			return;
		}
		double[] donde = {suelto.getX(), Math.floor(suelto.getY()), suelto.getZ()};
		suelto.discard();
		this.e.estado[c] = SUELTA;
		this.e.suelta[c] = donde;
		this.guardar();
		Long murio = this.muertes.get(id);
		boolean muerto = !j.isAlive() || murio != null && this.ticks - murio < 400;
		this.anunciar(Component.literal(!muerto ? j.getGameProfile().name() + " ha soltado la tarjeta " + NOMBRES[c] + "."
			: "La tarjeta " + NOMBRES[c] + " ha caído donde murió " + j.getGameProfile().name() + ".").withStyle(TINTAS[c]));
	}

	private void volver(int c, String aviso) {
		this.e.estado[c] = EN_SITIO;
		this.e.quien[c] = null;
		this.e.suelta[c] = null;
		this.guardar();
		this.anunciar(Component.literal(aviso).withStyle(TINTAS[c]));
	}

	private boolean tiene(ServerPlayer j, int c) {
		for (int i = 0; i < j.getInventory().getContainerSize(); i++) {
			if (j.getInventory().getItem(i).is(Objetos.TARJETAS[c])) {
				return true;
			}
		}
		return j.containerMenu.getCarried().is(Objetos.TARJETAS[c]);
	}

	/** Le quita esa tarjeta del inventario (toda copia). */
	private void sacar(ServerPlayer j, int c) {
		for (int i = 0; i < j.getInventory().getContainerSize(); i++) {
			if (j.getInventory().getItem(i).is(Objetos.TARJETAS[c])) {
				j.getInventory().setItem(i, ItemStack.EMPTY);
			}
		}
		if (j.containerMenu.getCarried().is(Objetos.TARJETAS[c])) {
			j.containerMenu.setCarried(ItemStack.EMPTY);
		}
	}

	private void anunciar(Component texto) {
		this.servidor.getPlayerList().broadcastSystemMessage(texto, false);
	}

	/* ----------------------------------------------------- coger y meter */

	public void coger(ServerPlayer j, TarjetaEntidad t) {
		int c = t.color();
		if (t.isRemoved() || j.isSpectator() || !Misiones.get().enExpedicion(j) || this.e.estado[c] == LLEVADA || this.e.estado[c] == METIDA) {
			return;
		}
		// las tarjetas se ven para todos, pero solo las coge quien ya cerro sus valvulas y grabo a la Serpiente
		if (!Misiones.get().hecha(j, TipoMision.VALVULAS) || !Misiones.get().hecha(j, TipoMision.SERPIENTE)) {
			j.displayClientMessage(Component.literal("Primero completa tus misiones anteriores: cierra tus válvulas y graba a la Serpiente.").withStyle(ChatFormatting.GOLD), true);
			j.level().playSound(null, t.getX(), t.getY(), t.getZ(), net.minecraft.sounds.SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6F, 0.8F);
			return;
		}
		if (!j.getInventory().add(new ItemStack(Objetos.TARJETAS[c]))) {
			j.displayClientMessage(Component.literal("No te cabe: haz sitio en el inventario.").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		ServerLevel nivel = j.level();
		nivel.playSound(null, t.getX(), t.getY(), t.getZ(), Sonidos.TARJETA_COGER, SoundSource.PLAYERS, 1.0F, 1.0F);
		this.quitar(c);
		t.discard();
		this.e.estado[c] = LLEVADA;
		this.e.quien[c] = j.getUUID().toString();
		this.e.nombre[c] = j.getGameProfile().name();
		this.e.suelta[c] = null;
		this.guardar();
		this.anunciar(Component.literal(j.getGameProfile().name() + " ha obtenido la tarjeta " + NOMBRES[c] + ".").withStyle(TINTAS[c], ChatFormatting.BOLD));
		BackroomsEvento.LOG.info("{} coge la tarjeta {}", j.getGameProfile().name(), NOMBRES[c]);
	}

	/** Alguien pulsa el lector de ese color: si lleva su tarjeta, la mete. */
	public void meter(ServerPlayer j, int c, BlockPos lector) {
		ServerLevel nivel = j.level();
		if (this.e.estado[c] == METIDA) {
			j.displayClientMessage(Component.literal("Este lector ya tiene su tarjeta.").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		if (!this.tiene(j, c)) {
			nivel.playSound(null, lector, Sonidos.ASCENSOR_DENEGADO, SoundSource.BLOCKS, 0.8F, 1.2F);
			j.displayClientMessage(Component.literal("Falta la tarjeta " + NOMBRES[c] + ".").withStyle(TINTAS[c]), true);
			return;
		}
		this.sacar(j, c);
		this.e.estado[c] = METIDA;
		this.e.quien[c] = null;
		this.e.nombre[c] = j.getGameProfile().name();
		this.guardar();
		BlockState s = nivel.getBlockState(lector);
		if (s.is(BloquesPiscinas.LECTOR)) {
			nivel.setBlock(lector, s.setValue(LectorTarjeta.LIT, true), Block.UPDATE_CLIENTS);
		}
		nivel.playSound(null, lector, Sonidos.LECTOR_ACEPTA, SoundSource.BLOCKS, 1.2F, 1.0F);
		int n = this.metidas();
		this.anunciar(Component.literal(j.getGameProfile().name() + " ha metido la tarjeta " + NOMBRES[c] + " en la salida de emergencia (" + n + "/3).").withStyle(TINTAS[c]));
		BackroomsEvento.LOG.info("{} mete la tarjeta {} ({}/3)", j.getGameProfile().name(), NOMBRES[c], n);
		if (n == 3) {
			this.abrir(nivel);
		}
	}

	private void abrir(ServerLevel nivel) {
		PlanoPiscinas p = Valvulas.plano(nivel);
		if (p != null) {
			this.ajustarPuerta(nivel, p.salida());
			int[] c = p.salida().columna(1);
			nivel.playSound(null, c[0] + 0.5, PlanoPiscinas.PISO + 1, c[1] + 0.5, Sonidos.PUERTA_SALIDA, SoundSource.BLOCKS, 3.0F, 1.0F);
		}
		this.anunciar(Component.literal("LA SALIDA DE EMERGENCIA ESTÁ ABIERTA. Quien haya hecho sus misiones puede cruzarla.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		Misiones.get().completarParaTodos(TipoMision.SALIDA);
		BackroomsEvento.LOG.info("La salida de emergencia de las Piscinas se abre");
	}

	/** Lectores encendidos y puerta abierta segun el estado (los bloques salen del plano apagados y cerrados). */
	private void ajustarPuerta(ServerLevel nivel, PlanoPiscinas.Salida s) {
		for (int c = 0; c < 3; c++) {
			int[] l = s.lector(c);
			BlockPos pos = new BlockPos(l[0], l[1], l[2]);
			if (!nivel.isLoaded(pos)) {
				continue;
			}
			BlockState b = nivel.getBlockState(pos);
			boolean lit = this.e.estado[c] == METIDA;
			if (b.is(BloquesPiscinas.LECTOR) && b.getValue(LectorTarjeta.LIT) != lit) {
				nivel.setBlock(pos, b.setValue(LectorTarjeta.LIT, lit), Block.UPDATE_CLIENTS);
			}
		}
		boolean abierta = this.abierta();
		for (int a = 1; a <= 2; a++) {
			int[] w = s.pared(a);
			for (int f = 0; f < 3; f++) {
				BlockPos pos = new BlockPos(w[0], PlanoPiscinas.PISO + f, w[1]);
				if (!nivel.isLoaded(pos)) {
					continue;
				}
				BlockState b = nivel.getBlockState(pos);
				if (b.is(BloquesPiscinas.PUERTA_SALIDA) && b.getValue(PuertaSalida.ABIERTA) != abierta) {
					nivel.setBlock(pos, b.setValue(PuertaSalida.ABIERTA, abierta), Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** La Serpiente que guarda la roja mientras esta en su escondite (la del fondo de la piscina). */
	private void guardiana(ServerLevel nivel, int[] sitio) {
		if (this.guardiana != null && (this.guardiana.isRemoved() || !this.guardiana.isAlive())) {
			this.guardiana = null;
		}
		if (this.e.estado[PlanoPiscinas.T_ROJA] != EN_SITIO) {
			if (this.guardiana != null) {
				this.guardiana.casa(null);
				this.guardiana = null;
			}
			return;
		}
		BlockPos p = new BlockPos(sitio[0], sitio[1], sitio[2]);
		if (this.guardiana == null && net.backrooms.evento.entidad.Acechadores.get().activos()
			&& nivel.getNearestPlayer(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, GUARDIANA, true) != null
			&& nivel.getNearestPlayer(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 24, true) == null
			&& nivel.isPositionEntityTicking(p)) {
			this.guardiana = Habitantes.poner(nivel, p.above());
			if (this.guardiana != null) {
				this.guardiana.casa(p);
			}
		}
	}

	/* -------------------------------------------------------------- cruzar */

	/** Alguien entra en la puerta abierta. */
	/** Ticks (de 10 en 10) que lleva cada uno dentro de la cabina del ascensor de salida. */
	private final Map<UUID, Integer> enCabina = new HashMap<>();

	/**
	 * El ascensor de salida: quien ha hecho sus misiones y se queda un momento dentro de la
	 * cabina (con la puerta abierta) sube: cruza la salida (cruzar).
	 */
	private void cabina(ServerLevel nivel, PlanoPiscinas.Salida s) {
		if (!this.abierta()) {
			this.enCabina.clear();
			return;
		}
		java.util.Set<UUID> vistos = new java.util.HashSet<>();
		// sobre una copia: quien sube cambia de dimension y sale de la lista (si no, el servidor se cae)
		for (ServerPlayer j : new java.util.ArrayList<>(nivel.players())) {
			if (j.isSpectator() || !s.enCabina(j.getX(), j.getY(), j.getZ())) {
				continue;
			}
			vistos.add(j.getUUID());
			int n = this.enCabina.merge(j.getUUID(), 10, Integer::sum);
			if (n == 10) {
				nivel.playSound(null, j.blockPosition(), Sonidos.ASCENSOR_PANEL, SoundSource.BLOCKS, 1.0F, 1.0F);
			}
			if (n >= 30) {
				this.enCabina.remove(j.getUUID());
				vistos.remove(j.getUUID());
				this.cruzar(j, j.blockPosition(), Direction.from2DDataValue(0));
			}
		}
		this.enCabina.keySet().retainAll(vistos);
	}

	public void cruzar(ServerPlayer j, BlockPos pos, Direction mira) {
		if (this.cruzando.containsKey(j.getUUID()) || j.isSpectator()) {
			return;
		}
		this.cruzando.put(j.getUUID(), this.ticks);
		if (!Misiones.get().todasHechas(j)) {
			// fuera: medio paso hacia atras
			j.teleportTo(j.level(), pos.getX() + 0.5 + mira.getStepX() * 1.4, j.getY(), pos.getZ() + 0.5 + mira.getStepZ() * 1.4,
				java.util.Set.of(), j.getYRot(), j.getXRot(), true);
			j.level().playSound(null, pos, Sonidos.ASCENSOR_DENEGADO, SoundSource.BLOCKS, 0.9F, 1.0F);
			j.displayClientMessage(Component.literal("ACCESO DENEGADO · Completa tus misiones").withStyle(ChatFormatting.RED), true);
			return;
		}
		BackroomsEvento.LOG.info("{} cruza la salida de emergencia de las Piscinas", j.getGameProfile().name());
		// aqui empezara el siguiente nivel; mientras no exista, cruzar es escapar
		Fases.get().escapar(j);
	}

	/* --------------------------------------------------------------- staff */

	/** Todo como al principio: cada tarjeta en su escondite y la puerta cerrada. */
	public void reponer() {
		for (int c = 0; c < 3; c++) {
			if (this.e.estado[c] == LLEVADA && this.servidor != null) {
				ServerPlayer j = this.servidor.getPlayerList().getPlayer(UUID.fromString(this.e.quien[c]));
				if (j != null) {
					this.sacar(j, c);
				}
			}
			this.quitar(c);
		}
		this.e = new Estado();
		this.guardar();
		ServerLevel nivel = this.nivel();
		PlanoPiscinas p = nivel == null ? null : Valvulas.plano(nivel);
		if (p != null) {
			this.ajustarPuerta(nivel, p.salida());
		}
	}

	/** Abre la puerta ya (pruebas): las tres metidas. */
	public void abrirYa() {
		for (int c = 0; c < 3; c++) {
			this.quitar(c);
			this.e.estado[c] = METIDA;
			this.e.nombre[c] = "staff";
		}
		this.guardar();
		ServerLevel nivel = this.nivel();
		if (nivel != null) {
			this.abrir(nivel);
		}
	}
}
