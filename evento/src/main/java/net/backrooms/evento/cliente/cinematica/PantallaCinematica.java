package net.backrooms.evento.cliente.cinematica;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import net.backrooms.evento.BackroomsEvento;
import net.backrooms.menu.render.Texto;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Pantalla que dibuja la cinematica (ver CinematicaCliente para el guion).
 * Bloquea el control y Esc no la cierra; se va sola al terminar.
 *
 * La cinta del ascensor es un shader a pantalla completa (cinematica.fsh) con
 * el HUD de la videocamara encima; al despertar se ve el mundo de verdad
 * desenfocado (el blur de Minecraft con el radio animado) tras los parpados
 * (parpados.fsh).
 */
public class PantallaCinematica extends Screen {
	private static final RenderPipeline CINTA = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
			.withLocation(BackroomsEvento.id("pipeline/cinematica"))
			.withVertexShader(Identifier.fromNamespaceAndPath("backrooms", "core/fondo"))
			.withFragmentShader(BackroomsEvento.id("core/cinematica"))
			.withSampler("Sampler0")
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withDepthWrite(false)
			.build());
	private static final RenderPipeline PARPADOS = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
			.withLocation(BackroomsEvento.id("pipeline/parpados"))
			.withVertexShader(Identifier.fromNamespaceAndPath("backrooms", "core/fondo"))
			.withFragmentShader(BackroomsEvento.id("core/parpados"))
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
			.withBlend(BlendFunction.TRANSLUCENT)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withDepthWrite(false)
			.build());

	/** Abertura de los ojos al despertar: {segundo desde DESPERTAR, abertura}. */
	private static final float[][] OJOS = {
		{0.0F, 0.0F}, {1.0F, 0.0F}, {2.0F, 0.22F}, {2.4F, 0.17F}, {2.8F, 0.0F}, {3.4F, 0.0F},
		{4.5F, 0.5F}, {5.1F, 0.42F}, {5.4F, 0.1F}, {5.8F, 0.3F}, {7.2F, 0.85F}, {8.4F, 1.0F}};

	private final String fecha;

	/** Carga la clase al arrancar para que sus shaders se registren con los demas. */
	public static void cargar() {
	}

	public PantallaCinematica() {
		super(Component.literal("Backrooms"));
		LocalDateTime ahora = LocalDateTime.now();
		String mes = ahora.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ROOT);
		this.fecha = String.format(Locale.ROOT, "%s. %02d %d", mes, ahora.getDayOfMonth(), ahora.getYear());
	}

	@Override
	protected void init() {
		GLFW.glfwSetInputMode(this.minecraft.getWindow().handle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
	}

	/**
	 * Devuelve el cursor que oculta init(). Minecraft solo lo vuelve a ensenar al soltar el
	 * raton capturado, y con esta pantalla abierta no lo esta: si algo la cierra antes de
	 * tiempo (una desconexion, la pantalla de carga al cambiar de mundo) el cursor seguia
	 * invisible en SenalPerdida y en todos los menus. setScreen llama a removed() antes de
	 * capturar el raton, asi que al volver a la partida se captura como siempre.
	 */
	@Override
	public void removed() {
		long ventana = this.minecraft.getWindow().handle();
		if (GLFW.glfwGetInputMode(ventana, GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_HIDDEN) {
			GLFW.glfwSetInputMode(ventana, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void tick() {
		if (CinematicaCliente.segundos() >= CinematicaCliente.FIN) {
			CinematicaCliente.terminar();
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		float t = CinematicaCliente.segundos();
		if (t < 0) {
			return;
		}
		int w = this.width;
		int h = this.height;
		if (t < CinematicaCliente.FIN_CINTA) {
			g.guiRenderState.submitGuiElement(new Capa(CINTA, new Matrix3x2f(g.pose()), w, h, t, 0.0F,
				TextureSetup.singleTexture(AtlasCinematica.vista(), AtlasCinematica.muestreo())));
			this.hudCamara(g, t);
		} else if (t < CinematicaCliente.DESPERTAR) {
			g.fill(0, 0, w, h, 0xFF000000);
			// lo ultimo que saca la camara rota
			if (t < CinematicaCliente.FIN_CINTA + 1.4F && (int) (t * 4) % 2 == 0) {
				Texto.hud(g, "SIN SEÑAL", w / 2.0F - Texto.anchoHud("SIN SEÑAL", h / 16.0F, 0.1F) / 2.0F, h / 2.0F - h / 32.0F, h / 16.0F, 0.1F, 0x55E8E0C8);
			}
		} else {
			if (CinematicaCliente.desenfoque() > 0) {
				g.blurBeforeThisStratum();
			}
			float u = t - CinematicaCliente.DESPERTAR;
			g.guiRenderState.submitGuiElement(new Capa(PARPADOS, new Matrix3x2f(g.pose()), w, h, abertura(u), t, TextureSetup.noTexture()));
			this.titulo(g, t);
		}
	}

	/** Lo que pinta la videocamara encima de la imagen. */
	private void hudCamara(GuiGraphics g, float t) {
		if (t < 1.3F || t > 39.1F) {
			return;
		}
		int w = this.width;
		int h = this.height;
		float tam = h / 16.0F;
		float m = h / 20.0F;
		// al caer, el HUD tiembla con la imagen
		float dx = t > 26.5F ? (float) Math.sin(t * 53.0) * h / 160.0F : 0.0F;
		float dy = t > 26.5F ? (float) Math.cos(t * 41.0) * h / 200.0F : 0.0F;
		int blanco = 0xFFF2EEE4;
		if ((int) (t * 1.8F) % 2 == 0) {
			g.fill(Math.round(m + dx), Math.round(m + dy + tam * 0.25F), Math.round(m + dx + tam * 0.55F), Math.round(m + dy + tam * 0.8F), 0xFFE5281E);
		}
		Texto.hud(g, "REC", m + dx + tam * 0.8F, m + dy, tam, 0.08F, blanco);
		int s = 47 * 60 + 13 + (int) t;
		String contador = String.format(Locale.ROOT, "0:%02d:%02d", s / 60, s % 60);
		Texto.hud(g, contador, w - m + dx - Texto.anchoHud(contador, tam, 0.08F), m + dy, tam, 0.08F, blanco);
		// bateria: al caer se queda en la ultima raya, parpadeando
		float bx = w - m + dx - tam * 1.6F;
		float by = m + dy + tam * 1.3F;
		g.renderOutline(Math.round(bx), Math.round(by), Math.round(tam * 1.3F), Math.round(tam * 0.6F), blanco);
		int rayas = t < 20.0F ? 3 : t < 26.5F ? 2 : ((int) (t * 3) % 2 == 0 ? 1 : 0);
		for (int i = 0; i < rayas; i++) {
			float x0 = bx + tam * (0.12F + i * 0.39F);
			g.fill(Math.round(x0), Math.round(by + tam * 0.12F), Math.round(x0 + tam * 0.3F), Math.round(by + tam * 0.48F), blanco);
		}
		Texto.hud(g, "PM 8:59", m + dx, h - m + dy - tam * 2.1F, tam, 0.08F, blanco);
		Texto.hud(g, this.fecha, m + dx, h - m + dy - tam, tam, 0.08F, blanco);
		Texto.hud(g, "SP", w - m + dx - Texto.anchoHud("SP", tam, 0.08F), h - m + dy - tam, tam, 0.08F, blanco);
	}

	/** "NIVEL 0" mientras se te aclara la vista. */
	private void titulo(GuiGraphics g, float t) {
		float a = Mth.clamp((t - 50.5F) / 1.0F, 0.0F, 1.0F) * Mth.clamp((CinematicaCliente.FIN - t) / 1.2F, 0.0F, 1.0F);
		if (a <= 0.01F) {
			return;
		}
		int alfa = Math.round(a * 230) << 24;
		float tam = this.height / 7.0F;
		String nivel = "NIVEL 0";
		float x = this.width / 2.0F - Texto.anchoHud(nivel, tam, 0.18F) / 2.0F;
		float y = this.height * 0.62F;
		Texto.hud(g, nivel, x, y, tam, 0.18F, alfa | 0xE8D9A0);
		String sub = "LOS CUARTOS AMARILLOS";
		float ts = tam * 0.28F;
		Texto.hud(g, sub, this.width / 2.0F - Texto.anchoHud(sub, ts, 0.3F) / 2.0F, y + tam * 1.05F, ts, 0.3F, alfa | 0xC8BC90);
	}

	static float abertura(float u) {
		for (int i = 1; i < OJOS.length; i++) {
			if (u < OJOS[i][0]) {
				float k = (u - OJOS[i - 1][0]) / (OJOS[i][0] - OJOS[i - 1][0]);
				k = k * k * (3 - 2 * k);
				return Mth.lerp(k, OJOS[i - 1][1], OJOS[i][1]);
			}
		}
		return 1.0F;
	}

	/** Un rectangulo a pantalla completa para un shader; U y V llevan sus datos. */
	private record Capa(RenderPipeline pipeline, Matrix3x2f pose, int w, int h, float u, float v, TextureSetup textureSetup)
		implements GuiElementRenderState {
		@Override
		public void buildVertices(VertexConsumer c) {
			c.addVertexWith2DPose(this.pose, 0, 0).setUv(this.u, this.v).setColor(0xFFFFFFFF);
			c.addVertexWith2DPose(this.pose, 0, this.h).setUv(this.u, this.v).setColor(0xFFFFFFFF);
			c.addVertexWith2DPose(this.pose, this.w, this.h).setUv(this.u, this.v).setColor(0xFFFFFFFF);
			c.addVertexWith2DPose(this.pose, this.w, 0).setUv(this.u, this.v).setColor(0xFFFFFFFF);
		}

		@Override
		public @Nullable ScreenRectangle scissorArea() {
			return null;
		}

		@Override
		public @Nullable ScreenRectangle bounds() {
			return new ScreenRectangle(0, 0, this.w, this.h).transformMaxBounds(this.pose);
		}
	}
}
