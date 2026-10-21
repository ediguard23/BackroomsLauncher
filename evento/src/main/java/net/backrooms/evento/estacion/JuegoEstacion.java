package net.backrooms.evento.estacion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import net.backrooms.evento.ambiente.Ambiente;
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.mision.TipoMision;
import net.backrooms.evento.red.EstadoEstacion;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

/**
 * El juego de la Estacion Electrica (Nivel 3), de todos a la vez:
 *
 *   1. COMBUSTIBLE  el generador gasta (CONSUMO por segundo mientras hay alguien en el nivel). Se
 *                   llena con bidones (BIDON cada uno) en su boca. Sin combustible: apagon en todo el
 *                   nivel y no funciona nada electrico. La etapa se da por hecha al arrancarlo la
 *                   primera vez.
 *   2. PANELES      ocho paneles averiados (dos por sector), cada uno con su material. Con los dos de
 *                   un sector se abre la persiana de su sala de fusibles.
 *   3. FUSIBLES     los cuatro fusibles en la sala de control; luego los interruptores en el orden de
 *                   la pantalla (se sortea). Un error: fallo (apagon de FALLO segundos) y a empezar.
 *   4. PASARELA     con la secuencia hecha se abre la persiana de la pasarela, los focos se encienden
 *                   uno tras otro y se abre la puerta del final: quien llega a la plataforma, sale.
 *
 * Los puntos de recogida (materiales, bidones y fusibles) se vacian al coger y se reponen solos
 * (los fusibles, solo si nadie tiene el suyo y no esta puesto). El estado se guarda en
 * <mundo>/backrooms/estacion.json; los bloques los pone al dia el tick (ajustar).
 */
public final class JuegoEstacion {
	private static final JuegoEstacion INSTANCIA = new JuegoEstacion();
	/** Combustible: el maximo, lo que da un bidon, el gasto por segundo y con lo que se empieza. */
	public static final float MAXIMO = 100.0F;
	private static final float BIDON = 30.0F;
	private static final float CONSUMO = 100.0F / (7 * 60);
	private static final float INICIAL = 35.0F;
	/** Por debajo de esto, la alarma del deposito. */
	private static final float RESERVA = 15.0F;
	private static final int FALLO = 12;
	/** Segundos que tarda en reponerse un punto (material, bidon). */
	private static final int REPONER_MATERIAL = 90;
	private static final int REPONER_BIDON = 120;
	private static final String[] MATERIAL = {"un cable eléctrico", "la caja de herramientas", "cinta aislante"};
	private static final ChatFormatting[] TINTAS = {ChatFormatting.RED, ChatFormatting.YELLOW, ChatFormatting.BLUE, ChatFormatting.GREEN};
	private static final String[] NOMBRES_COLOR = {"ROJO", "AMARILLO", "AZUL", "VERDE"};

	private static final class Estado {
		float combustible = INICIAL;
		boolean arrancado;
		Set<Integer> reparados = new HashSet<>();
		boolean[] fusibles = new boolean[4];
		int[] secuencia;
		boolean pasarela;
	}

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private Estado e = new Estado();
	private MinecraftServer servidor;
	private long ticks;
	private boolean sucio;
	/** Los interruptores subidos en esta tanda (en orden). */
	private final List<Integer> pulsados = new ArrayList<>();
	/** Hasta cuando dura el fallo (tick), y desde cuando se encienden los focos de la pasarela. */
	private long falloHasta = -1;
	private long focosDesde = -1;
	/** Cuando se vacio cada punto (tick), para reponerlo. */
	private final Map<Integer, Long> vacios = new HashMap<>();
	private Display.@Nullable TextDisplay pantalla;
	private boolean apagonPuesto;

	private JuegoEstacion() {
	}

	public static JuegoEstacion get() {
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
			INSTANCIA.pantalla = null;
		});
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
	}

	/* ------------------------------------------------------------- guardado */

	private Path archivo() {
		return this.servidor.getWorldPath(LevelResource.ROOT).resolve("backrooms").resolve("estacion.json");
	}

	private void cargar() {
		this.e = new Estado();
		try {
			Path f = this.archivo();
			if (Files.exists(f)) {
				Estado l = this.gson.fromJson(Files.readString(f, StandardCharsets.UTF_8), Estado.class);
				if (l != null) {
					this.e = l;
					if (this.e.fusibles == null || this.e.fusibles.length != 4) {
						this.e.fusibles = new boolean[4];
					}
					if (this.e.reparados == null) {
						this.e.reparados = new HashSet<>();
					}
				}
			}
		} catch (Exception ex) {
			BackroomsEvento.LOG.error("No se pudo leer estacion.json", ex);
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
			BackroomsEvento.LOG.error("No se pudo guardar estacion.json", ex);
		}
		this.sucio = false;
	}

	/* ------------------------------------------------------------- consultas */

	/** Etapas hechas: 0 nada, 1 generador arrancado, 2 paneles, 3 circuito (pasarela con corriente). */
	public int etapa() {
		if (this.e.pasarela) {
			return 3;
		}
		if (this.e.reparados.size() >= PlanoEstacion.PANELES.size() && this.e.arrancado) {
			return 2;
		}
		return this.e.arrancado ? 1 : 0;
	}

	public boolean corriente() {
		return this.e.combustible > 0 && (this.falloHasta < 0 || this.ticks >= this.falloHasta);
	}

	public float combustible() {
		return this.e.combustible;
	}

	public int reparados() {
		return this.e.reparados.size();
	}

	public int fusiblesPuestos() {
		int n = 0;
		for (boolean b : this.e.fusibles) {
			n += b ? 1 : 0;
		}
		return n;
	}

	public boolean pasarela() {
		return this.e.pasarela;
	}

	/** Lo que cuenta la hoja de misiones ahora: {hechos, total}. */
	public int[] cuenta() {
		return switch (this.etapa()) {
			case 0 -> new int[] {Math.round(this.e.combustible), 100};
			case 1 -> new int[] {this.reparados(), PlanoEstacion.PANELES.size()};
			case 2 -> new int[] {this.fusiblesPuestos(), 4};
			default -> new int[] {1, 1};
		};
	}

	/** Adonde lleva el radar: {x, z}, o null. */
	public int @Nullable [] objetivo(ServerPlayer j) {
		switch (this.etapa()) {
			case 0 -> {
				return new int[] {PlanoEstacion.BOCA[0], PlanoEstacion.BOCA[2]};
			}
			case 1 -> {
				PlanoEstacion.Panel mejor = null;
				double d = Double.MAX_VALUE;
				for (PlanoEstacion.Panel p : PlanoEstacion.PANELES) {
					double dd = j.distanceToSqr(p.x() + 0.5, p.y(), p.z() + 0.5);
					if (!this.e.reparados.contains(p.id()) && dd < d) {
						d = dd;
						mejor = p;
					}
				}
				return mejor == null ? null : new int[] {mejor.x(), mejor.z()};
			}
			case 2 -> {
				for (int c = 0; c < 4; c++) {
					if (!this.e.fusibles[c] && j.getInventory().contains(new ItemStack(BloquesEstacion.FUSIBLES[c]))) {
						return new int[] {PlanoEstacion.FUSIBLERA[c][0], PlanoEstacion.FUSIBLERA[c][2]};
					}
				}
				if (this.fusiblesPuestos() == 4) {
					return new int[] {PlanoEstacion.PALANCAS[0][0], PlanoEstacion.PALANCAS[0][2]};
				}
				PlanoEstacion.Punto mejor = null;
				double d = Double.MAX_VALUE;
				for (PlanoEstacion.Punto p : PlanoEstacion.PUNTOS) {
					if (p.tipo() < 4 || this.e.fusibles[p.tipo() - 4]) {
						continue;
					}
					double dd = j.distanceToSqr(p.x() + 0.5, p.y(), p.z() + 0.5);
					if (dd < d) {
						d = dd;
						mejor = p;
					}
				}
				return mejor == null ? null : new int[] {mejor.x(), mejor.z()};
			}
			default -> {
				return new int[] {0, PlanoEstacion.FINAL[1]};
			}
		}
	}

	/* ------------------------------------------------------------------ tick */

	private void tick() {
		if (this.servidor == null) {
			return;
		}
		this.ticks++;
		ServerLevel nivel = Estacion.nivel();
		if (nivel == null) {
			return;
		}
		boolean alguien = !nivel.players().isEmpty();
		if (alguien && this.e.combustible > 0) {
			this.e.combustible = Math.max(0.0F, this.e.combustible - CONSUMO / 20.0F);
			if (this.e.combustible <= 0) {
				this.anunciar(nivel, Component.literal("El generador se ha parado: sin combustible. Todo se queda a oscuras.").withStyle(ChatFormatting.RED), Sonidos.ESTACION_PARADA);
			}
			this.sucio = true;
		}
		// la luz: apagon mientras no haya corriente (y en el fallo)
		boolean apagon = alguien && !this.corriente();
		if (apagon && !Ambiente.get().apagon(nivel)) {
			Ambiente.get().apagon(nivel, 3600);
			this.apagonPuesto = true;
		} else if (!apagon && this.apagonPuesto) {
			Ambiente.get().terminar(nivel);
			this.apagonPuesto = false;
		}
		if (this.falloHasta >= 0 && this.ticks >= this.falloHasta) {
			this.falloHasta = -1;
			this.anunciar(nivel, Component.literal("Vuelve la corriente. Los interruptores se han bajado: otra vez desde el primero.").withStyle(ChatFormatting.YELLOW), Sonidos.ESTACION_ARRANCA);
		}
		if (this.ticks % 40 == 0) {
			this.limpiarFuera();
		}
		if (this.ticks % 10 == 0) {
			if (alguien) {
				this.reponer(nivel);
				this.ajustar(nivel);
				this.pantalla(nivel);
				this.salida(nivel);
				this.enviarATodos(nivel);
			}
			// la alarma del deposito en reserva, cada 5 s
			if (alguien && this.e.combustible > 0 && this.e.combustible < RESERVA && this.ticks % 100 == 0) {
				nivel.playSound(null, PlanoEstacion.GENERADOR[0], PlanoEstacion.GENERADOR[1], PlanoEstacion.GENERADOR[2], Sonidos.ESTACION_RESERVA, SoundSource.BLOCKS, 2.5F, 1.0F);
			}
		}
		if (this.sucio && this.ticks % 200 == 0) {
			this.guardar();
		}
	}

	/** Los puntos vacios que ya toca reponer (los fusibles, solo si nadie lo tiene ni esta puesto). */
	private void reponer(ServerLevel nivel) {
		if (this.vacios.isEmpty()) {
			return;
		}
		for (Integer id : new ArrayList<>(this.vacios.keySet())) {
			PlanoEstacion.Punto p = PlanoEstacion.PUNTOS.get(id);
			long desde = this.vacios.get(id);
			if (p.tipo() >= 4) {
				int c = p.tipo() - 4;
				if (this.e.fusibles[c] || this.alguienTiene(BloquesEstacion.FUSIBLES[c]) || this.ticks - desde < 20 * 20) {
					continue;
				}
			} else if (this.ticks - desde < 20L * (p.tipo() == 3 ? REPONER_BIDON : REPONER_MATERIAL)) {
				continue;
			}
			this.vacios.remove(id);
		}
	}

	/**
	 * Lo de la Estacion no sale de la Estacion: a quien ya no esta (eliminado, escapado, staff que
	 * se fue) se le quitan los materiales, bidones y fusibles. Si no, un fusible se iria con el y
	 * no volveria a salir en su sala.
	 */
	private void limpiarFuera() {
		for (ServerPlayer j : this.servidor.getPlayerList().getPlayers()) {
			if (Estacion.es(j.level())) {
				continue;
			}
			var inv = j.getInventory();
			for (int i = 0; i < inv.getContainerSize(); i++) {
				if (BloquesEstacion.esDeLaEstacion(inv.getItem(i))) {
					inv.setItem(i, ItemStack.EMPTY);
				}
			}
			if (BloquesEstacion.esDeLaEstacion(j.containerMenu.getCarried())) {
				j.containerMenu.setCarried(ItemStack.EMPTY);
			}
		}
	}

	private boolean alguienTiene(Item item) {
		for (ServerPlayer j : this.servidor.getPlayerList().getPlayers()) {
			if (!Estacion.es(j.level())) {
				continue;
			}
			if (j.getInventory().contains(new ItemStack(item)) || j.containerMenu.getCarried().is(item)) {
				return true;
			}
		}
		return false;
	}

	/** Pone los bloques como dice el estado (solo los cargados). */
	private void ajustar(ServerLevel nivel) {
		for (PlanoEstacion.Panel p : PlanoEstacion.PANELES) {
			this.poner(nivel, p.x(), p.y(), p.z(), Interactivos.HECHO, this.e.reparados.contains(p.id()));
		}
		for (int c = 0; c < 4; c++) {
			int[] f = PlanoEstacion.FUSIBLERA[c];
			this.poner(nivel, f[0], f[1], f[2], Interactivos.HECHO, this.e.fusibles[c]);
			int[] l = PlanoEstacion.PALANCAS[c];
			this.poner(nivel, l[0], l[1], l[2], Interactivos.HECHO, this.e.pasarela || this.pulsados.contains(c));
		}
		for (PlanoEstacion.Punto p : PlanoEstacion.PUNTOS) {
			boolean vacio = this.vacios.containsKey(p.id()) || p.tipo() >= 4 && this.e.fusibles[p.tipo() - 4];
			this.poner(nivel, p.x(), p.y(), p.z(), Interactivos.VACIO, vacio);
		}
		for (PlanoEstacion.Persiana p : PlanoEstacion.PERSIANAS) {
			boolean abierta = p.id() < 4 ? this.sectorReparado(p.id()) : this.e.pasarela;
			for (int k = 0; k < 9; k++) {
				int[] b = p.bloque(k);
				this.poner(nivel, b[0], b[1], b[2], Interactivos.ABIERTA, abierta);
			}
		}
		// los focos de la pasarela, de dos en dos desde que hay corriente
		for (int i = 0; i < PlanoEstacion.FOCOS.size(); i++) {
			int[] f = PlanoEstacion.FOCOS.get(i);
			boolean lit = this.e.pasarela && this.corriente() && (this.focosDesde < 0 || this.ticks - this.focosDesde >= (i / 2) * 8L);
			this.poner(nivel, f[0], f[1], f[2], Interactivos.LIT, lit);
		}
	}

	private <T extends Comparable<T>> void poner(ServerLevel nivel, int x, int y, int z, net.minecraft.world.level.block.state.properties.Property<T> prop, T valor) {
		BlockPos pos = new BlockPos(x, y, z);
		if (!nivel.isLoaded(pos)) {
			return;
		}
		BlockState s = nivel.getBlockState(pos);
		if (s.hasProperty(prop) && !s.getValue(prop).equals(valor)) {
			nivel.setBlock(pos, s.setValue(prop, valor), Block.UPDATE_CLIENTS);
		}
	}

	private boolean sectorReparado(int sector) {
		for (PlanoEstacion.Panel p : PlanoEstacion.PANELES) {
			if (p.sector() == sector && !this.e.reparados.contains(p.id())) {
				return false;
			}
		}
		return true;
	}

	/** El texto de la pantalla de la sala de control (una entidad de texto, solo con alguien cerca). */
	private void pantalla(ServerLevel nivel) {
		if (this.pantalla != null && (this.pantalla.isRemoved() || this.pantalla.level() != nivel)) {
			this.pantalla = null;
		}
		double[] p = PlanoEstacion.PANTALLA;
		boolean cerca = nivel.getNearestPlayer(p[0], p[1], p[2], 40, false) != null;
		if (!cerca) {
			if (this.pantalla != null) {
				this.pantalla.discard();
				this.pantalla = null;
			}
			return;
		}
		if (this.pantalla == null) {
			if (!nivel.isPositionEntityTicking(BlockPos.containing(p[0], p[1], p[2]))) {
				return;
			}
			Display.TextDisplay t = new Display.TextDisplay(EntityType.TEXT_DISPLAY, nivel) {
				@Override
				public boolean shouldBeSaved() {
					return false;
				}
			};
			t.setPos(p[0], p[1], p[2]);
			t.setYRot(0.0F);
			t.setViewRange(1.0F);
			nivel.addFreshEntity(t);
			this.pantalla = t;
		}
		this.pantalla.setText(this.textoPantalla());
	}

	private Component textoPantalla() {
		MutableComponent t = Component.empty();
		if (!this.corriente()) {
			return t.append(Component.literal("SIN CORRIENTE").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
		}
		if (this.e.pasarela) {
			return t.append(Component.literal("PASARELA ACTIVA\n").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
				.append(Component.literal("RUTA DE ESCAPE DISPONIBLE").withStyle(ChatFormatting.GREEN));
		}
		t.append(Component.literal("CIRCUITO PRINCIPAL\n").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
		if (this.fusiblesPuestos() < 4) {
			t.append(Component.literal("FUSIBLES  ").withStyle(ChatFormatting.GRAY));
			for (int c = 0; c < 4; c++) {
				t.append(Component.literal(this.e.fusibles[c] ? "■ " : "□ ").withStyle(TINTAS[c]));
			}
			return t;
		}
		t.append(Component.literal("SECUENCIA  ").withStyle(ChatFormatting.GRAY));
		int[] s = this.secuencia();
		for (int i = 0; i < 4; i++) {
			boolean hecho = i < this.pulsados.size();
			t.append(Component.literal((i + 1) + "·" + NOMBRES_COLOR[s[i]] + (i < 3 ? "  " : "")).withStyle(hecho ? ChatFormatting.DARK_GRAY : TINTAS[s[i]]));
		}
		return t;
	}

	private int[] secuencia() {
		if (this.e.secuencia == null || this.e.secuencia.length != 4) {
			List<Integer> l = new ArrayList<>(List.of(0, 1, 2, 3));
			java.util.Collections.shuffle(l);
			this.e.secuencia = l.stream().mapToInt(Integer::intValue).toArray();
			this.sucio = true;
		}
		return this.e.secuencia;
	}

	/** Quien llega a la plataforma del final con la pasarela encendida, sale. */
	private void salida(ServerLevel nivel) {
		if (!this.e.pasarela) {
			return;
		}
		int[] f = PlanoEstacion.FINAL;
		for (ServerPlayer j : new ArrayList<>(nivel.players())) {
			if (j.isSpectator() || j.isCreative() || !j.isAlive() || j.getX() < f[0] || j.getX() > f[2] + 1 || j.getZ() < f[1] - 3 || j.getZ() > f[3] + 1 || j.getY() < PlanoEstacion.SUELO) {
				continue;
			}
			Estacion.salir(j);
		}
	}

	/* ---------------------------------------------------------- lo que se hace */

	public void recoger(ServerPlayer j, BlockPos pos, BlockState s) {
		PlanoEstacion.Punto p = this.punto(pos);
		if (p == null) {
			return;
		}
		if (this.vacios.containsKey(p.id()) || p.tipo() >= 4 && this.e.fusibles[p.tipo() - 4]) {
			j.displayClientMessage(Component.literal(p.tipo() == 3 ? "Vacío. Alguien se lo ha llevado: vuelve más tarde." : "No queda nada aquí. Vuelve más tarde.").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		Item item = BloquesEstacion.deTipo(p.tipo());
		if (p.tipo() == 3 && j.getInventory().contains(new ItemStack(BloquesEstacion.BIDON))) {
			j.displayClientMessage(Component.literal("Ya llevas un bidón: pesa demasiado para dos.").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		if (!j.getInventory().add(new ItemStack(item))) {
			j.displayClientMessage(Component.literal("No te cabe: haz sitio en la barra (suelta algo con Q).").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		this.vacios.put(p.id(), this.ticks);
		ServerLevel nivel = j.level();
		nivel.playSound(null, pos, p.tipo() == 3 ? Sonidos.ESTACION_BIDON : Sonidos.ESTACION_COGER, SoundSource.PLAYERS, 1.0F, 1.0F);
		this.poner(nivel, pos.getX(), pos.getY(), pos.getZ(), Interactivos.VACIO, true);
		String que = p.tipo() < 3 ? MATERIAL[p.tipo()] : p.tipo() == 3 ? "un bidón de combustible" : "el fusible " + NOMBRES_COLOR[p.tipo() - 4].toLowerCase();
		j.displayClientMessage(Component.literal("Has cogido " + que + ".").withStyle(ChatFormatting.YELLOW), true);
		if (p.tipo() >= 4) {
			this.anunciar(nivel, Component.literal(j.getGameProfile().name() + " ha cogido el fusible " + NOMBRES_COLOR[p.tipo() - 4].toLowerCase() + ".").withStyle(TINTAS[p.tipo() - 4]), null);
		}
	}

	public void repostar(ServerPlayer j) {
		ServerLevel nivel = j.level();
		if (!this.quitar(j, BloquesEstacion.BIDON)) {
			j.displayClientMessage(Component.literal("Depósito al " + Math.round(this.e.combustible) + " %. Trae un bidón de combustible.").withStyle(ChatFormatting.GOLD), true);
			return;
		}
		boolean parado = this.e.combustible <= 0;
		this.e.combustible = Math.min(MAXIMO, this.e.combustible + BIDON);
		this.sucio = true;
		nivel.playSound(null, PlanoEstacion.BOCA[0] + 0.5, PlanoEstacion.BOCA[1], PlanoEstacion.BOCA[2] + 0.5, Sonidos.ESTACION_REPOSTAR, SoundSource.BLOCKS, 1.2F, 1.0F);
		j.displayClientMessage(Component.literal("Depósito al " + Math.round(this.e.combustible) + " %.").withStyle(ChatFormatting.GOLD), true);
		if (parado || !this.e.arrancado) {
			this.anunciar(nivel, Component.literal("El generador arranca. Vuelve la corriente.").withStyle(ChatFormatting.GREEN), Sonidos.ESTACION_ARRANCA);
		}
		if (!this.e.arrancado) {
			this.e.arrancado = true;
			Misiones.get().completarParaTodos(TipoMision.COMBUSTIBLE);
			AmasijoEstacion.get().despertar();
		}
	}

	public void reparar(ServerPlayer j, BlockPos pos) {
		PlanoEstacion.Panel p = null;
		for (PlanoEstacion.Panel q : PlanoEstacion.PANELES) {
			if (q.x() == pos.getX() && q.y() == pos.getY() && q.z() == pos.getZ()) {
				p = q;
			}
		}
		if (p == null) {
			return;
		}
		if (this.e.reparados.contains(p.id())) {
			j.displayClientMessage(Component.literal("Este panel ya funciona.").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		if (!this.corriente()) {
			j.displayClientMessage(Component.literal("Sin corriente no se puede: el generador está parado.").withStyle(ChatFormatting.RED), true);
			return;
		}
		if (!this.quitar(j, BloquesEstacion.MATERIALES[p.tipo()])) {
			j.displayClientMessage(Component.literal("Este panel necesita " + MATERIAL[p.tipo()] + ".").withStyle(ChatFormatting.GOLD), true);
			return;
		}
		ServerLevel nivel = j.level();
		this.e.reparados.add(p.id());
		this.sucio = true;
		nivel.playSound(null, pos, Sonidos.ESTACION_REPARAR, SoundSource.BLOCKS, 1.2F, 1.0F);
		this.poner(nivel, pos.getX(), pos.getY(), pos.getZ(), Interactivos.HECHO, true);
		int n = this.e.reparados.size();
		String sector = PlanoEstacion.SECTORES[p.sector()];
		this.anunciar(nivel, Component.literal(j.getGameProfile().name() + " ha reparado un panel del sector " + sector + " (" + n + "/" + PlanoEstacion.PANELES.size() + ").")
			.withStyle(TINTAS[p.sector()]), null);
		if (this.sectorReparado(p.sector())) {
			this.anunciar(nivel, Component.literal("Se abre la sala de fusibles del sector " + sector + ".").withStyle(TINTAS[p.sector()], ChatFormatting.BOLD), Sonidos.ESTACION_PERSIANA);
		}
		if (n >= PlanoEstacion.PANELES.size()) {
			Misiones.get().completarParaTodos(TipoMision.PANELES);
		}
		// cada reparacion despierta un poco mas la estacion
		AmasijoEstacion.get().ruido(nivel);
	}

	public void ponerFusible(ServerPlayer j, int color) {
		if (this.e.fusibles[color]) {
			j.displayClientMessage(Component.literal("El fusible " + NOMBRES_COLOR[color].toLowerCase() + " ya está puesto.").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		if (!this.quitar(j, BloquesEstacion.FUSIBLES[color])) {
			j.displayClientMessage(Component.literal("Hueco del fusible " + NOMBRES_COLOR[color].toLowerCase() + ": está en la sala de fusibles del sector "
				+ PlanoEstacion.SECTORES[color] + ".").withStyle(TINTAS[color]), true);
			return;
		}
		ServerLevel nivel = j.level();
		this.e.fusibles[color] = true;
		this.sucio = true;
		int[] f = PlanoEstacion.FUSIBLERA[color];
		nivel.playSound(null, f[0] + 0.5, f[1], f[2] + 0.5, Sonidos.ESTACION_FUSIBLE, SoundSource.BLOCKS, 1.2F, 1.0F);
		int n = this.fusiblesPuestos();
		this.anunciar(nivel, Component.literal(j.getGameProfile().name() + " ha puesto el fusible " + NOMBRES_COLOR[color].toLowerCase() + " (" + n + "/4).").withStyle(TINTAS[color]), null);
		if (n == 4) {
			this.secuencia();
			this.anunciar(nivel, Component.literal("Circuito montado. Sube los interruptores en el orden de la pantalla.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), Sonidos.ESTACION_ARRANCA);
		}
	}

	public void palanca(ServerPlayer j, int color) {
		ServerLevel nivel = j.level();
		if (this.e.pasarela) {
			j.displayClientMessage(Component.literal("La pasarela ya tiene corriente.").withStyle(ChatFormatting.GREEN), true);
			return;
		}
		if (!this.corriente()) {
			j.displayClientMessage(Component.literal("No responde: no hay corriente.").withStyle(ChatFormatting.RED), true);
			return;
		}
		if (this.fusiblesPuestos() < 4) {
			j.displayClientMessage(Component.literal("Faltan fusibles en la caja (" + this.fusiblesPuestos() + "/4).").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		if (this.pulsados.contains(color)) {
			return;
		}
		int[] l = PlanoEstacion.PALANCAS[color];
		nivel.playSound(null, l[0] + 0.5, l[1], l[2] + 0.5, Sonidos.ESTACION_PALANCA, SoundSource.BLOCKS, 1.2F, 1.0F);
		int[] s = this.secuencia();
		if (s[this.pulsados.size()] != color) {
			this.pulsados.clear();
			this.falloHasta = this.ticks + FALLO * 20L;
			this.anunciar(nivel, Component.literal("¡FALLO! " + j.getGameProfile().name() + " se ha equivocado de interruptor. Salta todo.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), Sonidos.ESTACION_FALLO);
			AmasijoEstacion.get().ruido(nivel);
			return;
		}
		this.pulsados.add(color);
		if (this.pulsados.size() < 4) {
			j.displayClientMessage(Component.literal("Interruptor " + NOMBRES_COLOR[color] + ": correcto (" + this.pulsados.size() + "/4).").withStyle(TINTAS[color]), true);
		}
		if (this.pulsados.size() == 4) {
			this.e.pasarela = true;
			this.focosDesde = this.ticks;
			this.sucio = true;
			this.guardar();
			this.anunciar(nivel, Component.literal("PASARELA ACTIVA. La ruta de escape está disponible: crúzala.").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), Sonidos.ESTACION_PASARELA);
			Misiones.get().completarParaTodos(TipoMision.FUSIBLES);
			AmasijoEstacion.get().ruido(nivel);
		}
	}

	public void persianaCerrada(ServerPlayer j, BlockPos pos) {
		for (PlanoEstacion.Persiana p : PlanoEstacion.PERSIANAS) {
			for (int k = 0; k < 9; k++) {
				int[] b = p.bloque(k);
				if (b[0] == pos.getX() && b[1] == pos.getY() && b[2] == pos.getZ()) {
					String m = p.id() < 4 ? "Cerrada. Se abre con los dos paneles del sector " + PlanoEstacion.SECTORES[p.id()] + " reparados."
						: "Cerrada. La pasarela no tiene corriente: hace falta el circuito principal.";
					j.displayClientMessage(Component.literal(m).withStyle(ChatFormatting.GRAY), true);
					return;
				}
			}
		}
	}

	/* ------------------------------------------------------------- utilidades */

	private PlanoEstacion.@Nullable Punto punto(BlockPos pos) {
		for (PlanoEstacion.Punto p : PlanoEstacion.PUNTOS) {
			if (p.x() == pos.getX() && p.y() == pos.getY() && p.z() == pos.getZ()) {
				return p;
			}
		}
		return null;
	}

	/** Le quita uno de ese objeto (de la mano o de donde lo tenga): false si no tiene. */
	private boolean quitar(ServerPlayer j, Item item) {
		if (j.getMainHandItem().is(item)) {
			j.getMainHandItem().shrink(1);
			return true;
		}
		for (int i = 0; i < j.getInventory().getContainerSize(); i++) {
			ItemStack s = j.getInventory().getItem(i);
			if (s.is(item)) {
				s.shrink(1);
				return true;
			}
		}
		return false;
	}

	private void anunciar(ServerLevel nivel, Component texto, @Nullable SoundEvent sonido) {
		for (ServerPlayer j : nivel.players()) {
			j.displayClientMessage(texto, false);
			if (sonido != null) {
				Sonidos.aJugador(j, sonido, 0.9F);
			}
		}
	}

	public void enviar(ServerPlayer j) {
		boolean aqui = Estacion.es(j.level());
		EstadoEstacion m = aqui ? new EstadoEstacion(this.e.combustible, this.reparados(), PlanoEstacion.PANELES.size(), this.fusiblesPuestos(), this.etapa(), this.corriente())
			: EstadoEstacion.FUERA;
		if (ServerPlayNetworking.canSend(j, EstadoEstacion.TYPE)) {
			ServerPlayNetworking.send(j, m);
		}
	}

	private void enviarATodos(ServerLevel nivel) {
		for (ServerPlayer j : nivel.players()) {
			this.enviar(j);
		}
	}

	/* ----------------------------------------------------------------- staff */

	/** Todo como al principio. */
	public void reponerTodo() {
		this.e = new Estado();
		this.pulsados.clear();
		this.vacios.clear();
		this.falloHasta = -1;
		this.focosDesde = -1;
		this.guardar();
	}

	public void combustible(float valor) {
		this.e.combustible = Math.max(0.0F, Math.min(MAXIMO, valor));
		if (this.e.combustible > 0 && !this.e.arrancado) {
			this.e.arrancado = true;
			Misiones.get().completarParaTodos(TipoMision.COMBUSTIBLE);
		}
		this.sucio = true;
	}

	/** Pasa a la etapa siguiente (pruebas): arranca, repara todo, pone los fusibles o da corriente a la pasarela. */
	public void avanzar() {
		switch (this.etapa()) {
			case 0 -> this.combustible(MAXIMO);
			case 1 -> {
				for (PlanoEstacion.Panel p : PlanoEstacion.PANELES) {
					this.e.reparados.add(p.id());
				}
				Misiones.get().completarParaTodos(TipoMision.PANELES);
			}
			default -> {
				java.util.Arrays.fill(this.e.fusibles, true);
				this.e.pasarela = true;
				this.focosDesde = this.ticks;
				Misiones.get().completarParaTodos(TipoMision.FUSIBLES);
			}
		}
		this.sucio = true;
	}

	/** Una linea por cosa, para el staff. */
	public List<String> resumen() {
		List<String> l = new ArrayList<>();
		l.add(String.format(java.util.Locale.ROOT, "Generador: %.0f %%%s", this.e.combustible, this.corriente() ? "" : " (SIN CORRIENTE)"));
		l.add("Paneles: " + this.reparados() + "/" + PlanoEstacion.PANELES.size() + "   ·   fusibles: " + this.fusiblesPuestos() + "/4   ·   pasarela: " + (this.e.pasarela ? "ACTIVA" : "sin corriente"));
		if (this.e.secuencia != null) {
			StringBuilder sb = new StringBuilder("Secuencia:");
			for (int c : this.e.secuencia) {
				sb.append(' ').append(NOMBRES_COLOR[c]);
			}
			l.add(sb.toString());
		}
		return l;
	}
}
