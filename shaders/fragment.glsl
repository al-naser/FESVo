#version 120

//-----------------------------
// THIS IS THE CURRENT VERSION
//-----------------------------


// v.8 [16/12/2011]
// perform normalization in shader
// colours horizons based on local max/min height

uniform sampler3D seismicTexture;
uniform sampler3D featuresTexture;
uniform float min_amplitude, max_amplitude, alpha, seismicTexDepth;
uniform vec3 feature1;
uniform vec3 feature2;

// a palette of blue - light blue - green - yellow - red
// takes a value on range [0, 1], returns a vec3 color
vec3 palette1(float i){

	vec3 color;

	if (i > 0.75){
		float c = (i - 0.75) / .25;
		color = vec3(1.0, (1 - c), 0.0); // toward red
	}
	else if (i > 0.5){
		float c = (i - 0.5) / 0.25;
		color = vec3(c, 1.0, 0.0); // toward yellow
	}
	else if (i > 0.25){
		float c = (i - .25) / 0.25;
		color = vec3(0, 1.0, (1.0 - c)); // toward green
	}
	else {
		float c = i / 0.25;
		color = vec3(0, c, 1.0); // from blue toward light blue
	}

	return color;
}

// a reverse of palette1
vec3 palette1_rev(float i){
    return palette1(1-i);
}

void main(void)
{
    // access the seismic texture sampler, as a base color:
    float luminance = float(texture3D(seismicTexture, gl_TexCoord[0].stp));

    // access the features texture sampler:
    float feature = float(texture3D(featuresTexture, gl_TexCoord[1].stp));

    // normalize data:
    luminance = ((luminance - min_amplitude) /  (max_amplitude - min_amplitude));

    float h;

    if (feature == feature1[0]){
            h = (gl_TexCoord[1].s - feature1[1]) / (feature1[2] - feature1[1]);
            gl_FragColor = vec4(palette1_rev(h), 1.0);//vec4(1.0, 0.0, 0.0, 1.0);//
	}

    else if (feature == feature2[0]){
            h = (gl_TexCoord[1].s - feature2[1]) / (feature2[2] - feature2[1]);
            gl_FragColor = vec4(palette1_rev(h), 1.0);//vec4(0.0, 0.0, 1.0, 1.0);//
	}

    else if((1 - gl_TexCoord[0].p) < seismicTexDepth){
            gl_FragColor = vec4(luminance, luminance, luminance, alpha); // gray
	}

    else{
            gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0); // clear
    }
		
}

