package net.backrooms.evento.fiesta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.backrooms.evento.Sonidos;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Busca los regalos: la sala de los regalos, casi a oscuras, detras del tabique de la sala de
 * juegos. Quien pisa su estrella tiene SEGUNDOS para encontrar REGALOS regalos que solo ve el
 * (un brillo de colores en el suelo, PlanoFiesta.REGALOS_SITIOS) y que de vez en cuando tintinean
 * para orientarse; se cogen al pasar. Los diez ultimos segundos, un reloj. Con todos, tickets
 * (ConfigFiesta); si se acaba el tiempo, algunos por cada uno. Cada jugador juega su partida: se
 * puede jugar a la vez, y cada uno ve solo los suyos. Salir de la sala la acaba.
 */
public final class JuegoRegalos {
	private static final JuegoRegalos INSTANCIA = new JuegoRegalos();
	public static final int REGALOS = 8;
	public static final int SEGUNDOS = 60;
	private static final double COGER = 1.3;
	private static final int[] COLORES = {0xE8384A, 0xF6CE40, 0x3A96E6, 0x4AC46E};

	public static JuegoRegalos get() {
		return INSTANCIA;
	}

	private static final class Partida {
		final List<double[]> regalos = new ArrayList<>();
		final List<Boolean> cogidos = new ArrayList<>();
		long fin;
		long siguientePing;
	}

	private final Random azar = new Random();
	private final Map<UUID, Partida> partidas = new HashMap<>();
	private final Map<UUID, Long> espera = new HashMap<>();
	private long ticks;

	private JuegoRegalos() {
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(INSTANCIA::tick);
	}

	private static boolean enSala(ServerPlayer j) {
		int[] s = PlanoFiesta.REGALOS_SALA;
		return Fiesta.es(j.level()) && j.getX() >= s[0] && j.getX() < s[2] + 1 && j.getZ() >= s[1] && j.getZ() < s[3] + 1;
	}

	private static boolean enBoton(ServerLevel nivel, ServerPlayer j) {
		BlockState e = nivel.getBlockState(BlockPos.containing(j.getX(), PlanoFiesta.SUELO + 0.5, j.getZ()));
		return e.is(BloquesFiesta.BALDOSA_JUEGO) && e.getValue(BaldosaJuego.COLOR) == 4 && enSala(j) && j.getY() < PlanoFiesta.SUELO + 1.3;
	}

	private void tick(MinecraftServer s) {
		this.ticks++;
		ServerLevel nivel = Fiesta.nivel();
		if (nivel == null) {
			return;
		}
		this.partidas.keySet().removeIf(u -> {
			ServerPlayer j = s.getPlayerList().getPlayer(u);
			return j == null || j.level() != nivel || !j.isAlive();
		});
		for (ServerPlayer j : new ArrayList<>(nivel.players())) {
			if (j.isSpectator()) {
				continue;
			}
			Partida p = this.partidas.get(j.getUUID());
			if (p == null) {
				Long hasta = this.espera.get(j.getUUID());
				if (enBoton(nivel, j) && (hasta == null || this.ticks >= hasta)) {
					this.empezar(nivel, j);
				}
				continue;
			}
			if (!enSala(j)) {
				this.acabar(j, p, "Has salido de la sala de los regalos.");
				continue;
			}
			this.jugar(nivel, j, p);
		}
	}

	private void empezar(ServerLevel nivel, ServerPlayer j) {
		Partida p = new Partida();
		List<double[]> sitios = new ArrayList<>(PlanoFiesta.REGALOS_SITIOS);
		Collections.shuffle(sitios, this.azar);
		for (int i = 0; i < Math.min(REGALOS, sitios.size()); i++) {
			p.regalos.add(sitios.get(i));
			p.cogidos.add(false);
		}
		p.fin = this.ticks + SEGUNDOS * 20L;
		p.siguientePing = this.ticks + 40;
		this.partidas.put(j.getUUID(), p);
		nivel.playSound(null, j.blockPosition(), Sonidos.MEMORIA_TURNO, SoundSource.BLOCKS, 1.2F, 1.2F);
		j.displayClientMessage(Component.literal("REGALOS · Encuentra los " + p.regalos.size() + " regalos en " + SEGUNDOS + " s. Solo tú los ves brillar.")
			.withStyle(ChatFormatting.LIGHT_PURPLE), false);
	}

	private void jugar(ServerLevel nivel, ServerPlayer j, Partida p) {
		int quedan = 0;
		for (int i = 0; i < p.regalos.size(); i++) {
			if (p.cogidos.get(i)) {
				continue;
			}
			double[] r = p.regalos.get(i);
			double dx = j.getX() - r[0];
			double dz = j.getZ() - r[2];
			if (dx * dx + dz * dz <= COGER * COGER) {
				p.cogidos.set(i, true);
				nivel.sendParticles(j, new DustParticleOptions(COLORES[i % 4], 1.6F), true, false, r[0], r[1] + 0.5, r[2], 16, 0.3, 0.3, 0.3, 0.0);
				sonar(j, Sonidos.REGALOS_COGER, r, 1.0F);
				continue;
			}
			quedan++;
			if (this.ticks % 8 == 0) {
				// el brillo del regalo: solo para quien juega
				nivel.sendParticles(j, new DustParticleOptions(COLORES[i % 4], 1.3F), true, false, r[0], r[1] + 0.35, r[2], 4, 0.2, 0.15, 0.2, 0.0);
			}
		}
		long resta = p.fin - this.ticks;
		int cogidos = p.regalos.size() - quedan;
		if (quedan == 0) {
			this.ganar(nivel, j, p);
			return;
		}
		if (this.ticks >= p.siguientePing) {
			// un tintineo desde algun regalo que falte, para orientarse
			p.siguientePing = this.ticks + 50 + this.azar.nextInt(30);
			List<double[]> faltan = new ArrayList<>();
			for (int i = 0; i < p.regalos.size(); i++) {
				if (!p.cogidos.get(i)) {
					faltan.add(p.regalos.get(i));
				}
			}
			sonar(j, Sonidos.REGALOS_PING, faltan.get(this.azar.nextInt(faltan.size())), 0.9F);
		}
		if (resta <= 200 && resta % 20 == 0 && resta > 0) {
			sonar(j, Sonidos.REGALOS_RELOJ, new double[] {j.getX(), j.getEyeY(), j.getZ()}, 0.7F);
		}
		if (this.ticks % 10 == 0) {
			j.displayClientMessage(Component.literal("REGALOS " + cogidos + "/" + p.regalos.size() + " · " + Math.max(0, resta / 20) + " s")
				.withStyle(resta <= 200 ? ChatFormatting.RED : ChatFormatting.YELLOW), true);
		}
		if (resta <= 0) {
			sonar(j, Sonidos.REGALOS_FIN, new double[] {j.getX(), j.getEyeY(), j.getZ()}, 1.0F);
			int consuelo = cogidos * ConfigFiesta.get().regalosPorRegalo;
			if (consuelo > 0) {
				JuegoFiesta.get().sumarTickets(j, consuelo, cogidos + " regalos");
			}
			this.acabar(j, p, "¡Se acabó el tiempo! Encontraste " + cogidos + " de " + p.regalos.size() + ".");
		}
	}

	private void ganar(ServerLevel nivel, ServerPlayer j, Partida p) {
		JuegoFiesta.Datos d = JuegoFiesta.get().datos(j);
		ConfigFiesta c = ConfigFiesta.get();
		int premio = d.regalos == 0 ? c.regalosPremio : c.regalosPremioRepetido;
		d.regalos++;
		nivel.playSound(null, j.getX(), j.getEyeY(), j.getZ(), Sonidos.SEMAFORO_GANA, SoundSource.BLOCKS, 2.0F, 1.05F);
		JuegoFiesta.get().sumarTickets(j, premio, "¡todos los regalos!");
		this.acabar(j, p, null);
	}

	private void acabar(ServerPlayer j, Partida p, String aviso) {
		this.partidas.remove(j.getUUID());
		this.espera.put(j.getUUID(), this.ticks + 100);
		if (aviso != null) {
			j.displayClientMessage(Component.literal(aviso + " Pisa la estrella para volver a jugar.").withStyle(ChatFormatting.GRAY), false);
		}
	}

	/** Un sonido en un sitio que solo oye ese jugador. */
	private static void sonar(ServerPlayer j, SoundEvent s, double[] p, float volumen) {
		j.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(s), SoundSource.BLOCKS,
			p[0], p[1], p[2], volumen, 1.0F, j.getRandom().nextLong()));
	}
}
