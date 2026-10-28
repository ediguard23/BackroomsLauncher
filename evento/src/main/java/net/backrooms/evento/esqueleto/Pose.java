package net.backrooms.evento.esqueleto;

import net.minecraft.util.Mth;

/**
 * Una postura del esqueleto: el giro de cada hueso (x, y, z en radianes, respecto a su padre) y como
 * esta el cuerpo entero (CUERPO: girado sobre los pies y desplazado, en bloques). Es solo datos: se
 * mezclan (para pasar de una a otra), se suman (una capa encima de otra: correr y, encima, mirar
 * atras) y se espejan.
 */
public final class Pose {
	private static final int N = Hueso.TODOS.length;
	/** Los giros: 3 por hueso. */
	final float[] giro = new float[N * 3];
	/** El cuerpo entero: giro x, y, z (radianes, sobre los pies) y desplazamiento x, y, z (bloques). */
	public float cuerpoX;
	public float cuerpoY;
	public float cuerpoZ;
	public float despX;
	public float despY;
	public float despZ;

	public Pose() {
	}

	public static Pose quieta() {
		return new Pose();
	}

	public Pose copia() {
		Pose p = new Pose();
		System.arraycopy(this.giro, 0, p.giro, 0, this.giro.length);
		p.cuerpoX = this.cuerpoX;
		p.cuerpoY = this.cuerpoY;
		p.cuerpoZ = this.cuerpoZ;
		p.despX = this.despX;
		p.despY = this.despY;
		p.despZ = this.despZ;
		return p;
	}

	public float x(Hueso h) {
		return this.giro[h.ordinal() * 3];
	}

	public float y(Hueso h) {
		return this.giro[h.ordinal() * 3 + 1];
	}

	public float z(Hueso h) {
		return this.giro[h.ordinal() * 3 + 2];
	}

	/** Pone el giro de un hueso (en grados, que es como se piensan las poses). */
	public Pose gira(Hueso h, float x, float y, float z) {
		int i = h.ordinal() * 3;
		this.giro[i] = x * Mth.DEG_TO_RAD;
		this.giro[i + 1] = y * Mth.DEG_TO_RAD;
		this.giro[i + 2] = z * Mth.DEG_TO_RAD;
		return this;
	}

	/** Suma a un hueso (grados). */
	public Pose suma(Hueso h, float x, float y, float z) {
		int i = h.ordinal() * 3;
		this.giro[i] += x * Mth.DEG_TO_RAD;
		this.giro[i + 1] += y * Mth.DEG_TO_RAD;
		this.giro[i + 2] += z * Mth.DEG_TO_RAD;
		return this;
	}

	/** El cuerpo entero (grados y bloques). */
	public Pose cuerpo(float gx, float gy, float gz, float dx, float dy, float dz) {
		this.cuerpoX = gx * Mth.DEG_TO_RAD;
		this.cuerpoY = gy * Mth.DEG_TO_RAD;
		this.cuerpoZ = gz * Mth.DEG_TO_RAD;
		this.despX = dx;
		this.despY = dy;
		this.despZ = dz;
		return this;
	}

	/** Lo mismo pero con la derecha por la izquierda (y los giros de lado al reves). */
	public Pose espejo() {
		Pose p = new Pose();
		for (Hueso h : Hueso.TODOS) {
			int a = h.ordinal() * 3;
			int b = h.espejo().ordinal() * 3;
			p.giro[b] = this.giro[a];
			p.giro[b + 1] = -this.giro[a + 1];
			p.giro[b + 2] = -this.giro[a + 2];
		}
		p.cuerpoX = this.cuerpoX;
		p.cuerpoY = -this.cuerpoY;
		p.cuerpoZ = -this.cuerpoZ;
		p.despX = -this.despX;
		p.despY = this.despY;
		p.despZ = this.despZ;
		return p;
	}

	/** De a (k = 0) a b (k = 1), hueso a hueso. */
	public static Pose mezcla(Pose a, Pose b, float k) {
		Pose p = new Pose();
		for (int i = 0; i < p.giro.length; i++) {
			p.giro[i] = Mth.lerp(k, a.giro[i], b.giro[i]);
		}
		p.cuerpoX = Mth.lerp(k, a.cuerpoX, b.cuerpoX);
		p.cuerpoY = Mth.lerp(k, a.cuerpoY, b.cuerpoY);
		p.cuerpoZ = Mth.lerp(k, a.cuerpoZ, b.cuerpoZ);
		p.despX = Mth.lerp(k, a.despX, b.despX);
		p.despY = Mth.lerp(k, a.despY, b.despY);
		p.despZ = Mth.lerp(k, a.despZ, b.despZ);
		return p;
	}

	/** Esta mas otra encima, con un peso (capas: la carrera y, encima, el giro de cabeza). */
	public Pose mas(Pose otra, float peso) {
		Pose p = this.copia();
		for (int i = 0; i < p.giro.length; i++) {
			p.giro[i] += otra.giro[i] * peso;
		}
		p.cuerpoX += otra.cuerpoX * peso;
		p.cuerpoY += otra.cuerpoY * peso;
		p.cuerpoZ += otra.cuerpoZ * peso;
		p.despX += otra.despX * peso;
		p.despY += otra.despY * peso;
		p.despZ += otra.despZ * peso;
		return p;
	}
}
