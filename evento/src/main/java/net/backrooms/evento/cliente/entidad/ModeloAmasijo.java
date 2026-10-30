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
		// el salto sobre la presa: los brazos abiertos y estirados hacia delante, las patas atras
		float e = s.embiste;
		if (e > 0.0F) {
			for (int i = 0; i < BRAZOS; i++) {
				this.brazos[i].xRot = Mth.lerp(e, this.brazos[i].xRot, 0.55F);
				this.brazos[i].yRot = Mth.lerp(e, this.brazos[i].yRot, (i % 2 == 0 ? 0.75F : -0.75F) * (i < 4 ? 1.0F : 0.6F));
				this.antes[i].xRot = Mth.lerp(e, this.antes[i].xRot, -0.15F);
			}
			for (int i = 0; i < PATAS; i++) {
				this.patas[i].xRot += 0.9F * e;
			}
			this.cuerpo.xRot -= 0.25F * e;
		}
		// comiendo: se hunde sobre la presa a tirones y desgarra con los brazos por parejas (bajan, agarran,
		// tiran hacia arriba), con sacudidas del cuerpo en cada tiron
		float k = s.come;
		if (k > 0.0F) {
			float tiron = (float) Math.pow(Math.max(0.0F, Mth.sin(t * 0.32F)), 6.0);
			this.cuerpo.xRot += (0.42F + Mth.sin(t * 0.16F) * 0.1F - tiron * 0.3F) * k;
			this.cuerpo.y += (2.5F + Mth.sin(t * 0.16F) * 1.2F) * k;
			this.cuerpo.zRot += tiron * Mth.sin(t * 2.1F) * 0.12F * k;
			for (int i = 0; i < BRAZOS; i++) {
				float f = t * 0.24F + (i / 2) * 1.6F;
				float baja = 0.5F + 0.5F * Mth.sin(f);
				float desgarra = (float) Math.pow(Math.max(0.0F, Mth.sin(f + 1.2F)), 3.0);
				this.brazos[i].xRot = Mth.lerp(k, this.brazos[i].xRot, 0.9F + 0.5F * baja - 1.1F * desgarra);
				this.brazos[i].yRot = Mth.lerp(k, this.brazos[i].yRot, (i % 2 == 0 ? 0.25F : -0.25F) + Mth.sin(f * 0.7F) * 0.15F);
				this.antes[i].xRot = Mth.lerp(k, this.antes[i].xRot, 1.25F * baja + 0.3F - 0.6F * desgarra);
			}
			for (int i = 0; i < PATAS; i++) {
				this.patas[i].xRot += Mth.sin(t * 0.32F + i) * 0.08F * k;
			}
		}
		// alerta (ha oido algo): se yergue, los brazos se quedan quietos a medio alzar, como escuchando
		float al = s.alerta;
		if (al > 0.0F) {
			this.cuerpo.xRot -= 0.38F * al;
			this.cuerpo.y -= 1.5F * al;
			for (int i = 0; i < BRAZOS; i++) {
				this.brazos[i].xRot = Mth.lerp(al, this.brazos[i].xRot, 0.2F + (i % 3) * 0.12F);
				this.antes[i].xRot = Mth.lerp(al, this.antes[i].xRot, 0.5F);
			}
		}
	}
}
