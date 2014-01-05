varying vec3 normal;
varying vec4 pos;
varying vec3 trans;

varying float timeRatio2;
varying float lightTimerOut;
varying vec3 isMainMush;

float rand(vec2 co){
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

vec4 light(vec3 lightPos, vec3 diffuseColor, vec3 specColor, float atten) {
	bool doingTheMain = length(isMainMush) != 0.0;
	vec4 color = gl_Color;
	vec4 matspec = vec4(0.0, 0.0, 0.0, 1.0);
	float shininess = 0.0;
	vec4 lightspec = vec4(specColor, 1.0);
	float add = 0.0;
	if (doingTheMain)
		add = trans.y;
	vec4 lpos = vec4(lightPos, 1.0);
	vec4 vecBetween = pos-lpos;
	float length = length(vecBetween);
	vec4 s = -normalize(vecBetween);
	if (doingTheMain) 
		s = normalize(vec4(1.0));

	vec3 light = s.xyz;

	vec3 normalToUse = normal;
	if (doingTheMain)
		normalToUse = vec3(1.0);
	vec3 n = normalize(normalToUse);
	vec3 r = -reflect(light, n);
	r = normalize(r);
	vec3 v = -pos.xyz;
	v = normalize(v);
	   
	vec4 lightSourceDiffuse = vec4(diffuseColor, 1); //gl_LightSource[0].diffuse;
	vec4 diffuse  = color * max(0.0, dot(n, s.xyz)) * lightSourceDiffuse;
	if (!doingTheMain)
		diffuse /= pow(length/140.0, 2.2)*atten;
	else
		diffuse *= 8.0;
	vec4 specular;
	if (shininess != 0.0) {
	  specular = lightspec * matspec * pow(max(0.0,                 dot(r, v)), shininess);
	  //specular /= (length/20.0);
	} else {
	  specular = vec4(0.0, 0.0, 0.0, 0.0);
	}

	return diffuse + specular;
}

void main(){
	bool doingTheMain = length(isMainMush) != 0.0; //refers to the main mushroom

	vec4 firstLight = light(vec3(0, 60.0, 120.0), vec3(0.15, 0.25, 1), vec3(0, 0, 0), 1.0);
	vec4 secondLight = light(vec3(-60.0 + sin(lightTimerOut*6.0)*120.0, 60.0, -80.0), vec3(0.05, 0.55, 0.15), vec3(0, 0, 0), 5.0);
	vec4 ambient = vec4(0.03, 0.03, 0.03, 1.0);

	if (doingTheMain)
		gl_FragColor = firstLight + ambient;
	else {
		gl_FragColor = secondLight	 + firstLight + ambient;
	}
	
}