package net.backrooms.evento.cliente.cinematica;

import com.mojang.math.Transformation;
import java.util.HashMap;
import java.util.Map;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.esqueleto.Animacion;
import net.backrooms.evento.esqueleto.Ciclos;
import net.backrooms.evento.esqueleto.Figura;
import net.backrooms.evento.esqueleto.Hueso;
import net.backrooms.evento.esqueleto.Pose;
import net.backrooms.evento.estacion.Amasijo;
import net.backrooms.evento.estacion.PlanoEstacion;
import net.backrooms.evento.fiesta.BloquesFiesta;
import net.backrooms.evento.fiesta.ParedVariante;
import net.backrooms.evento.mision.Entidades;
import net.backrooms.evento.vestibulo.PlanoVestibulo;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * El final del evento (lo empieza SalidaEstacion al entrar en el ascensor del final de la Estacion). La
 * camara va sola, sin HUD, con franjas (PantallaSalidaEstacion); el otro superviviente (una Figura con el
 * esqueleto articulado), el Amasijo que lo atrapa, las hojas de las puertas, lo que marca el indicador y
 * las manos son solo de este cliente. Guion (segundos):
 *
 *   0   - 5     entra en la cabina y se da la vuelta; tranquilo, mira el indicador (3)
 *   APARECE     al fondo de la pasarela sale otro superviviente corriendo hacia el ascensor (zoom)
 *   SUBE        el Amasijo trepa desde el pozo por la barandilla; ATAQUE: se le echa encima y lo tumba
 *   ATAQUE-PANEL el superviviente forcejea y deja de moverse; el Amasijo se alimenta de el
 *   PANEL       se vuelve a la botonera y marca el piso (pitidos y el zumbido de confirmacion)
 *   OYE         el Amasijo deja de comer, se queda quieto, se gira hacia el ascensor (GIRA) y chilla
 *   CORRE       viene corriendo por la pasarela; CIERRA - CERRADA: las puertas se cierran por poco
 *   CERRADA     se corta todo el sonido de fuera: silencio; MUSICA: la musica del ascensor
 *   MANOS       baja la vista: las manos manchadas de sangre; despues vuelve a mirar las puertas
 *   CAMPANA     la campanilla de llegada; CORTE: la imagen se corta a negro
 *
 * El servidor le lleva entonces al ascensor del vestibulo: alli (llegada) se ve la cabina con las puertas
 * cerradas, se abren (ABRE) y aparece el vestibulo; el servidor le da por escapado con las puertas ya
 * abiertas (SalidaEstacion.ESCAPA) y al acabar se le devuelve la camara.
 *
 * El sonido (tools/sonidos/salida-estacion.js, con estos tiempos) va en dos archivos: la Estacion y la llegada.
 */
public final class SalidaEstacionCliente {
	private static final Identifier BANDA = BackroomsEvento.id("cinematica.salida_estacion");
	private static final Identifier BANDA_FIN = BackroomsEvento.id("cinematica.salida_estacion.despues");
	private static final Identifier PIEL = BackroomsEvento.id("textures/entity/superviviente.png");
	private static final Identifier PIEL_SANGRE = BackroomsEvento.id("textures/entity/superviviente_sangre.png");

	static final float APARECE = 5.0F;
	static final float SUBE = 9.6F;
	static final float ATAQUE = 11.2F;
	static final float PANEL = 17.5F;
	static final float[] PULSA = {18.5F, 18.85F, 19.2F, 19.55F};
	static final float CONFIRMA = 19.9F;
	static final float OYE = 20.2F;
	static final float GIRA = 21.0F;
	static final float CORRE = 22.3F;
	static final float CIERRA = 24.9F;
	static final float CERRADA = 26.0F;
	static final float LLEGA = 26.1F;
	static final float MUSICA = 28.0F;
	static final float MANOS = 30.0F;
	static final float CAMPANA = 41.5F;
	static final float CORTE = 42.2F;
	/** La llegada (segundos desde que se ve el vestibulo). */
	static final float ABRE = 1.0F;
	static final float ABIERTA = 2.6F;
	static final float FIN_LLEGADA = 5.0F;
	/** Lo que corre el superviviente (bloques/s) y el Amasijo. */
	private static final double VEL_SUPERVIVIENTE = (double) 34 / (ATAQUE - APARECE);
	private static final float CICLO = 3.8F;
	/** Lo que marca el indicador: {desde el segundo, piso}. */
	private static final float[][] PISOS = {{0.0F, 3}, {33.0F, 2}, {36.0F, 1}, {CAMPANA, 0}};

	private static int ticks = -1;
	private static float llegada = -1.0F;
	private static boolean llegando;
	private static @Nullable Marker camara;
	private static @Nullable Object nivel;
	private static boolean guiAntes;
	private static float giro0;
	private static float cabeceo0;
	private static double px;
	private static double py;
	private static double pz;
	private static float giroSur;
	private static @Nullable SoundInstance banda;
	private static @Nullable Figura superviviente;
	private static @Nullable Figura manos;
	/** El brazo que pulsa la botonera (CuerpoPrimeraPersona). */
	private static @Nullable Figura brazo;
	private static @Nullable Amasijo amasijo;
	private static float pasos;
	private static final Display.BlockDisplay[] hojas = new Display.BlockDisplay[2];
	private static final Map<BlockPos, BlockState> originales = new HashMap<>();
	private static final Map<BlockPos, BlockState> puestos = new HashMap<>();
	private static int piso = -1;
	private static int siguienteId = -7300;

	/** La geometria (de PlanoEstacion): la x del centro, el suelo, la plataforma, el ataque. */
	private static double X;
	private static double Y;
	private static double Z0;
	private static double ZC;
	private static double ZA;
	private static double ZINICIO;
	private static double OJOS;

	private SalidaEstacionCliente() {
	}

	public static void registrar() {
		ClientTickEvents.END_CLIENT_TICK.register(SalidaEstacionCliente::tick);
	}

	public static boolean activa() {
		return ticks >= 0;
	}

	public static boolean despertando() {
		return activa() && llegada >= 0;
	}

	public static float segundos(float parcial) {
		return ticks < 0 ? -1.0F : (ticks + parcial) / 20.0F;
	}

	/** Si ese Amasijo es el de la cinematica (los demas no se ven mientras dura). */
	public static boolean esSuyo(Entity e) {
		return e == amasijo;
	}

	public static boolean esBanda(Identifier id) {
		return id.equals(BANDA) || id.equals(BANDA_FIN);
	}

	public static void empezar() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return;
		}
		if (activa()) {
			terminar();
		}
		int[] p = PlanoEstacion.PASARELA;
		X = p[0] + 1.5;
		Y = p[1] + 1;
		Z0 = p[2];
		ZC = Z0 - 4.0;
		ZA = p[3] + 18.0;
		ZINICIO = p[4] + 0.5;
		OJOS = Y + 1.62;
		ticks = 0;
		llegada = -1.0F;
		llegando = false;
		piso = -1;
		pasos = 0.0F;
		nivel = mc.level;
		giro0 = Mth.wrapDegrees(mc.player.getYRot());
		cabeceo0 = mc.player.getXRot();
		px = mc.player.getX();
		py = mc.player.getEyeY();
		pz = mc.player.getZ();
		giroSur = giro0 + Mth.wrapDegrees(0.0F - giro0);
		Marker m = new Marker(EntityType.MARKER, mc.level);
		camara = m;
		colocar(m, 0.0F, true);
		tomarCamara(mc, m);
		guiAntes = mc.options.hideGui;
		mc.options.hideGui = true;
		originales.clear();
		puestos.clear();
		hojas[0] = hoja(mc, p[0], p[1] + 1, p[2] - 2);
		hojas[1] = hoja(mc, p[0], p[1] + 1, p[2] - 2);
		puertas(0.0F, 0.0F);
		banda = sonar(mc, BANDA);
		mc.setScreen(new PantallaSalidaEstacion());
	}

	/* ----------------------------------------------------------- el decorado */

	private static Display.BlockDisplay hoja(Minecraft mc, int x, int y, int z) {
		Display.BlockDisplay d = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, mc.level);
		d.setId(siguienteId--);
		d.setBlockState(BloquesFiesta.HOJA_ASCENSOR.defaultBlockState());
		d.setPos(x, y, z);
		d.setViewRange(4.0F);
		d.setTransformationInterpolationDuration(2);
		mc.level.addEntity(d);
		return d;
	}

	/** Las hojas: cierre 0 abiertas (metidas en el marco), 1 cerradas; golpe: lo que se abollan. */
	private static void puertas(float cierre, float golpe) {
		for (int k = 0; k < 2; k++) {
			if (hojas[k] == null) {
				continue;
			}
			float x = k == 0 ? Mth.lerp(cierre, -1.5F, 0.0F) : Mth.lerp(cierre, 3.0F, 1.5F);
			hojas[k].setTransformation(new Transformation(new Vector3f(x, 0.0F, 0.3F + golpe * 0.06F), new Quaternionf(),
				new Vector3f(1.5F, 1.5F, 1.0F), new Quaternionf()));
		}
	}

	private static Figura figura(Minecraft mc, Identifier piel) {
		Figura f = Entidades.FIGURA.create(mc.level, EntitySpawnReason.EVENT);
		if (f == null) {
			return null;
		}
		f.setId(siguienteId--);
		f.piel = piel;
		mc.level.addEntity(f);
		return f;
	}

	/** Cambia un bloque solo en este cliente (el indicador), apuntando como estaba; se reafirma cada tick. */
	private static void poner(Minecraft mc, BlockPos p, BlockState estado) {
		originales.putIfAbsent(p.immutable(), mc.level.getBlockState(p));
		puestos.put(p.immutable(), estado);
		if (mc.level.getBlockState(p) != estado) {
			mc.level.setBlock(p, estado, 18);
		}
	}

	private static void indicadores(Minecraft mc, float t) {
		int v = 3;
		for (float[] p : PISOS) {
			if (t >= p[0]) {
				v = (int) p[1];
			}
		}
		if (v != piso) {
			piso = v;
			int[] p = PlanoEstacion.PASARELA;
			for (int x : new int[] {p[0], p[0] + 2}) {
				BlockPos pos = new BlockPos(x, p[1] + 3, p[2] - 3);
				BlockState e = originales.getOrDefault(pos, mc.level.getBlockState(pos));
				if (e.is(BloquesFiesta.INDICADOR_ASCENSOR)) {
					poner(mc, pos, e.setValue(ParedVariante.VARIANTE, v));
				}
			}
		}
		puestos.forEach((pos, e) -> {
			if (mc.level.getBlockState(pos) != e) {
				mc.level.setBlock(pos, e, 18);
			}
		});
	}

	/** Donde va el superviviente (z) mientras corre. */
	private static double zSuperviviente(float t) {
		double s = Math.max(0.0, t - APARECE) * VEL_SUPERVIVIENTE;
		return Math.max(ZA, ZINICIO - s);
	}

	/** El superviviente: corre, le golpea, cae boca arriba, forcejea y se queda quieto. */
	private static Animacion caidaSuperviviente() {
		Pose golpe = Ciclos.correr(0.3F, 1.0F).mas(Ciclos.susto(1.0F), 1.0F).suma(Hueso.TORSO, -25, -20, 0)
			.suma(Hueso.BRAZO_D, -50, 0, 30).suma(Hueso.BRAZO_I, -70, 0, -20).cuerpo(-20, 0, 8, 0, 0, 0.1F);
		return new Animacion()
			.clave(ATAQUE - 0.01F, t -> Ciclos.correr((float) ((ZINICIO - zSuperviviente(t)) / CICLO), 1.0F))
			.claveLineal(ATAQUE + 0.15F, golpe)
			.claveLineal(ATAQUE + 0.5F, Ciclos.bocaArriba())
			.clave(ATAQUE + 0.75F, Ciclos::forcejeo)
			.clave(ATAQUE + 3.4F, Ciclos::forcejeo)
			.clave(ATAQUE + 4.6F, t -> Ciclos.bocaArriba().suma(Hueso.BRAZO_I, (float) Math.sin(t * 3.0) * 4.0F, 0, 0));
	}

	private static final Animacion CAIDA = caidaSuperviviente();

	private static void decorado(Minecraft mc, float t) {
		// las puertas: abiertas; se cierran por poco antes de que llegue; el golpe del Amasijo
		float c = Mth.clamp((t - CIERRA) / (CERRADA - CIERRA), 0.0F, 1.0F);
		float cierre = 1.0F - (1.0F - c) * (1.0F - c) * (1.0F - c);
		float golpe = t >= LLEGA && t < LLEGA + 0.4F ? (1.0F - (t - LLEGA) / 0.4F) : 0.0F;
		puertas(cierre, golpe);
		indicadores(mc, t);
		// el superviviente
		if (t >= APARECE && superviviente == null) {
			Figura f = figura(mc, PIEL);
			if (f != null) {
				f.snapTo(X - 0.3, Y, zSuperviviente(t), 180.0F, 0.0F);
				f.animador = (fig, p) -> CAIDA.en(segundos(p));
				superviviente = f;
			}
		}
		Figura f = superviviente;
		if (f != null) {
			f.xo = f.getX();
			f.yo = f.getY();
			f.zo = f.getZ();
			f.yRotO = f.getYRot();
			// derribado: le empuja un poco hacia atras
			double z = zSuperviviente(t) + (t >= ATAQUE ? 0.7 * suave((t - ATAQUE) / 0.5F) : 0.0);
			f.setPos(X - 0.3, Y, z);
		}
		// el Amasijo
		if (t >= SUBE - 0.5F && t < CERRADA && amasijo == null) {
			Amasijo a = Entidades.AMASIJO.create(mc.level, EntitySpawnReason.EVENT);
			if (a != null) {
				a.setId(-7350);
				a.noPhysics = true;
				double[] p = poseAmasijo(t);
				a.snapTo(p[0], p[1], p[2], (float) p[3], 0.0F);
				mc.level.addEntity(a);
				amasijo = a;
			}
		}
		Amasijo a = amasijo;
		if (a != null && t >= CERRADA + 0.4F) {
			// ya esta detras de las puertas cerradas: se va (no puede quedarse dentro ni asomar)
			mc.level.removeEntity(a.getId(), Entity.RemovalReason.DISCARDED);
			amasijo = null;
			a = null;
		}
		if (a != null) {
			double[] p = poseAmasijo(t);
			a.xo = a.getX();
			a.yo = a.getY();
			a.zo = a.getZ();
			a.setPos(p[0], p[1], p[2]);
			float g = (float) p[3];
			a.yRotO = a.getYRot();
			a.setYRot(g);
			a.yBodyRotO = a.yBodyRot;
			a.setYBodyRot(g);
			a.yHeadRotO = a.yHeadRot;
			a.setYHeadRot(g);
			// agarra al caer encima; luego come a tirones (ModeloAmasijo: come), se queda alerta al oir la botonera
			boolean agarra = t >= ATAQUE + 0.2F && t < ATAQUE + 1.1F;
			a.cazandoCliente(t >= SUBE && t < ATAQUE + 0.2F || t >= GIRA + 0.8F && t < CERRADA - 0.3F, agarra);
			a.embisteCliente = suave((t - ATAQUE + 0.6F) / 0.3F) * (1.0F - suave((t - ATAQUE - 0.1F) / 0.25F));
			a.comeCliente = suave((t - ATAQUE - 0.9F) / 0.5F) * (1.0F - suave((t - OYE) / 0.25F));
			a.alertaCliente = suave((t - OYE) / 0.35F) * (1.0F - suave((t - CORRE + 0.2F) / 0.4F));
			andar(a, t >= CORRE && t < LLEGA ? 1.3F : t >= SUBE && t < ATAQUE ? 0.6F : t >= GIRA && t < CORRE ? 0.45F : 0.0F);
		}
		// el brazo derecho que marca el piso en la botonera
		if (t >= PANEL && t < OYE + 0.6F && brazo == null) {
			brazo = net.backrooms.evento.cliente.esqueleto.CuerpoPrimeraPersona.crear(mc, PIEL_SANGRE, siguienteId--, Hueso.BRAZO_D, Hueso.ANTEBRAZO_D,
				Hueso.MANO_D);
			if (brazo != null) {
				float[] pulsa = {PULSA[0], PULSA[1], PULSA[2], PULSA[3], CONFIRMA};
				brazo.animador = (fig, p) -> net.backrooms.evento.cliente.esqueleto.CuerpoPrimeraPersona.pulsar(segundos(p), PANEL + 0.45F, OYE - 0.1F,
					pulsa, 22.0F);
			}
		}
		if (brazo != null) {
			if (t >= OYE + 0.6F) {
				net.backrooms.evento.cliente.esqueleto.CuerpoPrimeraPersona.quitar(mc, brazo);
				brazo = null;
			} else {
				double[] c0 = pose(t);
				net.backrooms.evento.cliente.esqueleto.CuerpoPrimeraPersona.colocar(brazo, c0[0], c0[1], c0[2], (float) c0[3], 0.0);
			}
		}
		// las manos, al mirarlas
		if (t >= MANOS && t < MANOS + 9.0F && manos == null) {
			// el cuerpo entero del jugador menos la cabeza: al bajar la vista se ven el pecho, los brazos
			// unidos a los hombros y las manos manchadas, todo de una pieza
			Figura m = net.backrooms.evento.cliente.esqueleto.CuerpoPrimeraPersona.crear(mc, PIEL_SANGRE, siguienteId--, Hueso.TODOS);
			if (m != null) {
				m.animador = (fig, p) -> poseManos(segundos(p));
				manos = m;
			}
		}
		if (manos != null) {
			if (t >= MANOS + 9.0F) {
				mc.level.removeEntity(manos.getId(), Entity.RemovalReason.DISCARDED);
				manos = null;
			} else if (camara != null) {
				// el cuerpo bajo los ojos, con el pecho justo por detras de ellos: al bajar la vista suben los
				// brazos desde abajo, unidos a los hombros, y las manos quedan delante
				double[] c0 = pose(t);
				net.backrooms.evento.cliente.esqueleto.CuerpoPrimeraPersona.colocar(manos, c0[0], c0[1] - 0.14, c0[2], (float) c0[3], -0.22);
			}
		}
	}

	/** Las manos: suben despacio desde abajo, se quedan (temblando) y bajan. */
	private static Pose poseManos(float t) {
		Pose abajo = new Pose().gira(Hueso.BRAZO_D, 10, 0, 0).gira(Hueso.BRAZO_I, 10, 0, 0).gira(Hueso.ANTEBRAZO_D, -10, 0, 0).gira(Hueso.ANTEBRAZO_I, -10, 0, 0);
		float sube = suave((t - MANOS - 1.0F) / 1.8F) * (1.0F - suave((t - MANOS - 6.6F) / 1.6F));
		Pose p = Pose.mezcla(abajo, Ciclos.manos(t), sube);
		float tiembla = (float) Math.sin(t * 23.0) * 1.2F * sube;
		// los brazos mas caidos desde el hombro: las manos quedan mas lejos de los ojos
		p = p.suma(Hueso.BRAZO_D, 12.0F * sube, 0, 0).suma(Hueso.BRAZO_I, 12.0F * sube, 0, 0)
			.suma(Hueso.ANTEBRAZO_D, -8.0F * sube, 0, 0).suma(Hueso.ANTEBRAZO_I, -8.0F * sube, 0, 0);
		return p.suma(Hueso.ANTEBRAZO_D, tiembla, 0, 0).suma(Hueso.ANTEBRAZO_I, -tiembla, 0, 0)
			.suma(Hueso.MANO_D, 0, 12.0F * suave((t - MANOS - 3.5F) / 1.5F), 0).suma(Hueso.MANO_I, 0, -12.0F * suave((t - MANOS - 3.5F) / 1.5F), 0);
	}

	/**
	 * Donde esta el Amasijo {x, y, z, giro}: escondido en el pozo, trepa por el lado este, salta encima del
	 * superviviente, come, se gira hacia el ascensor y viene corriendo hasta las puertas.
	 */
	private static double[] poseAmasijo(float t) {
		double xLado = X + 3.0;
		double zSube = ZA - 1.2;
		double zCome = ZA + 1.0;
		// se para justo fuera de las puertas: las hojas estan en Z0 - 1,6 y las manos le llegan ~1,6 bloques por
		// delante del centro, asi que las toca sin atravesarlas
		double zPuerta = Z0 + 0.45;
		if (t < SUBE) {
			return new double[] {xLado, Y - 8.0, zSube, 90.0};
		}
		if (t < ATAQUE - 0.3F) {
			// trepa: sube por el borde del pozo y asoma por la barandilla
			double k = suave((t - SUBE) / (ATAQUE - 0.3F - SUBE));
			return new double[] {xLado, Mth.lerp(k, Y - 6.0, Y + 0.2), zSube, 90.0};
		}
		if (t < ATAQUE + 0.2F) {
			// el salto: en arco, encima de el
			double k = Mth.clamp((t - ATAQUE + 0.3F) / 0.5F, 0.0F, 1.0F);
			double arco = Math.sin(k * Math.PI) * 0.8;
			return new double[] {Mth.lerp(k, xLado, X - 0.1), Y + 0.2 * (1 - k) + arco, Mth.lerp(k, zSube, zCome), 90.0};
		}
		if (t < GIRA) {
			// comiendo: se mece encima del cuerpo
			double m = t < OYE ? Math.sin(t * 6.0) * 0.06 : 0.0;
			double g = 90.0 + (t < OYE ? Math.sin(t * 2.7) * 8.0 : 0.0);
			return new double[] {X - 0.1, Y + m, zCome, g};
		}
		if (t < CORRE) {
			// se gira despacio hacia el ascensor
			double k = suave((t - GIRA) / 1.0F);
			return new double[] {X - 0.1, Y, zCome, Mth.lerp(k, 90.0, 180.0)};
		}
		// viene corriendo (7 bloques por segundo), arrancando en medio segundo, hasta las puertas
		double total = zCome - zPuerta;
		double v = total / (LLEGA - CORRE - 0.25);
		double tau = t - CORRE;
		double s = tau < 0.5 ? v * tau * tau : v * (tau - 0.25);
		return new double[] {Mth.lerp(suave((t - CORRE) / 1.5F), X - 0.1, X), Y, zCome - Math.min(s, total), 180.0};
	}

	/** La animacion de andar a ese ritmo (como en SalidaFiestaCliente#andar). */
	private static void andar(Amasijo a, float ritmo) {
		float hecho = a.walkAnimation.position() - pasos;
		if (ritmo > 0.0F) {
			a.walkAnimation.update(Math.max(0.0F, ritmo - hecho), 1.0F, 1.0F);
			a.walkAnimation.setSpeed(ritmo);
		}
		pasos = a.walkAnimation.position();
	}

	/* ------------------------------------------------------------ la camara */

	private static SoundInstance sonar(Minecraft mc, Identifier id) {
		SoundInstance s = new SimpleSoundInstance(id, SoundSource.MASTER, 1.0F, 1.0F, RandomSource.create(), false, 0,
			SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
		mc.getSoundManager().play(s);
		return s;
	}

	private static void tomarCamara(Minecraft mc, Marker m) {
		mc.setCameraEntity(m);
		((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjos(0.0F);
		((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjosAntes(0.0F);
	}

	private static void tick(Minecraft mc) {
		if (!activa()) {
			return;
		}
		if (mc.level == null || mc.player == null) {
			olvidar();
			return;
		}
		ticks++;
		float t = ticks / 20.0F;
		if (llegada < 0 && mc.level != nivel) {
			// ya en el vestibulo: lo de la Estacion se queda alli; se espera a que acabe la pantalla de carga
			if (!llegando) {
				llegando = true;
				camara = null;
				superviviente = null;
				manos = null;
				brazo = null;
				amasijo = null;
				java.util.Arrays.fill(hojas, null);
				originales.clear();
				puestos.clear();
			}
			if (mc.screen instanceof LevelLoadingScreen) {
				return;
			}
			llegar(mc, t);
		}
		if (llegada < 0) {
			decorado(mc, t);
			if (camara != null) {
				colocar(camara, t, false);
			}
			if (t > net.backrooms.evento.estacion.SalidaEstacion.DURACION / 20.0F + 8.0F) {
				terminar();
			}
		} else {
			float a = t - llegada;
			float k = suave((a - ABRE) / (ABIERTA - ABRE));
			puertas(1.0F - k, 0.0F);
			if (camara != null) {
				colocar(camara, t, false);
				if (mc.getCameraEntity() != camara) {
					tomarCamara(mc, camara);
				}
			}
			if (a >= FIN_LLEGADA) {
				terminar();
			}
		}
	}

	/** Llega al vestibulo: la camara en la cabina, las puertas cerradas, la banda de la llegada. */
	private static void llegar(Minecraft mc, float t) {
		llegada = t;
		nivel = mc.level;
		Marker m = new Marker(EntityType.MARKER, mc.level);
		camara = m;
		colocar(m, t, true);
		tomarCamara(mc, m);
		mc.options.hideGui = true;
		hojas[0] = hoja(mc, -1, PlanoVestibulo.SUELO + 1, PlanoVestibulo.ASCENSOR_PUERTA_Z);
		hojas[1] = hoja(mc, -1, PlanoVestibulo.SUELO + 1, PlanoVestibulo.ASCENSOR_PUERTA_Z);
		puertas(1.0F, 0.0F);
		banda = sonar(mc, BANDA_FIN);
		if (!(mc.screen instanceof PantallaSalidaEstacion)) {
			mc.setScreen(new PantallaSalidaEstacion());
		}
	}

	private static void colocar(Marker m, float t, boolean tambienAntes) {
		double[] antes = pose(Math.max(0.0F, t - 0.05F));
		double[] ahora = pose(t);
		if (tambienAntes) {
			antes = ahora;
		}
		m.setPos(ahora[0], ahora[1], ahora[2]);
		m.xo = antes[0];
		m.yo = antes[1];
		m.zo = antes[2];
		m.xOld = antes[0];
		m.yOld = antes[1];
		m.zOld = antes[2];
		m.setYRot((float) ahora[3]);
		m.yRotO = (float) antes[3];
		m.setXRot((float) ahora[4]);
		m.xRotO = (float) antes[4];
	}

	/** El giro para mirar la botonera de la derecha desde donde se pega a ella (X + 0.9, Z0 - 2.9). */
	private static double giroPanel() {
		return giroSur - Math.toDegrees(Math.atan2(0.54, 0.4));
	}

	/** El cabeceo para mirar algo a esa altura y distancia. */
	private static double mirar(double alto, double d) {
		return Math.toDegrees(Math.atan2(OJOS - alto, Math.max(1.0, d)));
	}

	/** Lo que se mira fuera en cada momento: {altura, z} (el superviviente y despues el Amasijo). */
	private static double[] objetivo(float t) {
		if (t < ATAQUE) {
			return new double[] {Y + 1.1, zSuperviviente(t)};
		}
		if (t < CORRE) {
			return new double[] {Y + 0.6, ZA + 1.0};
		}
		return new double[] {Y + 1.0, poseAmasijo(t)[2]};
	}

	/** {x, y, z, giro, cabeceo} en el segundo t (giro 0 = mirando al sur, a la pasarela). */
	static double[] pose(float t) {
		if (llegada >= 0) {
			return poseLlegada(t - llegada);
		}
		double x = X;
		double y = OJOS;
		double z = ZC;
		double giro = giroSur;
		double cabeceo = 2.0;
		double sacude = 0.0;
		// vuelto hacia la botonera (la de la derecha, al este)
		double giroBoton = giroSur - Math.toDegrees(Math.atan2(1.45, (Z0 - 2.5) - ZC));
		if (t < 3.0F) {
			// entra hasta el centro de la cabina y se da la vuelta
			double k = suave(t / 3.0F);
			x = Mth.lerp(k, px, X);
			y = Mth.lerp(k, py, OJOS);
			z = Mth.lerp(k, pz, ZC);
			giro = Mth.lerp(k, giro0, giroSur);
			cabeceo = Mth.lerp(k, cabeceo0, 2.0);
		} else if (t < APARECE) {
			// tranquilo: mira el indicador de arriba de la botonera y vuelve a mirar fuera
			double k = suave((t - 3.0F) / 0.8F) * (1.0 - suave((t - 4.2F) / 0.8F));
			giro = Mth.lerp(k, giroSur, giroBoton + 4.0);
			cabeceo = Mth.lerp(k, 2.0, -22.0);
		} else if (t < PANEL) {
			// fuera: el superviviente que viene, el ataque y lo que pasa despues
			double[] o = objetivo(t);
			double k = suave((t - APARECE) / 1.2F);
			cabeceo = Mth.lerp(k, 2.0, mirar(o[0], o[1] - z));
			giro = giroSur + Math.sin(t * 0.5) * 0.15;
			sacude = 0.12 + pulso(t, ATAQUE, 0.6F) * 0.6;
		} else if (t < OYE + 0.1F) {
			// se lanza a la botonera (se pega a ella para alcanzarla) y marca el piso, deprisa
			double k = suave((t - PANEL) / 0.6F);
			giro = Mth.lerp(k, giroSur, giroPanel());
			double[] o = objetivo(PANEL);
			cabeceo = Mth.lerp(k, mirar(o[0], o[1] - z), 9.0);
			x = Mth.lerp(k, X, X + 0.9);
			z = Mth.lerp(k, ZC, Z0 - 2.9);
			sacude = 0.25;
			for (float p : PULSA) {
				cabeceo += 3.0 * pulso(t, p, 0.14F);
				sacude = Math.max(sacude, pulso(t, p, 0.14F) * 0.7);
			}
			sacude = Math.max(sacude, pulso(t, CONFIRMA, 0.3F) * 0.6);
		} else if (t < CERRADA + 0.3F) {
			// se vuelve de golpe hacia fuera: el Amasijo le ha oido y viene; se echa hacia el fondo
			double k = suave((t - OYE - 0.1F) / 0.45F);
			double[] o = objetivo(t);
			z = Mth.lerp(suave((t - 23.5F) / 2.0F), Mth.lerp(k, Z0 - 2.9, ZC), Z0 - 4.7);
			giro = Mth.lerp(k, giroPanel(), giroSur);
			cabeceo = Mth.lerp(k, 9.0, mirar(o[0], o[1] - z));
			x = Mth.lerp(k, X + 0.9, X);
			sacude = 0.3 + 0.9 * suave((t - CORRE) / 3.5F) + pulso(t, LLEGA, 0.5F) * 2.5;
		} else if (t < MANOS) {
			// las puertas cerradas; el silencio
			z = Z0 - 4.7;
			double k = suave((t - CERRADA - 0.3F) / 1.5F);
			cabeceo = Mth.lerp(k, mirar(Y + 1.0, Z0 - 1.55 - z), 3.0) + Math.sin(t * 0.9) * 0.4;
			y += Math.sin(t * 1.5) * 0.006;
		} else if (t < CAMPANA - 1.5F) {
			// baja la vista a las manos, se queda mirandolas y vuelve a levantarla
			z = Z0 - 4.7;
			double baja = suave((t - MANOS) / 2.0F) * (1.0 - suave((t - MANOS - 7.4F) / 2.0F));
			cabeceo = Mth.lerp(baja, 3.0, 50.0) + Math.sin(t * 0.8) * 0.5;
			giro = giroSur + Math.sin(t * 0.37) * 1.5 * baja;
			y += Math.sin(t * 1.5) * 0.006;
		} else {
			// mira las puertas, esperando
			z = Z0 - 4.7;
			cabeceo = 3.0 - 2.0 * suave((t - (CAMPANA - 1.5F)) / 1.5F);
		}
		double r1 = Math.sin(t * 37.0) * 0.6 + Math.sin(t * 59.0) * 0.4;
		double r2 = Math.sin(t * 43.0 + 1.3) * 0.6 + Math.sin(t * 67.0) * 0.4;
		return new double[] {x + r1 * 0.012 * sacude, y + r2 * 0.012 * sacude, z, giro + r2 * 0.5 * sacude, cabeceo + r1 * 0.5 * sacude};
	}

	/** En el vestibulo: dentro de la cabina, mirando las puertas que se abren (giro 180: al norte). */
	private static double[] poseLlegada(float a) {
		double[] l = PlanoVestibulo.LLEGADA_ASCENSOR;
		double c = 1.0 + Math.sin(a * 0.9) * 0.4;
		return new double[] {l[0], l[1] + 1.62 + Math.sin(a * 1.4) * 0.005, l[2], l[3], c * (1.0 - suave((a - 3.0F) / 2.0F))};
	}

	private static float pulso(float t, float en, float dura) {
		float d = t - en;
		return d < 0 || d >= dura ? 0.0F : (1.0F - d / dura) * (1.0F - d / dura);
	}

	private static float suave(float k) {
		k = Mth.clamp(k, 0.0F, 1.0F);
		return k * k * (3.0F - 2.0F * k);
	}

	private static double suave(double k) {
		return suave((float) k);
	}

	/* ------------------------------------------------------------- pantalla */

	/** 0..1 de negro (el corte tras la campanilla, el viaje y el principio de la llegada). */
	public static float negro(float parcial) {
		if (!activa()) {
			return 0.0F;
		}
		float t = segundos(parcial);
		if (llegada >= 0) {
			return 1.0F - Mth.clamp((t - llegada) / 0.6F, 0.0F, 1.0F);
		}
		return llegando || t >= CORTE ? 1.0F : 0.0F;
	}

	public static float franjas(float parcial) {
		if (!activa()) {
			return 0.0F;
		}
		float t = segundos(parcial);
		if (llegada >= 0) {
			return 1.0F - Mth.clamp((t - llegada - FIN_LLEGADA + 1.5F) / 1.5F, 0.0F, 1.0F);
		}
		return Mth.clamp(t / 0.8F, 0.0F, 1.0F);
	}

	/** El zoom (1 = normal): como un teleobjetivo hacia la pasarela, que se abre con lo cerca que esta. */
	public static float zoom(float parcial) {
		if (!activa() || llegada >= 0) {
			return 1.0F;
		}
		float t = segundos(parcial);
		if (t < APARECE || t >= CERRADA) {
			return 1.0F;
		}
		double[] o = objetivo(t);
		double d = Math.max(1.0, o[1] - pose(t)[2]);
		float z = (float) Math.min(1.0, 7.0 / d);
		float entra = suave((t - APARECE) / 1.5F);
		// mirando la botonera, normal
		float panel = suave((t - PANEL) / 0.5F) * (1.0F - suave((t - OYE - 0.1F) / 0.45F));
		return Mth.lerp(entra * (1.0F - panel), 1.0F, z);
	}

	/** Cuanto se oye el resto del mundo: la Estacion de fondo hasta que se cierran las puertas; en el vestibulo vuelve. */
	public static float volumenMundo() {
		if (!activa()) {
			return 1.0F;
		}
		float t = segundos(0.0F);
		if (llegada >= 0) {
			return Mth.clamp((t - llegada - ABRE) / 2.0F, 0.0F, 1.0F);
		}
		return llegando || t >= CERRADA ? 0.0F : 0.55F;
	}

	/* --------------------------------------------------------------- final */

	private static void quitarDecorado(Minecraft mc) {
		if (mc.level != null && mc.level == nivel) {
			for (Entity e : new Entity[] {superviviente, manos, brazo, amasijo, hojas[0], hojas[1]}) {
				if (e != null) {
					mc.level.removeEntity(e.getId(), Entity.RemovalReason.DISCARDED);
				}
			}
			originales.forEach((p, e) -> mc.level.setBlock(p, e, 18));
		}
		originales.clear();
		puestos.clear();
		superviviente = null;
		manos = null;
		brazo = null;
		amasijo = null;
		java.util.Arrays.fill(hojas, null);
	}

	static void terminar() {
		Minecraft mc = Minecraft.getInstance();
		if (camara != null && mc.player != null) {
			if (llegada >= 0) {
				mc.player.setYRot(camara.getYRot());
				mc.player.setXRot(camara.getXRot());
			}
			mc.setCameraEntity(mc.player);
			((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjos(mc.player.getEyeHeight());
			((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjosAntes(mc.player.getEyeHeight());
		}
		camara = null;
		quitarDecorado(mc);
		ticks = -1;
		llegada = -1.0F;
		llegando = false;
		nivel = null;
		mc.options.hideGui = guiAntes;
		if (banda != null) {
			mc.getSoundManager().stop(banda);
			banda = null;
		}
		if (mc.screen instanceof PantallaSalidaEstacion) {
			mc.setScreen(null);
		}
	}

	/** Al desconectarse no queda nada a medias. */
	public static void olvidar() {
		if (activa()) {
			ticks = -1;
			llegada = -1.0F;
			llegando = false;
			camara = null;
			nivel = null;
			banda = null;
			superviviente = null;
			manos = null;
			brazo = null;
			amasijo = null;
			originales.clear();
			puestos.clear();
			java.util.Arrays.fill(hojas, null);
			Minecraft.getInstance().options.hideGui = guiAntes;
		}
	}
}
