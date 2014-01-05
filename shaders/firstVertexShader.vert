uniform float timeRatio;
uniform float scaleX;
uniform float scaleY;
uniform float scaleZ;
uniform float transX;
uniform float transY;
uniform float transZ;

uniform int isHead;
uniform float stemTip;
uniform int isMainMushroom;

uniform vec4 headCenter;
uniform float lightTimer;
varying float lightTimerOut;

varying vec3 normal;
varying vec4 pos;
varying vec3 trans;
varying float timeRatio2;
varying vec3 isMainMush;

float impulse( float k, float x )
{
    float h = k*x;
    return h*exp(1.0-h);
}

float rand(vec2 co){
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}
 
void main(){
	float PI = 3.14159265358979323846264;

	vec4 scaleCol1 = vec4(scaleX, 0, 0, 0);
	vec4 scaleCol2 = vec4(0.0, scaleY, 0, 0);
	vec4 scaleCol3 = vec4(0.0, 0, scaleZ, 0);
	vec4 scaleCol4 = vec4(0.0, 0.0, 0.0, 1.0);
	mat4 scaleMat = mat4(scaleCol1, scaleCol2, scaleCol3, scaleCol4);

	vec4 transCol1 = vec4(1.0, 0, 0, 0);
	vec4 transCol2 = vec4(0.0, 1.0, 0, 0);
	vec4 transCol3 = vec4(0.0, 0, 1.0, 0);

	float adder = 0.0;
	if (isHead == 1)
		adder = stemTip;
	vec4 transCol4 = vec4(transX, transY + adder, transZ, 1.0);
	mat4 translateMat = mat4(transCol1, transCol2, transCol3, transCol4);
 	
	mat4 transformMat = translateMat * scaleMat;

	float theRand = 1.0;
	if (timeRatio < 0.15)
		theRand = rand(vec2(timeRatio, -timeRatio));
	float modifiedTime = smoothstep(0.0, 1.0, timeRatio);
	if (isMainMushroom != 1)
		modifiedTime = sin(timeRatio*PI/2.0) * theRand;
	vec4 interpPos = (gl_Vertex*(1.0 - modifiedTime) + gl_MultiTexCoord0 * modifiedTime);

	vec4 firstTransformPosition = transformMat * interpPos;
	vec4 transformHeadCenter = transformMat * headCenter;
	
	vec4 preNormal = firstTransformPosition - transformHeadCenter;
	//if (isHead == 1)
	//	normal = vec3(preNormal.x, preNormal.y, preNormal.z);
	//else
		normal = normalize(gl_Normal);

	gl_Position = gl_ModelViewProjectionMatrix * firstTransformPosition;
	
	pos = firstTransformPosition;
	trans = vec3(transX, transY, transZ);
	timeRatio2 = timeRatio;
	lightTimerOut = lightTimer;
	gl_FrontColor = gl_Color;
	isMainMush = vec3(isMainMushroom);
}