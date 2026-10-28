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
import net.backrooms.evento.mision.Misiones;
import net.backrooms.evento.mision.TipoMision;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
 *   REJAS      puertas corredizas: con un fusible en la caja de su marco, la hoja se desliza a un lado
 *              (en ANIMA ticks); si alguien saca el fusible (y se lo lleva), vuelve. Sirven para
 *              cerrarle el paso al Amasijo.
 *   BUZONES    se abren con un fusible (que se queda dentro) y tienen algo... o nada.
 *   CENTRALES  al final de cada piso: piden fusibles, cinta y cable (PlanoEstacion.NECESITA). Con todo
 *              puesto se activan y se abre su puerta al piso siguiente. La del piso 3 da corriente a
 *              la pasarela.
 *   LA CADENA  la puerta del final, al otro lado de la pasarela, esta encadenada: hace falta la cizalla
 *              (una sola, en un trastero del piso 3). Detras, la salida.
 *
 * Los puntos de recogida se vacian al coger y se reponen solos. El estado se guarda en
 * <mundo>/backrooms/estacion.json; los bloques los pone al dia el tick (ajustar).
 */
public final class JuegoEstacion {
	private static final JuegoEstacion INSTANCIA = new JuegoEstacion();
	/** Segundos que tarda en reponerse cada cosa (por tipo: cable, cizalla, cinta, -, fusible): 5 minutos. */
	private static final int[] REPONER = {300, 300, 300, 300, 300};
	private static final String[] NOMBRE = {"un cable", "la cizalla", "cinta aislante", "un fusible", "un fusible"};
	private static final String[] PIEZA = {"fusible", "cinta aislante", "cable"};
	private static final String[] PIEZAS = {"fusibles", "cintas aislantes", "cables"};
	/** Lo que tarda en abrirse o cerrarse una reja o una persiana (ticks). */
	private static final int ANIMA = 20;
	private static final ChatFormatting[] TINTAS = {ChatFormatting.RED, ChatFormatting.YELLOW, ChatFormatting.AQUA};

	private static final class Estado {
		/** Las rejas con su fusible puesto (abiertas). */
		Set<Integer> rejas = new HashSet<>();
		/** Lo puesto en la central de cada piso: {fusibles, cintas, cables}. */
		int[][] puestos = new int[PlanoEstacion.PISOS][3];
		boolean[] activa = new boolean[PlanoEstacion.PISOS];
		/** La cadena de la puerta del final, cortada. */
		boolean cadena;
		/** Los buzones ya abiertos (vacios). */
		Set<Integer> buzones = new HashSet<>();
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
	/** Las puertas que se estan moviendo (subiendo o bajando), con sus piezas dibujadas. */
	private final Map<PlanoEstacion.Puerta, Animacion> animando = new HashMap<>();
	/** El corte de la cadena en marcha (la animacion) y cuando empezo: la persiana sube a los CorteCadena.PUERTA ticks. */
	private CorteCadena corte;
	private long corteDesde = -1;
	private final java.util.Random azar = new java.util.Random();

	/** Una puerta abriendose o cerrandose: sus piezas como BlockDisplay que se deslizan. */
	private static final class Animacion {
		final boolean abriendo;
		final long inicio;
		final List<Display.BlockDisplay> piezas = new ArrayList<>();
		boolean lanzada;

		Animacion(boolean abriendo, long inicio) {
			this.abriendo = abriendo;
			this.inicio = inicio;
		}
	}

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
					if (this.e.buzones == null) {
						this.e.buzones = new HashSet<>();
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

	/** Etapas hechas: las centrales activas (3: la pasarela tiene corriente). */
	public int etapa() {
		return this.centrales();
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
		int p = PlanoEstacion.piso(j.getZ());
		if (this.pasarela()) {
			return new int[] {this.e.cadena ? 1 : 0, 1};
		}
		return new int[] {this.puestos(p), necesarios(p)};
	}

	/**
	 * Adonde lleva el radar: en la Estacion, a ningun sitio (como en Escape Together: hay que
	 * encontrar la central y los materiales sin ayuda).
	 */
	public int @Nullable [] objetivo(ServerPlayer j) {
		return null;
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
		if (this.ticks % 40 == 0) {
			this.limpiarFuera();
		}
		this.animar(nivel);
		if (this.corte != null && this.corte.tick(nivel)) {
			this.corte = null;
		}
		if (this.corteDesde >= 0 && this.ticks - this.corteDesde == CorteCadena.PUERTA) {
			int[] b = PlanoEstacion.puertaFinal.bloque(4);
			nivel.playSound(null, b[0] + 0.5, b[1] + 0.5, b[2] + 0.5, Sonidos.ESTACION_PERSIANA, SoundSource.BLOCKS, 2.0F, 0.9F);
			this.abrir(nivel, PlanoEstacion.puertaFinal, true);
		}
		if (this.ticks % 10 == 0 && alguien) {
			for (int p = 0; p < PlanoEstacion.PISOS; p++) {
				if (!this.e.activa[p] && this.completa(p)) {
					this.activar(nivel, p, null);
				}
			}
			this.reponer();
			this.ajustar(nivel);
			this.pantallas(nivel);
			this.salida(nivel);
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
	 * se fue) se le quitan los fusibles, los materiales y la cizalla.
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
			this.abrir(nivel, PlanoEstacion.SALIDAS[p], this.e.activa[p]);
		}
		boolean cortando = this.corteDesde >= 0 && this.ticks - this.corteDesde < CorteCadena.PUERTA;
		this.abrir(nivel, PlanoEstacion.puertaFinal, this.e.cadena && !cortando);
		if (!cortando) {
			this.cadena(nivel);
		}
		for (PlanoEstacion.Buzon b : PlanoEstacion.BUZONES) {
			this.poner(nivel, b.x(), b.y(), b.z(), Interactivos.ABIERTA, this.e.buzones.contains(b.id()));
		}
		// los focos de la pasarela, de dos en dos desde que hay corriente
		for (int i = 0; i < PlanoEstacion.FOCOS.size(); i++) {
			int[] f = PlanoEstacion.FOCOS.get(i);
			boolean lit = this.pasarela() && (this.focosDesde < 0 || this.ticks - this.focosDesde >= (i / 2) * 8L);
			this.poner(nivel, f[0], f[1], f[2], Interactivos.LIT, lit);
		}
	}

	/**
	 * Que la puerta acabe abierta o cerrada. Si esta a la vista de alguien, se mueve en ANIMA ticks: la
	 * reja corrediza se desliza hacia su marco de la izquierda y la persiana sube (abriendose sigue
	 * chocando hasta el final; cerrandose ya se puede pasar hasta que llega). Si no hay nadie cerca, se
	 * pone sin mas.
	 */
	private void abrir(ServerLevel nivel, PlanoEstacion.Puerta p, boolean abierta) {
		Animacion a = this.animando.get(p);
		if (a != null) {
			if (a.abriendo == abierta) {
				return;
			}
			// cambia de idea a medio camino: se para y empieza al reves
			a.piezas.forEach(Display.BlockDisplay::discard);
			this.animando.remove(p);
		}
		int[] b0 = p.bloque(0);
		BlockPos pos = new BlockPos(b0[0], b0[1], b0[2]);
		if (!nivel.isLoaded(pos)) {
			return;
		}
		BlockState s0 = nivel.getBlockState(pos);
		if (!s0.hasProperty(Interactivos.ABIERTA)) {
			return;
		}
		if (s0.getValue(Interactivos.ABIERTA) == abierta && !s0.getValue(Interactivos.OCULTA)) {
			return;
		}
		boolean visto = nivel.getNearestPlayer(b0[0] + 0.5, b0[1], b0[2] + 0.5, 40, false) != null;
		if (!visto || !nivel.isPositionEntityTicking(pos)) {
			this.fijar(nivel, p, abierta);
			return;
		}
		Animacion n = new Animacion(abierta, this.ticks);
		for (int k = 0; k < p.partes(); k++) {
			int[] b = p.bloque(k);
			BlockPos bp = new BlockPos(b[0], b[1], b[2]);
			BlockState st = nivel.getBlockState(bp);
			if (!st.hasProperty(Interactivos.PARTE)) {
				continue;
			}
			// abriendose: sigue chocando, sin dibujarse; bajando: ya se puede pasar hasta que llega
			nivel.setBlock(bp, st.setValue(Interactivos.ABIERTA, !abierta).setValue(Interactivos.OCULTA, true), Block.UPDATE_CLIENTS);
			Display.BlockDisplay d = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, nivel) {
				@Override
				public boolean shouldBeSaved() {
					return false;
				}
			};
			d.setBlockState(st.setValue(Interactivos.ABIERTA, false).setValue(Interactivos.OCULTA, false));
			d.setPos(b[0], b[1], b[2]);
			d.setViewRange(1.0F);
			d.setBrightnessOverride(new net.minecraft.util.Brightness(nivel.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, bp), 0));
			d.setTransformation(new com.mojang.math.Transformation(abierta ? new org.joml.Vector3f() : desplazada(p), null, null, null));
			nivel.addFreshEntity(d);
			n.piezas.add(d);
		}
		this.animando.put(p, n);
	}

	/** Donde queda la hoja abierta respecto a la cerrada: un bloque hacia el marco (corrediza) o 3 arriba. */
	private static org.joml.Vector3f desplazada(PlanoEstacion.Puerta p) {
		return p.corrediza() ? new org.joml.Vector3f(-p.lado().getStepX(), 0.0F, -p.lado().getStepZ()) : new org.joml.Vector3f(0.0F, 3.0F, 0.0F);
	}

	/** Cada tick: lanza el movimiento de las piezas (un tick despues de crearlas) y acaba los que han llegado. */
	private void animar(ServerLevel nivel) {
		if (this.animando.isEmpty()) {
			return;
		}
		for (var en : new ArrayList<>(this.animando.entrySet())) {
			Animacion a = en.getValue();
			long edad = this.ticks - a.inicio;
			if (!a.lanzada && edad >= 2) {
				a.lanzada = true;
				for (Display.BlockDisplay d : a.piezas) {
					d.setTransformationInterpolationDelay(0);
					d.setTransformationInterpolationDuration(ANIMA);
					d.setTransformation(new com.mojang.math.Transformation(a.abriendo ? desplazada(en.getKey()) : new org.joml.Vector3f(), null, null, null));
				}
			}
			if (edad >= ANIMA + 3) {
				this.fijar(nivel, en.getKey(), a.abriendo);
				a.piezas.forEach(Display.BlockDisplay::discard);
				this.animando.remove(en.getKey());
			}
		}
	}

	/** La cadena de la puerta del final como dice el estado: puesta, o cortada y tirada en el suelo. */
	private void cadena(ServerLevel nivel) {
		int[] c = PlanoEstacion.CADENA;
		BlockPos monton = new BlockPos(c[0] + 1, c[1] - 1, c[2]);
		if (!nivel.isLoaded(monton)) {
			return;
		}
		for (int k = 0; k < 3; k++) {
			BlockPos p = new BlockPos(c[0] + k, c[1], c[2]);
			BlockState s = nivel.getBlockState(p);
			if (this.e.cadena && s.is(BloquesEstacion.CADENA)) {
				nivel.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			} else if (!this.e.cadena && s.isAir()) {
				nivel.setBlock(p, CorteCadena.estado(k), Block.UPDATE_CLIENTS);
			}
		}
		BlockState m = nivel.getBlockState(monton);
		if (this.e.cadena && m.isAir()) {
			nivel.setBlock(monton, CorteCadena.estado(5), Block.UPDATE_CLIENTS);
		} else if (!this.e.cadena && m.is(BloquesEstacion.CADENA)) {
			nivel.setBlock(monton, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** Pone la puerta abierta o cerrada del todo (sin animacion). */
	private void fijar(ServerLevel nivel, PlanoEstacion.Puerta p, boolean abierta) {
		for (int k = 0; k < p.partes(); k++) {
			int[] b = p.bloque(k);
			BlockPos bp = new BlockPos(b[0], b[1], b[2]);
			if (!nivel.isLoaded(bp)) {
				continue;
			}
			BlockState st = nivel.getBlockState(bp);
			if (st.hasProperty(Interactivos.ABIERTA)
				&& (st.getValue(Interactivos.ABIERTA) != abierta || st.getValue(Interactivos.OCULTA))) {
				nivel.setBlock(bp, st.setValue(Interactivos.ABIERTA, abierta).setValue(Interactivos.OCULTA, false), Block.UPDATE_CLIENTS);
			}
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
			return t.append(Component.literal(p < PlanoEstacion.PISOS - 1 ? "ACTIVA · PUERTA ABIERTA" : "ACTIVA · PASARELA CON CORRIENTE").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
		}
		for (int k = 0; k < 3; k++) {
			int n = PlanoEstacion.NECESITA[p][k];
			if (n == 0) {
				continue;
			}
			int h = this.e.puestos[p][k];
			t.append(Component.literal(PIEZA[k].toUpperCase() + (n > 1 ? "S" : "") + " " + h + "/" + n + "   ").withStyle(h >= n ? ChatFormatting.GREEN : TINTAS[k]));
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
			if (j.isSpectator() || j.isCreative() || !j.isAlive() || j.getY() < f[4]
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
			this.anunciar(nivel, Component.literal("CENTRAL DEL " + PlanoEstacion.PISO[p] + " ACTIVA. Se abre la puerta al " + PlanoEstacion.PISO[p + 1] + ".")
				.withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), Sonidos.ESTACION_ARRANCA);
			PlanoEstacion.Puerta s = PlanoEstacion.SALIDAS[p];
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
		if (!j.getInventory().add(new ItemStack(item))) {
			j.displayClientMessage(Component.literal("No te cabe: haz sitio en la barra (suelta algo con Q).").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		this.vacios.put(p.id(), this.ticks);
		ServerLevel nivel = j.level();
		nivel.playSound(null, pos, Sonidos.ESTACION_COGER, SoundSource.PLAYERS, 1.0F, 1.0F);
		this.poner(nivel, pos.getX(), pos.getY(), pos.getZ(), Interactivos.VACIO, true);
		j.displayClientMessage(Component.literal("Has cogido " + NOMBRE[Math.min(p.tipo(), NOMBRE.length - 1)] + ".").withStyle(ChatFormatting.YELLOW), true);
		if (p.tipo() == PlanoEstacion.CIZALLA) {
			this.anunciar(nivel, Component.literal(j.getGameProfile().name() + " ha encontrado la cizalla.").withStyle(ChatFormatting.GOLD), null);
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
		int[] c = r.bloque(1);
		if (this.animando.containsKey(r)) {
			j.displayClientMessage(Component.literal("La reja se está moviendo.").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		if (this.e.rejas.contains(r.id())) {
			// sacarlo: la reja baja (si no hay nadie debajo)
			int[] a = r.bloque(0);
			int[] b = r.bloque(r.partes() - 1);
			AABB hueco = new AABB(Math.min(a[0], b[0]), a[1], Math.min(a[2], b[2]), Math.max(a[0], b[0]) + 1, b[1] + 1, Math.max(a[2], b[2]) + 1);
			if (!nivel.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, hueco, en -> en.isAlive() && !en.isSpectator()).isEmpty()) {
				j.displayClientMessage(Component.literal("Hay alguien en medio de la reja: no se cierra.").withStyle(ChatFormatting.GRAY), true);
				return;
			}
			if (!j.getInventory().add(new ItemStack(BloquesEstacion.FUSIBLE))) {
				j.displayClientMessage(Component.literal(LimiteFusibles.fusibles(j.getInventory()) >= LimiteFusibles.MAX
					? "Ya llevas " + LimiteFusibles.MAX + " fusibles: no puedes llevar más." : "No te cabe el fusible: haz sitio en la barra.").withStyle(ChatFormatting.GRAY), true);
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

	/** La central de un piso: pone lo que lleves y le falte; con todo, se activa. */
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
			j.displayClientMessage(Component.literal(p < PlanoEstacion.PISOS - 1 ? "Esta central ya funciona: su puerta está abierta." : "Esta central ya funciona: la pasarela tiene corriente.")
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
				l.add(puso[t] + " " + (puso[t] > 1 ? PIEZAS[t] : PIEZA[t]));
			}
		}
		String lista = l.size() > 1 ? String.join(", ", l.subList(0, l.size() - 1)) + " y " + l.get(l.size() - 1) : l.get(0);
		this.anunciar(nivel, Component.literal(j.getGameProfile().name() + " ha colocado " + lista + " en la central del " + PlanoEstacion.PISO[p].toLowerCase()
			+ " (" + this.puestos(p) + "/" + necesarios(p) + ").").withStyle(TINTAS[p]), null);
		if (this.completa(p)) {
			this.activar(nivel, p, j);
		} else {
			j.displayClientMessage(Component.literal("Aún le falta: " + this.falta(p) + ".").withStyle(ChatFormatting.GOLD), true);
		}
	}

	private String falta(int p) {
		List<String> l = new ArrayList<>();
		for (int t = 0; t < 3; t++) {
			int n = PlanoEstacion.NECESITA[p][t] - this.e.puestos[p][t];
			if (n > 0) {
				l.add(n + " " + (n > 1 ? PIEZAS[t] : PIEZA[t]));
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
				j.displayClientMessage(Component.literal("Reja cerrada. Su caja de fusible está en el marco de al lado.").withStyle(ChatFormatting.GRAY), true);
				return;
			}
		}
		for (int p = 0; p < PlanoEstacion.PISOS; p++) {
			if (PlanoEstacion.SALIDAS[p].es(x, y, z)) {
				String m = p < PlanoEstacion.PISOS - 1 ? "Cerrada. Se abre al activar esta central."
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
			// el corte: el candado salta, la cadena cae y despues sube la persiana (CorteCadena)
			if (this.corte != null) {
				this.corte.quitar();
			}
			this.corte = CorteCadena.empezar(nivel);
			this.corteDesde = this.ticks;
			this.anunciar(nivel, Component.literal(j.getGameProfile().name() + " ha cortado la cadena. La puerta del final está abierta: ¡cruzad la pasarela!")
				.withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), null);
		}
	}

	/**
	 * Un buzon: con un fusible (que se queda dentro) se abre y da lo que tenga. No siempre hay
	 * material: a veces comida, a veces un fusible, a veces nada.
	 */
	public void buzon(ServerPlayer j, BlockPos pos) {
		PlanoEstacion.Buzon b = null;
		for (PlanoEstacion.Buzon q : PlanoEstacion.BUZONES) {
			if (q.x() == pos.getX() && q.y() == pos.getY() && q.z() == pos.getZ()) {
				b = q;
			}
		}
		if (b == null) {
			return;
		}
		if (this.e.buzones.contains(b.id())) {
			j.displayClientMessage(Component.literal("Este buzón ya está abierto y vacío.").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		if (!this.quitar(j, BloquesEstacion.FUSIBLE)) {
			j.displayClientMessage(Component.literal("Un buzón con una ranura para fusible. El fusible se queda dentro: no se recupera.").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		ServerLevel nivel = j.level();
		this.e.buzones.add(b.id());
		this.sucio = true;
		this.poner(nivel, pos.getX(), pos.getY(), pos.getZ(), Interactivos.ABIERTA, true);
		nivel.playSound(null, pos, Sonidos.ESTACION_FUSIBLE, SoundSource.BLOCKS, 1.0F, 1.1F);
		nivel.playSound(null, pos, Sonidos.ESTACION_PALANCA, SoundSource.BLOCKS, 0.8F, 1.5F);
		int r = this.azar.nextInt(100);
		ItemStack premio;
		String que;
		if (r < 15) {
			premio = new ItemStack(BloquesEstacion.CINTA);
			que = "cinta aislante";
		} else if (r < 30) {
			premio = new ItemStack(BloquesEstacion.CABLE);
			que = "un cable";
		} else if (r < 40) {
			premio = new ItemStack(BloquesEstacion.FUSIBLE);
			que = "un fusible";
		} else if (r < 66) {
			int c = this.azar.nextInt(4);
			premio = new ItemStack(c == 0 ? net.backrooms.evento.objetos.Objetos.AGUA_ALMENDRAS : c == 1 ? net.backrooms.evento.objetos.Objetos.PIZZA : net.backrooms.evento.objetos.Objetos.GALLETAS);
			que = c == 0 ? "agua de almendras" : c == 1 ? "pizza" : "galletas";
		} else {
			premio = ItemStack.EMPTY;
			que = null;
		}
		if (premio.isEmpty()) {
			j.displayClientMessage(Component.literal("El buzón está vacío. Solo hay polvo y un papel con un número tachado.").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		if (!j.getInventory().add(premio)) {
			j.drop(premio, false);
		}
		j.displayClientMessage(Component.literal("Dentro del buzón había " + que + ".").withStyle(ChatFormatting.YELLOW), true);
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

	/* ----------------------------------------------------------------- staff */

	/** Todo como al principio. */
	public void reponerTodo() {
		this.e = new Estado();
		if (this.corte != null) {
			this.corte.quitar();
			this.corte = null;
		}
		this.corteDesde = -1;
		this.vacios.clear();
		this.focosDesde = -1;
		this.animando.values().forEach(a -> a.piezas.forEach(Display.BlockDisplay::discard));
		this.animando.clear();
		this.guardar();
	}

	/** Pasa a lo siguiente (pruebas): la central siguiente o la cadena. */
	public void avanzar() {
		ServerLevel nivel = Estacion.nivel();
		for (int p = 0; p < PlanoEstacion.PISOS; p++) {
			if (!this.e.activa[p]) {
				this.e.puestos[p] = PlanoEstacion.NECESITA[p].clone();
				if (nivel != null) {
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
		StringBuilder sb = new StringBuilder("Centrales:");
		for (int p = 0; p < PlanoEstacion.PISOS; p++) {
			sb.append("  ").append(PlanoEstacion.PISO[p]).append(' ').append(this.e.activa[p] ? "ACTIVA" : this.puestos(p) + "/" + necesarios(p));
		}
		l.add(sb.toString());
		l.add("Rejas abiertas: " + this.e.rejas.size() + "/" + PlanoEstacion.REJAS.size() + "   ·   buzones abiertos: " + this.e.buzones.size() + "/" + PlanoEstacion.BUZONES.size()
			+ "   ·   cadena del final: " + (this.e.cadena ? "cortada" : "puesta"));
		return l;
	}
}
