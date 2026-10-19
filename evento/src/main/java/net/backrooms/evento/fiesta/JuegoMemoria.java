package net.backrooms.evento.fiesta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.backrooms.evento.Sonidos;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * La memoria de colores de la sala de juegos (PlanoFiesta.MEMORIA): quien pisa el boton de la
 * estrella empieza. Las cuatro baldosas (roja, amarilla, azul y verde) se encienden en orden,
 * cada una con su nota; luego hay que pisarlas en el mismo orden. Cada ronda la secuencia es una
 * mas larga (de 3 a RONDAS + 2); acertar la ultima da tickets (ConfigFiesta). Un fallo, salir de
 * la sala o tardar mas de 8 s en pisar la siguiente acaba la partida. Juega uno cada vez; los
 * demas miran (y oyen) y esperan.
 */
public final class JuegoMemoria {
	private static final JuegoMemoria INSTANCIA = new JuegoMemoria();
	/** Rondas que hay que superar: la primera secuencia es de 3 colores y la ultima de RONDAS + 2. */
	public static final int RONDAS = 5;
	private static final int PASO = 14;
	private static final int ENCENDIDA = 9;
	private static final int ESPERA = 20 * 8;

	public static JuegoMemoria get() {
		return INSTANCIA;
	}

	private final Random azar = new Random();
	private long ticks;
	private UUID jugador;
	private final List<Integer> secuencia = new ArrayList<>();
	private int ronda;
	/** Mostrando: desde que tick; esperando: cuantas lleva bien. -1 sin partida. */
	private long mostrando = -1;
	private int acertadas;
	private long ultimoPaso;
	private int pisando = -1;
	/** Tick en que se apaga cada baldosa (0: nada pendiente). */
	private final long[] apagar = new long[4];

	private JuegoMemoria() {
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(INSTANCIA::tick);
	}

	/** Para el staff (/backrooms fiesta juegos): quien juega y la secuencia (R, A, Z, V). */
	public String estado() {
		if (this.jugador == null) {
			return "memoria: libre";
		}
		StringBuilder s = new StringBuilder("memoria: ronda " + (this.ronda + 1) + ", secuencia ");
		for (int c : this.secuencia) {
			s.append("RAZV".charAt(c));
		}
		return s.append(this.mostrando >= 0 ? " (ensenando)" : " (lleva " + this.acertadas + ")").toString();
	}

	private static boolean enSala(ServerPlayer j) {
		PlanoFiesta.Sala s = PlanoFiesta.sala(j.getX(), j.getZ());
		return Fiesta.es(j.level()) && s != null && s.nombre().equals("memoria");
	}

	/** La baldosa de color que pisa (0-3), 4 el boton, -1 ninguna. */
	private static int pisa(ServerLevel nivel, ServerPlayer j) {
		BlockState e = nivel.getBlockState(BlockPos.containing(j.getX(), PlanoFiesta.SUELO + 0.5, j.getZ()));
		return e.is(BloquesFiesta.BALDOSA_JUEGO) && j.getY() < PlanoFiesta.SUELO + 1.3 ? e.getValue(BaldosaJuego.COLOR) : -1;
	}

	private void tick(MinecraftServer s) {
		this.ticks++;
		ServerLevel nivel = Fiesta.nivel();
		if (nivel == null) {
			return;
		}
		for (int k = 0; k < 4; k++) {
			if (this.apagar[k] > 0 && this.ticks >= this.apagar[k]) {
				this.apagar[k] = 0;
				this.encender(nivel, k, false);
			}
		}
		ServerPlayer j = this.jugador == null ? null : s.getPlayerList().getPlayer(this.jugador);
		if (this.jugador != null && (j == null || j.level() != nivel || !enSala(j) || !j.isAlive())) {
			this.acabar(nivel, j, "Has salido de la memoria de colores.");
			return;
		}
		if (this.jugador == null) {
			// alguien pisa el boton de la estrella: empieza
			for (ServerPlayer o : nivel.players()) {
				if (!o.isSpectator() && enSala(o) && pisa(nivel, o) == 4) {
					this.empezar(nivel, o);
					break;
				}
			}
			return;
		}
		if (this.mostrando >= 0) {
			this.mostrar(nivel, j);
			return;
		}
		int c = pisa(nivel, j);
		if (c == this.pisando) {
			if (this.ticks - this.ultimoPaso > ESPERA) {
				this.fallar(nivel, j, "¡Demasiado lento!");
			}
			return;
		}
		this.pisando = c;
		if (c < 0 || c > 3) {
			return;
		}
		this.ultimoPaso = this.ticks;
		if (c != this.secuencia.get(this.acertadas)) {
			this.fallar(nivel, j, "¡Ese no era!");
			return;
		}
		this.encender(nivel, c, true);
		this.nota(nivel, c);
		this.acertadas++;
		if (this.acertadas >= this.secuencia.size()) {
			if (this.ronda >= RONDAS - 1) {
				this.ganar(nivel, j);
			} else {
				this.ronda++;
				this.secuencia.add(this.azar.nextInt(4));
				j.displayClientMessage(Component.literal("¡Bien! Ronda " + (this.ronda + 1) + " de " + RONDAS).withStyle(ChatFormatting.GREEN), true);
				this.mostrando = this.ticks + 30;
			}
		}
	}

	private void empezar(ServerLevel nivel, ServerPlayer j) {
		this.jugador = j.getUUID();
		this.secuencia.clear();
		for (int i = 0; i < 3; i++) {
			this.secuencia.add(this.azar.nextInt(4));
		}
		this.ronda = 0;
		this.mostrando = this.ticks + 20;
		this.pisando = 4;
		j.displayClientMessage(Component.literal("MEMORIA · Mira el orden de los colores y písalos igual.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
		nivel.playSound(null, j.blockPosition(), Sonidos.MEMORIA_TURNO, SoundSource.BLOCKS, 1.2F, 0.8F);
	}

	/** Enciende la secuencia, una baldosa cada PASO ticks; al acabar, te toca. */
	private void mostrar(ServerLevel nivel, ServerPlayer j) {
		long t = this.ticks - this.mostrando;
		if (t < 0) {
			return;
		}
		int i = (int) (t / PASO);
		int dentro = (int) (t % PASO);
		if (i >= this.secuencia.size()) {
			this.mostrando = -1;
			this.acertadas = 0;
			this.ultimoPaso = this.ticks;
			this.pisando = pisa(nivel, j);
			nivel.playSound(null, j.blockPosition(), Sonidos.MEMORIA_TURNO, SoundSource.BLOCKS, 1.2F, 1.0F);
			j.displayClientMessage(Component.literal("¡Te toca!").withStyle(ChatFormatting.YELLOW), true);
			return;
		}
		int c = this.secuencia.get(i);
		if (dentro == 0) {
			this.encender(nivel, c, true);
			this.nota(nivel, c);
		}
	}

	private void nota(ServerLevel nivel, int c) {
		int[] b = PlanoFiesta.MEMORIA[c];
		nivel.playSound(null, b[0] + 1.0, PlanoFiesta.SUELO + 1, b[1] + 1.0, Sonidos.MEMORIA_NOTA[c], SoundSource.BLOCKS, 1.4F, 1.0F);
		// se apaga sola al rato
		this.apagar[c] = this.ticks + ENCENDIDA;
	}

	private void encender(ServerLevel nivel, int c, boolean si) {
		int[] b = PlanoFiesta.MEMORIA[c];
		for (int dx = 0; dx < 2; dx++) {
			for (int dz = 0; dz < 2; dz++) {
				BlockPos p = new BlockPos(b[0] + dx, PlanoFiesta.SUELO, b[1] + dz);
				BlockState e = nivel.getBlockState(p);
				if (e.is(BloquesFiesta.BALDOSA_JUEGO) && e.getValue(BaldosaJuego.LUZ) != si) {
					nivel.setBlock(p, e.setValue(BaldosaJuego.LUZ, si), Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	private void fallar(ServerLevel nivel, ServerPlayer j, String porque) {
		nivel.playSound(null, j.blockPosition(), Sonidos.MEMORIA_ERROR, SoundSource.BLOCKS, 1.4F, 1.0F);
		this.acabar(nivel, j, porque + " Llegaste a la ronda " + (this.ronda + 1) + ". Pisa la estrella para volver a jugar.");
	}

	private void ganar(ServerLevel nivel, ServerPlayer j) {
		JuegoFiesta.Datos d = JuegoFiesta.get().datos(j);
		ConfigFiesta c = ConfigFiesta.get();
		int premio = d.memoria == 0 ? c.memoriaPremio : c.memoriaPremioRepetido;
		d.memoria++;
		nivel.playSound(null, j.getX(), j.getEyeY(), j.getZ(), Sonidos.SEMAFORO_GANA, SoundSource.BLOCKS, 2.0F, 1.0F);
		for (int k = 0; k < 4; k++) {
			this.encender(nivel, k, true);
		}
		JuegoFiesta.get().sumarTickets(j, premio, "¡memoria perfecta!");
		this.acabar(nivel, null, null);
	}

	private void acabar(ServerLevel nivel, ServerPlayer j, String aviso) {
		if (j != null && aviso != null) {
			j.displayClientMessage(Component.literal(aviso).withStyle(ChatFormatting.GRAY), false);
		}
		this.jugador = null;
		this.mostrando = -1;
		this.secuencia.clear();
		for (int k = 0; k < 4; k++) {
			this.apagar[k] = this.ticks + 30;
		}
	}
}
