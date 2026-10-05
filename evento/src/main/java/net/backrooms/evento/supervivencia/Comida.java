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
 * La comida tirada por el Nivel 0: sitios fijos que salen del plano
 * (Plano.comida; una cada ~70 celdas, el agua de almendras es la mas rara) y
 * que se materializan como ComidaEntidad cuando alguien pasa cerca. Lo que
 * ya se ha cogido no vuelve: se apunta por dimension en
 * <mundo>/backrooms/comida.json.
 */
public final class Comida {
	private static final Comida INSTANCIA = new Comida();
	private static final int RADIO = 40;
	private static final int QUITAR = 72;

	public static Comida get() {
		return INSTANCIA;
	}

	private final Gson gson = new Gson();
	/** dimension -> sitios ya cogidos (x << 32 | z). */
	private Map<String, Set<Long>> cogidas = new HashMap<>();
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

	private void cargar() {
		try {
			Path f = this.archivo();
			if (Files.exists(f)) {
				Map<String, Set<Long>> l = this.gson.fromJson(Files.readString(f, StandardCharsets.UTF_8), new TypeToken<Map<String, Set<Long>>>() { }.getType());
				if (l != null) {
					this.cogidas = new HashMap<>(l);
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
			this.sucio = false;
		} catch (Exception e) {
			BackroomsEvento.LOG.error("No se pudo guardar comida.json", e);
		}
	}

	/** Vuelve a poner toda la comida (staff, entre partidas). */
	public void reponer() {
		this.cogidas.clear();
		this.sucio = true;
		this.guardar();
	}

	private static long clave(int x, int z) {
		return ((long) x << 32) | (z & 0xFFFFFFFFL);
	}

	private Set<Long> cogidas(ServerLevel nivel) {
		return this.cogidas.computeIfAbsent(nivel.dimension().identifier().toString(), k -> new HashSet<>());
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
				for (int gx = gx0; gx <= gx1; gx++) {
					for (int gz = gz0; gz <= gz1; gz++) {
						int[] c = p.comidaEnCelda(gx, gz);
						if (c == null || cogidas.contains(clave(c[0], c[1]))) {
							continue;
						}
						String k = dim + " " + c[0] + " " + c[1];
						ComidaEntidad viva = this.vivas.get(k);
						if (viva != null && !viva.isRemoved()) {
							continue;
						}
						ComidaEntidad nueva = new ComidaEntidad(Entidades.COMIDA, nivel);
						nueva.preparar(c[2], Math.floorMod(c[0] * 53 + c[1] * 29, 360));
						nueva.setPos(c[0] + 0.5, GeneradorNivel0.SUELO + 1.0, c[1] + 0.5);
						nivel.addFreshEntity(nueva);
						this.vivas.put(k, nueva);
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
