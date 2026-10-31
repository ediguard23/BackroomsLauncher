package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * El Amasijo (malla en MallaAmasijo, generada por tools/texturas/entidades.js): anda sobre seis
 * brazos como una arana torpe, de tres en tres. Tiene columna: la cadera (con las patas), el torso
 * (la boca y seis brazos) y la joroba (dos brazos); cada tramo sigue al anterior con un poco de
 * retraso, asi que se encorva, se estira y ondula en vez de moverse de una pieza. Los ocho brazos
 * tantean el aire sin parar, cada uno a su ritmo; cazando los estira hacia ti y los agita deprisa.
 * Agarrando, los cierra todos alrededor de la presa y la boca tiembla.
 */
public class ModeloAmasijo extends EntityModel<EstadoAmasijo> {
	private static final int PATAS = 6;
	private static final int BRAZOS = 8;

	private final ModelPart cuerpo;
	private final ModelPart torso;
	private final ModelPart joroba;
	private final ModelPart[] patas = new ModelPart[PATAS];
	private final ModelPart[] brazos = new ModelPart[BRAZOS];
	private final ModelPart[] antes = new ModelPart[BRAZOS];

	public ModeloAmasijo(ModelPart raiz) {
		super(raiz);
		this.cuerpo = raiz.getChild("cuerpo");
		this.torso = this.cuerpo.getChild("torso");
		this.joroba = this.torso.getChild("joroba");
		for (int i = 0; i < PATAS; i++) {
			this.patas[i] = this.cuerpo.getChild("pata_" + i);
		}
		for (int i = 0; i < BRAZOS; i++) {
			this.brazos[i] = (i == 4 || i == 5 ? this.joroba : this.torso).getChild("brazo_" + i);
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
		// la cadera se bambolea al andar; el torso y la joroba la siguen con retraso (latigazo suave)
		this.cuerpo.y -= Math.abs(Mth.sin(p * 0.9F)) * 0.8F * v;
		this.cuerpo.zRot += Mth.sin(p * 0.45F) * 0.06F * v + Mth.sin(t * 0.05F) * 0.02F;
		this.torso.zRot += Mth.sin(p * 0.45F - 0.7F) * 0.07F * v;
		this.torso.xRot += Mth.sin(p * 0.9F + 0.6F) * 0.06F * v;
		this.joroba.zRot += Mth.sin(p * 0.45F - 1.4F) * 0.06F * v;
		this.joroba.xRot += Mth.sin(p * 0.9F + 1.2F) * 0.07F * v;
		// respira: una onda que sube por la columna
		this.torso.xRot += Mth.sin(t * 0.08F) * 0.03F;
		this.joroba.xRot += Mth.sin(t * 0.08F - 0.8F) * 0.05F;
		float hinchado = 1.0F + Mth.sin(t * 0.12F) * 0.02F;
		this.torso.xScale = hinchado;
		this.torso.zScale = hinchado;
		// cazando se encorva hacia delante, sobre todo por arriba
		this.cuerpo.xRot += 0.04F * c;
		this.torso.xRot += 0.08F * c;
		this.joroba.xRot += 0.1F * c;
		// al girar, el torso se adelanta al giro y la joroba un poco mas; la cadera va detras
		float giro = Mth.clamp(s.gira * 0.05F, -0.4F, 0.4F);
		this.torso.yRot += giro;
		this.joroba.yRot += giro * 0.6F;
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
			this.torso.xRot += 0.15F + Mth.sin(t * 1.4F) * 0.05F;
			this.joroba.xRot += 0.2F + Mth.sin(t * 1.4F - 0.5F) * 0.06F;
		}
		// el salto sobre la presa: se estira entero hacia delante, los brazos abiertos, las patas atras
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
			this.cuerpo.xRot -= 0.1F * e;
			this.torso.xRot -= 0.12F * e;
			this.joroba.xRot -= 0.1F * e;
		}
		// comiendo: la cadera baja con las patas abiertas y se queda casi quieta; el torso se encorva sobre la
		// presa y la joroba muerde y tira hacia atras a tirones, con la cabeza de lado a lado mientras mastica;
		// los brazos desgarran por parejas (bajan, agarran, tiran hacia arriba)
		float k = s.come;
		if (k > 0.0F) {
			float tiron = (float) Math.pow(Math.max(0.0F, Mth.sin(t * 0.32F)), 6.0);
			float tironJ = (float) Math.pow(Math.max(0.0F, Mth.sin(t * 0.32F - 0.5F)), 6.0);
			this.cuerpo.y += (1.6F + Mth.sin(t * 0.16F) * 0.5F) * k;
			this.cuerpo.xRot += 0.08F * k;
			for (int i = 0; i < PATAS; i++) {
				// las de delante hacia delante, las de detras hacia atras, todas un poco abiertas
				float abre = i < 2 ? -0.3F : i > 3 ? 0.3F : 0.0F;
				this.patas[i].xRot += (abre + Mth.sin(t * 0.32F + i) * 0.06F) * k;
				this.patas[i].zRot += (i % 2 == 0 ? 0.22F : -0.22F) * k;
			}
			this.torso.xRot += (0.34F + Mth.sin(t * 0.16F) * 0.08F - tiron * 0.18F) * k;
			this.torso.yRot += Mth.sin(t * 0.11F) * 0.14F * k;
			this.torso.zRot += tiron * Mth.sin(t * 2.1F) * 0.08F * k;
			this.joroba.xRot += (0.3F + Mth.sin(t * 0.16F - 0.7F) * 0.1F - tironJ * 0.45F) * k;
			this.joroba.yRot += Mth.sin(t * 0.11F - 0.9F) * 0.12F * k;
			this.joroba.zRot += tironJ * Mth.sin(t * 2.4F) * 0.1F * k;
			for (int i = 0; i < BRAZOS; i++) {
				float f = t * 0.24F + (i / 2) * 1.6F;
				float baja = 0.5F + 0.5F * Mth.sin(f);
				float desgarra = (float) Math.pow(Math.max(0.0F, Mth.sin(f + 1.2F)), 3.0);
				this.brazos[i].xRot = Mth.lerp(k, this.brazos[i].xRot, 0.7F + 0.5F * baja - 1.1F * desgarra);
				this.brazos[i].yRot = Mth.lerp(k, this.brazos[i].yRot, (i % 2 == 0 ? 0.25F : -0.25F) + Mth.sin(f * 0.7F) * 0.15F);
				this.antes[i].xRot = Mth.lerp(k, this.antes[i].xRot, 1.25F * baja + 0.3F - 0.6F * desgarra);
			}
		}
		// alerta (ha oido algo): se yergue tramo a tramo, ladea la joroba como escuchando y los brazos se
		// quedan quietos a medio alzar
		float al = s.alerta;
		if (al > 0.0F) {
			this.cuerpo.xRot -= 0.06F * al;
			this.cuerpo.y -= 1.5F * al;
			this.torso.xRot -= 0.22F * al;
			this.joroba.xRot -= 0.16F * al;
			this.joroba.zRot += 0.14F * al;
			for (int i = 0; i < BRAZOS; i++) {
				this.brazos[i].xRot = Mth.lerp(al, this.brazos[i].xRot, 0.2F + (i % 3) * 0.12F);
				this.antes[i].xRot = Mth.lerp(al, this.antes[i].xRot, 0.5F);
			}
		}
	}
}
