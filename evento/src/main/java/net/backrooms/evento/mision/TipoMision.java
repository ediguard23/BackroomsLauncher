package net.backrooms.evento.mision;

/**
 * Las misiones de un explorador. La primera siempre es recoger casetes; las
 * otras dos salen al azar entre las de grabar con la camara (C), asi que dos
 * jugadores casi nunca tienen las mismas. La de la entidad tiene dos
 * versiones (sola o con las alarmas encendidas) y nunca tocan las dos.
 *
 * Reglas del organizador: los Smilers solo salen de noche, y la Bacteria
 * esta mas furiosa con las alarmas (las luces rojas) encendidas.
 */
public enum TipoMision {
	CASETES("Recoge los casetes", "Alguien estuvo grabando antes que tú. Sus cintas están tiradas por el Nivel 0, lejos unas de otras. El detector solo las capta de cerca."),
	LUCES_ROJAS("Graba las alarmas", "Cuando los tubos se pongan en rojo, grábalo con la cámara (C)."),
	ENTIDAD("Graba una entidad", "Consigue grabar a la Bacteria unos segundos sin que te atrape."),
	ENTIDAD_ALARMA("Graba la Bacteria en alarma", "Grábala con las alarmas encendidas. En rojo está más furiosa: no dejes que te vea primero."),
	SMILER("Graba un Smiler", "Solo salen de noche, en la oscuridad. Grábalo con la cámara antes de que se acerque.");

	public final String titulo;
	public final String descripcion;

	TipoMision(String titulo, String descripcion) {
		this.titulo = titulo;
		this.descripcion = descripcion;
	}

	/** Las de grabar que se sortean: la de la entidad, en una de sus dos versiones. */
	public static java.util.List<TipoMision> deGrabar(java.util.Random azar) {
		return new java.util.ArrayList<>(java.util.List.of(LUCES_ROJAS, azar.nextBoolean() ? ENTIDAD : ENTIDAD_ALARMA, SMILER));
	}
}
