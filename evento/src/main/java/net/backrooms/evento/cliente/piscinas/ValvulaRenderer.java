package net.backrooms.evento.cliente.piscinas;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.backrooms.evento.objetos.Objetos;
import net.backrooms.evento.piscinas.ValvulaEntidad;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** El volante de una valvula (modelo del objeto volante_valvula), girando suave sobre su eje. */
public class ValvulaRenderer implements BlockEntityRenderer<ValvulaEntidad, ValvulaRenderer.Estado> {
	private static final ItemStack VOLANTE = new ItemStack(Objetos.VOLANTE_VALVULA);
	private final ItemModelResolver modelos;

	public ValvulaRenderer(BlockEntityRendererProvider.Context ctx) {
		this.modelos = ctx.itemModelResolver();
	}

	public static class Estado extends BlockEntityRenderState {
		public final ItemStackRenderState volante = new ItemStackRenderState();
		public float angulo;
		public Direction mira = Direction.NORTH;
	}

	@Override
	public Estado createRenderState() {
		return new Estado();
	}

	@Override
	public void extractRenderState(ValvulaEntidad v, Estado estado, float parcial, Vec3 camara, ModelFeatureRenderer.@Nullable CrumblingOverlay rotura) {
		BlockEntityRenderer.super.extractRenderState(v, estado, parcial, camara, rotura);
		this.modelos.updateForTopItem(estado.volante, VOLANTE, ItemDisplayContext.NONE, v.getLevel(), null, 0);
		estado.angulo = v.angulo(parcial);
		estado.mira = v.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
	}

	@Override
	public void submit(Estado estado, PoseStack pose, SubmitNodeCollector nodos, CameraRenderState camara) {
		pose.pushPose();
		pose.translate(0.5F, 0.5F, 0.5F);
		// el modelo mira al norte; los demas, girados como en el blockstate
		pose.mulPose(Axis.YP.rotationDegrees(-estado.mira.toYRot() + 180.0F));
		pose.mulPose(Axis.ZP.rotationDegrees(estado.angulo));
		estado.volante.submit(pose, nodos, estado.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		pose.popPose();
	}
}
