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
 * Luz verde, luz roja: el juego del pasillo del semaforo (PlanoFiesta.SEM_*). Con alguien en el
 * pasillo, el semaforo del fondo alterna verde y rojo (tiempos al azar de ConfigFiesta); en
 * verde suena la musiquilla y se puede avanzar, en rojo se corta y una voz de nina lo canta.
 *
 * Se juega desde la zona de salida: al cruzar la linea (x = SEM_SALIDA) empiezas la carrera.
 * Si te mueves (o saltas) en rojo, pasado el margen para frenar, te pilla: pierdes
 * ConfigFiesta.semaforoCastigo tickets y vuelves a la salida. Al cruzar la meta ganas tickets
 * (la primera vez mas) y puedes volver a jugar. Lo juegan a la vez todos los del pasillo.
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
	/** En la zona de salida, listos para empezar. */
	private final Set<UUID> listos = new HashSet<>();
	/** Corriendo (ya cruzaron la salida). */
	private final Set<UUID> carrera = new HashSet<>();
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

	private static boolean enPasillo(ServerPlayer j) {
		return Fiesta.es(j.level()) && !j.isSpectator() && j.getX() >= PlanoFiesta.SEM_X0 && j.getX() < PlanoFiesta.SEM_X1 + 1
			&& j.getZ() >= PlanoFiesta.SEM_Z0 && j.getZ() < PlanoFiesta.SEM_Z1 + 1;
	}

	private void tick() {
		this.ticks++;
		ServerLevel nivel = Fiesta.nivel();
		if (nivel == null) {
			return;
		}
		List<ServerPlayer> dentro = new ArrayList<>();
		for (ServerPlayer j : nivel.players()) {
			if (enPasillo(j)) {
				dentro.add(j);
			}
		}
		Set<UUID> ids = new HashSet<>();
		dentro.forEach(j -> ids.add(j.getUUID()));
		this.listos.retainAll(ids);
		this.carrera.retainAll(ids);
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
			double x = j.getX();
			if (x < PlanoFiesta.SEM_SALIDA) {
				if (this.carrera.remove(u)) {
					JuegoFiesta.get().enviar(j);
				}
				this.listos.add(u);
				continue;
			}
			if (this.listos.remove(u)) {
				this.carrera.add(u);
				JuegoFiesta.get().enviar(j);
				j.displayClientMessage(Component.literal("¡A JUGAR! Avanza solo con la luz verde.").withStyle(ChatFormatting.GREEN), true);
			}
			if (!this.carrera.contains(u)) {
				continue;
			}
			if (x >= PlanoFiesta.SEM_META) {
				this.ganar(nivel, j);
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

	/** Cambia la luz: las lamparas, la voz y la musica (los del pasillo, por EstadoSemaforo). */
	private void poner(ServerLevel nivel, int luz, List<ServerPlayer> dentro) {
		this.luz = luz;
		this.cambio = this.ticks;
		this.quietos.clear();
		ConfigFiesta c = ConfigFiesta.get();
		float[] rango = luz == VERDE ? c.semaforoVerde : c.semaforoRoja;
		this.proximo = this.ticks + Math.round((rango[0] + this.azar.nextFloat() * (rango[1] - rango[0])) * 20);
		for (int[] p : PlanoFiesta.SEMAFORO_ROJO) {
			encender(nivel, p, luz == ROJA);
		}
		for (int[] p : PlanoFiesta.SEMAFORO_VERDE) {
			encender(nivel, p, luz == VERDE);
		}
		if (luz != PARADO) {
			int[] f = PlanoFiesta.SEMAFORO_ROJO.isEmpty() ? new int[] {PlanoFiesta.SEM_X1, PlanoFiesta.SUELO + 6, -1} : PlanoFiesta.SEMAFORO_ROJO.get(0);
			// la voz desde el semaforo, que se oiga en todo el pasillo
			nivel.playSound(null, f[0] + 0.5, f[1], f[2] + 0.5, luz == VERDE ? Sonidos.SEMAFORO_VERDE : Sonidos.SEMAFORO_ROJA,
				SoundSource.AMBIENT, 4.0F, 1.0F);
		}
		for (ServerPlayer j : dentro) {
			if (ServerPlayNetworking.canSend(j, Semaforo.TYPE)) {
				ServerPlayNetworking.send(j, new Semaforo(luz));
			}
		}
	}

	private static void encender(ServerLevel nivel, int[] p, boolean si) {
		BlockPos pos = new BlockPos(p[0], p[1], p[2]);
		BlockState e = nivel.getBlockState(pos);
		if (e.is(BloquesFiesta.LUZ_SEMAFORO) && e.getValue(LuzSemaforo.LUZ) != si) {
			nivel.setBlock(pos, e.setValue(LuzSemaforo.LUZ, si), Block.UPDATE_CLIENTS);
		}
	}

	/** Se ha movido en rojo: a la salida y sin unos tickets. */
	private void pillar(ServerLevel nivel, ServerPlayer j) {
		this.carrera.remove(j.getUUID());
		this.quietos.remove(j.getUUID());
		nivel.playSound(null, j.getX(), j.getEyeY(), j.getZ(), Sonidos.SEMAFORO_PILLADO, SoundSource.AMBIENT, 2.0F, 1.0F);
		j.teleportTo(nivel, PlanoFiesta.SEM_X0 + 2.5, PlanoFiesta.SUELO + 1, j.getZ(), java.util.Set.of(), -90.0F, 0.0F, true);
		JuegoFiesta.get().sumarTickets(j, -ConfigFiesta.get().semaforoCastigo, "¡te has movido en rojo!");
	}

	private void ganar(ServerLevel nivel, ServerPlayer j) {
		this.carrera.remove(j.getUUID());
		JuegoFiesta.Datos d = JuegoFiesta.get().datos(j);
		ConfigFiesta c = ConfigFiesta.get();
		int premio = d.semaforo == 0 ? c.semaforoPremio : c.semaforoPremioRepetido;
		d.semaforo++;
		nivel.playSound(null, j.getX(), j.getEyeY(), j.getZ(), Sonidos.SEMAFORO_GANA, SoundSource.AMBIENT, 2.0F, 1.0F);
		nivel.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, j.getX(), j.getY() + 1, j.getZ(), 30, 0.5, 0.8, 0.5, 0.3);
		JuegoFiesta.get().sumarTickets(j, premio, "¡meta del semáforo!");
	}
}
