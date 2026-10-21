package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * El Amasijo (malla en MallaAmasijo, generada por tools/texturas/entidades.js): anda sobre seis
 * brazos como una arana torpe, de tres en tres, y el cuerpo se le bambolea. Los otros ocho brazos
 * tantean el aire sin parar, cada uno a su ritmo; cazando los estira hacia ti y los agita deprisa.
 * Agarrando, los cierra todos alrededor de la presa y la boca tiembla.
 */
public class ModeloAmasijo extends EntityModel<EstadoAmasijo> {
	private static final int PATAS = 6;
	private static final int BRAZOS = 8;

	private final ModelPart cuerpo;
	private final ModelPart[] patas = new ModelPart[PATAS];
	private final ModelPart[] brazos = new ModelPart[BRAZOS];
	private final ModelPart[] antes = new ModelPart[BRAZOS];

	public ModeloAmasijo(ModelPart raiz) {
		super(raiz);
		this.cuerpo = raiz.getChild("cuerpo");
		for (int i = 0; i < PATAS; i++) {
			this.patas[i] = this.cuerpo.getChild("pata_" + i);
		}
		for (int i = 0; i < BRAZOS; i++) {
			this.brazos[i] = this.cuerpo.getChild("brazo_" + i);
			this.antes[i] = this.brazos[i].getChild("ante_" + i);
		}
	}

	@Override
	public void setupAnim(EstadoAmasijo s) {
		super.setupAnim(s);
		float t = s.ageInTicks;
		float v = Mth.clamp(s.walkAnimationSpeed * 1.6F, 0.0F, 1.0F);
		float p = s.walkAnimationPos;
		float c = s.caza;
		// las patas, en dos tripodes alternos (0,3,4 y 1,2,5)
		for (int i = 0; i < PATAS; i++) {
			boolean grupo = i == 0 || i == 3 || i == 4;
			float fase = p * 0.9F + (grupo ? 0.0F : Mth.PI);
			this.patas[i].xRot += Mth.cos(fase) * 0.7F * v;
			this.patas[i].zRot += Math.max(0.0F, Mth.sin(fase)) * 0.25F * v * (i % 2 == 0 ? 1.0F : -1.0F);
		}
		// el cuerpo: se bambolea al andar y respira quieto
		this.cuerpo.y -= Math.abs(Mth.sin(p * 0.9F)) * 0.8F * v;
		this.cuerpo.zRot += Mth.sin(p * 0.45F) * 0.08F * v + Mth.sin(t * 0.05F) * 0.03F;
		this.cuerpo.xRot += 0.12F * c + Mth.sin(t * 0.08F) * 0.02F;
		float hinchado = 1.0F + Mth.sin(t * 0.12F) * 0.02F;
		this.cuerpo.xScale = hinchado;
		this.cuerpo.zScale = hinchado;
		// los brazos: tantean, cada uno a su ritmo; cazando, mas deprisa y hacia delante
		float ritmo = 0.09F + c * 0.25F;
		for (int i = 0; i < BRAZOS; i++) {
			float fase = t * ritmo + i * 1.7F;
			float amplitud = 0.18F + c * 0.3F;
			this.brazos[i].xRot += Mth.sin(fase) * amplitud + c * 0.35F;
			this.brazos[i].yRot += Mth.cos(fase * 0.7F) * amplitud * 0.6F - this.brazos[i].yRot * 0.35F * c;
			this.antes[i].xRot += Mth.sin(fase * 1.3F + 0.8F) * (0.3F + c * 0.25F) - c * 0.2F;
		}
		if (s.agarrando) {
			for (int i = 0; i < BRAZOS; i++) {
				this.brazos[i].xRot = 0.15F + Mth.sin(t * 0.9F + i) * 0.05F;
				this.brazos[i].yRot *= 0.3F;
				this.antes[i].xRot = 0.9F;
			}
			this.cuerpo.xRot += Mth.sin(t * 1.4F) * 0.04F;
		}
	}
}
