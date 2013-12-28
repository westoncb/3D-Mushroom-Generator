package com.cyntaks.botgame;

import org.lwjgl.opengl.GL11;

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
		
		this.initVAO();
	}
	
	private void initVAO() {
		
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
			
			int pointIndex = i;
			
			float thisRotation = i%2 == 0 ? rotation : nextRotation;
			
			sectorPoints[pointIndex][X] = (float)Math.cos(thisRotation)*point[X];
			sectorPoints[pointIndex][Y] = point[Y];
			sectorPoints[pointIndex][Z] = (float)Math.sin(thisRotation)*point[X];
		}
		
		return sectorPoints;
	}
	
	private HeadConfig generateHeadConfig() {
		HeadConfig config = new HeadConfig();
		config.diameter = (float)(Math.random()*60 + 30);
		config.innerLipScale = (float)(Math.random()*0.7f + 0.2f);
		config.firstTaperScale = (float)(Math.random()*0.7f + 1f);
		config.secondPosition = (float)(Math.random()*15 + 15);
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
		
		float[][] stemLine = createStemLine(config);
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
			stemLine[i][Y] = bezierPoint[Y] - (RING_HEIGHT*STEM_RINGS)/2.0f;
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
				y = RING_HEIGHT*1.5f;
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
		float timeRatio = (float)timePassed/transitionDelay;
		float yShift = STEM_RINGS*RING_HEIGHT*scale/2; //so that its bottom is at zero rather than its center
		
		setupRenderingAttributes();
		
		GL11.glBegin(GL11.GL_TRIANGLE_STRIP); //render stem
			for (int i = 0; i < stemPoints.length; i++) {
					float pointRatio = (float)i/stemPoints.length;
					
					float[] point = interpolate(stemPoints[i], nextStemPoints[i], timeRatio);
					
					float brightnessDiff = this.stemShadeBrightest - this.stemShadeDarkest;
					float currentBrightness = this.stemShadeBrightest - pointRatio * brightnessDiff;
					float currentRed = this.stemRed*currentBrightness;
					float currentGreen = this.stemGreen*currentBrightness;
					float currentBlue = this.stemBlue*currentBrightness;
					
					GL11.glColor4f(currentRed, currentGreen, currentBlue, this.stemAlpha);
					
					if (i < stemPoints.length - 2 && this.calculateNormals) {
						float[] point2 = interpolate(stemPoints[i+1], nextStemPoints[i+1], timeRatio);
						float[] point3 = interpolate(stemPoints[i+2], nextStemPoints[i+2], timeRatio);
						float[] normal = Test.getNormal(point, point2, point3);
						GL11.glNormal3f(normal[X], normal[Y], normal[Z]);
					}
					
					float renderX = point[X]*scale + xTranslate;
					float renderY = point[Y]*scale + yTranslate + yShift;
					float renderZ = point[Z]*scale + zTranslate;
					
					GL11.glVertex3f(renderX, renderY, renderZ);
			}
		GL11.glEnd();
		
			for (int i = 0; i < headPoints.length; i++) {
				float headShift = this.headShift*(1 - timeRatio) + this.nextHeadShift*timeRatio;
				
				int positionInSector = i % POINTS_IN_HEAD_SECTOR;
				if (positionInSector == 0)
					GL11.glBegin(GL11.GL_TRIANGLE_STRIP); //render head
				float sectorRatio = (float)positionInSector / POINTS_IN_HEAD_SECTOR;
				
				float[] point = interpolate(headPoints[i], nextHeadPoints[i], timeRatio);
				
				float brightnessDiff = this.headShadeBrightest - this.headShadeDarkest;
				float currentBrightness = this.headShadeBrightest - (1 - sectorRatio) * brightnessDiff;
				float currentRed = this.headRed*currentBrightness;
				float currentGreen = this.headGreen*currentBrightness;
				float currentBlue = this.headBlue*currentBrightness;
				
				GL11.glColor4f(currentRed, currentGreen, currentBlue, this.headAlpha);
				
				if (i < headPoints.length - 2 && this.calculateNormals) {
					float[] point2 = interpolate(headPoints[i+1], nextHeadPoints[i+1], timeRatio);
					float[] point3 = interpolate(headPoints[i+2], nextHeadPoints[i+2], timeRatio);
					float[] normal = Test.getNormal(point, point2, point3);
					GL11.glNormal3f(normal[X], normal[Y], normal[Z]);
				}
				
				float renderX = point[X]*scale + xTranslate;
				float renderY = point[Y]*scale + yTranslate + headShift*scale + yShift;
				float renderZ = point[Z]*scale + zTranslate;
				
				//shift head to account for stem position
				float[] topRingCenter = getCenterOfNthRing(STEM_RINGS - 1, true);
				float xTrans = topRingCenter[X];
				float zTrans = topRingCenter[Z];
				
				//System.out.println("xTrans: " + xTrans + ", zTrans: " + zTrans);
				
//				renderX += xTrans;
//				renderZ += zTrans;
				
				GL11.glVertex3f(renderX, renderY, renderZ);
				if (positionInSector == POINTS_IN_HEAD_SECTOR-1)
					GL11.glEnd();
			}
	}
	
	private float[] interpolate(float[] point1, float[] point2, float t) {
		float[] point = new float[3];
		
		point[X] = point1[X]*(1 - t) + point2[X]*t;
		point[Y] = point1[Y]*(1 - t) + point2[Y]*t;
		point[Z] = point1[Z]*(1 - t) + point2[Z]*t;
		
		return point;
	}
	
	private float[] getCenterOfNthRing(int n, boolean scale) {
		float timeRatio = (float)timePassed/transitionDelay;
		int startingPointIndex = POINTS_IN_RING*n - 1;
		int endingPointIndex = startingPointIndex + POINTS_IN_RING;
		
		float leastX = Float.MAX_VALUE;
		float leastY = Float.MAX_VALUE;
		float leastZ = Float.MAX_VALUE;
		
		float greatestX = Float.MIN_VALUE;
		float greatestY = Float.MIN_VALUE;
		float greatestZ = Float.MIN_VALUE;
		for (int i = startingPointIndex; i <= endingPointIndex; i++) {
			float[] point = interpolate(nextStemPoints[i], stemPoints[i], timeRatio);
			if (scale) {
				point[X] *= this.scale;
				point[Y] *= this.scale;
				point[Z] *= this.scale;
			}
			
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
		
		return new float[] {(greatestX - leastX)/2, (greatestY - leastY)/2, (greatestZ - leastZ)/2}; 
	}
	
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
}
	