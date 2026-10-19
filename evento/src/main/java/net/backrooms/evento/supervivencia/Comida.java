package net.backrooms.evento.supervivencia;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.fase.Fase;
import net.backrooms.evento.mision.Entidades;
import net.backrooms.evento.mundo.GeneradorNivel0;
import net.backrooms.evento.mundo.Plano;
import net.backrooms.evento.piscinas.GeneradorPiscinas;
import net.backrooms.evento.piscinas.Piscinas;
import net.backrooms.evento.piscinas.PlanoPiscinas;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

/**
 * La comida tirada por el Nivel 0. Galletas y pizza: sitios fijos que salen
 * del plano (Plano.comida). Agua de almendras (lo unico que sube la cordura):
 * depende de cuantos juegan, AGUAS_POR_JUGADOR por cada uno que llega a una
 * fase, a entre 40 y 220 bloques de donde aparece; como cada uno llega a una
 * zona distinta, quedan regadas por todo el mapa (400 con 200 jugadores). Todo
 * que se materializan como ComidaEntidad cuando alguien pasa cerca. Lo que
 * ya se ha cogido no vuelve: se apunta por dimension en
 * <mundo>/backrooms/comida.json.
 */
public final class Comida {
	private static final Comida INSTANCIA = new Comida();
	private static final int RADIO = 40;
	private static final int QUITAR = 72;
	public static final int AGUAS_POR_JUGADOR = 5;

	public static Comida get() {
		return INSTANCIA;
	}

	private final Gson gson = new Gson();
	/** dimension -> sitios ya cogidos (x << 32 | z). */
	private Map<String, Set<Long>> cogidas = new HashMap<>();
	/** dimension -> donde hay agua de almendras en esta partida (x << 32 | z). */
	private Map<String, Set<Long>> aguas = new HashMap<>();
	private final java.util.Random azar = new java.util.Random();
	/** "dimension x z" -> entidad en el mundo. */
	private final Map<String, ComidaEntidad> vivas = new HashMap<>();
	private MinecraftServer servidor;
	private boolean sucio;
	private long ticks;

	private Comida() {
	}

	public static void registrar() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			INSTANCIA.servidor = s;
			INSTANCIA.cargar();
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(s -> INSTANCIA.guardar());
		ServerTickEvents.END_SERVER_TICK.register(s -> INSTANCIA.tick());
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((ent, fuente) -> {
			if (ent instanceof ServerPlayer j) {
				soltarComida(j);
			}
		});
	}

	/**
	 * El evento juega con keep_inventory (el traje, la linterna y la camara no se pierden), pero la
	 * comida y el agua de almendras se caen donde murio: otro puede aprovecharlas. La tarjeta la
	 * suelta Tarjetas#alMorir.
	 */
	private static void soltarComida(ServerPlayer j) {
		if (!net.backrooms.evento.piscinas.Piscinas.delEvento(j.level())) {
			return;
		}
		net.minecraft.world.entity.player.Inventory inv = j.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			net.minecraft.world.item.ItemStack s = inv.getItem(i);
			if (!s.isEmpty() && s.has(net.minecraft.core.component.DataComponents.FOOD)) {
				inv.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
				j.drop(s, true, false);
			}
		}
	}

	private Path archivo() {
		return this.servidor.getWorldPath(LevelResource.ROOT).resolve("backrooms").resolve("comida.json");
	}

	private Path archivoAguas() {
		return this.servidor.getWorldPath(LevelResource.ROOT).resolve("backrooms").resolve("aguas.json");
	}

	private void cargar() {
		try {
			Path f = this.archivo();
			if (Files.exists(f)) {
				Map<String, Set<Long>> l = this.gson.fromJson(Files.readString(f, StandardCharsets.UTF_8), new TypeToken<Map<String, Set<Long>>>() { }.getType());
				if (l != null) {
					this.cogidas = new HashMap<>(l);
				}
			}
			Path fa = this.archivoAguas();
			if (Files.exists(fa)) {
				Map<String, Set<Long>> l = this.gson.fromJson(Files.readString(fa, StandardCharsets.UTF_8), new TypeToken<Map<String, Set<Long>>>() { }.getType());
				if (l != null) {
					this.aguas = new HashMap<>(l);
				}
			}
		} catch (Exception e) {
			BackroomsEvento.LOG.error("No se pudo leer comida.json", e);
		}
	}

	private void guardar() {
		if (this.servidor == null || !this.sucio) {
			return;
		}
		try {
			Path f = this.archivo();
			Files.createDirectories(f.getParent());
			Files.writeString(f, this.gson.toJson(this.cogidas), StandardCharsets.UTF_8);
			Files.writeString(this.archivoAguas(), this.gson.toJson(this.aguas), StandardCharsets.UTF_8);
			this.sucio = false;
		} catch (Exception e) {
			BackroomsEvento.LOG.error("No se pudo guardar comida.json", e);
		}
	}

	/** Vuelve a poner toda la comida y quita las aguas de la partida anterior (staff, entre partidas). */
	public void reponer() {
		this.cogidas.clear();
		this.aguas.clear();
		for (ComidaEntidad c : this.vivas.values()) {
			c.discard();
		}
		this.vivas.clear();
		this.sucio = true;
		this.guardar();
	}

	private static long clave(int x, int z) {
		return ((long) x << 32) | (z & 0xFFFFFFFFL);
	}

	private Set<Long> cogidas(ServerLevel nivel) {
		return this.cogidas.computeIfAbsent(nivel.dimension().identifier().toString(), k -> new HashSet<>());
	}

	private Set<Long> aguas(ServerLevel nivel) {
		return this.aguas.computeIfAbsent(nivel.dimension().identifier().toString(), k -> new HashSet<>());
	}

	/** Cuantas aguas de almendras hay en esta partida en esa dimension (y cuantas quedan sin coger). */
	public int[] cuentaAguas(ServerLevel nivel) {
		Set<Long> a = this.aguas(nivel);
		Set<Long> c = this.cogidas(nivel);
		int quedan = 0;
		for (long k : a) {
			if (!c.contains(k)) {
				quedan++;
			}
		}
		return new int[] {a.size(), quedan};
	}

	/** Pone AGUAS_POR_JUGADOR aguas de almendras cerca de donde llega un jugador. */
	public void repartirAguas(ServerLevel nivel, net.minecraft.core.BlockPos llegada) {
		if (nivel.getChunkSource().getGenerator() instanceof GeneradorPiscinas piscinas) {
			this.repartirAguasPiscinas(nivel, piscinas.plano(nivel.getChunkSource().randomState()), llegada);
			return;
		}
		if (!(nivel.getChunkSource().getGenerator() instanceof GeneradorNivel0 gen)) {
			return;
		}
		Plano p = gen.plano(nivel.getChunkSource().randomState());
		Fase f = Fase.de(nivel);
		Set<Long> a = this.aguas(nivel);
		int puestas = 0;
		for (int intento = 0; intento < 80 && puestas < AGUAS_POR_JUGADOR; intento++) {
			double ang = this.azar.nextDouble() * Math.PI * 2;
			double d = 25 + this.azar.nextDouble() * 150;
			int x = llegada.getX() + (int) Math.round(Math.cos(ang) * d);
			int z = llegada.getZ() + (int) Math.round(Math.sin(ang) * d);
			if (f != null && (Math.abs(x) > f.radio() || Math.abs(z) > f.radio())) {
				continue;
			}
			if (p.pared(x, z) || p.decoracion(x, z).tipo() != Plano.D_NADA || p.comida(x, z) != Plano.C_NADA || a.contains(clave(x, z))) {
				continue;
			}
			a.add(clave(x, z));
			puestas++;
		}
		this.sucio = true;
	}

	/** Lo mismo en las Piscinas: en suelo seco, donde se pueda estar de pie, y no encima de otra comida. */
	private void repartirAguasPiscinas(ServerLevel nivel, PlanoPiscinas p, net.minecraft.core.BlockPos llegada) {
		Set<Long> a = this.aguas(nivel);
		byte[] col = new byte[PlanoPiscinas.ALTO];
		int puestas = 0;
		for (int intento = 0; intento < 120 && puestas < AGUAS_POR_JUGADOR; intento++) {
			double ang = this.azar.nextDouble() * Math.PI * 2;
			double d = 25 + this.azar.nextDouble() * 150;
			int x = llegada.getX() + (int) Math.round(Math.cos(ang) * d);
			int z = llegada.getZ() + (int) Math.round(Math.sin(ang) * d);
			p.columna(x, z, col);
			if (!p.pisable(x, PlanoPiscinas.PISO, z, col) || !p.detallesEn(x, z).isEmpty() || a.contains(clave(x, z))) {
				continue;
			}
			int[] fija = p.comidaEnCelda(Math.floorDiv(x, PlanoPiscinas.C), Math.floorDiv(z, PlanoPiscinas.C));
			if (fija != null && fija[0] == x && fija[1] == z) {
				continue;
			}
			a.add(clave(x, z));
			puestas++;
		}
		this.sucio = true;
	}

	public void coger(ServerPlayer j, ComidaEntidad c) {
		if (c.isRemoved() || j.isSpectator()) {
			return;
		}
		ItemStack s = new ItemStack(c.objeto());
		if (!j.getInventory().add(s)) {
			j.displayClientMessage(Component.literal("No te cabe nada más.").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		ServerLevel nivel = j.level();
		this.cogidas(nivel).add(clave(c.getBlockX(), c.getBlockZ()));
		this.vivas.remove(nivel.dimension().identifier() + " " + c.getBlockX() + " " + c.getBlockZ());
		c.discard();
		this.sucio = true;
		nivel.playSound(null, c.getX(), c.getY(), c.getZ(), c.tipo() == 1 ? net.backrooms.evento.Sonidos.AGUA_COGER : net.backrooms.evento.Sonidos.COMIDA_COGER,
			SoundSource.PLAYERS, 0.9F, 0.95F + nivel.getRandom().nextFloat() * 0.1F);
	}

	private void materializar(ServerLevel nivel, String dim, int x, int z, int tipo, double y) {
		String k = dim + " " + x + " " + z;
		// con su chunk sin cargar del todo (justo al llegar o reaparecer) no se pone: se quedaria
		// fuera del mundo, sin verse ni quitarse, y esa comida ya no volvia
		if (!nivel.isPositionEntityTicking(new net.minecraft.core.BlockPos(x, (int) Math.floor(y), z))) {
			return;
		}
		ComidaEntidad viva = this.vivas.get(k);
		if (viva != null && !viva.isRemoved()) {
			if (nivel.getEntity(viva.getId()) == viva) {
				return;
			}
			viva.discard();
		}
		ComidaEntidad nueva = new ComidaEntidad(Entidades.COMIDA, nivel);
		nueva.preparar(tipo, Math.floorMod(x * 53 + z * 29, 360));
		nueva.setPos(x + 0.5, y, z + 0.5);
		nivel.addFreshEntity(nueva);
		this.vivas.put(k, nueva);
	}

	private void tick() {
		if (this.servidor == null || ++this.ticks % 20 != 0) {
			return;
		}
		for (ServerLevel nivel : this.servidor.getAllLevels()) {
			if (Piscinas.es(nivel) && nivel.getChunkSource().getGenerator() instanceof GeneradorPiscinas piscinas) {
				this.piscinas(nivel, piscinas.plano(nivel.getChunkSource().randomState()));
				continue;
			}
			if (net.backrooms.evento.fiesta.Fiesta.es(nivel)) {
				this.fiesta(nivel);
				continue;
			}
			if (Fase.de(nivel) == null || !(nivel.getChunkSource().getGenerator() instanceof GeneradorNivel0 gen)) {
				continue;
			}
			Plano p = gen.plano(nivel.getChunkSource().randomState());
			Set<Long> cogidas = this.cogidas(nivel);
			String dim = nivel.dimension().identifier().toString();
			for (ServerPlayer j : nivel.players()) {
				if (j.isSpectator()) {
					continue;
				}
				int gx0 = Math.floorDiv(j.getBlockX() - RADIO, Plano.G);
				int gx1 = Math.floorDiv(j.getBlockX() + RADIO, Plano.G);
				int gz0 = Math.floorDiv(j.getBlockZ() - RADIO, Plano.G);
				int gz1 = Math.floorDiv(j.getBlockZ() + RADIO, Plano.G);
				// las aguas de almendras de la partida que esten cerca
				for (long k : this.aguas(nivel)) {
					int ax = (int) (k >> 32);
					int az = (int) k;
					if (Math.abs(ax - j.getBlockX()) > RADIO || Math.abs(az - j.getBlockZ()) > RADIO || cogidas.contains(k)) {
						continue;
					}
					this.materializar(nivel, dim, ax, az, Plano.C_AGUA, GeneradorNivel0.SUELO + 1);
				}
				for (int gx = gx0; gx <= gx1; gx++) {
					for (int gz = gz0; gz <= gz1; gz++) {
						int[] c = p.comidaEnCelda(gx, gz);
						if (c == null || cogidas.contains(clave(c[0], c[1]))) {
							continue;
						}
						this.materializar(nivel, dim, c[0], c[1], c[2], GeneradorNivel0.SUELO + 1);
					}
				}
			}
		}
		// las que se han quedado lejos de todos se quitan (vuelven al pasar)
		Iterator<Map.Entry<String, ComidaEntidad>> it = this.vivas.entrySet().iterator();
		while (it.hasNext()) {
			ComidaEntidad c = it.next().getValue();
			if (c.isRemoved()) {
				it.remove();
			} else if (c.level().getNearestPlayer(c, QUITAR) == null) {
				c.discard();
				it.remove();
			}
		}
		if (this.ticks % 1200 == 0) {
			this.guardar();
		}
	}

	/** La Fiesta: la comida de las mesas y encimeras del plano (PlanoFiesta.COMIDA), a su altura. */
	private void fiesta(ServerLevel nivel) {
		Set<Long> cogidas = this.cogidas(nivel);
		String dim = nivel.dimension().identifier().toString();
		for (ServerPlayer j : nivel.players()) {
			if (j.isSpectator()) {
				continue;
			}
			for (net.backrooms.evento.fiesta.PlanoFiesta.Plato p : net.backrooms.evento.fiesta.PlanoFiesta.COMIDA) {
				if (Math.abs(p.x() - j.getBlockX()) <= RADIO && Math.abs(p.z() - j.getBlockZ()) <= RADIO && !cogidas.contains(clave(p.x(), p.z()))) {
					this.materializar(nivel, dim, p.x(), p.z(), p.tipo(), p.y());
				}
			}
		}
	}

	/** Las Piscinas: lo mismo que en el Nivel 0, con la comida de cada celda del plano y las aguas de la partida. */
	private void piscinas(ServerLevel nivel, PlanoPiscinas p) {
		Set<Long> cogidas = this.cogidas(nivel);
		String dim = nivel.dimension().identifier().toString();
		for (ServerPlayer j : nivel.players()) {
			if (j.isSpectator()) {
				continue;
			}
			for (long k : this.aguas(nivel)) {
				int ax = (int) (k >> 32);
				int az = (int) k;
				if (Math.abs(ax - j.getBlockX()) <= RADIO && Math.abs(az - j.getBlockZ()) <= RADIO && !cogidas.contains(k)) {
					this.materializar(nivel, dim, ax, az, Plano.C_AGUA, PlanoPiscinas.PISO);
				}
			}
			int cx0 = Math.floorDiv(j.getBlockX() - RADIO, PlanoPiscinas.C);
			int cx1 = Math.floorDiv(j.getBlockX() + RADIO, PlanoPiscinas.C);
			int cz0 = Math.floorDiv(j.getBlockZ() - RADIO, PlanoPiscinas.C);
			int cz1 = Math.floorDiv(j.getBlockZ() + RADIO, PlanoPiscinas.C);
			for (int cx = cx0; cx <= cx1; cx++) {
				for (int cz = cz0; cz <= cz1; cz++) {
					int[] c = p.comidaEnCelda(cx, cz);
					if (c != null && !cogidas.contains(clave(c[0], c[1]))) {
						this.materializar(nivel, dim, c[0], c[1], c[2], PlanoPiscinas.PISO);
					}
				}
			}
		}
	}
}
