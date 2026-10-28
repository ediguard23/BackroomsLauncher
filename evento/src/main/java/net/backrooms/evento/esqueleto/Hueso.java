package net.backrooms.evento.esqueleto;

import org.jspecify.annotations.Nullable;

/**
 * Los huesos del esqueleto articulado de una figura humana (ModeloArticulado). Cada uno gira desde su
 * articulacion (su pivote, en unidades de modelo de Minecraft: 16 por bloque, y hacia abajo, los pies en
 * y = 24) y arrastra a sus hijos:
 *
 *   CADERA ─ TORSO ─ CABEZA
 *     │        ├─ BRAZO_D ─ ANTEBRAZO_D ─ MANO_D     (hombro, codo, muneca)
 *     │        └─ BRAZO_I ─ ANTEBRAZO_I ─ MANO_I
 *     ├─ MUSLO_D ─ PIERNA_D ─ PIE_D                  (cadera, rodilla, tobillo)
 *     └─ MUSLO_I ─ PIERNA_I ─ PIE_I
 *
 * La geometria es la de un jugador (cabeza 8, cuerpo 8x12x4, brazos y piernas 4x12x4) partida en las
 * articulaciones, y cada trozo toma de la piel (formato de piel de jugador, 64x64) justo las filas que le
 * tocan: el brazo de arriba las 6 primeras, el antebrazo 4 y la mano 2; igual la pierna. Asi la figura se
 * ve como un personaje normal de Minecraft, pero dobla codos, rodillas, munecas, tobillos y cintura.
 *
 * Convencion de giros (radianes, como ModelPart): en brazos y piernas, x negativo lleva el miembro hacia
 * delante y x positivo hacia atras; en el antebrazo, x negativo dobla el codo; en la pierna (espinilla), x
 * positivo dobla la rodilla. En el torso, x positivo inclina hacia delante; en la cabeza, x positivo mira
 * abajo. y gira a la izquierda de la figura; z la ladea.
 */
public enum Hueso {
	CADERA(null, 0, 12, 0, -4, -4, -2, 8, 4, 4, 16, 16, 8),
	TORSO(CADERA, 0, 8, 0, -4, -8, -2, 8, 8, 4, 16, 16, 0),
	CABEZA(TORSO, 0, 0, 0, -4, -8, -4, 8, 8, 8, 0, 0, 0),
	BRAZO_D(TORSO, -5, 2, 0, -3, -2, -2, 4, 6, 4, 40, 16, 0),
	ANTEBRAZO_D(BRAZO_D, -5, 6, 0, -3, 0, -2, 4, 4, 4, 40, 16, 6),
	MANO_D(ANTEBRAZO_D, -5, 10, 0, -3, 0, -2, 4, 2, 4, 40, 16, 10),
	BRAZO_I(TORSO, 5, 2, 0, -1, -2, -2, 4, 6, 4, 32, 48, 0),
	ANTEBRAZO_I(BRAZO_I, 5, 6, 0, -1, 0, -2, 4, 4, 4, 32, 48, 6),
	MANO_I(ANTEBRAZO_I, 5, 10, 0, -1, 0, -2, 4, 2, 4, 32, 48, 10),
	MUSLO_D(CADERA, -1.9F, 12, 0, -2, 0, -2, 4, 6, 4, 0, 16, 0),
	PIERNA_D(MUSLO_D, -1.9F, 18, 0, -2, 0, -2, 4, 4, 4, 0, 16, 6),
	PIE_D(PIERNA_D, -1.9F, 22, 0, -2, 0, -2, 4, 2, 4, 0, 16, 10),
	MUSLO_I(CADERA, 1.9F, 12, 0, -2, 0, -2, 4, 6, 4, 16, 48, 0),
	PIERNA_I(MUSLO_I, 1.9F, 18, 0, -2, 0, -2, 4, 4, 4, 16, 48, 6),
	PIE_I(PIERNA_I, 1.9F, 22, 0, -2, 0, -2, 4, 2, 4, 16, 48, 10);

	public static final Hueso[] TODOS = values();

	/** El padre (null: cuelga de la raiz, en los pies). */
	public final @Nullable Hueso padre;
	/** El pivote, absoluto en el modelo (sin girar). */
	public final float px;
	public final float py;
	public final float pz;
	/** La caja, relativa al pivote: esquina y tamano. */
	public final float bx;
	public final float by;
	public final float bz;
	public final float ancho;
	public final float alto;
	public final float fondo;
	/** De la piel: la esquina (u, v) de la parte entera y la primera fila de este trozo. */
	public final int u;
	public final int v;
	public final int fila;

	Hueso(@Nullable Hueso padre, float px, float py, float pz, float bx, float by, float bz, float ancho, float alto, float fondo, int u, int v,
		int fila) {
		this.padre = padre;
		this.px = px;
		this.py = py;
		this.pz = pz;
		this.bx = bx;
		this.by = by;
		this.bz = bz;
		this.ancho = ancho;
		this.alto = alto;
		this.fondo = fondo;
		this.u = u;
		this.v = v;
		this.fila = fila;
	}

	/** El nombre de su ModelPart. */
	public String nombre() {
		return this.name().toLowerCase(java.util.Locale.ROOT);
	}

	/** El pivote relativo al de su padre (lo que pide PartPose). */
	public float rx() {
		return this.padre == null ? this.px : this.px - this.padre.px;
	}

	public float ry() {
		return this.padre == null ? this.py - 24.0F : this.py - this.padre.py;
	}

	public float rz() {
		return this.padre == null ? this.pz : this.pz - this.padre.pz;
	}

	/** El espejo (derecha por izquierda), para las poses simetricas. */
	public Hueso espejo() {
		return switch (this) {
			case BRAZO_D -> BRAZO_I;
			case BRAZO_I -> BRAZO_D;
			case ANTEBRAZO_D -> ANTEBRAZO_I;
			case ANTEBRAZO_I -> ANTEBRAZO_D;
			case MANO_D -> MANO_I;
			case MANO_I -> MANO_D;
			case MUSLO_D -> MUSLO_I;
			case MUSLO_I -> MUSLO_D;
			case PIERNA_D -> PIERNA_I;
			case PIERNA_I -> PIERNA_D;
			case PIE_D -> PIE_I;
			case PIE_I -> PIE_D;
			default -> this;
		};
	}
}
