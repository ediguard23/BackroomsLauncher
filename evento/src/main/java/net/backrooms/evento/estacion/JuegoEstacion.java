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
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * El juego de la Estacion Electrica (Nivel 3), de todos a la vez (como en Backrooms: Escape Together):
 *
 *   GENERADOR  abajo, en la nave del piso 1: gasta gasolina (CONSUMO por segundo mientras hay alguien
 *              en el nivel) y se llena con bidones en su boca. Sin gasolina: apagon en todo el nivel y
 *              las centrales no se activan.
 *   REJAS      en los laberintos: una reja se abre con un fusible en su caja (en el muro de al lado) y
 *              se cierra si alguien lo saca (y te lo llevas). Sirven para cerrarle el paso al Amasijo.
 *   CENTRALES  al final de cada piso: piden fusibles, cinta y cable (PlanoEstacion.NECESITA). Con todo
 *              puesto y corriente, se activa: se abre la escalera al piso de arriba. La del piso 3 da
 *              corriente a la pasarela.
 *   LA CADENA  la puerta del final, al otro lado de la pasarela, esta encadenada: hace falta la cizalla
 *              (una sola, en un trastero del piso 3). Detras, la salida.
 *
 * Los puntos de recogida se vacian al coger y se reponen solos. El estado se guarda en
 * <mundo>/backrooms/estacion.json; los bloques los pone al dia el tick (ajustar).
 */
public final class JuegoEstacion {
	private static final JuegoEstacion INSTANCIA = new JuegoEstacion();
	/** Gasolina: el maximo, lo que da un bidon, el gasto por segundo y con lo que se empieza. */
	public static final float MAXIMO = 100.0F;
	private static final float BIDON = 30.0F;
	private static final float CONSUMO = 100.0F / (10 * 60);
	private static final float INICIAL = 40.0F;
	/** Por debajo de esto, la alarma del deposito. */
	private static final float RESERVA = 15.0F;
	/** Segundos que tarda en reponerse cada cosa (por tipo: cable, cizalla, cinta, bidon, fusible). */
	private static final int[] REPONER = {90, 60, 90, 120, 100};
	private static final String[] NOMBRE = {"un cable", "la cizalla", "cinta aislante", "un bidón de gasolina", "un fusible"};
	private static final String[] PIEZA = {"fusible", "cinta", "cable"};
	private static final ChatFormatting[] TINTAS = {ChatFormatting.RED, ChatFormatting.YELLOW, ChatFormatting.AQUA};

	private static final class Estado {
		float combustible = INICIAL;
		boolean arrancado;
		/** Las rejas con su fusible puesto (abiertas). */
		Set<Integer> rejas = new HashSet<>();
		/** Lo puesto en la central de cada piso: {fusibles, cintas, cables}. */
		int[][] puestos = new int[PlanoEstacion.PISOS][3];
		boolean[] activa = new boolean[PlanoEstacion.PISOS];
		/** La cadena de la puerta del final, cortada. */
		boolean cadena;
	}

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private Estado e = new Estado();
	private MinecraftServer servidor;
	private long ticks;
	private boolean sucio;
	/** Desde cuando se encienden los focos de la pasarela. */
	private long focosDesde = -1;
	/** Cuando se vacio cada punto (tick), para reponerlo. */
	private final Map<Integer, Long> vacios = new HashMap<>();
	private final Display.TextDisplay[] pantallas = new Display.TextDisplay[PlanoEstacion.PISOS];
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
			java.util.Arrays.fill(INSTANCIA.pantallas, null);
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
				if (l != null && l.puestos != null && l.puestos.length == PlanoEstacion.PISOS && l.activa != null && l.activa.length == PlanoEstacion.PISOS) {
					this.e = l;
					if (this.e.rejas == null) {
						this.e.rejas = new HashSet<>();
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

	/** Etapas hechas: 0 nada, 1 generador con gasolina, 2..4 centrales activas (4: la pasarela). */
	public int etapa() {
		int a = this.centrales();
		return a == 0 ? (this.e.arrancado ? 1 : 0) : 1 + a;
	}

	public int centrales() {
		int a = 0;
		for (boolean b : this.e.activa) {
			a += b ? 1 : 0;
		}
		return a;
	}

	public boolean activa(int piso) {
		return this.e.activa[piso];
	}

	public boolean corriente() {
		return this.e.combustible > 0;
	}

	public float combustible() {
		return this.e.combustible;
	}

	public boolean arrancado() {
		return this.e.arrancado;
	}

	public boolean pasarela() {
		return this.e.activa[PlanoEstacion.PISOS - 1];
	}

	/** El piso mas alto al que se puede llegar (donde se reaparece). */
	public int pisoAbierto() {
		int p = 0;
		while (p < PlanoEstacion.PISOS - 1 && this.e.activa[p]) {
			p++;
		}
		return p;
	}

	private boolean completa(int p) {
		for (int t = 0; t < 3; t++) {
			if (this.e.puestos[p][t] < PlanoEstacion.NECESITA[p][t]) {
				return false;
			}
		}
		return true;
	}

	private int puestos(int p) {
		return this.e.puestos[p][0] + this.e.puestos[p][1] + this.e.puestos[p][2];
	}

	private static int necesarios(int p) {
		int[] n = PlanoEstacion.NECESITA[p];
		return n[0] + n[1] + n[2];
	}

	/** Lo que cuenta la hoja de misiones ahora para ese jugador: {hechos, total} (la central de su piso). */
	public int[] cuenta(ServerPlayer j) {
		int p = PlanoEstacion.piso(j.getY());
		if (!this.e.arrancado && this.centrales() == 0) {
			return new int[] {Math.round(this.e.combustible), 100};
		}
		if (this.pasarela()) {
			return new int[] {this.e.cadena ? 1 : 0, 1};
		}
		return new int[] {this.puestos(p), necesarios(p)};
	}

	/** Adonde lleva el radar: {x, z}, o null. */
	public int @Nullable [] objetivo(ServerPlayer j) {
		int p = PlanoEstacion.piso(j.getY());
		if (this.e.combustible <= 0 || !this.e.arrancado && this.centrales() == 0 && p == 0 && this.e.combustible < 25) {
			return new int[] {PlanoEstacion.BOCA[0], PlanoEstacion.BOCA[2]};
		}
		if (this.pasarela()) {
			PlanoEstacion.Puerta f = PlanoEstacion.puertaFinal;
			if (this.e.cadena || this.tiene(j, BloquesEstacion.CIZALLA)) {
				return new int[] {PlanoEstacion.FINAL[0], PlanoEstacion.FINAL[1] + 1};
			}
			int[] c = this.puntoCercano(j, 2, new int[] {PlanoEstacion.CIZALLA});
			return c != null ? c : new int[] {f.x(), f.z()};
		}
		if (this.e.activa[p]) {
			PlanoEstacion.Puerta s = PlanoEstacion.ESCALERAS[p];
			return new int[] {s.x(), s.z()};
		}
		PlanoEstacion.Central c = PlanoEstacion.CENTRALES[p];
		if (this.completa(p)) {
			return new int[] {c.x(), c.z()};
		}
		// lo que falta en su central: si lo lleva, a la central; si no, al sitio mas cercano donde haya
		List<Integer> faltan = new ArrayList<>();
		Item[] piezas = {BloquesEstacion.FUSIBLE, BloquesEstacion.CINTA, BloquesEstacion.CABLE};
		int[] tipos = {PlanoEstacion.FUSIBLE, PlanoEstacion.CINTA, PlanoEstacion.CABLE};
		for (int t = 0; t < 3; t++) {
			if (this.e.puestos[p][t] < PlanoEstacion.NECESITA[p][t]) {
				if (this.tiene(j, piezas[t])) {
					return new int[] {c.x(), c.z()};
				}
				faltan.add(tipos[t]);
			}
		}
		int[] cerca = this.puntoCercano(j, p, faltan.stream().mapToInt(Integer::intValue).toArray());
		return cerca != null ? cerca : new int[] {c.x(), c.z()};
	}

	private int @Nullable [] puntoCercano(ServerPlayer j, int piso, int[] tipos) {
		PlanoEstacion.Punto mejor = null;
		double d = Double.MAX_VALUE;
		for (PlanoEstacion.Punto q : PlanoEstacion.PUNTOS) {
			if (q.piso() != piso || this.vacio(q)) {
				continue;
			}
			boolean sirve = false;
			for (int t : tipos) {
				sirve |= q.tipo() == t;
			}
			double dd = j.distanceToSqr(q.x() + 0.5, q.y(), q.z() + 0.5);
			if (sirve && dd < d) {
				d = dd;
				mejor = q;
			}
		}
		return mejor == null ? null : new int[] {mejor.x(), mejor.z()};
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
				this.anunciar(nivel, Component.literal("El generador se ha parado: sin gasolina. Todo se queda a oscuras.").withStyle(ChatFormatting.RED), Sonidos.ESTACION_PARADA);
			}
			this.sucio = true;
		}
		// la luz: apagon mientras no haya corriente
		boolean apagon = alguien && !this.corriente();
		if (apagon && !Ambiente.get().apagon(nivel)) {
			Ambiente.get().apagon(nivel, 3600);
			this.apagonPuesto = true;
		} else if (!apagon && this.apagonPuesto) {
			Ambiente.get().terminar(nivel);
			this.apagonPuesto = false;
		}
		if (this.ticks % 40 == 0) {
			this.limpiarFuera();
		}
		if (this.ticks % 10 == 0 && alguien) {
			// una central con todo puesto se activa en cuanto hay corriente
			for (int p = 0; p < PlanoEstacion.PISOS; p++) {
				if (!this.e.activa[p] && this.completa(p) && this.corriente()) {
					this.activar(nivel, p, null);
				}
			}
			this.reponer();
			this.ajustar(nivel);
			this.pantallas(nivel);
			this.salida(nivel);
			this.enviarATodos(nivel);
			// la alarma del deposito en reserva, cada 5 s
			if (this.e.combustible > 0 && this.e.combustible < RESERVA && this.ticks % 100 == 0) {
				nivel.playSound(null, PlanoEstacion.GENERADOR[0], PlanoEstacion.GENERADOR[1], PlanoEstacion.GENERADOR[2], Sonidos.ESTACION_RESERVA, SoundSource.BLOCKS, 2.5F, 1.0F);
			}
		}
		if (this.sucio && this.ticks % 200 == 0) {
			this.guardar();
		}
	}

	private boolean vacio(PlanoEstacion.Punto p) {
		if (p.tipo() == PlanoEstacion.CIZALLA) {
			return this.e.cadena || this.vacios.containsKey(p.id()) || this.alguienTiene(BloquesEstacion.CIZALLA);
		}
		return this.vacios.containsKey(p.id());
	}

	/** Los puntos vacios que ya toca reponer (la cizalla, solo si nadie la tiene). */
	private void reponer() {
		if (this.vacios.isEmpty()) {
			return;
		}
		for (Integer id : new ArrayList<>(this.vacios.keySet())) {
			PlanoEstacion.Punto p = PlanoEstacion.PUNTOS.get(id);
			if (this.ticks - this.vacios.get(id) < 20L * REPONER[Math.min(p.tipo(), REPONER.length - 1)]) {
				continue;
			}
			if (p.tipo() == PlanoEstacion.CIZALLA && this.alguienTiene(BloquesEstacion.CIZALLA)) {
				continue;
			}
			this.vacios.remove(id);
		}
	}

	/**
	 * Lo de la Estacion no sale de la Estacion: a quien ya no esta (eliminado, escapado, staff que
	 * se fue) se le quitan los fusibles, materiales, bidones y la cizalla.
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
			if (Estacion.es(j.level()) && this.tiene(j, item)) {
				return true;
			}
		}
		return false;
	}

	private boolean tiene(ServerPlayer j, Item item) {
		return j.getInventory().contains(new ItemStack(item)) || j.containerMenu.getCarried().is(item);
	}

	/** Pone los bloques como dice el estado (solo los cargados). */
	private void ajustar(ServerLevel nivel) {
		for (PlanoEstacion.Punto p : PlanoEstacion.PUNTOS) {
			this.poner(nivel, p.x(), p.y(), p.z(), Interactivos.VACIO, this.vacio(p));
		}
		for (PlanoEstacion.Puerta r : PlanoEstacion.REJAS) {
			boolean abierta = this.e.rejas.contains(r.id());
			this.abrir(nivel, r, abierta);
			this.poner(nivel, r.caja()[0], r.caja()[1], r.caja()[2], Interactivos.HECHO, abierta);
		}
		for (int p = 0; p < PlanoEstacion.PISOS; p++) {
			PlanoEstacion.Central c = PlanoEstacion.CENTRALES[p];
			this.poner(nivel, c.x(), c.y(), c.z(), Interactivos.HECHO, this.e.activa[p]);
			this.abrir(nivel, PlanoEstacion.ESCALERAS[p], this.e.activa[p]);
		}
		this.abrir(nivel, PlanoEstacion.puertaFinal, this.e.cadena);
		// los focos de la pasarela, de dos en dos desde que hay corriente
		for (int i = 0; i < PlanoEstacion.FOCOS.size(); i++) {
			int[] f = PlanoEstacion.FOCOS.get(i);
			boolean lit = this.pasarela() && this.corriente() && (this.focosDesde < 0 || this.ticks - this.focosDesde >= (i / 2) * 8L);
			this.poner(nivel, f[0], f[1], f[2], Interactivos.LIT, lit);
		}
	}

	private void abrir(ServerLevel nivel, PlanoEstacion.Puerta p, boolean abierta) {
		for (int k = 0; k < 9; k++) {
			int[] b = p.bloque(k);
			this.poner(nivel, b[0], b[1], b[2], Interactivos.ABIERTA, abierta);
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

	/** Las pantallas de encima de cada central (entidades de texto, solo con alguien cerca). */
	private void pantallas(ServerLevel nivel) {
		for (int p = 0; p < PlanoEstacion.PISOS; p++) {
			Display.TextDisplay t = this.pantallas[p];
			if (t != null && (t.isRemoved() || t.level() != nivel)) {
				t = null;
			}
			double[] c = PlanoEstacion.CENTRALES[p].pantalla();
			boolean cerca = nivel.getNearestPlayer(c[0], c[1], c[2], 32, false) != null;
			if (!cerca) {
				if (t != null) {
					t.discard();
				}
				this.pantallas[p] = null;
				continue;
			}
			if (t == null) {
				if (!nivel.isPositionEntityTicking(BlockPos.containing(c[0], c[1], c[2]))) {
					continue;
				}
				t = new Display.TextDisplay(EntityType.TEXT_DISPLAY, nivel) {
					@Override
					public boolean shouldBeSaved() {
						return false;
					}
				};
				t.setPos(c[0], c[1], c[2]);
				t.setYRot((float) c[3]);
				t.setViewRange(1.0F);
				nivel.addFreshEntity(t);
			}
			this.pantallas[p] = t;
			t.setText(this.textoPantalla(p));
		}
	}

	private Component textoPantalla(int p) {
		MutableComponent t = Component.literal("CENTRAL · " + PlanoEstacion.PISO[p] + "\n").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
		if (this.e.activa[p]) {
			return t.append(Component.literal(p < PlanoEstacion.PISOS - 1 ? "ACTIVA · ESCALERA ABIERTA" : "ACTIVA · PASARELA CON CORRIENTE").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
		}
		for (int k = 0; k < 3; k++) {
			int n = PlanoEstacion.NECESITA[p][k];
			if (n == 0) {
				continue;
			}
			int h = this.e.puestos[p][k];
			t.append(Component.literal(PIEZA[k].toUpperCase() + (n > 1 ? "S" : "") + " " + h + "/" + n + "   ").withStyle(h >= n ? ChatFormatting.GREEN : TINTAS[k]));
		}
		if (!this.corriente()) {
			t.append(Component.literal("\nSIN CORRIENTE").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
		}
		return t;
	}

	/** Quien llega al otro lado de la puerta del final, sale. */
	private void salida(ServerLevel nivel) {
		if (!this.e.cadena) {
			return;
		}
		int[] f = PlanoEstacion.FINAL;
		for (ServerPlayer j : new ArrayList<>(nivel.players())) {
			if (j.isSpectator() || j.isCreative() || !j.isAlive() || j.getY() < PlanoEstacion.SUELOS[2]
				|| j.getX() < f[0] || j.getX() > f[2] + 1 || j.getZ() < f[1] || j.getZ() > f[3] + 1) {
				continue;
			}
			Estacion.salir(j);
		}
	}

	/** La central del piso p se activa: escalera abierta (o la pasarela con corriente). */
	private void activar(ServerLevel nivel, int p, @Nullable ServerPlayer quien) {
		if (this.e.activa[p]) {
			return;
		}
		if (!this.e.arrancado) {
			this.e.arrancado = true;
			Misiones.get().completarParaTodos(TipoMision.COMBUSTIBLE);
			AmasijoEstacion.get().despertar();
		}
		this.e.activa[p] = true;
		this.sucio = true;
		this.guardar();
		TipoMision[] mision = {TipoMision.PISO1, TipoMision.PISO2, TipoMision.PISO3};
		Misiones.get().completarParaTodos(mision[p]);
		if (p == PlanoEstacion.PISOS - 1) {
			this.focosDesde = this.ticks;
			this.anunciar(nivel, Component.literal("CENTRAL DEL PISO 3 ACTIVA. La pasarela tiene corriente: crúzala. Al otro lado, una puerta encadenada.")
				.withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), Sonidos.ESTACION_PASARELA);
		} else {
			this.anunciar(nivel, Component.literal("CENTRAL DEL " + PlanoEstacion.PISO[p] + " ACTIVA. Se abre la escalera al " + PlanoEstacion.PISO[p + 1] + ".")
				.withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), Sonidos.ESTACION_ARRANCA);
			PlanoEstacion.Puerta s = PlanoEstacion.ESCALERAS[p];
			nivel.playSound(null, s.x() + 0.5, s.y() + 2, s.z() + 0.5, Sonidos.ESTACION_PERSIANA, SoundSource.BLOCKS, 2.0F, 1.0F);
		}
		BackroomsEvento.LOG.info("Estacion: central del piso {} activa{}", p + 1, quien == null ? "" : " (" + quien.getGameProfile().name() + ")");
		// cada sistema que vuelve despierta un poco mas la estacion
		AmasijoEstacion.get().ruido(nivel);
	}

	/* ---------------------------------------------------------- lo que se hace */

	public void recoger(ServerPlayer j, BlockPos pos, BlockState s) {
		PlanoEstacion.Punto p = this.punto(pos);
		if (p == null) {
			return;
		}
		if (this.vacio(p)) {
			String m = p.tipo() == PlanoEstacion.CIZALLA && this.e.cadena ? "La cizalla ya no hace falta: la cadena está cortada."
				: p.tipo() == PlanoEstacion.CIZALLA ? "Aquí estaba la cizalla. La tiene alguien." : "Vacío. Alguien se lo ha llevado: vuelve más tarde.";
			j.displayClientMessage(Component.literal(m).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		Item item = BloquesEstacion.deTipo(p.tipo());
		if (p.tipo() == PlanoEstacion.BIDON && this.tiene(j, BloquesEstacion.BIDON)) {
			j.displayClientMessage(Component.literal("Ya llevas un bidón: pesa demasiado para dos.").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		if (!j.getInventory().add(new ItemStack(item))) {
			j.displayClientMessage(Component.literal("No te cabe: haz sitio en la barra (suelta algo con Q).").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		this.vacios.put(p.id(), this.ticks);
		ServerLevel nivel = j.level();
		nivel.playSound(null, pos, p.tipo() == PlanoEstacion.BIDON ? Sonidos.ESTACION_BIDON : Sonidos.ESTACION_COGER, SoundSource.PLAYERS, 1.0F, 1.0F);
		this.poner(nivel, pos.getX(), pos.getY(), pos.getZ(), Interactivos.VACIO, true);
		j.displayClientMessage(Component.literal("Has cogido " + NOMBRE[Math.min(p.tipo(), NOMBRE.length - 1)] + ".").withStyle(ChatFormatting.YELLOW), true);
		if (p.tipo() == PlanoEstacion.CIZALLA) {
			this.anunciar(nivel, Component.literal(j.getGameProfile().name() + " ha encontrado la cizalla.").withStyle(ChatFormatting.GOLD), null);
		}
	}

	public void repostar(ServerPlayer j) {
		ServerLevel nivel = j.level();
		if (!this.quitar(j, BloquesEstacion.BIDON)) {
			j.displayClientMessage(Component.literal("Depósito al " + Math.round(this.e.combustible) + " %. Trae un bidón de gasolina.").withStyle(ChatFormatting.GOLD), true);
			return;
		}
		boolean parado = this.e.combustible <= 0;
		this.e.combustible = Math.min(MAXIMO, this.e.combustible + BIDON);
		this.sucio = true;
		nivel.playSound(null, PlanoEstacion.BOCA[0] + 0.5, PlanoEstacion.BOCA[1], PlanoEstacion.BOCA[2] + 0.5, Sonidos.ESTACION_REPOSTAR, SoundSource.BLOCKS, 1.2F, 1.0F);
		j.displayClientMessage(Component.literal("Depósito al " + Math.round(this.e.combustible) + " %.").withStyle(ChatFormatting.GOLD), true);
		if (parado) {
			this.anunciar(nivel, Component.literal("El generador arranca. Vuelve la corriente.").withStyle(ChatFormatting.GREEN), Sonidos.ESTACION_ARRANCA);
		}
		if (!this.e.arrancado) {
			this.e.arrancado = true;
			Misiones.get().completarParaTodos(TipoMision.COMBUSTIBLE);
			AmasijoEstacion.get().despertar();
		}
	}

	/** La caja de una reja: si tiene fusible, se saca (y la reja se cierra); si no, se pone el tuyo. */
	public void cajaReja(ServerPlayer j, BlockPos pos) {
		PlanoEstacion.Puerta r = null;
		for (PlanoEstacion.Puerta q : PlanoEstacion.REJAS) {
			if (q.caja()[0] == pos.getX() && q.caja()[1] == pos.getY() && q.caja()[2] == pos.getZ()) {
				r = q;
			}
		}
		if (r == null) {
			return;
		}
		ServerLevel nivel = j.level();
		int[] c = r.bloque(4);
		if (this.e.rejas.contains(r.id())) {
			// sacarlo: la reja baja (si no hay nadie debajo)
			int[] a = r.bloque(0);
			int[] b = r.bloque(8);
			AABB hueco = new AABB(Math.min(a[0], b[0]), a[1], Math.min(a[2], b[2]), Math.max(a[0], b[0]) + 1, b[1] + 1, Math.max(a[2], b[2]) + 1);
			if (!nivel.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, hueco, en -> en.isAlive() && !en.isSpectator()).isEmpty()) {
				j.displayClientMessage(Component.literal("Hay alguien debajo de la reja: no baja.").withStyle(ChatFormatting.GRAY), true);
				return;
			}
			if (!j.getInventory().add(new ItemStack(BloquesEstacion.FUSIBLE))) {
				j.displayClientMessage(Component.literal("No te cabe el fusible: haz sitio en la barra.").withStyle(ChatFormatting.GRAY), true);
				return;
			}
			this.e.rejas.remove(r.id());
			this.sucio = true;
			nivel.playSound(null, pos, Sonidos.ESTACION_FUSIBLE, SoundSource.BLOCKS, 1.0F, 0.7F);
			nivel.playSound(null, c[0] + 0.5, c[1], c[2] + 0.5, Sonidos.ESTACION_PALANCA, SoundSource.BLOCKS, 1.6F, 0.7F);
			j.displayClientMessage(Component.literal("Has sacado el fusible: la reja se cierra.").withStyle(ChatFormatting.GOLD), true);
		} else {
			if (!this.quitar(j, BloquesEstacion.FUSIBLE)) {
				j.displayClientMessage(Component.literal("Caja de fusible vacía: pon un fusible para abrir la reja.").withStyle(ChatFormatting.GRAY), true);
				return;
			}
			this.e.rejas.add(r.id());
			this.sucio = true;
			nivel.playSound(null, pos, Sonidos.ESTACION_FUSIBLE, SoundSource.BLOCKS, 1.0F, 1.0F);
			nivel.playSound(null, c[0] + 0.5, c[1], c[2] + 0.5, Sonidos.ESTACION_PERSIANA, SoundSource.BLOCKS, 1.4F, 1.4F);
			j.displayClientMessage(Component.literal("La reja se abre. Si alguien saca el fusible, se vuelve a cerrar.").withStyle(ChatFormatting.YELLOW), true);
		}
		this.abrir(nivel, r, this.e.rejas.contains(r.id()));
		this.poner(nivel, pos.getX(), pos.getY(), pos.getZ(), Interactivos.HECHO, this.e.rejas.contains(r.id()));
	}

	/** La central de un piso: pone lo que lleves y le falte; con todo y corriente, se activa. */
	public void central(ServerPlayer j, BlockPos pos) {
		int p = -1;
		for (PlanoEstacion.Central c : PlanoEstacion.CENTRALES) {
			if (c.x() == pos.getX() && c.y() == pos.getY() && c.z() == pos.getZ()) {
				p = c.piso();
			}
		}
		if (p < 0) {
			return;
		}
		ServerLevel nivel = j.level();
		if (this.e.activa[p]) {
			j.displayClientMessage(Component.literal(p < PlanoEstacion.PISOS - 1 ? "Esta central ya funciona: la escalera está abierta." : "Esta central ya funciona: la pasarela tiene corriente.")
				.withStyle(ChatFormatting.GREEN), true);
			return;
		}
		Item[] piezas = {BloquesEstacion.FUSIBLE, BloquesEstacion.CINTA, BloquesEstacion.CABLE};
		int[] puso = new int[3];
		for (int t = 0; t < 3; t++) {
			while (this.e.puestos[p][t] < PlanoEstacion.NECESITA[p][t] && this.quitar(j, piezas[t])) {
				this.e.puestos[p][t]++;
				puso[t]++;
			}
		}
		if (puso[0] + puso[1] + puso[2] == 0) {
			j.displayClientMessage(Component.literal("Le falta: " + this.falta(p) + ".").withStyle(ChatFormatting.GOLD), true);
			return;
		}
		this.sucio = true;
		nivel.playSound(null, pos, Sonidos.ESTACION_REPARAR, SoundSource.BLOCKS, 1.2F, 1.0F);
		List<String> l = new ArrayList<>();
		for (int t = 0; t < 3; t++) {
			if (puso[t] > 0) {
				l.add(puso[t] + " " + PIEZA[t] + (puso[t] > 1 ? "s" : ""));
			}
		}
		this.anunciar(nivel, Component.literal(j.getGameProfile().name() + " pone " + String.join(" y ", l) + " en la central del " + PlanoEstacion.PISO[p]
			+ " (" + this.puestos(p) + "/" + necesarios(p) + ").").withStyle(TINTAS[p]), null);
		if (this.completa(p)) {
			if (this.corriente()) {
				this.activar(nivel, p, j);
			} else {
				this.anunciar(nivel, Component.literal("La central del " + PlanoEstacion.PISO[p] + " está lista, pero no hay corriente: gasolina al generador.").withStyle(ChatFormatting.GOLD), null);
			}
		} else {
			j.displayClientMessage(Component.literal("Aún le falta: " + this.falta(p) + ".").withStyle(ChatFormatting.GOLD), true);
		}
	}

	private String falta(int p) {
		List<String> l = new ArrayList<>();
		for (int t = 0; t < 3; t++) {
			int n = PlanoEstacion.NECESITA[p][t] - this.e.puestos[p][t];
			if (n > 0) {
				l.add(n + " " + PIEZA[t] + (n > 1 ? "s" : ""));
			}
		}
		return String.join(", ", l);
	}

	/** Alguien empuja una puerta cerrada: que sepa por que. La del final se abre con la cizalla. */
	public void persianaCerrada(ServerPlayer j, BlockPos pos) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		for (PlanoEstacion.Puerta r : PlanoEstacion.REJAS) {
			if (r.es(x, y, z)) {
				j.displayClientMessage(Component.literal("Reja cerrada. Su caja de fusible está en el muro, al lado.").withStyle(ChatFormatting.GRAY), true);
				return;
			}
		}
		for (int p = 0; p < PlanoEstacion.PISOS; p++) {
			if (PlanoEstacion.ESCALERAS[p].es(x, y, z)) {
				String m = p < PlanoEstacion.PISOS - 1 ? "Cerrada. Se abre al activar la central de este piso."
					: "Cerrada. La pasarela no tiene corriente: hace falta la central de este piso.";
				j.displayClientMessage(Component.literal(m).withStyle(ChatFormatting.GRAY), true);
				return;
			}
		}
		if (PlanoEstacion.puertaFinal.es(x, y, z) && !this.e.cadena) {
			if (!this.tiene(j, BloquesEstacion.CIZALLA)) {
				j.displayClientMessage(Component.literal("Una cadena gruesa la cierra. Hace falta una cizalla: hay una en algún trastero del piso 3.").withStyle(ChatFormatting.GOLD), true);
				return;
			}
			ServerLevel nivel = j.level();
			this.e.cadena = true;
			this.sucio = true;
			this.guardar();
			nivel.playSound(null, pos, Sonidos.ESTACION_PALANCA, SoundSource.BLOCKS, 2.0F, 1.4F);
			this.anunciar(nivel, Component.literal(j.getGameProfile().name() + " ha cortado la cadena. La puerta del final está abierta: ¡cruzad la pasarela!")
				.withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), Sonidos.ESTACION_PERSIANA);
			this.abrir(nivel, PlanoEstacion.puertaFinal, true);
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
		EstadoEstacion m = EstadoEstacion.FUERA;
		if (Estacion.es(j.level())) {
			int p = PlanoEstacion.piso(j.getY());
			m = new EstadoEstacion(this.e.combustible, p, this.puestos(p), necesarios(p), this.centrales(), this.corriente(), this.e.cadena);
		}
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
		this.vacios.clear();
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

	/** Pasa a lo siguiente (pruebas): gasolina, la central siguiente o la cadena. */
	public void avanzar() {
		ServerLevel nivel = Estacion.nivel();
		if (!this.e.arrancado) {
			this.combustible(MAXIMO);
			return;
		}
		for (int p = 0; p < PlanoEstacion.PISOS; p++) {
			if (!this.e.activa[p]) {
				this.e.puestos[p] = PlanoEstacion.NECESITA[p].clone();
				if (nivel != null) {
					if (this.e.combustible <= 0) {
						this.e.combustible = MAXIMO;
					}
					this.activar(nivel, p, null);
				}
				return;
			}
		}
		this.e.cadena = true;
		this.sucio = true;
	}

	/** Una linea por cosa, para el staff. */
	public List<String> resumen() {
		List<String> l = new ArrayList<>();
		l.add(String.format(java.util.Locale.ROOT, "Generador: %.0f %%%s", this.e.combustible, this.corriente() ? "" : " (SIN CORRIENTE)"));
		StringBuilder sb = new StringBuilder("Centrales:");
		for (int p = 0; p < PlanoEstacion.PISOS; p++) {
			sb.append("  ").append(PlanoEstacion.PISO[p]).append(' ').append(this.e.activa[p] ? "ACTIVA" : this.puestos(p) + "/" + necesarios(p));
		}
		l.add(sb.toString());
		l.add("Rejas abiertas: " + this.e.rejas.size() + "/" + PlanoEstacion.REJAS.size() + "   ·   cadena del final: " + (this.e.cadena ? "cortada" : "puesta"));
		return l;
	}
}
