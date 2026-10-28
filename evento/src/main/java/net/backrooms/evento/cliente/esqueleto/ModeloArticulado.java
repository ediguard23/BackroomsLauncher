package net.backrooms.evento.cliente.esqueleto;

import java.util.EnumMap;
import java.util.Map;
import net.backrooms.evento.esqueleto.Hueso;
import net.backrooms.evento.esqueleto.Pose;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * El modelo del esqueleto articulado: un ModelPart por Hueso, colgados unos de otros como dice el hueso
 * (asi un giro del hombro mueve el antebrazo y la mano), con el pivote en la articulacion. Encima de todo,
 * CUERPO (en los pies) para tumbar o mover la figura entera. Cada fotograma se pone la Pose: primero
 * todo a cero y despues el giro de cada hueso.
 */
public class ModeloArticulado extends EntityModel<FiguraRenderer.Estado> {
	private final ModelPart cuerpo;
	private final Map<Hueso, ModelPart> partes = new EnumMap<>(Hueso.class);

	public ModeloArticulado(ModelPart raiz) {
		super(raiz);
		this.cuerpo = raiz.getChild("cuerpo");
		for (Hueso h : Hueso.TODOS) {
			this.partes.put(h, this.buscar(h));
		}
	}

	private ModelPart buscar(Hueso h) {
		if (h.padre == null) {
			return this.cuerpo.getChild(h.nombre());
		}
		return this.buscar(h.padre).getChild(h.nombre());
	}

	public static LayerDefinition crear() {
		MeshDefinition malla = new MeshDefinition();
		PartDefinition cuerpo = malla.getRoot().addOrReplaceChild("cuerpo", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		Map<Hueso, PartDefinition> defs = new EnumMap<>(Hueso.class);
		for (Hueso h : Hueso.TODOS) {
			PartDefinition padre = h.padre == null ? cuerpo : defs.get(h.padre);
			// el trozo de la piel: la columna de la parte entera, desde su primera fila
			CubeListBuilder c = CubeListBuilder.create().texOffs(h.u, h.v + h.fila).addBox(h.bx, h.by, h.bz, h.ancho, h.alto, h.fondo);
			if (h == Hueso.CABEZA) {
				// el sombrero de la piel (la capa de fuera de la cabeza)
				c.texOffs(32, 0).addBox(h.bx, h.by, h.bz, h.ancho, h.alto, h.fondo, new CubeDeformation(0.5F));
			}
			defs.put(h, padre.addOrReplaceChild(h.nombre(), c, PartPose.offset(h.rx(), h.ry(), h.rz())));
		}
		return LayerDefinition.create(malla, 64, 64);
	}

	@Override
	public void setupAnim(FiguraRenderer.Estado e) {
		super.setupAnim(e);
		Pose p = e.pose;
		if (p == null) {
			return;
		}
		this.cuerpo.xRot = p.cuerpoX;
		this.cuerpo.yRot = p.cuerpoY;
		this.cuerpo.zRot = p.cuerpoZ;
		// desplazamiento en bloques: y hacia arriba; z positivo hacia atras de la figura
		this.cuerpo.x += p.despX * 16.0F;
		this.cuerpo.y -= p.despY * 16.0F;
		this.cuerpo.z += p.despZ * 16.0F;
		for (Hueso h : Hueso.TODOS) {
			ModelPart m = this.partes.get(h);
			m.xRot = p.x(h);
			m.yRot = p.y(h);
			m.zRot = p.z(h);
			m.skipDraw = (e.ocultos & 1 << h.ordinal()) != 0;
		}
	}
}
