package net.backrooms.evento.esqueleto;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Una animacion por fotogramas clave: en cada segundo marcado, una pose; entre uno y otro se pasa con
 * suavidad (sale y llega despacio, o lineal si se pide). Las poses pueden ser fijas o cambiar con el
 * tiempo (un ciclo de carrera dentro de un tramo). Ejemplo:
 *
 * <pre>
 *   Animacion a = new Animacion()
 *       .clave(0.0F, Ciclos.quieto(0))
 *       .clave(0.4F, Ciclos.agachado())
 *       .clave(1.0F, t -> Ciclos.correr(t * 1.8F, 1.0F));
 *   Pose p = a.en(segundos);
 * </pre>
 */
public final class Animacion {
	private record Clave(float t, Function<Float, Pose> pose, boolean lineal) {
	}

	private final List<Clave> claves = new ArrayList<>();

	/** Una clave con una pose fija. */
	public Animacion clave(float t, Pose p) {
		return this.clave(t, s -> p);
	}

	/** Una clave cuya pose depende del segundo (para ciclos dentro de un tramo). */
	public Animacion clave(float t, Function<Float, Pose> p) {
		this.claves.add(new Clave(t, p, false));
		return this;
	}

	/** Llega a esta clave sin frenar (para golpes y caidas). */
	public Animacion claveLineal(float t, Pose p) {
		this.claves.add(new Clave(t, s -> p, true));
		return this;
	}

	public float duracion() {
		return this.claves.isEmpty() ? 0.0F : this.claves.get(this.claves.size() - 1).t();
	}

	/** La pose en el segundo t (antes de la primera clave, la primera; despues de la ultima, la ultima). */
	public Pose en(float t) {
		if (this.claves.isEmpty()) {
			return new Pose();
		}
		Clave c0 = this.claves.get(0);
		if (t <= c0.t()) {
			return c0.pose().apply(t);
		}
		for (int i = 1; i < this.claves.size(); i++) {
			Clave c1 = this.claves.get(i);
			if (t < c1.t()) {
				Clave a = this.claves.get(i - 1);
				float k = (t - a.t()) / (c1.t() - a.t());
				if (!c1.lineal()) {
					k = k * k * (3.0F - 2.0F * k);
				}
				return Pose.mezcla(a.pose().apply(t), c1.pose().apply(t), k);
			}
		}
		return this.claves.get(this.claves.size() - 1).pose().apply(t);
	}
}
