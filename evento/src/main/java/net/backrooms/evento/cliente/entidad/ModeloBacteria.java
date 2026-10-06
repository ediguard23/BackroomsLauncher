package net.backrooms.evento.cliente.entidad;

import net.backrooms.evento.entidad.Bacteria;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * La Bacteria animada (la malla es MallaBacteria, generada). Camina a
 * zancadas largas balanceando los brazos hasta el suelo; al cazar va
 * inclinada hacia delante, con los brazos por delante y la mandibula
 * abierta castaneteando. Siempre da tirones de cabeza, como si algo dentro
 * no encajara.
 *
 * El agarre (Bacteria#agarrar): estira los brazos con las garras abiertas, te
 * levanta con los codos en alto como una marioneta, estira el cuello hasta tu
 * cabeza y muerde tres veces. Los angulos salen de calcular donde caen manos y
 * boca con las medidas de MallaBacteria para la distancia a la que te sujeta
 * (Bacteria#AG_DELANTE): las garras cierran sobre ti y la boca llega a tu cara.
 */
public class ModeloBacteria extends EntityModel<EstadoBacteria> {
	private final ModelPart espina;
	private final ModelPart pecho;
	private final ModelPart cuello;
	private final ModelPart cabeza;
	private final ModelPart mandibula;
	private final ModelPart brazoIzq;
	private final ModelPart brazoDer;
	private final ModelPart antebrazoIzq;
	private final ModelPart antebrazoDer;
	private final ModelPart musloIzq;
	private final ModelPart musloDer;
	private final ModelPart piernaIzq;
	private final ModelPart piernaDer;
	private final ModelPart[] garras;

	public ModeloBacteria(ModelPart raiz) {
		super(raiz);
		ModelPart cadera = raiz.getChild("cadera");
		this.espina = cadera.getChild("espina");
		this.pecho = this.espina.getChild("pecho");
		ModelPart hombros = this.pecho.getChild("hombros");
		this.cuello = hombros.getChild("cuello");
		this.cabeza = this.cuello.getChild("cabeza");
		this.mandibula = this.cabeza.getChild("mandibula");
		this.brazoIzq = hombros.getChild("brazo_izq");
		this.brazoDer = hombros.getChild("brazo_der");
		this.antebrazoIzq = this.brazoIzq.getChild("antebrazo_izq");
		this.antebrazoDer = this.brazoDer.getChild("antebrazo_der");
		this.musloIzq = cadera.getChild("muslo_izq");
		this.musloDer = cadera.getChild("muslo_der");
		this.piernaIzq = this.musloIzq.getChild("pierna_izq");
		this.piernaDer = this.musloDer.getChild("pierna_der");
		ModelPart manoIzq = this.antebrazoIzq.getChild("mano_izq");
		ModelPart manoDer = this.antebrazoDer.getChild("mano_der");
		this.garras = new ModelPart[] {
			manoIzq.getChild("garra_izq_a"), manoIzq.getChild("garra_izq_b"), manoIzq.getChild("garra_izq_c"),
			manoDer.getChild("garra_der_a"), manoDer.getChild("garra_der_b"), manoDer.getChild("garra_der_c")};
	}

	@Override
	public void setupAnim(EstadoBacteria s) {
		super.setupAnim(s);
		float t = s.ageInTicks;
		float paso = s.walkAnimationPos * 0.45F;
		float fuerza = Math.min(1.0F, s.walkAnimationSpeed * 1.4F);
		float caza = s.caza;

		// piernas: zancadas largas; la rodilla se dobla al levantar
		float a = Mth.cos(paso) * 0.75F * fuerza;
		this.musloIzq.xRot += a;
		this.musloDer.xRot -= a;
		this.piernaIzq.xRot += Math.max(0.0F, -Mth.sin(paso)) * 0.9F * fuerza;
		this.piernaDer.xRot += Math.max(0.0F, Mth.sin(paso)) * 0.9F * fuerza;

		// brazos: cuelgan hasta el suelo y se balancean al reves que las piernas;
		// cazando se adelantan, como si quisiera agarrarte
		this.brazoIzq.xRot += -a * 0.8F - caza * 0.55F;
		this.brazoDer.xRot += a * 0.8F - caza * 0.55F;
		this.antebrazoIzq.xRot += -caza * 0.4F + Mth.sin(t * 0.07F) * 0.05F;
		this.antebrazoDer.xRot += -caza * 0.4F + Mth.sin(t * 0.07F + 1.7F) * 0.05F;
		this.brazoIzq.zRot += Mth.sin(t * 0.05F) * 0.04F;
		this.brazoDer.zRot -= Mth.sin(t * 0.05F + 0.8F) * 0.04F;

		// el cuerpo: respira, se balancea al andar y se echa hacia delante al cazar
		this.espina.xRot += caza * 0.32F + Mth.sin(t * 0.06F) * 0.03F;
		this.espina.zRot += Mth.cos(paso) * 0.06F * fuerza;
		this.pecho.xRot += caza * 0.12F;

		// cabeza: mira hacia ti y da tirones raros cada poco
		float tiron = tiron(t);
		this.cuello.yRot += s.yRot * Mth.DEG_TO_RAD * 0.5F;
		this.cabeza.yRot += s.yRot * Mth.DEG_TO_RAD * 0.5F + tiron * 0.35F;
		this.cabeza.zRot += tiron * 0.45F + Mth.sin(t * 0.09F) * 0.06F;
		this.cabeza.xRot += s.xRot * Mth.DEG_TO_RAD * 0.6F - caza * 0.35F;

		// mandibula: entreabierta; cazando, abierta del todo y castaneteando
		this.mandibula.xRot += caza * 0.45F + caza * Math.abs(Mth.sin(t * 1.3F)) * 0.25F;

		if (s.agarre >= 0.0F) {
			this.agarre(s.agarre);
		}
	}

	/** Lleva una parte hacia su pose de reposo mas (dx, dy, dz), con peso k (0 = como estaba). */
	private static void hacia(ModelPart p, float k, float dx, float dy, float dz) {
		p.xRot = Mth.lerp(k, p.xRot, p.getInitialPose().xRot() + dx);
		p.yRot = Mth.lerp(k, p.yRot, p.getInitialPose().yRot() + dy);
		p.zRot = Mth.lerp(k, p.zRot, p.getInitialPose().zRot() + dz);
	}

	private static float suave(float a, float b, float x) {
		float k = Mth.clamp((x - a) / (b - a), 0.0F, 1.0F);
		return k * k * (3.0F - 2.0F * k);
	}

	/** g: ticks desde que agarro (con fraccion). Ver Bacteria#AG_*. */
	private void agarre(float g) {
		float entra = suave(0.0F, 4.0F, g);
		float levanta = suave(Bacteria.AG_LEVANTA - 1, Bacteria.AG_LEVANTA + 8, g);
		float alcanza = suave(0.0F, 5.0F, g) * (1.0F - levanta);
		float come = suave(Bacteria.AG_COME - 3, Bacteria.AG_COME + 1, g);
		float muerde = Bacteria.mordisco(g);
		float tiembla = Mth.sin(g * 2.3F) * 0.05F * levanta;

		// cuerpo: se echa encima al alcanzar y se yergue al levantar, temblando
		hacia(this.espina, entra, 0.40F * alcanza + 0.05F * levanta, 0.0F, tiembla);
		hacia(this.pecho, entra, 0.0F, 0.0F, -tiembla * 0.6F);
		// brazos: alcanzar (brazo adelante, antebrazo doblado) -> codos arriba sujetando
		float bx = -0.22F * alcanza - 2.27F * levanta;
		float bz = 0.76F * alcanza + 1.08F * levanta;
		float ax = -1.26F * alcanza + 2.13F * levanta;
		hacia(this.brazoIzq, entra, bx, 0.0F, bz);
		hacia(this.brazoDer, entra, bx, 0.0F, -bz);
		hacia(this.antebrazoIzq, entra, ax + tiembla, 0.0F, 0.0F);
		hacia(this.antebrazoDer, entra, ax - tiembla, 0.0F, 0.0F);
		// garras: abiertas al alcanzar, cerradas sobre ti al levantar
		float abre = alcanza * (1.0F - suave(Bacteria.AG_LEVANTA - 2, Bacteria.AG_LEVANTA, g));
		float cierra = levanta;
		for (int i = 0; i < 6; i++) {
			int k = i % 3;
			float lado = i < 3 ? 1.0F : -1.0F;
			if (k == 0) {
				hacia(this.garras[i], entra, -0.6F * abre + 0.55F * cierra, 0.0F, 0.0F);
			} else {
				float fuera = k == 1 ? 1.0F : -1.0F;
				hacia(this.garras[i], entra, 0.35F * cierra, 0.0F, (0.5F * abre - 0.35F * cierra) * fuera * lado);
			}
		}
		// cabeza: te mira mientras te sube; al comer estira el cuello hasta tu cara
		// (cuello +0.94, cabeza -0.98 = la boca justo en tu cabeza) y retrocede entre mordiscos
		float lanza = come * (0.6F + 0.4F * muerde);
		hacia(this.cuello, entra, 0.94F * lanza, 0.0F, 0.0F);
		hacia(this.cabeza, entra, -0.2F * levanta * (1.0F - come) - 0.98F * lanza, 0.0F, tiembla * 1.5F);
		// mandibula: grunendo entreabierta; al comer, abierta del todo y se cierra de golpe en cada mordisco
		float abierta = 0.6F * (1.0F - come) * entra + 1.25F * come * (1.0F - muerde) + Math.abs(Mth.sin(g * 1.7F)) * 0.15F * (1.0F - come);
		hacia(this.mandibula, entra, abierta, 0.0F, 0.0F);
		// las piernas, quietas y abiertas, bien plantadas
		hacia(this.musloIzq, entra, 0.0F, 0.0F, -0.12F);
		hacia(this.musloDer, entra, 0.0F, 0.0F, 0.12F);
		hacia(this.piernaIzq, entra, 0.0F, 0.0F, 0.0F);
		hacia(this.piernaDer, entra, 0.0F, 0.0F, 0.0F);
	}

	/** Tirones de cabeza: casi siempre quieta y, de vez en cuando, un giro brusco. */
	private static float tiron(float t) {
		int ciclo = (int) (t / 37.0F);
		float dentro = t - ciclo * 37.0F;
		float lado = ((ciclo * 7919) & 2) == 0 ? 1.0F : -1.0F;
		if (dentro < 3.0F) {
			return lado * dentro / 3.0F;
		}
		if (dentro < 9.0F) {
			return lado * (1.0F - (dentro - 3.0F) / 6.0F);
		}
		return 0.0F;
	}
}
