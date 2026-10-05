package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * La Bacteria animada (la malla es MallaBacteria, generada). Camina a
 * zancadas largas balanceando los brazos hasta el suelo; al cazar va
 * inclinada hacia delante, con los brazos por delante y la mandibula
 * abierta castaneteando. Siempre da tirones de cabeza, como si algo dentro
 * no encajara.
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
