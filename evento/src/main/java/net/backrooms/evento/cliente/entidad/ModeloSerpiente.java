package net.backrooms.evento.cliente.entidad;

import net.backrooms.evento.cliente.piscinas.MordiscoCliente;
import net.backrooms.evento.entidad.Serpiente;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * La Serpiente (malla en MallaSerpiente, generada) y como se mueve:
 *  - siempre: respira (el tronco sube y baja), la mandibula se entreabre, la cola ondula y
 *    la cabeza mira a donde mira ella;
 *  - avanzando: la cola ondula mucho mas (una onda que va de la cabeza a la punta), el cuerpo
 *    se mece y los brazos se arrastran, uno y otro;
 *  - cazando: baja el cuello hacia delante, abre mas la boca y adelanta los brazos;
 *  - atacando (MordiscoCliente#lanzamiento, los mismos tiempos que Serpiente.AT_*): se echa
 *    atras con la boca abriendose, se lanza con la mandibula de par en par y los brazos por
 *    delante, cierra de golpe, sacude la cabeza y vuelve. Nada que ver con como se mueve.
 */
public class ModeloSerpiente extends EntityModel<EstadoSerpiente> {
	private final ModelPart base;
	private final ModelPart[] tronco = new ModelPart[3];
	private final ModelPart[] cuello = new ModelPart[3];
	private final ModelPart craneo;
	private final ModelPart mandibula;
	private final ModelPart[] cola = new ModelPart[10];
	/** Humeros: los dos de delante (cuello) y los dos de atras (tronco); y sus falanges. */
	private final ModelPart[] humeros = new ModelPart[4];
	private final ModelPart[][] falanges = new ModelPart[4][4];

	public ModeloSerpiente(ModelPart raiz) {
		super(raiz);
		this.base = raiz.getChild("base");
		ModelPart p = this.base;
		for (int k = 0; k < 3; k++) {
			p = p.getChild("tronco" + (k + 1));
			this.tronco[k] = p;
		}
		for (int k = 0; k < 3; k++) {
			p = p.getChild("cuello" + (k + 1));
			this.cuello[k] = p;
		}
		this.craneo = this.cuello[2].getChild("craneo");
		this.mandibula = this.craneo.getChild("mandibula");
		p = this.base;
		for (int k = 0; k < 10; k++) {
			p = p.getChild("cola" + (k + 1));
			this.cola[k] = p;
		}
		String[] brazos = {"izq_cuello1", "der_cuello1", "izq_tronco1", "der_tronco1"};
		ModelPart[] padres = {this.cuello[0], this.cuello[0], this.tronco[0], this.tronco[0]};
		for (int b = 0; b < 4; b++) {
			this.humeros[b] = padres[b].getChild("humero_" + brazos[b]);
			ModelPart mano = this.humeros[b].getChild("cubito_" + brazos[b]).getChild("mano_" + brazos[b]);
			for (int k = 0; k < 4; k++) {
				this.falanges[b][k] = mano.getChild("falange_" + brazos[b] + "_" + k);
			}
		}
	}

	@Override
	public void setupAnim(EstadoSerpiente s) {
		super.setupAnim(s);
		float t = s.ageInTicks;
		float v = Mth.clamp(s.walkAnimationSpeed * 1.6F, 0.0F, 1.0F);
		float paso = s.walkAnimationPos;
		float c = s.caza;

		// respirar
		this.tronco[1].xRot += Mth.sin(t * 0.06F) * 0.025F;
		this.cuello[1].xRot += Mth.sin(t * 0.06F + 1.0F) * 0.03F;
		// la cola: una onda que baja hasta la punta, mucho mas al avanzar
		float fase = t * 0.07F + paso * 0.55F;
		float amp = 0.05F + 0.24F * v;
		for (int k = 0; k < this.cola.length; k++) {
			this.cola[k].yRot += amp * (0.6F + k * 0.06F) * Mth.sin(fase - k * 0.75F);
		}
		// el cuerpo se mece y los brazos se arrastran, uno y otro
		for (int k = 0; k < 3; k++) {
			this.tronco[k].yRot += (0.03F + 0.06F * v) * Mth.sin(fase * 0.5F - k * 0.6F);
		}
		for (int b = 0; b < 4; b++) {
			this.humeros[b].xRot += Mth.sin(paso * 0.6F + b * Mth.PI) * 0.3F * v + Mth.sin(t * 0.05F + b) * 0.05F;
		}
		// la cabeza mira, y la mandibula se entreabre
		this.craneo.yRot += Mth.clamp(s.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD * 0.6F;
		this.craneo.xRot += Mth.clamp(s.xRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD * 0.5F;
		this.mandibula.xRot += 0.04F + 0.05F * Mth.sin(t * 0.05F) + 0.28F * c;
		// cazando: el cuello hacia delante y los brazos por delante
		this.cuello[0].xRot += 0.14F * c;
		this.cuello[1].xRot += 0.1F * c;
		this.craneo.xRot -= 0.12F * c;
		this.humeros[0].xRot -= 0.45F * c;
		this.humeros[1].xRot -= 0.45F * c;
		// en el agua flota un poco, arriba y abajo
		if (s.agua) {
			this.base.y += Mth.sin(t * 0.09F) * 0.8F;
		} else {
			// en el azulejo va un poco mas alta: al arrastrarse, las manos se metian en el suelo
			this.base.y -= 3.0F;
		}
		if (s.ataque >= 0.0F) {
			this.atacar(s.ataque);
		}
	}

	private void atacar(float a) {
		float r = MordiscoCliente.lanzamiento(a);
		this.tronco[1].xRot += 0.35F * r;
		this.tronco[2].xRot += 0.3F * r;
		this.cuello[0].xRot += 0.2F * r;
		this.cuello[1].xRot += 0.12F * r;
		this.craneo.xRot -= 0.3F * r;
		// la boca: se abre mientras se echa atras y se lanza, se cierra de golpe al morder
		float abre;
		if (a < Serpiente.AT_MUERDE) {
			abre = Mth.clamp(a / (Serpiente.AT_MUERDE - 1.0F), 0.0F, 1.0F);
		} else if (a < Serpiente.AT_MUERDE + 2) {
			abre = 0.0F;
		} else {
			abre = 0.25F;
		}
		this.mandibula.xRot += 0.95F * abre;
		// sacude a la presa
		if (a >= Serpiente.AT_MUERDE && a < Serpiente.AT_MATA) {
			this.craneo.yRot += 0.28F * Mth.sin(a * 2.4F);
			this.cuello[2].zRot += 0.16F * Mth.sin(a * 2.4F + 1.0F);
		}
		// los brazos de delante agarran y las garras se cierran
		float agarra = Math.max(0.0F, r);
		this.humeros[0].xRot -= 0.9F * agarra;
		this.humeros[1].xRot -= 0.9F * agarra;
		for (int b = 0; b < 2; b++) {
			for (int k = 0; k < 4; k++) {
				this.falanges[b][k].xRot += 0.7F * agarra;
			}
		}
		// la cola da latigazos
		for (int k = 0; k < this.cola.length; k++) {
			this.cola[k].yRot += 0.35F * Math.abs(r) * Mth.sin(a * 0.6F - k * 0.5F);
		}
	}
}
