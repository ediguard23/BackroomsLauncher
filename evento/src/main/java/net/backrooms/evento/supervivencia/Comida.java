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
	public static final int AGUAS_POR_JUGADOR = 2;

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
		if (!(nivel.getChunkSource().getGenerator() instanceof GeneradorNivel0 gen)) {
			return;
		}
		Plano p = gen.plano(nivel.getChunkSource().randomState());
		Fase f = Fase.de(nivel);
		Set<Long> a = this.aguas(nivel);
		int puestas = 0;
		for (int intento = 0; intento < 80 && puestas < AGUAS_POR_JUGADOR; intento++) {
			double ang = this.azar.nextDouble() * Math.PI * 2;
			double d = 40 + this.azar.nextDouble() * 180;
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
		nivel.playSound(null, c.getX(), c.getY(), c.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.1F);
	}

	private void materializar(ServerLevel nivel, String dim, int x, int z, int tipo) {
		String k = dim + " " + x + " " + z;
		ComidaEntidad viva = this.vivas.get(k);
		if (viva != null && !viva.isRemoved()) {
			return;
		}
		ComidaEntidad nueva = new ComidaEntidad(Entidades.COMIDA, nivel);
		nueva.preparar(tipo, Math.floorMod(x * 53 + z * 29, 360));
		nueva.setPos(x + 0.5, GeneradorNivel0.SUELO + 1.0, z + 0.5);
		nivel.addFreshEntity(nueva);
		this.vivas.put(k, nueva);
	}

	private void tick() {
		if (this.servidor == null || ++this.ticks % 20 != 0) {
			return;
		}
		for (ServerLevel nivel : this.servidor.getAllLevels()) {
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
					this.materializar(nivel, dim, ax, az, Plano.C_AGUA);
				}
				for (int gx = gx0; gx <= gx1; gx++) {
					for (int gz = gz0; gz <= gz1; gz++) {
						int[] c = p.comidaEnCelda(gx, gz);
						if (c == null || cogidas.contains(clave(c[0], c[1]))) {
							continue;
						}
						this.materializar(nivel, dim, c[0], c[1], c[2]);
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
}
