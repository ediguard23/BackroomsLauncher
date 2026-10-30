package net.backrooms.evento.cliente.entidad;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * El Anfitrion (malla en MallaAnfitrion, generada por tools/texturas/entidades.js): anda a
 * pasos largos y saltarines de mascota, con la cabezota ladeada y el gorrito temblando. Quieto,
 * se balancea y ladea la cabeza a un lado y a otro, como si escuchara. Cazando se inclina hacia
 * delante y lleva los brazos estirados; al abrazar los cierra. Dobla codos y rodillas (el
 * brazo y la pierna van en dos trozos: ver tools/texturas/entidades.js).
 */
public class ModeloAnfitrion extends EntityModel<EstadoAnfitrion> {
	private final ModelPart cadera;
	private final ModelPart cabeza;
	private final ModelPart gorro;
	private final ModelPart brazoIzq;
	private final ModelPart brazoDer;
	private final ModelPart piernaIzq;
	private final ModelPart piernaDer;
	/** Lo que cuelga de codos y rodillas (el esqueleto: ver tools/texturas/entidades.js). */
	private final ModelPart antebrazoIzq;
	private final ModelPart antebrazoDer;
	private final ModelPart espinillaIzq;
	private final ModelPart espinillaDer;

	public ModeloAnfitrion(ModelPart raiz) {
		super(raiz);
		this.cadera = raiz.getChild("cadera");
		this.cabeza = this.cadera.getChild("cabeza");
		this.gorro = this.cabeza.getChild("gorro");
		this.brazoIzq = this.cadera.getChild("brazo_izq");
		this.brazoDer = this.cadera.getChild("brazo_der");
		this.piernaIzq = this.cadera.getChild("pierna_izq");
		this.piernaDer = this.cadera.getChild("pierna_der");
		this.antebrazoIzq = this.brazoIzq.getChild("antebrazo_izq");
		this.antebrazoDer = this.brazoDer.getChild("antebrazo_der");
		this.espinillaIzq = this.piernaIzq.getChild("espinilla_izq");
		this.espinillaDer = this.piernaDer.getChild("espinilla_der");
	}

	@Override
	public void setupAnim(EstadoAnfitrion s) {
		super.setupAnim(s);
		float t = s.ageInTicks;
		float v = Mth.clamp(s.walkAnimationSpeed * 1.4F, 0.0F, 1.0F);
		float p = s.walkAnimationPos;
		float c = s.caza;
		// piernas y brazos a zancadas, y un saltito en cada paso
		float zancada = Mth.cos(p * 0.55F) * 0.9F * v;
		this.piernaIzq.xRot += zancada;
		this.piernaDer.xRot -= zancada;
		this.cadera.y -= Math.abs(Mth.sin(p * 0.55F)) * 1.4F * v;
		this.brazoIzq.xRot -= zancada * 0.8F * (1.0F - c);
		this.brazoDer.xRot += zancada * 0.8F * (1.0F - c);
		// las rodillas: se doblan al recoger la pierna (cuando va hacia delante) y apenas al apoyar
		float recoge = Mth.sin(p * 0.55F);
		this.espinillaIzq.xRot += (0.12F + 1.15F * Math.max(0.0F, recoge)) * v;
		this.espinillaDer.xRot += (0.12F + 1.15F * Math.max(0.0F, -recoge)) * v;
		// los codos: al correr, doblados y bombeando con el paso; quieto, casi rectos y colgando
		this.antebrazoIzq.xRot -= (0.15F + (0.55F - zancada * 0.25F) * v) * (1.0F - c);
		this.antebrazoDer.xRot -= (0.15F + (0.55F + zancada * 0.25F) * v) * (1.0F - c);
		// corriendo, el cuerpo hacia delante
		this.cadera.xRot += 0.1F * v;
		// quieto: se mece y ladea la cabeza, escuchando
		this.cadera.zRot += Mth.sin(t * 0.045F) * 0.03F;
		this.cabeza.zRot += (0.22F + Mth.sin(t * 0.021F) * 0.18F) * (1.0F - c * 0.6F);
		this.cabeza.yRot += Mth.clamp(s.yRot, -60.0F, 60.0F) * Mth.DEG_TO_RAD;
		this.cabeza.xRot += Mth.clamp(s.xRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD * 0.6F;
		this.gorro.zRot += Mth.sin(t * 0.3F) * 0.05F * (0.3F + v);
		this.brazoIzq.zRot -= Mth.sin(t * 0.05F) * 0.04F;
		this.brazoDer.zRot += Mth.sin(t * 0.05F + 1.0F) * 0.04F;
		// cazando: inclinado hacia delante y los brazos estirados hacia ti
		this.cadera.xRot += 0.18F * c;
		this.cabeza.xRot -= 0.15F * c;
		this.brazoIzq.xRot -= 1.35F * c + Mth.sin(t * 0.4F) * 0.08F * c;
		this.brazoDer.xRot -= 1.35F * c + Mth.sin(t * 0.4F + 1.5F) * 0.08F * c;
		// cazando, las manos como garras: los antebrazos un poco doblados, abriendo y cerrando
		this.antebrazoIzq.xRot -= (0.25F + Mth.sin(t * 0.55F) * 0.2F) * c;
		this.antebrazoDer.xRot -= (0.25F + Mth.sin(t * 0.55F + 1.2F) * 0.2F) * c;
		if (s.abrazando) {
			this.brazoIzq.xRot = -1.45F;
			this.brazoDer.xRot = -1.45F;
			this.brazoIzq.yRot = -0.55F;
			this.brazoDer.yRot = 0.55F;
			// el abrazo: los antebrazos se cierran alrededor
			this.antebrazoIzq.xRot = -0.2F;
			this.antebrazoDer.xRot = -0.2F;
			this.antebrazoIzq.yRot = -0.9F;
			this.antebrazoDer.yRot = 0.9F;
			this.cabeza.zRot = 0.35F + Mth.sin(t * 0.8F) * 0.1F;
		}
	}
}
