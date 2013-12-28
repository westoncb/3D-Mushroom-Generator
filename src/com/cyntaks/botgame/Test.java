package com.cyntaks.botgame;

import java.nio.FloatBuffer;

import org.lwjgl.LWJGLException;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.*;
import org.lwjgl.Sys;
import org.lwjgl.util.glu.*;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;

public class Test {
	private static final int X = 0;
	private static final int Y = 1;
	private static final int Z = 2;
	
	private long lastFrameTime;
	private float screenWidth = 1024;
	private float screenHeight = 768;
	
	private boolean lightOkToPause = true;;
	private float lightRotation = 0;
	private boolean lightRotationPausing;
	private int lightRotationPauseTimePassed;
	private int lightRotationPauseLength = 1000*25; 
	private float rotation = 0;
	private float rotationVelocity;
	private float zTranslation = 0;
	private float zTranslationVelocity = 0;
	
	private boolean mPressed;
	private boolean aPressed;
	private boolean pPressed;
	private boolean wPressed;
	
	private static boolean useAmbientLight;
	
	public static boolean MODE1 = true;
	public static boolean wireframe;
	
	private Mushroom mainMush;
	
	private ArrayList<Mushroom> mushrooms;
	
	private float[][] floor;
	
	public static void main(String[] args) 
	{
		Test test = new Test();
		test.start();
	}
	
	public void start() {
		try {
			Display.setDisplayMode(new DisplayMode((int)screenWidth, (int)screenHeight));
			Display.create();
		} catch (LWJGLException ex) {
			ex.printStackTrace();
			System.exit(0);
		}
		
		RenderState.initialize();
		initGL();
		
		createFloor();
		createMushrooms();
		
		
		while (!Display.isCloseRequested()) {
			int delta = getDelta();
			update(delta);
			render();
			
			Display.update();
			Display.sync(60);
		}
	}
	
	private void createMushrooms() {
		float mainMushZOffset = 100;
		
		mainMush = new Mushroom(20, 50, 70, 15);
		mainMush.continuallyTransition = true;
		mainMush.setScale(0.01f);
		mainMush.incrementScale = true;
		mainMush.zTranslate = mainMushZOffset;
		mainMush.transitionToNextMushroom(8000);
		mainMush.setHeadShading(0.8f, 0.2f);
		mainMush.setStemShading(0.7f, 0.1f);
		mainMush.polygonRenderMode = RenderState.RENDER_MODE_FILL;
		if (!MODE1) {
			mainMush.calculateNormals = true;
			mainMush.useLighting = true;
		}
		
		
		mushrooms = new ArrayList<Mushroom>();
		for (int i = 0; i < 25; i++) {
			Mushroom mush = new Mushroom(10, 45, 45, 10);
			mush.continuallyTransition = true;
			mush.setHeadShading(0.5f, 0.2f);
			mush.setStemShading(0.5f, 0.1f);
			mush.yTranslate = -5.0f;
			mush.useLighting = true;
			mush.calculateNormals = true;
			
			if (MODE1)
				mush.transitionToNextMushroom((int)(Math.random()*4000 + 5000));
			else
				mush.transitionToNextMushroom((int)(Math.random()*1500 + 500));
			do {
				mush.xTranslate = (float)Math.random()*200 - 100;
				mush.zTranslate = (float)Math.random()*200 - 100;
			} while (Math.sqrt(mush.xTranslate*mush.xTranslate + (mush.zTranslate-mainMushZOffset)*(mush.zTranslate - mainMushZOffset)) < 30);
			
			mush.setScale(0.15f);
			
			if (!MODE1) {
				mush.useBlending = true;
				mush.blendMode = RenderState.BLEND_MODE_ADDITIVE;
				mush.polygonRenderMode = RenderState.RENDER_MODE_POINT;
			} else {
				mush.useLighting = true;
			}
			
			if (MODE1)
				mush.setColor(1, 1, 1, 1);
			else {
				mush.setHeadShading(1, 1);
				mush.setStemShading(1, 1);
				mush.setColor(0.4f, 0.8f, 0.4f, 0.1f);
			}
			
			mushrooms.add(mush);
		}
	}
	
	private void createFloor() {
		floor = new float[800][3];
		
		for (int i = 0; i < floor.length; i+= 4) {
			int row = (i/4) / (20);
			int col = (i/4) % (20);
			int tileSize = 20;
			
			float y = 0;
			float xMod = -170;
			float zMod = -60;
			
			floor[i][X] = col*tileSize + xMod + tileSize;
			floor[i][Y] = y;
			floor[i][Z] = row*tileSize + zMod + tileSize;
			
			floor[i+1][X] = col*tileSize + xMod + tileSize;
			floor[i+1][Y] = y;
			floor[i+1][Z] = row*tileSize + zMod;
			
			floor[i+2][X] = col*tileSize + xMod;
			floor[i+2][Y] = y;
			floor[i+2][Z] = row*tileSize + zMod;
			
			floor[i+3][X] = col*tileSize + xMod;
			floor[i+3][Y] = y;
			floor[i+3][Z] = row*tileSize + zMod + tileSize;
		}
	}
	
	private void render() {
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glLoadIdentity();
		GL11.glTranslated(0, -15.0f, -200.0f + zTranslation);
		 
		GL11.glRotatef(5, 1, 0, 0);
		GL11.glTranslated(0, 0, 100);
		GL11.glRotatef(rotation, 0, 1, 0);
		GL11.glTranslated(0, 0, -100);
		
		mainMush.setColor(1, 1, 1, 1);
		mainMush.useBlending = false;
		mainMush.useLighting = true;
		mainMush.renderBackfaces = true;
		if (wireframe)
			mainMush.polygonRenderMode = RenderState.RENDER_MODE_LINE;
		else
			mainMush.polygonRenderMode = RenderState.RENDER_MODE_FILL;
		
		mainMush.render();
		
		if (MODE1) {
			mainMush.useBlending = true;
			mainMush.blendMode = RenderState.BLEND_MODE_ADDITIVE;
			mainMush.renderBackfaces = true;
			mainMush.polygonRenderMode = RenderState.RENDER_MODE_POINT;
			mainMush.useLighting = false;
			mainMush.setColor(0, 1, 0, 0.25f);
			mainMush.render();
		}
		
		for (Mushroom mush : mushrooms) {	
			mush.render();
		}
		
		renderFloor();
		configureLights();
	}
	
	private void renderFloor() {
		RenderState.enableLighting();
		RenderState.disableBlending();
		GL11.glColor4f(0.75f, 0.75f, 0.75f, 1);
		RenderState.setPolygonRenderingMode(true, RenderState.RENDER_MODE_LINE);
		
		GL11.glBegin(GL11.GL_QUADS);
			for (int i = 0; i < floor.length; i++) {
				float x = floor[i][X];
				float y = floor[i][Y];
				float z = floor[i][Z];
				
				if (i < floor.length - 3) {
					float[] normal = Test.getNormal(floor[i], floor[i + 1], floor[i + 2]);
					GL11.glNormal3f(normal[X], normal[Y], normal[Z]);
				}
				
				GL11.glVertex3f(x, y, z);
			}
		GL11.glEnd();
	}
	
	private void configureLights() {
		FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4);
		lightPosition.put(0.2f).put(0.2f).put(0.2f).put(0.0f).flip();
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, lightPosition);
		
		GL11.glRotatef(270, 1, 0, 0);
		FloatBuffer light2Position = BufferUtils.createFloatBuffer(4);
		light2Position.put(100).put(60.0f).put(160.0f).put(1.0f).flip();
		GL11.glLight(GL11.GL_LIGHT1, GL11.GL_POSITION, light2Position);
	}
	
	private void update(float delta) {
		handleInput();
		
		if (!lightRotationPausing) {
			lightRotation += Math.PI*2/25.0f;
			int periodicLightRotation = ((int)lightRotation)%360;
			
			if (lightOkToPause && periodicLightRotation < 20) {
				lightRotationPausing = true;
				lightOkToPause = false;
			}
			
			if (!lightOkToPause && periodicLightRotation > 200) {
				lightOkToPause = true;
			}
				
		}
		else {
			this.lightRotationPauseTimePassed += delta;
			if (this.lightRotationPauseTimePassed > this.lightRotationPauseLength) {
				this.lightRotationPausing = false;
				this.lightRotationPauseTimePassed = 0;
			}
		}
		
		rotation += rotationVelocity;
		zTranslation += zTranslationVelocity;
		
		mainMush.update(delta);
		for (Mushroom mush : mushrooms) {
			mush.update(delta);
		}
	}
	
	private void handleInput() {
		if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			this.rotationVelocity = (float)Math.PI*2/5.0f;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			this.rotationVelocity = (float)-Math.PI*2/5.0f;
		} else
			this.rotationVelocity = 0;
		
		if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
			this.zTranslationVelocity = 2;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
			this.zTranslationVelocity = -1;
		} else
			this.zTranslationVelocity = 0;
		
		if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
			if (!this.wPressed) {
				this.wPressed = true;
				wireframe = !wireframe;
			}
		} else
			this.wPressed = false;
		
		if(Keyboard.isKeyDown(Keyboard.KEY_P)) {
			if (!this.pPressed) {
				this.pPressed = true;
				mainMush.continuallyTransition = !mainMush.continuallyTransition;
				mainMush.incrementScale = !mainMush.incrementScale;
				
				if (mainMush.continuallyTransition)
					mainMush.transitioning = true;
				else
					mainMush.transitioning = false;
			}
		} else
			this.pPressed = false;
		
		if(Keyboard.isKeyDown(Keyboard.KEY_M) && !this.mPressed) {
			this.mPressed = true;
			//do thing
		} else
			this.mPressed = false;
		
		if(Keyboard.isKeyDown(Keyboard.KEY_A)) {
			if (!this.aPressed) {
				this.aPressed = true;
				if (!useAmbientLight) {
					GL11.glEnable(GL11.GL_LIGHT0);
					useAmbientLight = true;
				} else {
					GL11.glDisable(GL11.GL_LIGHT0);
					useAmbientLight = false;
				}
			}
		} else
			this.aPressed = false;
	} 
	
	//p1, p2, p3 - Vertices of triangle
	public static float[] getNormal(float[] p1, float[] p2, float[] p3) {

	    //Create normal vector we are going to output.
	    float[] output = new float[3];

	    //Calculate vectors used for creating normal (these are the edges of the triangle).
	    float[] calU = {p2[X]-p1[X], p2[Y]-p1[Y], p2[Z]-p1[Z]};
	    float[] calV = {p3[X]-p1[X], p3[Y]-p1[Y], p3[Z]-p1[Z]};

	    //The output vector is equal to the cross products of the two edges of the triangle
	    output[X] = calU[Y]*calV[Z] - calU[Z]*calV[Y];
	    output[Y] = calU[Z]*calV[X] - calU[X]*calV[Z];
	    output[Z] = calU[X]*calV[Y] - calU[Y]*calV[X];

	    //Return the resulting vector.
	    float length = (float)Math.sqrt(output[X]*output[X] + output[Y]*output[Y] + output[Z]*output[Z]);
	    if (!useAmbientLight)
	    	length /= 1.2f;
	    
	    output[X] /= length;
	    output[Y] /= length;
	    output[Z] /= length;
	    
	    return output;
	}
	
	private void initGL() {
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GLU.gluPerspective(45.0f, screenWidth/screenHeight, 0.1f, 1400.0f);
		
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		
		GL11.glClearColor(0, 0, 0, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glClearDepth(1.0f);
		GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
		
		GL11.glShadeModel(GL11.GL_SMOOTH);
		
		FloatBuffer lightDiffuse= BufferUtils.createFloatBuffer(4);
		lightDiffuse.put(0.0f).put(0).put(0.01f).put(0).flip();
		
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_AMBIENT, lightDiffuse);
		
		FloatBuffer light2Diffuse= BufferUtils.createFloatBuffer(4);
		light2Diffuse.put(0.15f).put(0.25f).put(1.0f).put(0.0f).flip();
		GL11.glLight(GL11.GL_LIGHT1, GL11.GL_DIFFUSE, light2Diffuse);
		GL11.glEnable(GL11.GL_LIGHT1);
		
		GL11.glFrontFace(GL11.GL_CW);
		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
		GL11.glColorMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT_AND_DIFFUSE);
	}
	
	public long getTime() {
		return (Sys.getTime() * 1000) / Sys.getTimerResolution();
	}
	
	public int getDelta() {
		long time = getTime();
		int delta = (int) (time - lastFrameTime);
		lastFrameTime = time;
		return delta;
	}
}
