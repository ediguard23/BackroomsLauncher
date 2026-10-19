package net.backrooms.evento.fiesta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.backrooms.evento.Sonidos;
import net.backrooms.evento.red.Semaforo;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Luz verde, luz roja: el juego del castillo de bolas (PlanoFiesta.CAS_*). Con alguien dentro, todos
 * sus semaforos (PlanoFiesta.SEMAFOROS) alternan verde y rojo a la vez (tiempos al azar de
 * ConfigFiesta); en verde suena la musiquilla y se puede avanzar, en rojo se corta y una voz de nina
 * lo canta. Quien se mueva (o salte) en rojo, pasado el margen para frenar, pierde
 * ConfigFiesta.semaforoCastigo tickets (una vez por cada luz roja) y se queda donde esta. Se juega
 * mientras se cruza el castillo hasta el tobogan; el premio es el bote de la vuelta (JuegoFiesta).
 */
public final class JuegoSemaforo {
	private static final JuegoSemaforo INSTANCIA = new JuegoSemaforo();
	public static final int PARADO = 0;
	public static final int VERDE = 1;
	public static final int ROJA = 2;
	/** Lo que se puede mover en rojo (bloques, en horizontal) sin que te pille: los temblores del cliente. */
	private static final double QUIETO = 0.12;

	public static JuegoSemaforo get() {
		return INSTANCIA;
	}

	private final Random azar = new Random();
	private MinecraftServer servidor;
	private long ticks;
	private int luz = PARADO;
	private long cambio;
	private long proximo;
	/** Dentro del castillo. */
	private final Set<UUID> carrera = new HashSet<>();
	/** Los que ya han perdido tickets en esta luz roja. */
	private final Set<UUID> pillados = new HashSet<>();
	/** Donde estaba cada uno cuando se acabo el margen de la roja. */
	private final Map<UUID, Vec3> quietos = new HashMap<>();

	private JuegoSemaforo() {
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(s -> {
			INSTANCIA.servidor = s;
			INSTANCIA.tick();
		});
	}

	public boolean jugando(ServerPlayer j) {
		return this.carrera.contains(j.getUUID());
	}

	private static boolean enCastillo(ServerPlayer j) {
		return Fiesta.es(j.level()) && !j.isSpectator() && PlanoFiesta.enCastillo(j.getX(), j.getZ())
			&& j.getY() > PlanoFiesta.SUELO && j.getY() < PlanoFiesta.CAS_TEJADO;
	}

	private void tick() {
		this.ticks++;
		ServerLevel nivel = Fiesta.nivel();
		if (nivel == null) {
			return;
		}
		List<ServerPlayer> dentro = new ArrayList<>();
		for (ServerPlayer j : nivel.players()) {
			if (enCastillo(j)) {
				dentro.add(j);
			}
		}
		Set<UUID> ids = new HashSet<>();
		dentro.forEach(j -> ids.add(j.getUUID()));
		for (ServerPlayer j : nivel.players()) {
			if (!ids.contains(j.getUUID()) && this.carrera.remove(j.getUUID())) {
				JuegoFiesta.get().enviar(j);
			}
		}
		this.carrera.retainAll(ids);
		this.pillados.retainAll(ids);
		this.quietos.keySet().retainAll(ids);
		if (dentro.isEmpty()) {
			if (this.luz != PARADO) {
				this.poner(nivel, PARADO, dentro);
			}
			return;
		}
		if (this.luz == PARADO) {
			this.poner(nivel, VERDE, dentro);
		} else if (this.ticks >= this.proximo) {
			this.poner(nivel, this.luz == VERDE ? ROJA : VERDE, dentro);
		}
		ConfigFiesta c = ConfigFiesta.get();
		boolean margen = this.ticks - this.cambio < Math.round(c.semaforoMargen * 20);
		for (ServerPlayer j : new ArrayList<>(dentro)) {
			UUID u = j.getUUID();
			if (this.carrera.add(u)) {
				JuegoFiesta.get().enviar(j);
				j.displayClientMessage(Component.literal("¡SEMÁFORO! Busca el tobogán y avanza solo con la luz verde.").withStyle(ChatFormatting.GREEN), true);
			}
			if (j.isCreative() || this.pillados.contains(u)) {
				continue;
			}
			if (this.luz == ROJA && !margen) {
				Vec3 antes = this.quietos.get(u);
				if (antes == null) {
					this.quietos.put(u, j.position());
				} else {
					double dx = j.getX() - antes.x;
					double dz = j.getZ() - antes.z;
					if (dx * dx + dz * dz > QUIETO * QUIETO || j.getY() - antes.y > 0.3) {
						this.pillar(nivel, j);
					}
				}
			}
		}
	}

	/** Cambia la luz: las lamparas, la voz y la musica (los del castillo, por el paquete Semaforo). */
	private void poner(ServerLevel nivel, int luz, List<ServerPlayer> dentro) {
		this.luz = luz;
		this.cambio = this.ticks;
		this.quietos.clear();
		this.pillados.clear();
		ConfigFiesta c = ConfigFiesta.get();
		float[] rango = luz == VERDE ? c.semaforoVerde : c.semaforoRoja;
		this.proximo = this.ticks + Math.round((rango[0] + this.azar.nextFloat() * (rango[1] - rango[0])) * 20);
		for (int[] p : PlanoFiesta.SEMAFOROS) {
			encender(nivel, p, luz == ROJA ? 1 : luz == VERDE ? 2 : 0);
		}
		if (luz != PARADO) {
			// la voz desde el medio del castillo, alto, que se oiga en todo el
			double cx = PlanoFiesta.CAS_X + PlanoFiesta.CAS_LADO / 2.0;
			double cz = PlanoFiesta.CAS_Z + PlanoFiesta.CAS_LADO / 2.0;
			nivel.playSound(null, cx, PlanoFiesta.CAS_TEJADO + 2, cz, luz == VERDE ? Sonidos.SEMAFORO_VERDE : Sonidos.SEMAFORO_ROJA,
				SoundSource.AMBIENT, 6.0F, 1.0F);
		}
		for (ServerPlayer j : dentro) {
			if (ServerPlayNetworking.canSend(j, Semaforo.TYPE)) {
				ServerPlayNetworking.send(j, new Semaforo(luz));
			}
		}
	}

	private static void encender(ServerLevel nivel, int[] p, int luz) {
		BlockPos pos = new BlockPos(p[0], p[1], p[2]);
		BlockState e = nivel.getBlockState(pos);
		if (e.is(BloquesFiesta.SEMAFORO) && e.getValue(SemaforoCastillo.LUZ) != luz) {
			nivel.setBlock(pos, e.setValue(SemaforoCastillo.LUZ, luz), Block.UPDATE_CLIENTS);
		}
	}

	/** Se ha movido en rojo: sin unos tickets (una vez por luz roja). */
	private void pillar(ServerLevel nivel, ServerPlayer j) {
		this.pillados.add(j.getUUID());
		this.quietos.remove(j.getUUID());
		nivel.playSound(null, j.getX(), j.getEyeY(), j.getZ(), Sonidos.SEMAFORO_PILLADO, SoundSource.AMBIENT, 2.0F, 1.0F);
		JuegoFiesta.get().sumarTickets(j, -ConfigFiesta.get().semaforoCastigo, "¡te has movido en rojo!");
	}
}
