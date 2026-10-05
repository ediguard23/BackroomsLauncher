package net.backrooms.evento.cliente.efectos;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.evento.cliente.AmbienteCliente;
import net.backrooms.evento.cliente.HerramientasCliente;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Pasada propia sobre la imagen del mundo, despues de dibujarlo y antes de la
 * mano (GameRendererEfectosMixin). Solo trabaja cuando hace falta:
 *
 *  - Apagon: todo negro menos lo que alumbran las linternas (la propia y las
 *    de los jugadores cercanos). Con la profundidad se reconstruye la
 *    posicion de cada pixel y se ilumina con los focos; las caras de los
 *    Smilers brillan solas en la oscuridad (BRILLOS).
 *  - Alarma: luz roja que late.
 *  - Camara levantada: imagen de videocamara (grano, lineas, aberracion) y,
 *    a oscuras, vision nocturna verde de corto alcance.
 *  - Cordura baja: la imagen ondula, se separa en colores y se cierra.
 *  - Miedo (una Bacteria cazandote cerca): los bordes se cierran y laten con
 *    el corazon (Miedo).
 *
 * Despues de la mano, otra pasada (MANO) la oscurece igual que el mundo:
 * si no, en pleno apagon la mano se veria iluminada.
 */
public final class EfectosMundo {
	private static final int MAX_LUCES = 16;
	private static final int MAX_BRILLOS = 8;
	private static final int TAMANO_UBO = 64 + 16 * 4 + 16 * MAX_LUCES * 2 + 16 * MAX_BRILLOS;

	private static final RenderPipeline MUNDO = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
			.withLocation(BackroomsEvento.id("pipeline/efectos_mundo"))
			.withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
			.withFragmentShader(BackroomsEvento.id("core/efectos"))
			.withSampler("ColorSampler")
			.withSampler("DepthSampler")
			.withUniform("Efectos", UniformType.UNIFORM_BUFFER)
			.build());
	private static final RenderPipeline MANO = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
			.withLocation(BackroomsEvento.id("pipeline/efectos_mano"))
			.withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
			.withFragmentShader(BackroomsEvento.id("core/efectos_mano"))
			.withSampler("DepthSampler")
			.withUniform("Efectos", UniformType.UNIFORM_BUFFER)
			.withBlend(new BlendFunction(SourceFactor.ZERO, DestFactor.SRC_COLOR))
			.build());

	private static GpuTexture copia;
	private static GpuTextureView copiaVista;
	private static MappableRingBuffer ubo;
	private static boolean pendienteMano;
	private static final List<float[]> brillos = new ArrayList<>();

	private EfectosMundo() {
	}

	/** Carga la clase al arrancar para que sus shaders se compilen con los demas. */
	public static void cargar() {
	}

	/** Las caras de los Smilers a la vista este frame: {x, y, z, radio} absolutos. Las rellena su renderer. */
	public static void brillo(double x, double y, double z, float radio) {
		if (brillos.size() < MAX_BRILLOS) {
			brillos.add(new float[] {(float) x, (float) y, (float) z, radio});
		}
	}

	private static boolean activo() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && (AmbienteCliente.oscuridad() > 0.001F || AmbienteCliente.rojo() > 0.001F
			|| HerramientasCliente.subida(1.0F) > 0.01F || Cordura.efecto() > 0.001F || Flash.blanco() > 0.001F
			|| Miedo.valor(1.0F) > 0.001F);
	}

	/** Despues del mundo, antes de la mano. */
	public static void mundo(Matrix4f proyeccion, Matrix4f vista, Camera camara, float parcial) {
		pendienteMano = false;
		if (!activo()) {
			brillos.clear();
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		RenderTarget rt = mc.getMainRenderTarget();
		if (rt.getColorTexture() == null || rt.getDepthTextureView() == null) {
			return;
		}
		int w = rt.width;
		int h = rt.height;
		if (copia == null || copia.getWidth(0) != w || copia.getHeight(0) != h) {
			if (copia != null) {
				copiaVista.close();
				copia.close();
			}
			copia = RenderSystem.getDevice().createTexture(() -> "Backrooms efectos", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
				TextureFormat.RGBA8, w, h, 1, 1);
			copiaVista = RenderSystem.getDevice().createTextureView(copia);
		}
		if (ubo == null) {
			ubo = new MappableRingBuffer(() -> "Backrooms efectos UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, TAMANO_UBO);
		}
		CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
		enc.copyTextureToTexture(rt.getColorTexture(), copia, 0, 0, 0, 0, 0, w, h);
		escribirUbo(enc, proyeccion, vista, camara, parcial, w, h);
		try (RenderPass p = enc.createRenderPass(() -> "Backrooms efectos", rt.getColorTextureView(), OptionalInt.empty())) {
			p.setPipeline(MUNDO);
			RenderSystem.bindDefaultUniforms(p);
			p.setUniform("Efectos", ubo.currentBuffer());
			p.bindTexture("ColorSampler", copiaVista, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			p.bindTexture("DepthSampler", rt.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			p.draw(0, 3);
		}
		pendienteMano = true;
		brillos.clear();
	}

	/** Despues de la mano: la oscurece como al resto. */
	public static void mano() {
		if (!pendienteMano) {
			return;
		}
		pendienteMano = false;
		Minecraft mc = Minecraft.getInstance();
		RenderTarget rt = mc.getMainRenderTarget();
		if (rt.getDepthTextureView() != null) {
			CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
			try (RenderPass p = enc.createRenderPass(() -> "Backrooms efectos mano", rt.getColorTextureView(), OptionalInt.empty())) {
				p.setPipeline(MANO);
				RenderSystem.bindDefaultUniforms(p);
				p.setUniform("Efectos", ubo.currentBuffer());
				p.bindTexture("DepthSampler", rt.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
				p.draw(0, 3);
			}
		}
		ubo.rotate();
	}

	private static void escribirUbo(CommandEncoder enc, Matrix4f proyeccion, Matrix4f vista, Camera camara, float parcial, int w, int h) {
		Minecraft mc = Minecraft.getInstance();
		Matrix4f inversa = new Matrix4f(proyeccion).mul(vista).invert();
		Vec3 cam = camara.position();
		float tiempo = (net.minecraft.util.Util.getMillis() % 3_600_000L) / 1000.0F;
		float camaraSubida = HerramientasCliente.subida(parcial);
		float oscuridad = AmbienteCliente.oscuridad();

		// las linternas: la propia sale de la mano derecha; las de los demas, de sus ojos
		List<float[]> luces = new ArrayList<>();
		Player yo = mc.player;
		if (HerramientasCliente.linterna() && yo != null) {
			Vec3 ojo = yo.getEyePosition(parcial);
			Vec3 dir = yo.getViewVector(parcial);
			Vec3 derecha = dir.cross(new Vec3(0, 1, 0)).normalize();
			Vec3 o = mc.options.getCameraType().isFirstPerson()
				? cam.add(derecha.scale(0.18)).add(0, -0.12, 0)
				: ojo.add(derecha.scale(0.25)).add(0, -0.3, 0);
			luces.add(luz(o.subtract(cam), dir, 1.0F));
		}
		List<Player> otros = new ArrayList<>();
		if (mc.level != null) {
			for (int id : HerramientasCliente.linternasCerca()) {
				Entity e = mc.level.getEntity(id);
				if (e instanceof Player p && p != yo && !p.isRemoved()) {
					otros.add(p);
				}
			}
		}
		otros.sort(Comparator.comparingDouble(p -> p.distanceToSqr(cam)));
		for (Player p : otros) {
			if (luces.size() >= MAX_LUCES) {
				break;
			}
			Vec3 dir = p.getViewVector(parcial);
			Vec3 derecha = dir.cross(new Vec3(0, 1, 0)).normalize();
			Vec3 o = p.getEyePosition(parcial).add(derecha.scale(0.25)).add(0, -0.3, 0);
			luces.add(luz(o.subtract(cam), dir, 0.95F));
		}

		try (GpuBuffer.MappedView m = enc.mapBuffer(ubo.currentBuffer(), false, true)) {
			Std140Builder b = Std140Builder.intoBuffer(m.data());
			b.putMat4f(inversa);
			b.putVec4(oscuridad, AmbienteCliente.rojo(), tiempo, Cordura.efecto());
			b.putVec4(camaraSubida, HerramientasCliente.linterna() ? 1.0F : 0.0F, Flash.blanco(), luces.size());
			b.putVec4(brillos.size(), (float) w / h, Cordura.susto(), 0.0F);
			b.putVec4(Miedo.valor(parcial), Miedo.latido(), 0.0F, 0.0F);
			for (int i = 0; i < MAX_LUCES; i++) {
				float[] l = i < luces.size() ? luces.get(i) : new float[8];
				b.putVec4(l[0], l[1], l[2], l[3]);
				b.putVec4(l[4], l[5], l[6], l[7]);
			}
			for (int i = 0; i < MAX_BRILLOS; i++) {
				if (i < brillos.size()) {
					float[] br = brillos.get(i);
					b.putVec4((float) (br[0] - cam.x), (float) (br[1] - cam.y), (float) (br[2] - cam.z), br[3]);
				} else {
					b.putVec4(0, 0, 0, 0);
				}
			}
		}
	}

	/** {x, y, z relativos a la camara, intensidad, direccion, coseno del cono}. */
	private static float[] luz(Vec3 pos, Vec3 dir, float intensidad) {
		Vector3f d = new Vector3f((float) dir.x, (float) dir.y, (float) dir.z).normalize();
		return new float[] {(float) pos.x, (float) pos.y, (float) pos.z, intensidad, d.x, d.y, d.z, 0.86F};
	}
}
