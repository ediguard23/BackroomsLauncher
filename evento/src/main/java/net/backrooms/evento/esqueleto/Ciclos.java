package net.backrooms.evento.esqueleto;

import static net.backrooms.evento.esqueleto.Hueso.ANTEBRAZO_D;
import static net.backrooms.evento.esqueleto.Hueso.ANTEBRAZO_I;
import static net.backrooms.evento.esqueleto.Hueso.BRAZO_D;
import static net.backrooms.evento.esqueleto.Hueso.BRAZO_I;
import static net.backrooms.evento.esqueleto.Hueso.CABEZA;
import static net.backrooms.evento.esqueleto.Hueso.CADERA;
import static net.backrooms.evento.esqueleto.Hueso.MANO_D;
import static net.backrooms.evento.esqueleto.Hueso.MANO_I;
import static net.backrooms.evento.esqueleto.Hueso.MUSLO_D;
import static net.backrooms.evento.esqueleto.Hueso.MUSLO_I;
import static net.backrooms.evento.esqueleto.Hueso.PIERNA_D;
import static net.backrooms.evento.esqueleto.Hueso.PIERNA_I;
import static net.backrooms.evento.esqueleto.Hueso.PIE_D;
import static net.backrooms.evento.esqueleto.Hueso.PIE_I;
import static net.backrooms.evento.esqueleto.Hueso.TORSO;

/**
 * Las poses y ciclos de base del esqueleto (grados). Los ciclos van por fase: 1 = un ciclo entero (dos
 * pasos); quien los usa decide cuantos ciclos por segundo, segun lo deprisa que se mueva la figura, para
 * que los pies no patinen (correr: ~1,4 ciclos/s a 4-7 bloques/s; andar: ~0,9 a 1,4 bloques/s).
 */
public final class Ciclos {
	private static final float TAU = (float) (Math.PI * 2.0);

	private Ciclos() {
	}

	private static float sin(float a) {
		return (float) Math.sin(a);
	}

	private static float cos(float a) {
		return (float) Math.cos(a);
	}

	/** De pie, respirando (t en segundos). */
	public static Pose quieto(float t) {
		float r = sin(t * 1.6F);
		return new Pose()
			.gira(TORSO, 1.5F * r, 0, 0)
			.gira(CABEZA, -1.0F * r, 0, 0)
			.gira(BRAZO_D, 0, 0, 3.0F + r)
			.gira(BRAZO_I, 0, 0, -3.0F - r)
			.gira(ANTEBRAZO_D, -6.0F, 0, 0)
			.gira(ANTEBRAZO_I, -6.0F, 0, 0)
			.cuerpo(0, 0, 0, 0, 0.004F * r, 0);
	}

	/**
	 * Correr: las piernas se alternan, la rodilla se dobla al recoger la pierna, los brazos (doblados por
	 * el codo) van al reves que las piernas, el torso inclinado hacia delante y girando con los hombros, la
	 * cadera al contrario, la cabeza mirando al frente y el cuerpo botando en cada zancada. `prisa` 0..1
	 * (1: corriendo con todo, desesperado).
	 */
	public static Pose correr(float fase, float prisa) {
		float a = fase * TAU;
		float s = sin(a);
		float rodD = 15.0F + 95.0F * (float) Math.pow(Math.max(0.0F, cos(a + 0.45F)), 1.3);
		float rodI = 15.0F + 95.0F * (float) Math.pow(Math.max(0.0F, cos(a + (float) Math.PI + 0.45F)), 1.3);
		float muslo = 48.0F + 12.0F * prisa;
		float brazo = 42.0F + 18.0F * prisa;
		float inclina = 14.0F + 10.0F * prisa;
		return new Pose()
			.gira(MUSLO_D, -muslo * s, 0, 0)
			.gira(MUSLO_I, muslo * s, 0, 0)
			.gira(PIERNA_D, rodD, 0, 0)
			.gira(PIERNA_I, rodI, 0, 0)
			.gira(PIE_D, -12.0F * s - 8.0F, 0, 0)
			.gira(PIE_I, 12.0F * s - 8.0F, 0, 0)
			.gira(BRAZO_D, brazo * s, 0, 8.0F)
			.gira(BRAZO_I, -brazo * s, 0, -8.0F)
			.gira(ANTEBRAZO_D, -75.0F + 18.0F * s, 0, 0)
			.gira(ANTEBRAZO_I, -75.0F - 18.0F * s, 0, 0)
			.gira(MANO_D, -10.0F, 0, 0)
			.gira(MANO_I, -10.0F, 0, 0)
			.gira(TORSO, inclina, 11.0F * s, 0)
			.gira(CADERA, 0, -8.0F * s, 0)
			.gira(CABEZA, -inclina + 4.0F, -11.0F * s, 0)
			.cuerpo(0, 0, 0, 0, 0.08F * Math.abs(s) - 0.05F, 0);
	}

	/** Andar: lo mismo, mas suave (pasos cortos, brazos casi rectos, el cuerpo derecho). */
	public static Pose caminar(float fase) {
		float a = fase * TAU;
		float s = sin(a);
		float rodD = 4.0F + 38.0F * (float) Math.pow(Math.max(0.0F, cos(a + 0.5F)), 1.5);
		float rodI = 4.0F + 38.0F * (float) Math.pow(Math.max(0.0F, cos(a + (float) Math.PI + 0.5F)), 1.5);
		return new Pose()
			.gira(MUSLO_D, -26.0F * s, 0, 0)
			.gira(MUSLO_I, 26.0F * s, 0, 0)
			.gira(PIERNA_D, rodD, 0, 0)
			.gira(PIERNA_I, rodI, 0, 0)
			.gira(PIE_D, -6.0F * s, 0, 0)
			.gira(PIE_I, 6.0F * s, 0, 0)
			.gira(BRAZO_D, 22.0F * s, 0, 3.0F)
			.gira(BRAZO_I, -22.0F * s, 0, -3.0F)
			.gira(ANTEBRAZO_D, -14.0F + 8.0F * s, 0, 0)
			.gira(ANTEBRAZO_I, -14.0F - 8.0F * s, 0, 0)
			.gira(TORSO, 3.0F, 5.0F * s, 0)
			.gira(CADERA, 0, -5.0F * s, 0)
			.gira(CABEZA, -3.0F, -5.0F * s, 0)
			.cuerpo(0, 0, 0, 0, 0.025F * Math.abs(s) - 0.01F, 0);
	}

	/** Agachado: rodillas dobladas, la cadera baja, el torso inclinado y la cabeza mirando al frente. */
	public static Pose agachado() {
		return new Pose()
			.gira(MUSLO_D, -62.0F, 0, 4.0F)
			.gira(MUSLO_I, -62.0F, 0, -4.0F)
			.gira(PIERNA_D, 118.0F, 0, 0)
			.gira(PIERNA_I, 118.0F, 0, 0)
			.gira(PIE_D, -56.0F, 0, 0)
			.gira(PIE_I, -56.0F, 0, 0)
			.gira(TORSO, 32.0F, 0, 0)
			.gira(CABEZA, -30.0F, 0, 0)
			.gira(BRAZO_D, -28.0F, 0, 6.0F)
			.gira(BRAZO_I, -28.0F, 0, -6.0F)
			.gira(ANTEBRAZO_D, -40.0F, 0, 0)
			.gira(ANTEBRAZO_I, -40.0F, 0, 0)
			.cuerpo(0, 0, 0, 0, -0.42F, 0.05F);
	}

	/** Mira atras por encima de un hombro (k 0..1; lado 1 por la izquierda, -1 por la derecha): capa para sumar. */
	public static Pose mirarAtras(float k, float lado) {
		return new Pose()
			.gira(CABEZA, -6.0F * k, 68.0F * k * lado, 0)
			.gira(TORSO, 0, 24.0F * k * lado, 0)
			.gira(CADERA, 0, 6.0F * k * lado, 0);
	}

	/** Susto: encogido, brazos hacia el pecho, la cabeza hundida (capa para sumar, k 0..1). */
	public static Pose susto(float k) {
		return new Pose()
			.gira(TORSO, -6.0F * k, 0, 0)
			.gira(CABEZA, 10.0F * k, 0, 0)
			.gira(BRAZO_D, -35.0F * k, 0, -10.0F * k)
			.gira(BRAZO_I, -35.0F * k, 0, 10.0F * k)
			.gira(ANTEBRAZO_D, -80.0F * k, 0, 0)
			.gira(ANTEBRAZO_I, -80.0F * k, 0, 0)
			.gira(MUSLO_D, -10.0F * k, 0, 0)
			.gira(PIERNA_D, 20.0F * k, 0, 0)
			.cuerpo(0, 0, 0, 0, -0.04F * k, 0);
	}

	/** Tirado boca arriba (derribado). */
	public static Pose bocaArriba() {
		return new Pose()
			.gira(CABEZA, -12.0F, 20.0F, 0)
			.gira(BRAZO_D, -20.0F, 0, 40.0F)
			.gira(BRAZO_I, -30.0F, 0, -55.0F)
			.gira(ANTEBRAZO_D, -50.0F, 0, 0)
			.gira(ANTEBRAZO_I, -25.0F, 0, 0)
			.gira(MUSLO_D, -30.0F, 0, 10.0F)
			.gira(MUSLO_I, -8.0F, 0, -12.0F)
			.gira(PIERNA_D, 55.0F, 0, 0)
			.gira(PIERNA_I, 15.0F, 0, 0)
			.cuerpo(-88.0F, 0, 0, 0, 0.12F, 0);
	}

	/** Forcejeando en el suelo, boca arriba: brazos que empujan hacia arriba y piernas que patalean. */
	public static Pose forcejeo(float t) {
		float a = sin(t * 9.0F);
		float b = sin(t * 7.3F + 1.0F);
		float c = sin(t * 11.0F + 2.0F);
		return bocaArriba()
			.gira(BRAZO_D, -140.0F + 30.0F * a, 0, 20.0F + 15.0F * c)
			.gira(BRAZO_I, -150.0F - 30.0F * b, 0, -25.0F - 10.0F * a)
			.gira(ANTEBRAZO_D, -25.0F + 20.0F * b, 0, 0)
			.gira(ANTEBRAZO_I, -30.0F + 20.0F * c, 0, 0)
			.gira(MUSLO_D, -45.0F + 30.0F * c, 0, 10.0F)
			.gira(MUSLO_I, -30.0F - 30.0F * a, 0, -10.0F)
			.gira(PIERNA_D, 70.0F + 30.0F * b, 0, 0)
			.gira(PIERNA_I, 60.0F - 30.0F * c, 0, 0)
			.gira(CABEZA, -20.0F + 10.0F * b, 30.0F * a, 0);
	}

	/** Mirandose las manos, delante de la cara: los antebrazos en alto y las palmas hacia uno. */
	public static Pose manos(float t) {
		float r = sin(t * 1.3F);
		return new Pose()
			.gira(BRAZO_D, -42.0F + r, 0, -12.0F)
			.gira(BRAZO_I, -40.0F - r, 0, 12.0F)
			.gira(ANTEBRAZO_D, -62.0F, 0, 0)
			.gira(ANTEBRAZO_I, -64.0F, 0, 0)
			.gira(MANO_D, -12.0F, 0, 0)
			.gira(MANO_I, -12.0F, 0, 0)
			.gira(CABEZA, 35.0F, 0, 0);
	}
}
