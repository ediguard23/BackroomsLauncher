package net.backrooms.evento.cliente.cinematica;

import com.mojang.math.Transformation;
import java.util.HashMap;
import java.util.Map;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.cliente.esqueleto.CuerpoPrimeraPersona;
import net.backrooms.evento.esqueleto.Figura;
import net.backrooms.evento.esqueleto.Hueso;
import net.backrooms.evento.esqueleto.Pose;
import net.backrooms.evento.fiesta.Anfitrion;
import net.backrooms.evento.fiesta.BloquesFiesta;
import net.backrooms.evento.fiesta.ParedVariante;
import net.backrooms.evento.fiesta.PlanoFiesta;
import net.backrooms.evento.fiesta.Torno;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * La salida de la Fiesta (la empieza SalidaFiesta al meterse en el ascensor del salon final). El
 * mundo de verdad desde una camara que va sola, sin HUD, con franjas de cine (PantallaSalidaFiesta)
 * y sin los demas jugadores. El Anfitrion, la puerta morada que cae, las hojas correderas del
 * ascensor y lo que marca el indicador de piso son solo de este cliente. Guion (segundos):
 *
 *   0    - 4.8   dentro de la cabina, tranquilo: mira la botonera, el indicador (5) y la cabina
 *   4.8          un ruido fuera; 5.0 - 6.8 se da la vuelta y se asoma a la puerta
 *   7.0  - 11.0  se acerca despacio (zoom) a la puerta morada, al fondo del salon; 11 - 12.5 nada
 *   GOLPES       cinco golpes, uno a uno: la puerta se sacude con cada uno
 *   ROMPE        la puerta cede y cae al suelo del pasillo (y ahi se queda)
 *   APARECE      el Anfitrion sale por el hueco y se queda mirando
 *   CORRE - LLEGA corre hacia el ascensor (la camara le sigue y se va metiendo en la cabina); VISTAZOS: se
 *                vuelve a la botonera y la aporrea, desesperado, y vuelve a mirarle, cada vez mas cerca
 *   CIERRA       las puertas se cierran justo antes de que llegue; CHOCA: se estampa contra ellas
 *   31.5 - 35.4  «¡NOOO!» (y se refugia al fondo); GOLPES_ASCENSOR: golpes contra las puertas, cada vez mas flojos
 *   40.4 - 43.6  silencio; AMENAZA (9.5 s) tras las puertas; ARRIBA - 57.4 manipula los cables (arriba)
 *   MOTOR        arranca y baja (el indicador: 4, 3); FALLO: parpadea la luz, chispazo, el diferencial
 *   CABLES       se parten los cables; CAIDA - CHOQUE: cae (el indicador cuenta deprisa y se apaga)
 *   CHOQUE       el golpe: destello y negro (pierde el conocimiento); el servidor le lleva a la Estacion
 *
 * Y en la Estacion, como al despertar en el Nivel 0 (PantallaCinematica): negro, todo amortiguado,
 * abre los ojos a ratos con la vista borrosa, tumbado mirando al techo; se incorpora, se levanta y
 * sale el titulo «NIVEL 3 / LA ESTACION ELECTRICA» (el del servidor no se pone).
 *
 * El sonido va en dos archivos (tools/sonidos/salida-fiesta.js, con los mismos tiempos): el de la
 * Fiesta y el del despertar, que suena ya en la Estacion.
 */
public final class SalidaFiestaCliente {
	private static final Identifier BANDA = BackroomsEvento.id("cinematica.salida_fiesta");
	private static final Identifier BANDA_FIN = BackroomsEvento.id("cinematica.salida_fiesta.despues");
	private static final double OJOS = PlanoFiesta.SUELO + 1 + 1.62;
	/** El centro del ascensor (y del pasillo de la puerta morada: estan en la misma x). */
	private static final double X = PlanoFiesta.FIN_PUERTA_X + 0.5;
	/** Donde mira la cabina al principio, la boca (asomado) y donde se queda cuando se cierra. */
	private static final double Z_CABINA = PlanoFiesta.FIN_Z0 - 2.4;
	private static final double Z_BOCA = PlanoFiesta.FIN_Z0 - 1.05;
	private static final double Z_DENTRO = PlanoFiesta.FIN_Z0 - 2.2;
	/** Pegado al fondo de la cabina (donde se refugia mientras golpean). */
	private static final double Z_FONDO = PlanoFiesta.FIN_Z0 - 3.2;
	private static final int PUERTA_Z = PlanoFiesta.FIN_PUERTA_Z;
	/** Las hojas del ascensor: en la fila de las puertas, un poco hacia el salon. */
	private static final double HOJAS_Z = 0.3;
	/** El zoom sobre la puerta morada, a unos 50 bloques: que llene un buen trozo de la imagen. */
	private static final float ZOOM = 0.1F;

	private static final float RUIDO = 4.8F;
	/** Los cinco golpes en la puerta morada; despues de una pausa, cede. */
	private static final float[] GOLPES = {12.6F, 14.4F, 16.0F, 17.8F, 19.5F};
	private static final float ROMPE = 21.3F;
	private static final float CAE = 0.6F;
	private static final float APARECE = 22.6F;
	private static final float SALE = 23.4F;
	private static final float CORRE = 23.8F;
	private static final float LLEGA = 31.2F;
	private static final float CIERRA = 29.8F;
	private static final float CERRADA = 31.0F;
	private static final float CHOCA = 31.3F;
	/** Los golpes contra las puertas del ascensor y su fuerza (van a menos). */
	private static final float[] GOLPES_ASCENSOR = {35.5F, 36.0F, 36.4F, 36.9F, 37.4F, 38.0F, 38.7F, 39.5F, 40.4F};
	private static final float[] FUERZA = {1.0F, 1.0F, 0.95F, 0.9F, 0.8F, 0.7F, 0.55F, 0.42F, 0.3F};
	/** Lo que hace arriba (cadena, radial, soldador): golpecitos en el techo de la cabina. */
	/** La amenaza tras las puertas (9,5 s) y cuando empieza a manipular el techo. */
	private static final float AMENAZA = 43.6F;
	private static final float ARRIBA = 53.5F;
	private static final float[] TECHO = {53.7F, 54.2F, 54.6F, 57.3F};
	private static final float MOTOR = 57.7F;
	private static final float FALLO = 60.5F;
	private static final float DIFERENCIAL = 61.1F;
	private static final float[] CABLES = {61.5F, 62.0F};
	private static final float CAIDA = 62.1F;
	private static final float CHOQUE = 65.7F;
	/** Mientras viene corriendo: se vuelve a la botonera y la aporrea ({desde, hasta}) y cada pulsacion. */
	private static final float[][] VISTAZOS = {{24.4F, 25.5F}, {27.2F, 28.2F}};
	private static final float[] PULSA = {24.75F, 24.95F, 25.12F, 25.28F, 27.5F, 27.65F, 27.8F, 27.95F};
	/** Lo que marca el indicador: {desde el segundo, variante} (0..5 el piso, 6 un guion, 7 apagado). */
	private static final float[][] PISOS = {{0.0F, 5}, {58.9F, 4}, {60.1F, 3}, {62.7F, 2}, {63.5F, 1}, {64.3F, 0}, {64.9F, 6}, {65.3F, 7}};

	/** El despertar en la Estacion (segundos desde que se ve el mundo nuevo). */
	private static final float NEGRO_LLEGADA = 2.5F;
	private static final float SE_SIENTA = 7.5F;
	private static final float SE_LEVANTA = 9.5F;
	private static final float DE_PIE = 11.2F;
	private static final float TITULO = 11.0F;
	private static final float FIN_LLEGADA = 16.2F;
	/** Abertura de los ojos al despertar: {segundo, abertura} (como OJOS en PantallaCinematica). */
	private static final float[][] OJOS_LLEGADA = {
		{0.0F, 0.0F}, {2.5F, 0.0F}, {3.5F, 0.24F}, {3.9F, 0.16F}, {4.4F, 0.0F}, {5.1F, 0.0F},
		{6.2F, 0.52F}, {6.8F, 0.44F}, {7.1F, 0.12F}, {7.6F, 0.36F}, {9.0F, 0.88F}, {10.4F, 1.0F}};

	private static int ticks = -1;
	/** Segundo en que se empieza a ver la Estacion (-1: aun en la Fiesta). */
	private static float llegada = -1.0F;
	/** Ya ha cambiado de mundo y espera a que acabe la pantalla de carga. */
	private static boolean llegando;
	private static @Nullable Marker camara;
	private static @Nullable Object nivel;
	private static boolean guiAntes;
	private static float giro0;
	private static float cabeceo0;
	private static double px;
	private static double py;
	private static double pz;
	/** Los giros de la primera parte (sin dar la vuelta entera al pasar de -180 a 180). */
	private static float giroPanel;
	private static float giroAtras;
	private static float giroSur;
	/** Donde y como llega a la Estacion, y cuanto queda la cabeza por detras de los pies, tumbado. */
	private static double lx;
	private static double ly;
	private static double lz;
	private static float lgiro;
	private static double tumbado;
	private static @Nullable SoundInstance banda;
	private static @Nullable Anfitrion anfitrion;
	/** El brazo derecho del jugador que aporrea la botonera (solo se ve en los VISTAZOS). */
	private static @Nullable Figura brazo;
	private static final Identifier PIEL = BackroomsEvento.id("textures/entity/superviviente.png");
	private static float pasos;
	private static final Display.BlockDisplay[] puerta = new Display.BlockDisplay[2];
	private static final Display.BlockDisplay[] hojas = new Display.BlockDisplay[2];
	/** Los bloques que cambia la escena (para dejarlos como estaban si no hay viaje). */
	private static final Map<BlockPos, BlockState> originales = new HashMap<>();
	/** Como tienen que estar (el servidor a veces reenvia un bloque y deshace el cambio: se vuelve a poner). */
	private static final Map<BlockPos, BlockState> puestos = new HashMap<>();
	private static int golpesVistos;
	private static boolean polvo;
	private static int piso = -1;
	/** Ids de las entidades de la escena (negativos: no chocan con las del servidor). */
	private static int siguienteId = -7000;

	private SalidaFiestaCliente() {
	}

	public static void registrar() {
		ClientTickEvents.END_CLIENT_TICK.register(SalidaFiestaCliente::tick);
	}

	public static boolean activa() {
		return ticks >= 0;
	}

	/** Si ya esta en la Estacion, despertando. */
	public static boolean despertando() {
		return activa() && llegada >= 0;
	}

	public static float segundos(float parcial) {
		return ticks < 0 ? -1.0F : (ticks + parcial) / 20.0F;
	}

	/** Segundos del despertar (desde que se ve la Estacion) o -1. */
	public static float despierta(float parcial) {
		return despertando() ? segundos(parcial) - llegada : -1.0F;
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
		ticks = 0;
		llegada = -1.0F;
		llegando = false;
		polvo = false;
		golpesVistos = 0;
		piso = -1;
		pasos = 0.0F;
		nivel = mc.level;
		giro0 = Mth.wrapDegrees(mc.player.getYRot());
		cabeceo0 = mc.player.getXRot();
		px = mc.player.getX();
		py = mc.player.getEyeY();
		pz = mc.player.getZ();
		giroPanel = giro0 + Mth.wrapDegrees(-54.0F - giro0);
		giroAtras = giroPanel - 6.0F - 100.0F;
		giroSur = giroAtras + Mth.wrapDegrees(0.0F - giroAtras);
		Marker m = new Marker(EntityType.MARKER, mc.level);
		camara = m;
		colocar(m, 0.0F, true);
		mc.setCameraEntity(m);
		((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjos(0.0F);
		((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjosAntes(0.0F);
		guiAntes = mc.options.hideGui;
		mc.options.hideGui = true;
		montar(mc);
		banda = sonar(mc, BANDA);
		mc.setScreen(new PantallaSalidaFiesta());
	}

	/* --------------------------------------------------------------- el decorado */

	/** Lo de la escena, solo en este cliente: la puerta que se cae, las hojas del ascensor y el Anfitrion. */
	private static void montar(Minecraft mc) {
		originales.clear();
		puestos.clear();
		// la puerta de verdad se quita (solo aqui) y en su sitio va una hoja que puede sacudirse y caerse
		BlockState base = BloquesFiesta.PUERTA.defaultBlockState().setValue(Torno.FACING, Direction.SOUTH).setValue(Torno.COSTE, 0)
			.setValue(Torno.TIPO, 2);
		for (int k = 0; k < 2; k++) {
			poner(mc, new BlockPos(PlanoFiesta.FIN_PUERTA_X, PlanoFiesta.SUELO + 1 + k, PUERTA_Z), Blocks.AIR.defaultBlockState());
			puerta[k] = pieza(mc, base.setValue(Torno.PARTE, k), PlanoFiesta.FIN_PUERTA_X, PlanoFiesta.SUELO + 1, PUERTA_Z);
		}
		// las dos hojas correderas del ascensor, abiertas (metidas en el marco de cada lado)
		for (int k = 0; k < 2; k++) {
			hojas[k] = pieza(mc, BloquesFiesta.HOJA_ASCENSOR.defaultBlockState(), PlanoFiesta.FINAL_TOB_X0, PlanoFiesta.SUELO + 1,
				PlanoFiesta.FIN_Z0 - 1);
		}
		// el Anfitrion, fuera, detras de la puerta (en lo oscuro)
		Anfitrion a = net.backrooms.evento.mision.Entidades.ANFITRION.create(mc.level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
		if (a != null) {
			a.setId(-7100);
			a.noPhysics = true;
			a.snapTo(X, PlanoFiesta.SUELO + 1, zAnfitrion(0.0F), 180.0F, 0.0F);
			a.setYHeadRot(180.0F);
			mc.level.addEntity(a);
			anfitrion = a;
		}
		decorado(mc, 0.0F);
	}

	/** Cambia un bloque solo en este cliente, apuntando como estaba. */
	private static void poner(Minecraft mc, BlockPos p, BlockState estado) {
		BlockState antes = mc.level.getBlockState(p);
		originales.putIfAbsent(p.immutable(), antes);
		puestos.put(p.immutable(), estado);
		if (antes != estado) {
			mc.level.setBlock(p, estado, 18);
		}
	}

	private static Display.BlockDisplay pieza(Minecraft mc, BlockState estado, int x, int y, int z) {
		Display.BlockDisplay d = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, mc.level);
		d.setId(siguienteId--);
		d.setBlockState(estado);
		d.setPos(x, y, z);
		d.setViewRange(4.0F);
		d.setTransformationInterpolationDuration(2);
		mc.level.addEntity(d);
		return d;
	}

	/** Un pulso que sube de golpe y se apaga en `dura` segundos (0 fuera de el). */
	private static float pulso(float t, float en, float dura) {
		float d = t - en;
		return d < 0 || d >= dura ? 0.0F : (1.0F - d / dura) * (1.0F - d / dura);
	}

	/** Lo fuerte que golpean ahora las puertas del ascensor (el choque y los golpes). */
	private static float golpeAscensor(float t) {
		float g = pulso(t, CHOCA, 0.45F) * 1.2F;
		for (int i = 0; i < GOLPES_ASCENSOR.length; i++) {
			g = Math.max(g, pulso(t, GOLPES_ASCENSOR[i], 0.35F) * FUERZA[i]);
		}
		return g;
	}

	/** La puerta, las hojas del ascensor, el indicador y el Anfitrion en el segundo t. */
	private static void decorado(Minecraft mc, float t) {
		// la puerta morada: delgada; con cada golpe se va hacia el salon y vuelve; al ceder cae al suelo y se queda
		float angulo = 0.0F;
		float empuje = 0.0F;
		for (int i = 0; i < GOLPES.length; i++) {
			float d = t - GOLPES[i];
			if (d >= 0 && d < 1.2F) {
				float a = (0.55F + i * 0.18F) * (float) (Math.exp(-d * 7.0) * Math.cos(d * 32.0));
				angulo -= a * 6.0F;
				empuje += a * 0.03F;
			}
		}
		if (t >= ROMPE) {
			// primero se vence un poco (los goznes), luego cae acelerando y rebota un pelo al llegar
			float k = Mth.clamp((t - ROMPE) / CAE, 0.0F, 1.0F);
			angulo = -90.0F * k * k;
			if (k >= 1.0F) {
				float d = t - ROMPE - CAE;
				angulo += (float) (Math.sin(d * 30.0) * Math.exp(-d * 12.0) * 3.0);
			}
			empuje = 0.0F;
		}
		Quaternionf giro = new Quaternionf().rotationX((float) Math.toRadians(angulo));
		Vector3f pivote = new Vector3f(0.0F, 0.0F, 0.375F);
		for (int k = 0; k < 2; k++) {
			if (puerta[k] == null) {
				continue;
			}
			Vector3f t0 = giro.transform(new Vector3f(0.0F, k, 0.0F)).add(pivote).add(0.0F, 0.0F, -empuje);
			puerta[k].setTransformation(new Transformation(t0, new Quaternionf(giro), new Vector3f(1.0F, 1.0F, 0.25F), new Quaternionf()));
		}
		// las hojas del ascensor: abiertas hasta CIERRA; se cierran frenando al final; tiemblan con los golpes y la caida
		float c = Mth.clamp((t - CIERRA) / (CERRADA - CIERRA), 0.0F, 1.0F);
		float cierre = 1.0F - (1.0F - c) * (1.0F - c) * (1.0F - c);
		float golpe = golpeAscensor(t);
		float traqueteo = t >= CAIDA && t < CHOQUE ? 0.012F * (float) Math.sin(t * 90.0) : 0.0F;
		for (int k = 0; k < 2; k++) {
			if (hojas[k] == null) {
				continue;
			}
			// abierta: metida en el marco de su lado; cerrada: tocando a la otra en el medio
			float x = k == 0 ? Mth.lerp(cierre, -1.5F, 0.0F) : Mth.lerp(cierre, 3.0F, 1.5F);
			float bollo = golpe * 0.07F * (0.7F + 0.3F * (float) Math.sin(t * 70.0 + k * 2.0));
			Quaternionf ladeo = new Quaternionf().rotationY((float) Math.toRadians(golpe * 1.5F * (k == 0 ? 1 : -1)));
			hojas[k].setTransformation(new Transformation(new Vector3f(x, 0.0F, (float) HOJAS_Z - bollo + traqueteo), ladeo,
				new Vector3f(1.5F, 1.5F, 1.0F), new Quaternionf()));
		}
		// el indicador de piso, dentro y fuera (y se va con la luz cuando falla)
		int v = 5;
		for (float[] p : PISOS) {
			if (t >= p[0]) {
				v = (int) p[1];
			}
		}
		if (oscuro(t) > 0.7F) {
			v = 7;
		}
		if (v != piso) {
			piso = v;
			indicador(mc, new BlockPos(PlanoFiesta.FINAL_TOB_X1, PlanoFiesta.SUELO + 3, PlanoFiesta.FIN_Z0 - 2), v);
			indicador(mc, new BlockPos(PlanoFiesta.FINAL_TOB_X0, PlanoFiesta.SUELO + 3, PlanoFiesta.FIN_Z0 - 2), v);
			indicador(mc, new BlockPos(PlanoFiesta.FINAL_TOB_X0 + 1, PlanoFiesta.SUELO + 5, PlanoFiesta.FIN_Z0), v);
		}
		puestos.forEach((p, e) -> {
			if (mc.level.getBlockState(p) != e) {
				mc.level.setBlock(p, e, 18);
			}
		});
		// el Anfitrion
		Anfitrion a = anfitrion;
		if (a != null) {
			a.xo = a.getX();
			a.yo = a.getY();
			a.zo = a.getZ();
			a.setPos(X, PlanoFiesta.SUELO + 1, zAnfitrion(t));
			a.setYRot(180.0F);
			a.yRotO = 180.0F;
			a.setYHeadRot(180.0F);
			a.yHeadRotO = 180.0F;
			a.setYBodyRot(180.0F);
			a.yBodyRotO = 180.0F;
			// al salir se queda mirando, ladeando la cabeza
			float cabeza = t > SALE && t < CORRE + 0.2F ? -6.0F + (float) Math.sin((t - SALE) * 5.0) * 4.0F : 0.0F;
			a.setXRot(cabeza);
			a.xRotO = cabeza;
			a.cazandoCliente(t >= CORRE && t < CHOCA + 0.5F);
			andar(a, zancada(t));
		}
		// el brazo que aporrea la botonera: sube, cada pulsacion estira el codo de golpe, y baja al volverse
		boolean pulsando = t >= VISTAZOS[0][0] - 0.1F && t < VISTAZOS[0][1] + 0.3F || t >= VISTAZOS[1][0] - 0.1F && t < VISTAZOS[1][1] + 0.3F;
		if (pulsando && brazo == null) {
			brazo = CuerpoPrimeraPersona.crear(mc, PIEL, siguienteId--, Hueso.BRAZO_D, Hueso.ANTEBRAZO_D, Hueso.MANO_D);
			if (brazo != null) {
				brazo.animador = (fig, p) -> poseBrazo(t0(p));
			}
		} else if (!pulsando && brazo != null) {
			CuerpoPrimeraPersona.quitar(mc, brazo);
			brazo = null;
		}
		if (brazo != null) {
			double[] ojos = pose(t);
			CuerpoPrimeraPersona.colocar(brazo, ojos[0], ojos[1], ojos[2], (float) ojos[3], 0.0);
		}
	}

	/** El segundo de la escena en este fotograma (con lo que va del tick). */
	private static float t0(float parcial) {
		return (ticks + parcial) / 20.0F;
	}

	/** El brazo en cada vistazo a la botonera (las pulsaciones de ese vistazo). */
	private static Pose poseBrazo(float t) {
		int w = t < (VISTAZOS[0][1] + VISTAZOS[1][0]) / 2.0F ? 0 : 1;
		return CuerpoPrimeraPersona.pulsar(t, VISTAZOS[w][0] + 0.02F, VISTAZOS[w][1] - 0.15F, PULSA, 20.0F);
	}

	private static void indicador(Minecraft mc, BlockPos p, int v) {
		BlockState e = mc.level.getBlockState(p);
		if (e.is(BloquesFiesta.INDICADOR_ASCENSOR) || originales.containsKey(p)) {
			BlockState base = originales.getOrDefault(p, e);
			if (base.is(BloquesFiesta.INDICADOR_ASCENSOR)) {
				poner(mc, p, base.setValue(ParedVariante.VARIANTE, v));
			}
		}
	}

	/**
	 * La animacion de andar a ese ritmo (1.2 corriendo). La entidad, en su tick de cliente, tambien
	 * la mueve (con la velocidad a 0: no se mueve sola), asi que aqui se pone lo que falta para que
	 * avance justo `ritmo` por tick y las piernas vayan seguidas, sin tirones.
	 */
	private static void andar(Anfitrion a, float ritmo) {
		float hecho = a.walkAnimation.position() - pasos;
		if (ritmo > 0.0F) {
			a.walkAnimation.update(Math.max(0.0F, ritmo - hecho), 1.0F, 1.0F);
			a.walkAnimation.setSpeed(ritmo);
		}
		pasos = a.walkAnimation.position();
	}

	private static float zancada(float t) {
		if (t >= APARECE && t < SALE) {
			return 0.55F;
		}
		if (t >= CORRE && t < LLEGA) {
			return 1.2F * Mth.clamp((t - CORRE) / 0.5F, 0.25F, 1.0F);
		}
		return 0.0F;
	}

	/** Donde va el Anfitrion (z): escondido, sale por el hueco, mira, y corre arrancando en medio segundo. */
	private static double zAnfitrion(float t) {
		double escondido = PUERTA_Z + 2.5;
		double fuera = PUERTA_Z - 1.0;
		double llega = PlanoFiesta.FIN_Z0 + 0.5;
		if (t < APARECE) {
			return escondido;
		}
		if (t < SALE) {
			return Mth.lerp(suave((t - APARECE) / (SALE - APARECE)), escondido, fuera);
		}
		if (t < CORRE) {
			return fuera;
		}
		double total = fuera - llega;
		double v = total / (LLEGA - CORRE - 0.25);
		double tau = t - CORRE;
		double s = tau < 0.5 ? v * tau * tau : v * (tau - 0.25);
		return fuera - Math.min(s, total);
	}

	/* ----------------------------------------------------------------- la camara */

	private static SoundInstance sonar(Minecraft mc, Identifier id) {
		SoundInstance s = new SimpleSoundInstance(id, SoundSource.MASTER, 1.0F, 1.0F, RandomSource.create(), false, 0,
			SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
		mc.getSoundManager().play(s);
		return s;
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
			// ya en la Estacion: lo de la escena era del mundo de antes; se espera a que acabe la pantalla de carga
			if (!llegando) {
				llegando = true;
				camara = null;
				anfitrion = null;
				brazo = null;
				java.util.Arrays.fill(puerta, null);
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
			while (golpesVistos < GOLPES.length && t >= GOLPES[golpesVistos]) {
				polvillo(mc, golpesVistos);
				golpesVistos++;
			}
			if (t >= ROMPE + CAE && !polvo) {
				polvo = true;
				nubeDePolvo(mc);
			}
			if (t > net.backrooms.evento.fiesta.SalidaFiesta.DURACION / 20.0F + 8.0F) {
				// el servidor no le ha llevado a ningun sitio (no tiene la Estacion): se acaba aqui
				terminar();
			}
		} else {
			if (camara != null) {
				colocar(camara, t, false);
				// al acabar de cargar el mundo nuevo Minecraft vuelve a poner la camara en el jugador
				if (mc.getCameraEntity() != camara) {
					tomarCamara(mc, camara);
				}
			}
			if (t - llegada >= FIN_LLEGADA) {
				terminar();
			}
		}
	}

	/** Empieza el despertar en la Estacion: la camara, tumbada donde ha caido, y el sonido de despues. */
	private static void llegar(Minecraft mc, float t) {
		llegada = t;
		lx = mc.player.getX();
		ly = mc.player.getY();
		lz = mc.player.getZ();
		lgiro = mc.player.getYRot();
		// tumbado boca arriba con los pies donde esta: la cabeza queda detras (si no hay pared)
		double fx = -Math.sin(Math.toRadians(lgiro));
		double fz = Math.cos(Math.toRadians(lgiro));
		tumbado = mc.level.getBlockState(BlockPos.containing(lx - fx * 0.9, ly + 0.3, lz - fz * 0.9)).isAir() ? 0.9 : 0.0;
		Marker m = new Marker(EntityType.MARKER, mc.level);
		camara = m;
		colocar(m, t, true);
		tomarCamara(mc, m);
		mc.options.hideGui = true;
		banda = sonar(mc, BANDA_FIN);
		if (!(mc.screen instanceof PantallaSalidaFiesta)) {
			mc.setScreen(new PantallaSalidaFiesta());
		}
	}

	private static void tomarCamara(Minecraft mc, Marker m) {
		mc.setCameraEntity(m);
		((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjos(0.0F);
		((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjosAntes(0.0F);
	}

	/** Un poco de polvo del marco con cada golpe en la puerta morada. */
	private static void polvillo(Minecraft mc, int golpe) {
		RandomSource r = RandomSource.create();
		for (int i = 0; i < 5 + golpe * 3; i++) {
			double x = X - 0.6 + r.nextDouble() * 1.2;
			double y = PlanoFiesta.SUELO + 1.2 + r.nextDouble() * 1.8;
			mc.level.addParticle(ParticleTypes.POOF, x, y, PUERTA_Z - 0.1, (r.nextDouble() - 0.5) * 0.03, -0.01, -0.02 - r.nextDouble() * 0.03);
		}
	}

	private static void nubeDePolvo(Minecraft mc) {
		RandomSource r = RandomSource.create();
		BlockParticleOption trozos = new BlockParticleOption(ParticleTypes.BLOCK, BloquesFiesta.PUERTA.defaultBlockState());
		for (int i = 0; i < 40; i++) {
			double x = X - 1.2 + r.nextDouble() * 2.4;
			double z = PUERTA_Z - 2.0 + r.nextDouble() * 2.5;
			double y = PlanoFiesta.SUELO + 1.05 + r.nextDouble() * 0.6;
			mc.level.addParticle(i % 3 == 0 ? trozos : ParticleTypes.POOF, x, y, z, (r.nextDouble() - 0.5) * 0.08, 0.03 + r.nextDouble() * 0.05,
				(r.nextDouble() - 0.5) * 0.08);
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

	/** {x, y, z, giro, cabeceo} en el segundo t. Giro 0 = mirando al sur (al salon, a la puerta morada). */
	private static double[] pose(float t) {
		if (llegada >= 0) {
			return poseLlegada(t - llegada);
		}
		double x = X;
		double y = OJOS;
		double z;
		double giro;
		double cabeceo;
		double sacude = 0.0;
		if (t < 1.8F) {
			// entra hasta el fondo de la cabina y se vuelve hacia la botonera
			double k = suave(t / 1.8F);
			x = Mth.lerp(k, px, X + 0.2);
			y = Mth.lerp(k, py, OJOS);
			z = Mth.lerp(k, pz, Z_CABINA);
			giro = Mth.lerp(k, giro0, giroPanel);
			cabeceo = Mth.lerp(k, cabeceo0, 6.0);
		} else if (t < 3.4F) {
			// los botones y, arriba, el indicador con su 5
			double k = suave((t - 2.0F) / 1.2F);
			x = X + 0.2;
			z = Z_CABINA;
			giro = giroPanel - 6.0 * k;
			cabeceo = Mth.lerp(k, 6.0, -27.0);
		} else if (t < 5.0F) {
			// echa un vistazo a la cabina (el pasamanos, el plafon); en RUIDO, un respingo
			double k = suave((t - 3.4F) / 1.4F);
			x = X + 0.2;
			z = Z_CABINA;
			giro = Mth.lerp(k, giroPanel - 6.0, giroAtras);
			cabeceo = Mth.lerp(k, -27.0, 12.0);
			sacude = pulso(t, RUIDO, 0.3F) * 1.2;
		} else if (t < 6.8F) {
			// se da la vuelta hacia el ruido y se asoma a la puerta
			double k = suave((t - 5.0F) / 1.1F);
			double k2 = suave((t - 5.4F) / 1.4F);
			giro = Mth.lerp(k, giroAtras, giroSur);
			cabeceo = Mth.lerp(k, 12.0, mirarAnfitrion(Z_BOCA, CORRE));
			x = Mth.lerp(k2, X + 0.2, X);
			z = Mth.lerp(k2, Z_CABINA, Z_BOCA);
		} else if (t < CORRE) {
			// asomado mirando la puerta morada (con el zoom); cada golpe le encoge; al caer la puerta, da un paso atras
			z = zAsomado(t);
			double quieto = 1.0 - suave((t - CORRE + 1.0F) / 1.0F);
			giro = giroSur + Math.sin((t - 6.8F) * 0.35) * 0.12 * quieto;
			cabeceo = mirarAnfitrion(Z_BOCA, CORRE) + Math.sin((t - 6.8F) * 0.5) * 0.1 * quieto;
			for (float g : GOLPES) {
				sacude = Math.max(sacude, pulso(t, g, 0.4F) * 0.5);
			}
			sacude = Math.max(sacude, pulso(t, ROMPE + CAE, 0.5F) * 0.9);
			// al verle, le tiembla todo
			sacude = Math.max(sacude, 0.3 * suave((t - APARECE) / 0.6F));
		} else if (t < CHOCA) {
			// le sigue mientras viene corriendo y se va metiendo en la cabina; dos veces se vuelve a la botonera y la
			// aporrea (las puertas no hacen caso) y vuelve a mirarle, cada vez mas cerca
			double k = suave((t - 27.2F) / 2.4F);
			z = Mth.lerp(k, zAsomado(t), Z_DENTRO);
			double v = vistazo(t);
			// se pega a la botonera (a 0,7 de ella) para alcanzarla con la mano
			double giroBoton = -Math.toDegrees(Math.atan2(0.7, PlanoFiesta.FIN_Z0 - 1.5 - z));
			giro = Mth.lerp(v, giroSur, giroSur + giroBoton);
			cabeceo = Mth.lerp(v, mirarAnfitrion(z, t), 12.0);
			x = X + 0.75 * v;
			sacude = 0.35 + 0.9 * suave((t - 26.0F) / 5.0F);
			for (float p : PULSA) {
				cabeceo += 3.0 * pulso(t, p, 0.14F);
				sacude = Math.max(sacude, pulso(t, p, 0.14F) * 0.9);
			}
		} else if (t < ARRIBA) {
			// el choque le echa al fondo de la cabina y ahi se queda, temblando: los golpes, el silencio; con la
			// amenaza se acerca un poco a escuchar
			z = Mth.lerp(suave((t - CHOCA) / 0.9F), Z_DENTRO, Z_FONDO)
				+ 0.15 * suave((t - AMENAZA + 0.5F) / 1.5F) * (1.0 - suave((t - AMENAZA - 9.0F) / 1.0F));
			giro = giroSur + Math.sin(t * 0.4) * 0.6;
			cabeceo = Mth.lerp(suave((t - CHOCA) / 0.8F), mirarAnfitrion(Z_DENTRO, CHOCA), 2.0) + Math.sin(t * 0.9) * 0.4;
			y += Math.sin(t * 1.7) * 0.008;
			sacude = 0.15 + golpeAscensor(t) * 2.4;
		} else if (t < MOTOR) {
			// algo anda en el techo: mira arriba y sigue los ruidos
			double k = suave((t - ARRIBA) / 1.4F);
			z = Z_FONDO;
			giro = giroSur + 14.0 * Math.sin((t - ARRIBA) * 0.8) * k;
			cabeceo = Mth.lerp(k, 2.0, -38.0);
			sacude = 0.15;
			for (float g : TECHO) {
				sacude = Math.max(sacude, pulso(t, g, 0.3F) * 0.6);
			}
		} else if (t < CAIDA) {
			// arranca (un tiron), mira el indicador; falla la luz, el chispazo, el diferencial y los cables
			double k = suave((t - MOTOR - 0.3F) / 1.1F);
			double k2 = suave((t - DIFERENCIAL) / 0.4F);
			z = Z_FONDO;
			giro = Mth.lerp(k2, Mth.lerp(k, giroSur + 14.0 * Math.sin((MOTOR - ARRIBA) * 0.8), giroSur + GIRO_INDICADOR), giroSur - 20.0);
			cabeceo = Mth.lerp(k2, Mth.lerp(k, -38.0, CABECEO_INDICADOR), -40.0);
			y -= 0.03 * suave((t - MOTOR) / 0.2F);
			sacude = 0.25 + pulso(t, MOTOR, 0.4F) * 1.5 + pulso(t, FALLO, 0.5F) * 1.6 + pulso(t, DIFERENCIAL, 0.3F) * 0.8;
			for (float c : CABLES) {
				y -= 0.1 * suave((t - c) / 0.1F);
				sacude = Math.max(sacude, pulso(t, c, 0.5F) * 4.0);
			}
		} else if (t < CHOQUE) {
			// cae: flota un poco, todo tiembla cada vez mas, mira las puertas y el indicador que baja
			double k = (t - CAIDA) / (CHOQUE - CAIDA);
			z = Z_FONDO;
			y += -0.23 + 0.12 * suave((t - CAIDA) / 0.6F);
			giro = Mth.lerp(suave((t - CAIDA - 0.6F) / 0.8F), giroSur - 20.0, giroSur + GIRO_INDICADOR) + Math.sin(t * 3.1) * 4.0 * k;
			cabeceo = Mth.lerp(suave((t - CAIDA - 0.2F) / 1.0F), -40.0, CABECEO_INDICADOR + 8.0) + Math.sin(t * 2.3) * 6.0 * k;
			sacude = 1.6 + 3.0 * k;
		} else {
			// el golpe: se derrumba
			z = Z_FONDO;
			y = OJOS - 1.3 * suave((t - CHOQUE) / 0.12F);
			giro = giroSur + GIRO_INDICADOR;
			cabeceo = Mth.lerp(suave((t - CHOQUE) / 0.12F), CABECEO_INDICADOR + 8.0, 35.0);
			sacude = 6.0 * Math.exp(-(t - CHOQUE) * 4.0);
		}
		return sacudir(x, y, z, giro, cabeceo, sacude, t);
	}

	/** Desde el fondo de la cabina, hacia el indicador de la derecha (encima de la botonera). */
	private static final double GIRO_INDICADOR = -40.5;
	private static final double CABECEO_INDICADOR = -21.5;

	/** Asomado a la puerta; cuando cae la puerta morada da un paso atras. */
	private static double zAsomado(float t) {
		return Z_BOCA - 0.2 * suave((t - ROMPE) / 0.5F);
	}

	/** 0..1: cuanto esta vuelto hacia la botonera (se gira deprisa, la aporrea y vuelve). */
	private static float vistazo(float t) {
		float v = 0.0F;
		for (float[] w : VISTAZOS) {
			v = Math.max(v, suave((t - w[0]) / 0.22F) * (1.0F - suave((t - w[1] + 0.25F) / 0.25F)));
		}
		return v;
	}

	/** El cabeceo para mirarle (al pecho; de lejos, a la puerta) desde z. */
	private static double mirarAnfitrion(double z, float t) {
		double d = Math.max(1.0, zAnfitrion(t) - z);
		return Math.toDegrees(Math.atan2(OJOS - (PlanoFiesta.SUELO + 1 + 1.3), d));
	}

	/** El despertar en la Estacion: tumbado mirando al techo, se incorpora y se levanta. */
	private static double[] poseLlegada(float a) {
		double fx = -Math.sin(Math.toRadians(lgiro));
		double fz = Math.cos(Math.toRadians(lgiro));
		double atras;
		double y;
		double cabeceo;
		double giro = lgiro;
		double sacude = 0.0;
		if (a < SE_SIENTA) {
			// tumbado: respira, gira un poco la cabeza a un lado y vuelve
			atras = tumbado;
			y = ly + 0.22 + Math.sin(a * 1.4) * 0.015;
			cabeceo = -86.0 + Math.sin(a * 0.5) * 2.0;
			giro += 22.0 * suave((a - 5.2F) / 1.0F) * (1.0 - suave((a - 6.6F) / 1.0F));
		} else if (a < SE_LEVANTA) {
			// se incorpora (sentado, mirandose las piernas)
			double k = suave((a - SE_SIENTA) / (SE_LEVANTA - SE_SIENTA));
			atras = Mth.lerp(k, tumbado, tumbado * 0.3);
			y = Mth.lerp(k, ly + 0.22, ly + 0.95);
			cabeceo = Mth.lerp(k, -86.0, 20.0);
			sacude = 0.3;
		} else if (a < DE_PIE) {
			// se levanta, aun torpe
			double k = suave((a - SE_LEVANTA) / (DE_PIE - SE_LEVANTA));
			atras = Mth.lerp(k, tumbado * 0.3, 0.0);
			y = Mth.lerp(k, ly + 0.95, ly + 1.62);
			cabeceo = Mth.lerp(k, 20.0, 3.0);
			sacude = 0.5;
		} else {
			// de pie, mareado: se balancea cada vez menos y acaba mirando al frente
			double k = 1.0 - suave((a - DE_PIE) / (FIN_LLEGADA - DE_PIE));
			atras = 0.0;
			y = ly + 1.62 + Math.sin(a * 1.3) * 0.02 * k;
			cabeceo = 3.0 * k + Math.sin(a * 1.1) * 1.5 * k;
			giro += Math.sin(a * 0.9) * 3.0 * k;
		}
		return sacudir(lx - fx * atras, y, lz - fz * atras, giro, cabeceo, sacude, a);
	}

	private static double[] sacudir(double x, double y, double z, double giro, double cabeceo, double sacude, float t) {
		double r1 = Math.sin(t * 37.0) * 0.6 + Math.sin(t * 59.0) * 0.4;
		double r2 = Math.sin(t * 43.0 + 1.3) * 0.6 + Math.sin(t * 67.0) * 0.4;
		return new double[] {x + r1 * 0.012 * sacude, y + r2 * 0.012 * sacude, z, giro + r2 * 0.5 * sacude, cabeceo + r1 * 0.5 * sacude};
	}

	private static float suave(float k) {
		k = Mth.clamp(k, 0.0F, 1.0F);
		return k * k * (3.0F - 2.0F * k);
	}

	private static double suave(double k) {
		return suave((float) k);
	}

	/** Un azar fijo para cada trocito de tiempo (el parpadeo de la luz sale igual en cada fotograma). */
	private static float azar(float t, float por) {
		double s = Math.sin(Math.floor(t * por) * 12.9898) * 43758.5453;
		return (float) (s - Math.floor(s));
	}

	/** 0..1 de oscuridad por la luz que falla: parpadea, baja con el diferencial y va y viene al caer. */
	private static float oscuro(float t) {
		if (t < FALLO || t >= CHOQUE) {
			return 0.0F;
		}
		if (t < DIFERENCIAL) {
			return azar(t, 14.0F) < 0.45F ? 0.85F : 0.0F;
		}
		if (t < CAIDA) {
			return azar(t, 9.0F) < 0.25F ? 0.9F : 0.5F;
		}
		return azar(t, 12.0F) < 0.35F ? 0.92F : 0.55F;
	}

	/* ------------------------------------------------------------------ pantalla */

	/** 0..1 de negro encima de todo (la luz que falla, tras el golpe, el viaje y antes de abrir los ojos). */
	public static float negro(float parcial) {
		if (!activa()) {
			return 0.0F;
		}
		float t = segundos(parcial);
		if (llegada >= 0) {
			return t - llegada < NEGRO_LLEGADA ? 1.0F : 0.0F;
		}
		if (llegando || t >= CHOQUE + 0.06F) {
			return 1.0F;
		}
		return oscuro(t);
	}

	/**
	 * Cuanto se cierra el angulo (1 = normal): como un teleobjetivo hacia la puerta morada, al fondo
	 * del salon; mientras viene corriendo se abre lo justo para que se le vea siempre entero.
	 */
	public static float zoom(float parcial) {
		if (!activa() || llegada >= 0) {
			return 1.0F;
		}
		float t = segundos(parcial);
		float z = Mth.lerp(suave((t - 7.0F) / 4.0F), 1.0F, ZOOM);
		if (t >= CORRE) {
			double d0 = zAnfitrion(CORRE) - Z_BOCA;
			double d = Math.max(1.0, zAnfitrion(t) - pose(t)[2]);
			float sigue = (float) Math.min(1.0, ZOOM * Math.pow(d0 / d, 0.9));
			z = Mth.lerp(Math.max(suave((t - 30.4F) / 0.8F), vistazo(t)), sigue, 1.0F);
		}
		return z;
	}

	/** 0..1 del destello del golpe (se apaga sobre el negro). */
	public static float destello(float parcial) {
		if (!activa() || llegada >= 0) {
			return 0.0F;
		}
		float t = segundos(parcial);
		float d = (t - CHOQUE) / 0.35F;
		return d < 0 || d >= 1 ? 0.0F : (1.0F - d) * (1.0F - d);
	}

	/** 0..1 de las franjas de cine (al despertar no hay). */
	public static float franjas(float parcial) {
		if (!activa() || llegada >= 0) {
			return 0.0F;
		}
		return Mth.clamp(segundos(parcial) / 0.6F, 0.0F, 1.0F);
	}

	/** Lo abiertos que tiene los ojos al despertar (0..1; 1 tambien fuera del despertar). */
	public static float abertura(float parcial) {
		float a = despierta(parcial);
		if (a < 0) {
			return 1.0F;
		}
		for (int i = 1; i < OJOS_LLEGADA.length; i++) {
			if (a < OJOS_LLEGADA[i][0]) {
				float k = (a - OJOS_LLEGADA[i - 1][0]) / (OJOS_LLEGADA[i][0] - OJOS_LLEGADA[i - 1][0]);
				k = k * k * (3 - 2 * k);
				return Mth.lerp(k, OJOS_LLEGADA[i - 1][1], OJOS_LLEGADA[i][1]);
			}
		}
		return 1.0F;
	}

	/** Radio del desenfoque al despertar (de muy borroso a nitido), o -1 si no toca. */
	public static int desenfoque() {
		float a = despierta(0.0F);
		if (a < NEGRO_LLEGADA) {
			return -1;
		}
		float k = Mth.clamp((a - NEGRO_LLEGADA) / 10.5F, 0.0F, 1.0F);
		int r = Math.round(Mth.lerp(k * (2.0F - k), 22.0F, 0.0F));
		return r > 0 ? r : -1;
	}

	/** Cuanto se oye el resto del mundo: bajo en la Fiesta; al despertar, nada y vuelve poco a poco. */
	public static float volumenMundo() {
		if (!activa()) {
			return 1.0F;
		}
		if (llegada < 0) {
			return llegando ? 0.0F : 0.3F;
		}
		return Mth.clamp((despierta(0.0F) - NEGRO_LLEGADA - 1.0F) / 9.0F, 0.0F, 1.0F);
	}

	/** 0..1 del titulo «NIVEL 3»: entra parpadeando como un fluorescente y se va despacio. */
	public static float titulo(float parcial) {
		float a = despierta(parcial);
		if (a < TITULO) {
			return 0.0F;
		}
		float sale = Mth.clamp((FIN_LLEGADA - 0.6F - a) / 1.2F, 0.0F, 1.0F);
		if (a < TITULO + 0.9F) {
			return azar(a, 16.0F) < 0.5F ? 0.15F * sale : sale;
		}
		return sale;
	}

	private static void soltarCamara(Minecraft mc) {
		if (mc.player != null) {
			mc.setCameraEntity(mc.player);
			((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjos(mc.player.getEyeHeight());
			((net.backrooms.evento.mixin.cliente.CamaraAccessor) mc.gameRenderer.getMainCamera()).backrooms$alturaOjosAntes(mc.player.getEyeHeight());
		}
		camara = null;
	}

	private static void quitarDecorado(Minecraft mc) {
		if (mc.level != null && mc.level == nivel) {
			for (Entity e : new Entity[] {anfitrion, brazo, puerta[0], puerta[1], hojas[0], hojas[1]}) {
				if (e != null) {
					mc.level.removeEntity(e.getId(), Entity.RemovalReason.DISCARDED);
				}
			}
			// los bloques, como estaban (si no ha habido viaje)
			originales.forEach((p, e) -> mc.level.setBlock(p, e, 18));
		}
		originales.clear();
		puestos.clear();
		anfitrion = null;
		brazo = null;
		java.util.Arrays.fill(puerta, null);
		java.util.Arrays.fill(hojas, null);
	}

	static void terminar() {
		Minecraft mc = Minecraft.getInstance();
		if (camara != null) {
			// el jugador se queda mirando hacia donde miraba la camara (sin saltos)
			if (mc.player != null && llegada >= 0) {
				mc.player.setYRot(camara.getYRot());
				mc.player.setXRot(camara.getXRot());
			}
			soltarCamara(mc);
		}
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
		if (mc.screen instanceof PantallaSalidaFiesta) {
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
			anfitrion = null;
			brazo = null;
			originales.clear();
			puestos.clear();
			java.util.Arrays.fill(puerta, null);
			java.util.Arrays.fill(hojas, null);
			Minecraft.getInstance().options.hideGui = guiAntes;
		}
	}
}
