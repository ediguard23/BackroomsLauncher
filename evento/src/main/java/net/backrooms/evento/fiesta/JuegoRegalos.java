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
 * Busca los regalos: una sala casi a oscuras llena de estanterias y montones de regalos. Quien pisa
 * su estrella tiene SEGUNDOS para encontrar REGALOS regalos escondidos entre todos los de la sala
 * (PlanoFiesta.REGALOS_SITIOS: el sorteo es de cada uno; los demas son de mentira). Se abren con clic
 * derecho (RegaloBloque): el bueno suena y echa confeti, el vacio no. De vez en cuando uno de los que
 * faltan tintinea para orientarse; los diez ultimos segundos, un reloj. Con todos, tickets
 * (ConfigFiesta.regalosPremio); si se acaba el tiempo, algunos por cada uno. Se juega UNA vez por
 * jugador (para que no se farmeen tickets). Salir de la sala la acaba.
 */
public final class JuegoRegalos {
	private static final JuegoRegalos INSTANCIA = new JuegoRegalos();
	public static final int REGALOS = 8;
	public static final int SEGUNDOS = 30;
	private static final int[] COLORES = {0xE8384A, 0xF6CE40, 0x3A96E6, 0x4AC46E};

	public static JuegoRegalos get() {
		return INSTANCIA;
	}

	private static final class Partida {
		final List<int[]> regalos = new ArrayList<>();
		final List<Boolean> cogidos = new ArrayList<>();
		long fin;
		long siguientePing;
	}

	private final Random azar = new Random();
	private final Map<UUID, Partida> partidas = new HashMap<>();
	private final Map<UUID, Long> aviso = new HashMap<>();
	private long ticks;

	private JuegoRegalos() {
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(INSTANCIA::tick);
	}

	/** Para el staff (/backrooms fiesta juegos): los regalos que le faltan a `j`. */
	public String estado(ServerPlayer j) {
		Partida p = this.partidas.get(j.getUUID());
		if (p == null) {
			return JuegoFiesta.get().datos(j).regalos > 0 ? "regalos: ya jugados" : "regalos: sin partida";
		}
		StringBuilder s = new StringBuilder("regalos que faltan:");
		for (int i = 0; i < p.regalos.size(); i++) {
			if (!p.cogidos.get(i)) {
				int[] r = p.regalos.get(i);
				s.append(' ').append(r[0]).append(',').append(r[1]).append(',').append(r[2]);
			}
		}
		return s.toString();
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
				if (enBoton(nivel, j)) {
					if (JuegoFiesta.get().datos(j).regalos > 0) {
						Long antes = this.aviso.get(j.getUUID());
						if (antes == null || this.ticks - antes > 100) {
							this.aviso.put(j.getUUID(), this.ticks);
							j.displayClientMessage(Component.literal("Ya buscaste tus regalos. Este juego es una sola vez.").withStyle(ChatFormatting.GRAY), true);
						}
					} else {
						this.empezar(nivel, j);
					}
				}
				continue;
			}
			if (!enSala(j)) {
				this.acabar(nivel, j, p, "Has salido de la sala de los regalos.");
				continue;
			}
			this.jugar(nivel, j, p);
		}
	}

	private void empezar(ServerLevel nivel, ServerPlayer j) {
		Partida p = new Partida();
		List<int[]> sitios = new ArrayList<>(PlanoFiesta.REGALOS_SITIOS);
		Collections.shuffle(sitios, this.azar);
		for (int i = 0; i < Math.min(REGALOS, sitios.size()); i++) {
			p.regalos.add(sitios.get(i));
			p.cogidos.add(false);
		}
		p.fin = this.ticks + SEGUNDOS * 20L;
		p.siguientePing = this.ticks + 60;
		this.partidas.put(j.getUUID(), p);
		// cuenta como jugado desde ya: si sale o se le acaba el tiempo, no puede repetir
		JuegoFiesta.get().datos(j).regalos++;
		nivel.playSound(null, j.blockPosition(), Sonidos.MEMORIA_TURNO, SoundSource.BLOCKS, 1.2F, 1.2F);
		j.displayClientMessage(Component.literal("REGALOS · " + p.regalos.size() + " de estos regalos son tuyos: ábrelos (clic derecho) en "
			+ SEGUNDOS + " s. Solo se juega una vez.").withStyle(ChatFormatting.LIGHT_PURPLE), false);
	}

	/** Clic derecho en un regalo de la sala (RegaloBloque): si es uno de los suyos, lo encuentra. */
	public void abrir(ServerPlayer j, BlockPos pos) {
		Partida p = this.partidas.get(j.getUUID());
		if (p == null || !enSala(j)) {
			return;
		}
		ServerLevel nivel = j.level();
		for (int i = 0; i < p.regalos.size(); i++) {
			int[] r = p.regalos.get(i);
			if (r[0] == pos.getX() && r[1] == pos.getY() && r[2] == pos.getZ()) {
				if (!p.cogidos.get(i)) {
					p.cogidos.set(i, true);
					nivel.sendParticles(j, new DustParticleOptions(COLORES[i % 4], 1.6F), true, false, r[0] + 0.5, r[1] + 0.8, r[2] + 0.5, 20, 0.3, 0.3, 0.3, 0.0);
					sonar(j, Sonidos.REGALOS_COGER, new double[] {r[0] + 0.5, r[1] + 0.5, r[2] + 0.5}, 1.0F);
				}
				return;
			}
		}
		// vacio: un sonido sordo de carton
		sonar(j, Sonidos.FIESTA_TORNO_NO, new double[] {pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5}, 0.4F);
	}

	private void jugar(ServerLevel nivel, ServerPlayer j, Partida p) {
		int quedan = 0;
		for (boolean c : p.cogidos) {
			quedan += c ? 0 : 1;
		}
		long resta = p.fin - this.ticks;
		int cogidos = p.regalos.size() - quedan;
		if (quedan == 0) {
			this.ganar(nivel, j, p);
			return;
		}
		if (this.ticks >= p.siguientePing) {
			// un tintineo desde algun regalo que falte, para orientarse
			p.siguientePing = this.ticks + 70 + this.azar.nextInt(40);
			List<int[]> faltan = new ArrayList<>();
			for (int i = 0; i < p.regalos.size(); i++) {
				if (!p.cogidos.get(i)) {
					faltan.add(p.regalos.get(i));
				}
			}
			int[] r = faltan.get(this.azar.nextInt(faltan.size()));
			sonar(j, Sonidos.REGALOS_PING, new double[] {r[0] + 0.5, r[1] + 0.5, r[2] + 0.5}, 0.8F);
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
			this.acabar(nivel, j, p, "¡Se acabó el tiempo! Encontraste " + cogidos + " de " + p.regalos.size() + ".");
		}
	}

	private void ganar(ServerLevel nivel, ServerPlayer j, Partida p) {
		nivel.playSound(null, j.getX(), j.getEyeY(), j.getZ(), Sonidos.SEMAFORO_GANA, SoundSource.BLOCKS, 2.0F, 1.05F);
		JuegoFiesta.get().sumarTickets(j, ConfigFiesta.get().regalosPremio, "¡todos los regalos!");
		this.acabar(nivel, j, p, null);
	}

	private void acabar(ServerLevel nivel, ServerPlayer j, Partida p, String aviso) {
		this.partidas.remove(j.getUUID());
		if (aviso != null) {
			j.displayClientMessage(Component.literal(aviso).withStyle(ChatFormatting.GRAY), false);
		}
	}

	/** Un sonido en un sitio que solo oye ese jugador. */
	private static void sonar(ServerPlayer j, SoundEvent s, double[] p, float volumen) {
		j.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(s), SoundSource.BLOCKS,
			p[0], p[1], p[2], volumen, 1.0F, j.getRandom().nextLong()));
	}
}
