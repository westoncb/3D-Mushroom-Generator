package com.cyntaks.botgame;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

public class Mushroom {
	private static final int X = 0;
	private static final int Y = 1;
	private static final int Z = 2;
	
	private float[][] stemPoints;
	private float[][] nextStemPoints;
	private int TRIANGLES_IN_RING;
	private int POINTS_IN_RING;
	private int STEM_RINGS;
	private float RING_HEIGHT;
	
	private float[][] headPoints;
	private float[][] nextHeadPoints;
	private float headShift;
	private float nextHeadShift;
	private float lastHeadExcess;
	private int HEAD_SECTORS;
	private int TRIANGLES_IN_HEAD_SECTOR;
	private int POINTS_IN_HEAD_SECTOR;
	
	private int transitionDelay = 2500;
	private int timePassed;
	private float timeRatio;
	
	boolean calculateNormals;
	boolean continuallyTransition;
	boolean transitioning;
	
	boolean incrementScale;
	private float scale;
	float nextScale;
	float xTranslate;
	float yTranslate;
	float zTranslate;
	
	private float stemShadeBrightest;
	private float stemShadeDarkest;
	private float headShadeBrightest;
	private float headShadeDarkest;
	
	private float stemRed;
	private float stemGreen;
	private float stemBlue;
	private float stemAlpha;
	private float headRed;
	private float headGreen;
	private float headBlue;
	private float headAlpha;
	
	//rendering properties
	int polygonRenderMode;
	
	boolean useBlending;
	int blendMode;
	
	boolean renderBackfaces;
	boolean useLighting;
	
	FloatBuffer normalData;
	FloatBuffer normalData2;
	
	static int vertexShader;
	static int fragmentShader;
	static int shaderProgram;
	static boolean shadersLoaded;
	static boolean useShader;
	
	float[][] stemLine;
	
	class HeadConfig {
		float diameter;
		float innerLipScale;
		float firstTaperScale;
		float bottomPosition;
		float secondPosition;
		float thirdPosition;
		float fourthPosition;
	}
	
	class StemConfig {
		float firstKink;
		float secondKink;
		float topPosition;
		float top;
		float firstKinkStrength;
		float secondKinkStrength;
		float baseRadius;
	}
	
	private int vertexVBOID;
	private int nextVertexVBOID;
	private int colorVBOID;
	private int normalVBOID;
	
	private int vertexVBOID2;
	private int nextVertexVBOID2;
	private int colorVBOID2;
	private int normalVBOID2;
	
	private float stemTipY;
	private float nextStemTipY;
	
	public Mushroom(int trianglesInStemRing, int stemRings, int headSectors, int trianglesInHeadSector) {
		TRIANGLES_IN_RING = trianglesInStemRing;
		STEM_RINGS = stemRings;
		
		HEAD_SECTORS = headSectors;
		TRIANGLES_IN_HEAD_SECTOR = trianglesInHeadSector;
		
		POINTS_IN_RING = TRIANGLES_IN_RING*3;
		POINTS_IN_HEAD_SECTOR = TRIANGLES_IN_HEAD_SECTOR*3;
		
		RING_HEIGHT = 100f/STEM_RINGS;
		
		this.setScale(1.0f);
		
		this.setColor(1, 1, 1, 1);
		this.calculateNormals = false;
		this.renderBackfaces = true;
		this.useLighting = false;
		this.useBlending = false;
		this.polygonRenderMode = RenderState.RENDER_MODE_FILL;
		this.blendMode = RenderState.BLEND_MODE_NORMAL;
		
		if (!shadersLoaded)
			setupShaders();
		
		normalVBOID2 = GL15.glGenBuffers();
		normalVBOID = GL15.glGenBuffers();
	}
	
	private static void setupShaders() {
		shadersLoaded = true;
		try {
			vertexShader = createShader("shaders/firstVertexShader.vert", ARBVertexShader.GL_VERTEX_SHADER_ARB);
			fragmentShader = createShader("shaders/firstFragmentShader.frag", ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (vertexShader == 0 || fragmentShader == 0)
				return;
		}
		
		shaderProgram = ARBShaderObjects.glCreateProgramObjectARB();
		if (shaderProgram == 0)
			return;
		
		ARBShaderObjects.glAttachObjectARB(shaderProgram, vertexShader);
		ARBShaderObjects.glAttachObjectARB(shaderProgram, fragmentShader);
		ARBShaderObjects.glLinkProgramARB(shaderProgram);
		
		ARBShaderObjects.glLinkProgramARB(shaderProgram);
		if (ARBShaderObjects.glGetObjectParameteriARB(shaderProgram, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
			System.err.println(getLogInfo(shaderProgram));
			return;
		}
		ARBShaderObjects.glValidateProgramARB(shaderProgram);
		if (ARBShaderObjects.glGetObjectParameteriARB(shaderProgram, ARBShaderObjects.GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
			System.err.println(getLogInfo(shaderProgram));
			return;
		}
		
		useShader = true;
	}
	
	public void transitionToNextMushroom(int delay) {
		this.transitionDelay = delay;
		this.transitioning = true;
	}
	
	private void createNextMushroom() {
		if (headPoints == null) {
			stemPoints = createStem();
			headShift = this.lastHeadExcess;
			nextStemPoints = createStem();
			nextHeadShift = this.lastHeadExcess;

			headPoints = createHead();
			nextHeadPoints = createHead();
		} else {
			stemPoints = nextStemPoints;
			headShift = nextHeadShift;
			nextStemPoints = createStem();
			nextHeadShift = this.lastHeadExcess;
			
			headPoints = nextHeadPoints;
			nextHeadPoints = createHead();
		}
		
		normalData = BufferUtils.createFloatBuffer(headPoints.length*3);
		normalData2 = BufferUtils.createFloatBuffer(stemPoints.length*3);
		
		//create buffer from vertex data
		FloatBuffer vertexData = BufferUtils.createFloatBuffer(headPoints.length*3);
		FloatBuffer nextVertexData = BufferUtils.createFloatBuffer(headPoints.length*3);
		FloatBuffer colorData = BufferUtils.createFloatBuffer(headPoints.length*4);
		
		for (int i = 0; i < headPoints.length; i++) {
			vertexData.put(headPoints[i]);
			
			nextVertexData.put(nextHeadPoints[i]);
			
			for (int j = 0; j < 1; j++) {
				int positionInSector = (i+j) % POINTS_IN_HEAD_SECTOR;
				float sectorRatio = (float)positionInSector / POINTS_IN_HEAD_SECTOR;
				float brightnessDiff = this.headShadeBrightest - this.headShadeDarkest;
				float currentBrightness = this.headShadeBrightest - (1 - sectorRatio) * brightnessDiff;
				float currentRed = this.headRed*currentBrightness;
				float currentGreen = this.headGreen*currentBrightness;
				float currentBlue = this.headBlue*currentBrightness;
				
				colorData.put(currentRed);
				colorData.put(currentGreen);
				colorData.put(currentBlue);
				colorData.put(1.0f);
			}
		}
		vertexData.flip();
		nextVertexData.flip();
		colorData.flip();
		
		this.stemTipY = this.getCenterOfNthRing(stemPoints, STEM_RINGS - 1)[Y];
		this.nextStemTipY = this.getCenterOfNthRing(nextStemPoints, STEM_RINGS - 1)[Y];
		
		//set up vbo for vertex data
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(vertexVBOID);
		vertexVBOID = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexVBOID);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexData, GL15.GL_STATIC_DRAW);
		
		//set up the next model's vertex data 
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(nextVertexVBOID);
		nextVertexVBOID = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, nextVertexVBOID);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, nextVertexData, GL15.GL_STATIC_DRAW);
		
		//set up vbo for color data
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(colorVBOID);
		colorVBOID = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, colorVBOID);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, colorData, GL15.GL_STATIC_DRAW);
		
		
		
		
		////////////
		//Start STEM!
		//////
		
		FloatBuffer vertexData2 = BufferUtils.createFloatBuffer(stemPoints.length*3);
		FloatBuffer nextVertexData2 = BufferUtils.createFloatBuffer(stemPoints.length*3);
		FloatBuffer colorData2 = BufferUtils.createFloatBuffer(stemPoints.length*4);
		for (int i = 0; i < stemPoints.length; i++) {
			vertexData2.put(stemPoints[i]);
			
			nextVertexData2.put(nextStemPoints[i]);
			
			for (int j = 0; j < 1; j++) {
				float pointRatio = (float)(i + j)/stemPoints.length;
				float brightnessDiff = this.stemShadeBrightest - this.stemShadeDarkest;
				float currentBrightness = this.stemShadeBrightest - pointRatio * brightnessDiff;
				float currentRed = this.stemRed*currentBrightness;
				float currentGreen = this.stemGreen*currentBrightness;
				float currentBlue = this.stemBlue*currentBrightness;
				
				colorData2.put(currentRed);
				colorData2.put(currentGreen);
				colorData2.put(currentBlue);
				colorData2.put(1.0f);
			}
		}
		vertexData2.flip();
		nextVertexData2.flip();
		colorData2.flip();
		
		//set up vbo for vertex data
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(vertexVBOID2);
		vertexVBOID2 = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexVBOID2);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexData2, GL15.GL_STATIC_DRAW);
		
		//set up the next model's vertex data 
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(nextVertexVBOID2);
		nextVertexVBOID2 = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, nextVertexVBOID2);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, nextVertexData2, GL15.GL_STATIC_DRAW);
		
		//set up vbo for color data
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(colorVBOID2);
		colorVBOID2 = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, colorVBOID2);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, colorData2, GL15.GL_STATIC_DRAW);
	}
	
	private static int createShader(String path, int shaderType) {
		int shader = 0;
		try {
			shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);
			if (shader == 0)
				return 0;
			ARBShaderObjects.glShaderSourceARB(shader, readFileAsString(path));
			ARBShaderObjects.glCompileShaderARB(shader);
			
			if (ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
				throw new RuntimeException("Error creating shader: " + getLogInfo(shader));
			
			return shader;
		} catch (Exception exc) {
			ARBShaderObjects.glDeleteObjectARB(shader);
			exc.printStackTrace();
			return 0;
		}
	}
	
	private static String getLogInfo(int obj) {
		return ARBShaderObjects.glGetInfoLogARB(obj, ARBShaderObjects.glGetObjectParameteriARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB));
	}
	
	private static String readFileAsString(String filename) throws Exception {
		StringBuilder source = new StringBuilder();
		FileInputStream in = new FileInputStream(filename);
		Exception exception = null;
		 
		BufferedReader reader;

		try{
		reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
		 
		Exception innerExc= null;
		try {
			String line;
			while((line = reader.readLine()) != null)
				source.append(line).append('\n');
			}
			catch(Exception exc) {
				exception = exc;
			}
			finally {
			try {
				reader.close();
			}
			catch(Exception exc) {
				if(innerExc == null)
					innerExc = exc;
				else
					exc.printStackTrace();
			}
		}
		if(innerExc != null)
			throw innerExc;
		}
		catch(Exception exc) {
			exception = exc;
		}
		finally {
			try {
				in.close();
			}
			catch(Exception exc) {
				if(exception == null)
					exception = exc;
				else
					exc.printStackTrace();
			}
		 
			if(exception != null)
				throw exception;
			}
			return source.toString();
	}
	
	private float[][] createHead() {
		float[][] headPoints = new float[POINTS_IN_HEAD_SECTOR*HEAD_SECTORS][3];
		
		HeadConfig config = generateHeadConfig();
		
		for (int i = 0; i < HEAD_SECTORS; i++) {
			float[][] sectorPoints = createHeadSector(i, config);
			for (int j = 0; j < sectorPoints.length; j++) {
				int pointIndex = j + i*POINTS_IN_HEAD_SECTOR;
				headPoints[pointIndex][X] = sectorPoints[j][X];
				headPoints[pointIndex][Y] = sectorPoints[j][Y];
				headPoints[pointIndex][Z] = sectorPoints[j][Z];
			}
		}
		
		return headPoints;
	}
	
	private float[][] createHeadSector(int sectorIndex, HeadConfig config) {
		float rotation = (float)sectorIndex/HEAD_SECTORS*(float)Math.PI*2;
		float nextRotation = (float)(sectorIndex + 1)/HEAD_SECTORS*(float)Math.PI*2;
		float[][] sectorPoints = new float[POINTS_IN_HEAD_SECTOR][3];
		
		for (int i = 0; i < POINTS_IN_HEAD_SECTOR; i++) {
			float[] point = getPointOnHeadCurve((float)i/POINTS_IN_HEAD_SECTOR, config);
			
			float thisRotation = i%2 == 0 ? rotation : nextRotation;
			
			sectorPoints[i][X] = (float)Math.cos(thisRotation)*point[X];
			sectorPoints[i][Y] = point[Y];
			sectorPoints[i][Z] = (float)Math.sin(thisRotation)*point[X];
		}
		
		return sectorPoints;
	}
	
	private HeadConfig generateHeadConfig() {
		HeadConfig config = new HeadConfig();
		config.diameter = (float)(Math.random()*60 + 30);
		config.innerLipScale = (float)(Math.random()*0.7f + 0.2f);
		config.firstTaperScale = (float)(Math.random()*0.7f + 1f);
		config.secondPosition = 0;
		config.bottomPosition = (float)(Math.random()*20 + config.secondPosition + 10);
		config.thirdPosition = (float)(Math.random()*20 + config.bottomPosition + 10);
		config.fourthPosition = (float)(Math.random()*20 + config.thirdPosition + 10);
		
		return config;
	}
	
	private float[] getPointOnHeadCurve(float ratio, HeadConfig config) {	
		float[][] controlPoints = new float[4][3];
		controlPoints[0][X] = config.diameter*config.innerLipScale;
		controlPoints[0][Y] = config.bottomPosition;
		controlPoints[0][Z] = 0;
		
		controlPoints[1][X] = config.diameter;
		controlPoints[1][Y] = config.secondPosition;
		controlPoints[1][Z] = 0;
		
		controlPoints[2][X] = config.diameter*config.firstTaperScale;
		controlPoints[2][Y] = config.thirdPosition;
		controlPoints[2][Z] = 0;
		
		controlPoints[3][X] = 0;
		controlPoints[3][Y] = config.fourthPosition;
		controlPoints[3][Z] = 0;
		
		return findPointOnBezierCurve(ratio, controlPoints);
	}
	
	private StemConfig generateStemConfig()
	{
		StemConfig config = new StemConfig();
		
		float stemHeight = RING_HEIGHT*STEM_RINGS;
		
		config.firstKink = (float)Math.random()*(stemHeight*0.4f) + stemHeight*0.06f;
		config.secondKink = (float)Math.random()*(stemHeight - config.firstKink) + config.firstKink;
		config.top = (float)Math.random()*(stemHeight*1.2f - config.secondKink) + config.secondKink;
		config.firstKinkStrength = (float)Math.random()*200 - 100;
		config.secondKinkStrength = (float)Math.random()*200 - 100;
		config.topPosition = 0; //Math.abs(config.secondKinkStrength)> 75 ? config.secondKinkStrength*0.5f : 0;
		config.baseRadius = (float)Math.random()*8 + 5;
		
		this.lastHeadExcess = config.top - stemHeight; //NOTE: this should be interpolated
		
		return config;
	}
	
	private float[][] createStem() {
		float[][] stemPoints = new float[STEM_RINGS*POINTS_IN_RING][3];
		
		StemConfig config = generateStemConfig();
		
		stemLine = createStemLine(config);
		float lastRadius = 0;
		for (int i = 0; i < STEM_RINGS; i++) {
			float completionRatio = ((float)i/STEM_RINGS);
			float radiusScale = 2.2f - (completionRatio*completionRatio)*1.6f;
			float[][] ringPoints = createStemRing(i, stemLine[i], radiusScale, lastRadius, config);
			lastRadius = radiusScale*config.baseRadius;
			for (int j = 0; j < ringPoints.length; j++) {
				stemPoints[i*POINTS_IN_RING + j] = ringPoints[j];
			}
		}
		
		return stemPoints;
	}
	
	private float[][] createStemLine(StemConfig config) {
		float[][] stemLine = new float[STEM_RINGS][3];
		
		float[][] controlPoints = new float[4][3];
		controlPoints[0][X] = 0;
		controlPoints[0][Y] = 0;
		controlPoints[0][Z] = 0;
		
		controlPoints[1][X] = config.firstKinkStrength;
		controlPoints[1][Y] = config.firstKink;
		controlPoints[1][Z] = 0;
		
		controlPoints[2][X] = config.secondKinkStrength;
		controlPoints[2][Y] = config.secondKink;
		controlPoints[2][Z] = 0;
		
		controlPoints[3][X] = config.topPosition;
		controlPoints[3][Y] = config.top;
		controlPoints[3][Z] = 0;
		
		for (int i = 0; i < stemLine.length; i++) {
			float completionRatio = (float)i/stemLine.length;
			float[] bezierPoint = findPointOnBezierCurve(completionRatio, controlPoints);
			
			float bezierX = bezierPoint[X];
			float progress = (float)i/stemLine.length;
			
			stemLine[i][X] = bezierX*progress;
			stemLine[i][Y] = bezierPoint[Y];
			stemLine[i][Z] = bezierX*(1 - progress);
		}
		
		return stemLine;
	}
	
	private float[][] createStemRing(int ringIndex, float[] centerPoint, float radiusScale, float lastRadius, StemConfig config) {
		float x = 0;
		float y = 0;
		float z = 0;
		
		float rotation = 0;
		float rotationIncrement = (float)(Math.PI*2/(POINTS_IN_RING-2));
		float[][] ringPoints = new float[POINTS_IN_RING][3];
		
		for (int i = 0; i < POINTS_IN_RING; i++) {
			rotation += rotationIncrement;
			float radiusToUse = lastRadius;
			
			if (i%2 == 0) {
				y = 0;
			} else {
				y = RING_HEIGHT*1.2f;
				radiusToUse = config.baseRadius*radiusScale;
			}
			
			x = (float)(Math.cos(rotation)*radiusToUse);
			z = (float)(Math.sin(rotation)*radiusToUse);
			
			ringPoints[i][X] = (x + centerPoint[X]);
			ringPoints[i][Y] = (y + centerPoint[Y]);
			ringPoints[i][Z] = (z + centerPoint[Z]);
		}
		
		return ringPoints;
	}
	
	private float[] findPointOnBezierCurve(float t, float[][] controlPoints) {
		if (controlPoints.length == 1)
			return controlPoints[0];
		
		float[][] nextControlPoints = new float[controlPoints.length - 1][3];
		int controlPointIndex = 0;
		float[] lastPoint = new float[3];
		
		for (int i = 0; i < controlPoints.length; i++) {
			float[] point = controlPoints[i];
			
			if (i > 0) {
				float xInterp = lastPoint[X] + t*(point[X] - lastPoint[X]);
	            float yInterp = lastPoint[Y] + t*(point[Y] - lastPoint[Y]);
	            nextControlPoints[controlPointIndex] = new float[] {xInterp, yInterp};
	            controlPointIndex++;
			}
			
			lastPoint = point;
		}
		
		return findPointOnBezierCurve(t, nextControlPoints);
	}
	
	private void setupRenderingAttributes()
	{
		if (this.useLighting)
			RenderState.enableLighting();
		else if (!this.useLighting)
			RenderState.disableLighting();
		
		if (this.useBlending)
			RenderState.enableBlending();	
		else if (!this.useBlending)
			RenderState.disableBlending();
		
		if (this.useBlending)
			RenderState.setBlendingMode(this.blendMode);
		
		if (this.renderBackfaces)
			RenderState.setPolygonRenderingMode(true, this.polygonRenderMode);
		else
			RenderState.setPolygonRenderingMode(false, this.polygonRenderMode);
	}
	
	public void render() {
		timeRatio = (float)timePassed/transitionDelay;
		float yShift = 0; //so that its bottom is at zero rather than its center
		
		setupRenderingAttributes();
		
//		GL11.glBegin(GL11.GL_TRIANGLE_STRIP); //render stem
//			for (int i = 0; i < stemPoints.length; i++) {
//					float pointRatio = (float)i/stemPoints.length;
//					
//					float[] point = interpolate(stemPoints[i], nextStemPoints[i], timeRatio);
//					
//					float brightnessDiff = this.stemShadeBrightest - this.stemShadeDarkest;
//					float currentBrightness = this.stemShadeBrightest - pointRatio * brightnessDiff;
//					float currentRed = this.stemRed*currentBrightness;
//					float currentGreen = this.stemGreen*currentBrightness;
//					float currentBlue = this.stemBlue*currentBrightness;
//					
//					GL11.glColor4f(currentRed, currentGreen, currentBlue, 0.1f);
//					
//					if (i < stemPoints.length - 2 && this.calculateNormals) {
//						float[] point2 = interpolate(stemPoints[i+1], nextStemPoints[i+1], timeRatio);
//						float[] point3 = interpolate(stemPoints[i+2], nextStemPoints[i+2], timeRatio);
//						float[] normal = Test.getNormal(point, point2, point3);
//						GL11.glNormal3f(normal[X], normal[Y], normal[Z]);
//					}
//					
//					float renderX = point[X]*scale + xTranslate;
//					float renderY = point[Y]*scale + yTranslate + yShift;
//					float renderZ = point[Z]*scale + zTranslate;
//					
//					GL11.glVertex3f(renderX, renderY, renderZ);
//			}
//		GL11.glEnd();
		
//			for (int i = 0; i < headPoints.length; i++) {
//				float headShift = this.headShift*(1 - timeRatio) + this.nextHeadShift*timeRatio;
//				
//				int positionInSector = i % POINTS_IN_HEAD_SECTOR;
//				if (positionInSector == 0)
//					GL11.glBegin(GL11.GL_TRIANGLE_STRIP); //render head
//				float sectorRatio = (float)positionInSector / POINTS_IN_HEAD_SECTOR;
//				
//				float[] point = interpolate(headPoints[i], nextHeadPoints[i], timeRatio);
//				
//				float brightnessDiff = this.headShadeBrightest - this.headShadeDarkest;
//				float currentBrightness = this.headShadeBrightest - (1 - sectorRatio) * brightnessDiff;
//				float currentRed = this.headRed*currentBrightness;
//				float currentGreen = this.headGreen*currentBrightness;
//				float currentBlue = this.headBlue*currentBrightness;
//				
//				GL11.glColor4f(currentRed, currentGreen, currentBlue, this.headAlpha);
//				
//				if (i < headPoints.length - 2 && this.calculateNormals) {
//					float[] point2 = interpolate(headPoints[i+1], nextHeadPoints[i+1], timeRatio);
//					float[] point3 = interpolate(headPoints[i+2], nextHeadPoints[i+2], timeRatio);
//					float[] normal = Test.getNormal(point, point2, point3);
//					GL11.glNormal3f(normal[X], normal[Y], normal[Z]);
//				}
//				
//				float renderX = point[X]*scale + xTranslate;
//				float renderY = point[Y]*scale + yTranslate + headShift*scale + yShift;
//				float renderZ = point[Z]*scale + zTranslate;
//				
//				//shift head to account for stem position
//				float[] topRingCenter = getCenterOfNthRing(STEM_RINGS - 1, true);
//				float xTrans = topRingCenter[X];
//				float zTrans = topRingCenter[Z];
//				
//				//System.out.println("xTrans: " + xTrans + ", zTrans: " + zTrans);
//				
////				renderX += xTrans;
////				renderZ += zTrans;
//				
//				GL11.glVertex3f(renderX, renderY, renderZ);
//				if (positionInSector == POINTS_IN_HEAD_SECTOR-1)
//					GL11.glEnd();
//			}
		
		if (useShader)
			ARBShaderObjects.glUseProgramObjectARB(shaderProgram);
//		GL11.glDisable(GL11.GL_LIGHTING);
		
		int timeRatioLocation = GL20.glGetUniformLocation(shaderProgram, "timeRatio");
		GL20.glUniform1f(timeRatioLocation, timeRatio);
		
		int transXLoc = GL20.glGetUniformLocation(shaderProgram, "transX");
		GL20.glUniform1f(transXLoc, this.xTranslate);
		int transYLoc = GL20.glGetUniformLocation(shaderProgram, "transY");
		GL20.glUniform1f(transYLoc, this.yTranslate);
		int transZLoc = GL20.glGetUniformLocation(shaderProgram, "transZ");
		GL20.glUniform1f(transZLoc, this.zTranslate);
		
		int scaleXLoc = GL20.glGetUniformLocation(shaderProgram, "scaleX");
		GL20.glUniform1f(scaleXLoc, this.scale);
		int scaleYLoc = GL20.glGetUniformLocation(shaderProgram, "scaleY");
		GL20.glUniform1f(scaleYLoc, this.scale);
		int scaleZLoc = GL20.glGetUniformLocation(shaderProgram, "scaleZ");
		GL20.glUniform1f(scaleZLoc, this.scale);
		
		int lightTimerLoc = GL20.glGetUniformLocation(shaderProgram, "lightTimer");
		GL20.glUniform1f(lightTimerLoc, Test.lightTimer);
		
		int isHeadLoc = GL20.glGetUniformLocation(shaderProgram, "isHead");
		GL20.glUniform1i(isHeadLoc, 1);
		
		int isMainLoc = GL20.glGetUniformLocation(shaderProgram, "isMainMushroom");
		GL20.glUniform1i(isMainLoc, this.incrementScale ? 1 : 0);
		
		float[] headCenter = getCenterOfPointSet(headPoints, 0, headPoints.length-1);
		float[] nextHeadCenter = getCenterOfPointSet(nextHeadPoints, 0, nextHeadPoints.length-1);
		float[] interpedHeadCenter = this.interpolate(headCenter, nextHeadCenter, timeRatio);
		int headCenterLoc = GL20.glGetUniformLocation(shaderProgram, "headCenter");
		GL20.glUniform4f(headCenterLoc, interpedHeadCenter[X], interpedHeadCenter[Y], interpedHeadCenter[Z], 1.0f);
		
		int stemTipLoc = GL20.glGetUniformLocation(shaderProgram, "stemTip");
		GL20.glUniform1f(stemTipLoc, this.stemTipY*(1 - timeRatio)*scale + this.nextStemTipY*timeRatio*scale - 30*scale);
		
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexVBOID);
		GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0);
		
		GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, colorVBOID);
		GL11.glColorPointer(4, GL11.GL_FLOAT, 0, 0);
		
		this.fillNormalBuffer();
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		GL11.glNormalPointer(GL11.GL_FLOAT, 0, 0);
		
		//send over the next model coords as texture data so that we can carry out interpolation on the vertex shader
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, nextVertexVBOID);
		GL11.glTexCoordPointer(3, GL11.GL_FLOAT, 0, 0);
		
		GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, headPoints.length);
		
		
		////////
		//Begin STEM
		/////////////
		

		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexVBOID2);
		GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0);
		
		GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, colorVBOID2);
		GL11.glColorPointer(4, GL11.GL_FLOAT, 0, 0);
		
		this.fillNormalBuffer2();
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		GL11.glNormalPointer(GL11.GL_FLOAT, 0, 0);
		
		//send over the next model coords as texture data so that we can carry out interpolation on the vertex shader
		GL20.glUniform1i(isHeadLoc, 0);
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, nextVertexVBOID2);
		GL11.glTexCoordPointer(3, GL11.GL_FLOAT, 0, 0);
		
		GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, stemPoints.length);
		
		if (useShader)
			ARBShaderObjects.glUseProgramObjectARB(0);
	}
	
	private void fillNormalBuffer2() {
		normalData2.rewind();
		int max = stemPoints.length - 2;
		for (int i = 0; i < max; i++) {
			float[] point = interpolate(stemPoints[i], nextStemPoints[i], timeRatio);
			float[] point2 = interpolate(stemPoints[i+1], nextStemPoints[i+1], timeRatio);
			float[] point3 = interpolate(stemPoints[i+2], nextStemPoints[i+2], timeRatio);
			float[] normal = Test.getNormal(point, point2, point3);
			normalData2.put(normal[X]);
			normalData2.put(normal[Y]);
			normalData2.put(normal[Z]);
		}
		normalData2.flip();
		
		//set up vbo for normaldata
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalVBOID2);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, normalData2, GL15.GL_DYNAMIC_DRAW);
	}
	
	private void fillNormalBuffer() {
		normalData.rewind();
		float max = headPoints.length - 2;
		for (int i = 0; i < max; i++) {			
			float[] point = interpolate(headPoints[i], nextHeadPoints[i], timeRatio);
			float[] point2 = interpolate(headPoints[i+1], nextHeadPoints[i+1], timeRatio);
			float[] point3 = interpolate(headPoints[i+2], nextHeadPoints[i+2], timeRatio);
			float[] normal = Test.getNormal(point, point2, point3);
			normalData.put(normal[X]);
			normalData.put(normal[Y]);
			normalData.put(normal[Z]);
		}
		normalData.flip();
		
		//set up vbo for normaldata
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalVBOID);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, normalData, GL15.GL_DYNAMIC_DRAW);
	}
	
	private float[] interpolate(float[] point1, float[] point2, float t) {
		float[] point = new float[3];
		
		point[X] = point1[X]*(1 - t) + point2[X]*t;
		point[Y] = point1[Y]*(1 - t) + point2[Y]*t;
		point[Z] = point1[Z]*(1 - t) + point2[Z]*t;
		
		return point;
	}
	
	public void renderCenter() {
		
		GL11.glTranslated(xTranslate, yTranslate, zTranslate);
		
		float[] centerPoint = getCenterOfNthRing(stemPoints, STEM_RINGS - 1);
		GL11.glPointSize(4);
		GL11.glColor4f(0, 0, 1, 1);
		GL11.glBegin(GL11.GL_POINTS);
			GL11.glVertex3f(centerPoint[X], centerPoint[Y], centerPoint[Z]);
		GL11.glEnd();
		
		GL11.glTranslated(-xTranslate, -yTranslate, -zTranslate);
		GL11.glPointSize(1);
	}
	
	private float[] getCenterOfPointSet(float[][] pointSet, int first, int last) {
		float leastX = Float.MAX_VALUE;
		float leastY = Float.MAX_VALUE;
		float leastZ = Float.MAX_VALUE;
		
		float greatestX = -Float.MAX_VALUE;
		float greatestY = -Float.MAX_VALUE;
		float greatestZ = -Float.MAX_VALUE;
		
		for (int i = first; i <= last; i++) {
			float[] point = pointSet[i];
			
			if (point[X] < leastX)
				leastX = point[X];
			if (point[Y] < leastY)
				leastY = point[Y];
			if (point[Z] < leastZ)
				leastZ = point[Z];
			
			if (point[X] > greatestX)
				greatestX = point[X];
			if (point[Y] > greatestY)
				greatestY = point[Y];
			if (point[Z] > greatestZ)
				greatestZ = point[Z];
		}
		
		float x = leastX + (greatestX - leastX)/2;
		float y = leastY + (greatestY - leastY)/2;
		float z = leastZ + (greatestZ - leastZ)/2;
				
		return new float[] {x, y, z};
	}
	
	private float[] getCenterOfNthRing(float[][] model, int n) {
		int startingPointIndex = POINTS_IN_RING*n - (n > 0 ? 1 : 0);
		int endingPointIndex = startingPointIndex + POINTS_IN_RING;
		
		 return getCenterOfPointSet(model, startingPointIndex, endingPointIndex);
	}
	
//	private float[] getCenterOfNthRing(int n, boolean scale) {
//		float timeRatio = (float)timePassed/transitionDelay;
//		int startingPointIndex = POINTS_IN_RING*n - 1;
//		int endingPointIndex = startingPointIndex + POINTS_IN_RING;
//		
//		float leastX = Float.MAX_VALUE;
//		float leastY = Float.MAX_VALUE;
//		float leastZ = Float.MAX_VALUE;
//		
//		float greatestX = Float.MIN_VALUE;
//		float greatestY = Float.MIN_VALUE;
//		float greatestZ = Float.MIN_VALUE;
//		for (int i = startingPointIndex; i <= endingPointIndex; i++) {
//			float[] point = interpolate(nextStemPoints[i], stemPoints[i], timeRatio);
//			if (scale) {
//				point[X] *= this.scale;
//				point[Y] *= this.scale;
//				point[Z] *= this.scale;
//			}
//			
//			if (point[X] < leastX)
//				leastX = point[X];
//			if (point[Y] < leastY)
//				leastY = point[Y];
//			if (point[Z] < leastZ)
//				leastZ = point[Z];
//			
//			if (point[X] > greatestX)
//				greatestX = point[X];
//			if (point[Y] > greatestY)
//				greatestY = point[Y];
//			if (point[Z] > greatestZ)
//				greatestZ = point[Z];
//		}
//		
//		return new float[] {(greatestX - leastX)/2, (greatestY - leastY)/2, (greatestZ - leastZ)/2}; 
//	}
	
	public void update(float delta) {
		if (this.transitioning) {
			timePassed += delta;
			
			float scaleIncrement = 0.03f;
			
			if (this.incrementScale) {
				float lastScale = (nextScale - scaleIncrement); 
				this.scale =  lastScale + scaleIncrement*((float)timePassed/transitionDelay);
			}
			
			if (timePassed > transitionDelay) {
				timePassed = 0;
				
				createNextMushroom();
				if (this.incrementScale)
					nextScale += scaleIncrement;
				
				if (this.continuallyTransition)
					this.transitionToNextMushroom(transitionDelay);
				else
					this.transitioning = false;
				
			}
		}
	}
	
	public void setStemShading(float brightest, float darkest) {
		this.stemShadeBrightest = brightest;
		this.stemShadeDarkest = darkest;
	}
	
	public void setHeadShading(float brightest, float darkest) {
		this.headShadeBrightest = brightest;
		this.headShadeDarkest = darkest;
	}
	
	public void setColor(float red, float green, float blue, float alpha) 
	{
		this.setStemColor(red, green, blue, alpha);
		this.setHeadColor(red, green, blue, alpha);
	}
	
	public void setStemColor(float red, float green, float blue, float alpha) {
		this.stemRed = red;
		this.stemGreen = green;
		this.stemBlue = blue;
		this.stemAlpha = alpha;
	}
	
	public void setHeadColor(float red, float green, float blue, float alpha) {
		this.headRed = red;
		this.headGreen = green;
		this.headBlue = blue;
		this.headAlpha = alpha;
	}
	
	public void setScale(float scale)
	{
		this.scale = scale;
		this.nextScale = scale;
	}
	
	public float getScale() {
		return this.scale;
	}
	
	public void destroy() {
		GL20.glDisableVertexAttribArray(0);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(vertexVBOID);
	}
}