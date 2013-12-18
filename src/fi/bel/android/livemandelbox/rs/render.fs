#pragma version(1)
#pragma rs java_package_name(fi.bel.android.livemandelbox.render)

static const int BADNESS_RAYS_SQRT = (1 << 6);
static const int BADNESS_RAYS = (BADNESS_RAYS_SQRT * BADNESS_RAYS_SQRT);
static const float INVSQRT3 = 0.5773502691896258f;
static const float3 LIGHT_DIST = { 0.0f, 0.0f, 0.1f };
static const int ITERATIONS = 20;
static const float DISTANCE_FOG = -0.15f;

/* External parameters */
int32_t seed = 1; /* is int32_t because we set this from java. Used as uint32_t everywhere. */
float scale = 2.0f;
float invDim;

static float rot_x;
static float3 pos;
static float exposure;

/* Return value [min, max[ */
static float linearRand(float min, float max) {
	seed = (uint32_t) seed * 1664525 + 1013904223;
	return (uint32_t) seed / 4294967296.0f * (max - min) + min;
}

static float mandelboxDistance(float3 pos) {
	float4 s = { scale, scale, scale, fabs(scale) };
	float4 pos0 = { pos.x, pos.y, pos.z, 1.0f };
    float4 iter = pos0;
    
    for (int i = 0; i < ITERATIONS; i ++) {
        iter.xyz = clamp(iter.xyz, -1.0f, 1.0f) * 2.0f - iter.xyz;
        float f = clamp(dot(iter.xyz, iter.xyz), 0.25f, 1.0f);
        iter = iter * s / f + pos0;
    }

    return length(iter.xyz) / iter.w;
}

static float3 mandelboxColor(float3 pos) {
    float3 iter = pos;
    float3 iter_out = { 0.0f, 0.0f, 1.0f };
	float3 iter_avg = normalize(iter);

    for (int i = 0; i < ITERATIONS; i ++) {
        iter = clamp(iter, -1.0f, 1.0f) * 2.0f - iter;
        float f = clamp(dot(iter, iter), 0.25f, 1.0f);
        iter = iter * scale / f + pos;

		iter_out.y += f;
		iter_out.z = min(iter_out.z, f);
		iter_avg += normalize(iter);
    }
	iter_out.y /= ITERATIONS;
	iter_out -= 0.25f;
	iter_out /= 0.75f;
    iter_out.x = pow(iter_out.y, 10.0f);
    iter_out.y = pow(iter_out.y, 20.0f);
    iter_out.z = pow(iter_out.z, 1.0f/10.0f);
    return .5f + .5f * normalize(iter_out * iter_avg);
}

static float intersectMandelbox(const float3 pos, const float3 dir, float t, const float detail) {
    while (t < 10.0f) {
        float dt = mandelboxDistance(pos + dir * t);
        t += dt * 0.5f;
        if (dt < detail * t) {
        	/* Step back slightly to stabilize the distance to detail * t - dt * .5 */
        	t -= detail * t - dt;
            break;
        }
    }

    return t;
}

/* Performs soft shadow measurement at given direction and factor.
 * The theory is that we shoot a cone of light from pos towards dir.
 * Shading is a value between 0 to 1, where 0 means fully in shadow
 * and 1 fully in light.
 *
 * As the cone steps out from the point, we estimate how far away
 * we are from the object based on the distance field, and if
 * dt/lightcone is less than 1, then the object seems to be inside
 * the cone and we factor that in as shading from that direction. */
static float computeShade(float3 pos, float3 dir, float maxDist, float cone_ratio, float start_length) {
    float t = start_length * 2.0f;
    float shade = 1.0f;
    while (1) {
        float dt = mandelboxDistance(pos + dir * t);
        /* Don't overstep in light calculation */
        if (t > maxDist - dt) {
	       	break;
	    }
		
		/* How wide is the projected light cone? */
        float lightcone = cone_ratio * t;
        
        /* This formula is ad-hoc, it's chosen because it shows attractive properties.
         * lightcone reduces any shading achieved from more distant objects
         * dt/lightdone is the term that approximates the degree of soft-shadowing
         * start_length adds a small bias that avoids detecting hard shadows while
         * very close to object */
        float shade_factor = lightcone + (dt + start_length) / lightcone;
        shade = min(shade, shade_factor);

		/* Termination condition: we can't accumulate any shading anymore */
        if (lightcone >= shade) {
            break;
        }
        
        /* Termination condition: we seem to have
         * hit object. (We could collect radiosity now.) */
        if (dt < start_length * 0.5f) {
            shade = lightcone;
            break;
        }
        
        t += dt;
    }
    return shade;
}

static float3 computeMandelboxColor(float3 pos, const float3 dir, const float t, const float detail) {
	float3 lightPos = pos + LIGHT_DIST;
    pos += dir * t;
	float3 lightDir = normalize(lightPos - pos);
    float dt = t * detail;

    float dx1 = mandelboxDistance(pos - (float3) { dt * 0.5f, 0, 0 });
    float dx2 = mandelboxDistance(pos + (float3) { dt * 0.5f, 0, 0 });
    float dy1 = mandelboxDistance(pos - (float3) { 0, dt * 0.5f, 0 });
    float dy2 = mandelboxDistance(pos + (float3) { 0, dt * 0.5f, 0 });
    float dz1 = mandelboxDistance(pos - (float3) { 0, 0, dt * 0.5f });
    float dz2 = mandelboxDistance(pos + (float3) { 0, 0, dt * 0.5f });
    float3 norm = normalize((float3) { dx2 - dx1, dy2 - dy1, dz2 - dz1 });
    
    /* Real world-space AO estimate */
    float ao = 0.0f;
    for (int i = 0; i < 8; i ++) {
        float3 aovec = (float3) { i & 1 ? 1 : -1, i & 2 ? -1 : 1, i & 4 ? -1 : 1 } * INVSQRT3;
        /* skip ao vectors behind wall plane */
        if (dot(aovec, norm) < 0.0f) {
            continue;
        }

		/* Build ambient occlusion estimate. How much AO is right? The factor 0.5 is matter of taste. */
        float aoshade = computeShade(pos, aovec, 1.0f, 1.0f, dt);
        ao += aoshade * 0.5f;
        
    	if (ao >= 1.0f) {
    		ao = 1.0f;
	    	break;
    	}
    }

    /* Estimate material type. */
    const float3 color = mandelboxColor(pos);

    /* Calculate diffuse and specular terms. */
    float diffuse = max(dot(lightDir, norm), 0.0f);
    const float3 microplane = normalize(lightDir - dir);
    float specular = max(dot(microplane, norm), 0.0f);

    float shadows = computeShade(pos, lightDir, length(lightPos - pos), 0.1f, dt);

    //ao = 1.0f;
    //shadows = 1.0f;
    //color = 1.0f;
    //diffuse = 1.0f;
    //specular = 0.0f;
    return color * (ao * (0.05f + shadows * diffuse))
        + 0.2f * shadows * pow(specular, 80.0f);
}

/* Return world color from pos to direction starting from distance at resolution indicated by detail */
static float3 render_world(float3 pos, float3 dir, float distance, float detail) {
    const float3 fog = (float3) { 0.035f, 0.05f, 0.075f } * (dot(dir, normalize(LIGHT_DIST)) * .5f + .5f);
    distance = intersectMandelbox(pos, dir, distance, detail);
    if (distance < 10.0f) {
        const float3 color = computeMandelboxColor(pos, dir, distance, detail);
        return mix(fog, color, exp(distance * DISTANCE_FOG));
    } else {
        return fog; // + pow(max(0.0f, dot(dir, LIGHT)), 30.0f);
    }
}

/* Return normalized vector in the current field of view in cylindrical projection */
static float3 project_cylindrical(float dx, float dy) {
    /* The view angle is 45 degrees in both directions, or pi/4. */
    float rot = rot_x + M_PI / 4.0f * dx;
    float factor = rsqrt(1 + dy * dy);
    return (float3) { sin(rot), cos(rot), -dy } * factor;
}

/* Approximated linear2srgb conversion */
static float3 srgb(const float3 color) {
    return color * -0.14f + sqrt(color) * 1.14f;
}

/* Shoot a ray into scene and return its color */
uchar4 __attribute__((kernel)) root(uint32_t x, uint32_t y) {
	float3 dir = project_cylindrical(x * invDim - 0.5f, y * invDim - 0.5f);

	float3 finalColor = render_world(pos, dir, 0.02f, invDim);

	/* Fix the color between the exposure range */
	finalColor = clamp(finalColor * exposure, 0.0f, 1.0f);
	/* sRGB transformation */
	finalColor = srgb(finalColor);
	/* Dither output with triangular dithering */
	finalColor += rsRand(0.0f, 1.0f / 255.0f) - rsRand(0.0f, 1.0f / 255.0f);
	/* Clamp to range */
	finalColor = clamp(finalColor, 0.0f, 1.0f);
    
	return rsPackColorTo8888(finalColor);
}

/* Build approximation of suitable brightness to select for the image
 * through averaging a few hundred samples across the image.
 * We use a power law with parameter topWeight to avoid
 * overexposing large parts of the image. 20 overexposes 10 % or so. */
 void collect_exposure() {
    exposure = 0.0f;
    float topWeight = 20.0f;
    int count = 0;
    for (float dy = -1.0f; dy <= 1.01f; dy += 0.1f) {
        for (float dx = -2.0f; dx <= 2.01f; dx += 0.1f) {
            float3 dir = project_cylindrical(dx, dy);
            float3 color = render_world(pos, dir, 0.01f, invDim);

            float brightness = max(max(color.x, color.y), color.z);
            exposure += pow(brightness, topWeight);
            count ++;
        }
    }

    exposure = 1.0f / pow(exposure / count, 1.0f / topWeight);
    rsSendToClient(2, &exposure, sizeof(exposure));
}

static float estimate_badness(float3 pos) {
    float distance_badness = 0.0f;
    float colored_sum = 0.0f;
    float chaos_sum = 0.0f;

    float3 previous_color = .5f;
    float previous_t = 1.0f;
    for (int d = 0; d < BADNESS_RAYS; d ++) {
    	/* We construct archimedes's spiral here as a sampling pattern. */
        float r = (float) d / (BADNESS_RAYS - 1);
        float arg = (float) d / (BADNESS_RAYS_SQRT - 0.5f) * 2 * M_PI;
        float dx = sin(arg) * r;
        float dy = cos(arg) * r;
        float3 dir = project_cylindrical(dx, dy);
        float t = intersectMandelbox(pos, dir, 0.01f, 1.0f / BADNESS_RAYS_SQRT);

        /* Maintain reasonableish distance to fractal */
        distance_badness += 0.2f / t + t;
        /* Penalize for changes in t (want smooth surfaces) */
        distance_badness += fabs(max(previous_t / t, t / previous_t) - 1.1f);
        previous_t = t;

        if (t < 10.0f) {
            const float3 color = mandelboxColor(pos);

            /* Measure changes in color */
            chaos_sum += length(color - previous_color);
            previous_color = color;

            /* Measure strongly saturated colors */
            colored_sum += max(max(color.x, color.y), color.z) - min(min(color.x, color.y), color.z);
        }
    }
    return distance_badness - colored_sum - chaos_sum;
}

/* Find a good starting position */
void randomize_position() {
    rot_x = linearRand(-M_PI, M_PI);
    rsSendToClient(0, &rot_x, sizeof(rot_x));

    float boundingbox;
    if (scale < 0) {
        boundingbox = 2;
    } else {
        boundingbox = 2.0f * (scale + 1) / (scale - 1);
    }

    /* Find a bunch of acceptable positions, pick the best one */
    float bestBadness = 1e18f;
    int j = 32;
    while (j > 0) {
        float3 trialPos = {
        	linearRand(-boundingbox, boundingbox),
        	linearRand(-boundingbox, boundingbox),
        	linearRand(-boundingbox, boundingbox)
        };
        /* Reject positions that seem to be very close / inside the object, or where the
         * light source would end up close/inside the object */
        if (mandelboxDistance(trialPos) < 0.02f || mandelboxDistance(trialPos + LIGHT_DIST) < 0.02f) {
            continue;
        }
        
        j --;
        float badness = estimate_badness(trialPos);
        if (badness < bestBadness) {
            pos = trialPos;
            bestBadness = badness;
        }
    }
    
    rsSendToClient(1, &pos, sizeof(pos));
}

void adjust_rot(float rot) {
    rot_x += rot;
    rsSendToClient(0, &rot_x, sizeof(rot_x));
}