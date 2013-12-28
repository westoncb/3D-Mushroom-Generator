package com.cyntaks.botgame;

import org.lwjgl.opengl.GL11;

public class RenderState {
	public static final int RENDER_MODE_LINE = GL11.GL_LINE;
	public static final int RENDER_MODE_POINT = GL11.GL_POINT;
	public static final int RENDER_MODE_FILL = GL11.GL_FILL;
	
	public static final int BLEND_MODE_ADDITIVE = 0;
	public static final int BLEND_MODE_NORMAL = 1;
	
	public static boolean usingBlending = false;
	public static boolean renderingBackfaces = true;
	public static boolean usingLighting = false;
	public static int polygonRenderingMode = RENDER_MODE_FILL;
	public static int blendMode = BLEND_MODE_NORMAL;
	
	public static void initialize() {
		if (RenderState.usingLighting)
			GL11.glEnable(GL11.GL_LIGHTING);
		else
			GL11.glDisable(GL11.GL_LIGHTING);
		
		if (RenderState.usingBlending)
			GL11.glEnable(GL11.GL_BLEND);	
		else
			GL11.glDisable(GL11.GL_BLEND);
		
		if (RenderState.blendMode == RenderState.BLEND_MODE_ADDITIVE)
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		else if (RenderState.blendMode == RenderState.BLEND_MODE_NORMAL)
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		if (RenderState.renderingBackfaces)
			GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, RenderState.polygonRenderingMode);
		else
			GL11.glPolygonMode(GL11.GL_FRONT, RenderState.polygonRenderingMode);
	}
	
	public static void enableBlending() {
		if (!RenderState.usingBlending)
			GL11.glEnable(GL11.GL_BLEND);
		
		RenderState.usingBlending = true;
	}
	
	public static void disableBlending() {
		if (RenderState.usingBlending)
			GL11.glDisable(GL11.GL_BLEND);
		
		RenderState.usingBlending = false;
	}
	
	public static void enableLighting() {
		if (!RenderState.usingLighting)
			GL11.glEnable(GL11.GL_LIGHTING);
		
		RenderState.usingLighting = true;
	}
	
	public static void disableLighting() {
		if (RenderState.usingLighting)
			GL11.glDisable(GL11.GL_LIGHTING);
		
		RenderState.usingLighting = false;
	}
	
	public static void setPolygonRenderingMode(boolean renderBackfaces, int renderMode) {
		if (renderMode != RenderState.polygonRenderingMode || renderBackfaces != RenderState.renderingBackfaces) {
			if (renderBackfaces)
				GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, renderMode);
			else
				GL11.glPolygonMode(GL11.GL_FRONT, renderMode);
		}
		
		RenderState.renderingBackfaces = renderBackfaces;
		RenderState.polygonRenderingMode = renderMode;
	}
	
	public static void setBlendingMode(int blendMode) {
		if (blendMode != RenderState.blendMode) {
			if (blendMode == RenderState.BLEND_MODE_ADDITIVE)
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			else if (blendMode == RenderState.BLEND_MODE_NORMAL)
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		}
		
		RenderState.blendMode = blendMode;
	}
}
