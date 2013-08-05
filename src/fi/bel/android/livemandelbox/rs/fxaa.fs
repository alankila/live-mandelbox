#pragma version(1)
#pragma rs java_package_name(fi.bel.android.livemandelbox.render)

static const float4 LUM_FACTORS = { 0.299f, 0.587f, 0.114f, 0.0f };

rs_allocation in;
rs_sampler sampler;
int dim;
float pixWidth;

static const float fxaaQualitySubpix = 1.00f;
static const float fxaaQualityEdgeThreshold = 0.125f;
static const float fxaaQualityEdgeThresholdMin = 0.0625f;

/* Note that there's no code to handle PS > 3.
 * This is the FXAA_QUALITY__PRESET 20 */
#define FXAA_QUALITY__PS 3
#define FXAA_QUALITY__P0 1.5f
#define FXAA_QUALITY__P1 2.0f
#define FXAA_QUALITY__P2 8.0f

static float4 get_rgb(float2 xy) {
	return rsSample(in, sampler, (xy + 0.5f) * pixWidth);
}

static float get_luma2(float2 xy) {
    return dot(get_rgb(xy), LUM_FACTORS);
} 

static float get_luma(int32_t x, int32_t y) {
	x = rsClamp(x, 0, dim - 1);
	y = rsClamp(y, 0, dim - 1);
	return dot(rsUnpackColor8888(rsGetElementAt_uchar4(in, x, y)), LUM_FACTORS);
} 

typedef float FxaaFloat;
typedef float2 FxaaFloat2;
typedef bool FxaaBool;
uchar4 __attribute__((kernel)) root(uint32_t x, uint32_t y) {
    FxaaFloat2 posM;
    posM.x = x;
    posM.y = y;
   
   	FxaaFloat lumaM = get_luma(x, y);
	FxaaFloat lumaS = get_luma(x, y + 1);
	FxaaFloat lumaE = get_luma(x + 1, y);
	FxaaFloat lumaN = get_luma(x, y - 1);
	FxaaFloat lumaW = get_luma(x - 1, y);
	
    FxaaFloat maxSM = max(lumaS, lumaM);
    FxaaFloat minSM = min(lumaS, lumaM);
    FxaaFloat maxESM = max(lumaE, maxSM);
    FxaaFloat minESM = min(lumaE, minSM);
    FxaaFloat maxWN = max(lumaN, lumaW);
    FxaaFloat minWN = min(lumaN, lumaW);
    FxaaFloat rangeMax = max(maxWN, maxESM);
    FxaaFloat rangeMin = min(minWN, minESM);
    FxaaFloat rangeMaxScaled = rangeMax * fxaaQualityEdgeThreshold;
    FxaaFloat range = rangeMax - rangeMin;
    FxaaFloat rangeMaxClamped = max(fxaaQualityEdgeThresholdMin, rangeMaxScaled);
    FxaaBool earlyExit = range < rangeMaxClamped;
	if (earlyExit) {
 		return rsGetElementAt_uchar4(in, x, y);
    }
    
    FxaaFloat lumaNW = get_luma(x - 1, y - 1);
    FxaaFloat lumaSE = get_luma(x + 1, y + 1);
    FxaaFloat lumaNE = get_luma(x + 1, y - 1);
    FxaaFloat lumaSW = get_luma(x - 1, y + 1);
    
    FxaaFloat lumaNS = lumaN + lumaS;
    FxaaFloat lumaWE = lumaW + lumaE;
    FxaaFloat subpixRcpRange = 1.0f/range;
    FxaaFloat subpixNSWE = lumaNS + lumaWE;
    FxaaFloat edgeHorz1 = (-2.0f * lumaM) + lumaNS;
    FxaaFloat edgeVert1 = (-2.0f * lumaM) + lumaWE;

    FxaaFloat lumaNESE = lumaNE + lumaSE;
    FxaaFloat lumaNWNE = lumaNW + lumaNE;
    FxaaFloat edgeHorz2 = (-2.0f * lumaE) + lumaNESE;
    FxaaFloat edgeVert2 = (-2.0f * lumaN) + lumaNWNE;

    FxaaFloat lumaNWSW = lumaNW + lumaSW;
    FxaaFloat lumaSWSE = lumaSW + lumaSE;
    FxaaFloat edgeHorz4 = (fabs(edgeHorz1) * 2.0f) + fabs(edgeHorz2);
    FxaaFloat edgeVert4 = (fabs(edgeVert1) * 2.0f) + fabs(edgeVert2);
    FxaaFloat edgeHorz3 = (-2.0f * lumaW) + lumaNWSW;
    FxaaFloat edgeVert3 = (-2.0f * lumaS) + lumaSWSE;
    FxaaFloat edgeHorz = fabs(edgeHorz3) + edgeHorz4;
    FxaaFloat edgeVert = fabs(edgeVert3) + edgeVert4;
    
    FxaaFloat subpixNWSWNESE = lumaNWSW + lumaNESE;
    FxaaFloat lengthSign = 1;
    FxaaBool horzSpan = edgeHorz >= edgeVert;
    FxaaFloat subpixA = subpixNSWE * 2.0f + subpixNWSWNESE;

    if(!horzSpan) lumaN = lumaW;
    if(!horzSpan) lumaS = lumaE;
    if(horzSpan) lengthSign = 1;
    FxaaFloat subpixB = (subpixA * (1.0f/12.0f)) - lumaM;
    
    FxaaFloat gradientN = lumaN - lumaM;
    FxaaFloat gradientS = lumaS - lumaM;
    FxaaFloat lumaNN = lumaN + lumaM;
    FxaaFloat lumaSS = lumaS + lumaM;
    FxaaBool pairN = fabs(gradientN) >= fabs(gradientS);
    FxaaFloat gradient = max(fabs(gradientN), fabs(gradientS));
    if(pairN) lengthSign = -lengthSign;
    FxaaFloat subpixC = clamp(fabs(subpixB) * subpixRcpRange, 0.0f, 1.0f);
    
    FxaaFloat2 posB;
    posB.x = posM.x;
    posB.y = posM.y;
    FxaaFloat2 offNP;
    offNP.x = (!horzSpan) ? 0.0f : 1.0f;
    offNP.y = ( horzSpan) ? 0.0f : 1.0f;
    if(!horzSpan) posB.x += lengthSign * 0.5f;
    if( horzSpan) posB.y += lengthSign * 0.5f;
    
    FxaaFloat2 posN;
    posN.x = posB.x - offNP.x * FXAA_QUALITY__P0;
    posN.y = posB.y - offNP.y * FXAA_QUALITY__P0;
    FxaaFloat2 posP;
    posP.x = posB.x + offNP.x * FXAA_QUALITY__P0;
    posP.y = posB.y + offNP.y * FXAA_QUALITY__P0;
    FxaaFloat subpixD = ((-2.0f)*subpixC) + 3.0f;
    FxaaFloat lumaEndN = get_luma2(posN);
    FxaaFloat subpixE = subpixC * subpixC;
    FxaaFloat lumaEndP = get_luma2(posP);
    
    if(!pairN) lumaNN = lumaSS;
    FxaaFloat gradientScaled = gradient * 1.0f/4.0f;
    FxaaFloat lumaMM = lumaM - lumaNN * 0.5f;
    FxaaFloat subpixF = subpixD * subpixE;
    FxaaBool lumaMLTZero = lumaMM < 0.0f;

    lumaEndN -= lumaNN * 0.5f;
    lumaEndP -= lumaNN * 0.5f;
    FxaaBool doneN = fabs(lumaEndN) >= gradientScaled;
    FxaaBool doneP = fabs(lumaEndP) >= gradientScaled;
    if(!doneN) posN.x -= offNP.x * FXAA_QUALITY__P1;
    if(!doneN) posN.y -= offNP.y * FXAA_QUALITY__P1;
    FxaaBool doneNP = (!doneN) || (!doneP);
    if(!doneP) posP.x += offNP.x * FXAA_QUALITY__P1;
    if(!doneP) posP.y += offNP.y * FXAA_QUALITY__P1;
    
    if(doneNP) {
        if(!doneN) lumaEndN = get_luma2(posN);
        if(!doneP) lumaEndP = get_luma2(posP);
        if(!doneN) lumaEndN = lumaEndN - lumaNN * 0.5f;
        if(!doneP) lumaEndP = lumaEndP - lumaNN * 0.5f;
        doneN = fabs(lumaEndN) >= gradientScaled;
        doneP = fabs(lumaEndP) >= gradientScaled;
        if(!doneN) posN.x -= offNP.x * FXAA_QUALITY__P2;
        if(!doneN) posN.y -= offNP.y * FXAA_QUALITY__P2;
        doneNP = (!doneN) || (!doneP);
        if(!doneP) posP.x += offNP.x * FXAA_QUALITY__P2;
        if(!doneP) posP.y += offNP.y * FXAA_QUALITY__P2;    
    }

    FxaaFloat dstN = posM.x - posN.x;
    FxaaFloat dstP = posP.x - posM.x;
    if(!horzSpan) dstN = posM.y - posN.y;
    if(!horzSpan) dstP = posP.y - posM.y;

    FxaaBool goodSpanN = (lumaEndN < 0.0f) != lumaMLTZero;
    FxaaFloat spanLength = (dstP + dstN);
    FxaaBool goodSpanP = (lumaEndP < 0.0f) != lumaMLTZero;
    FxaaFloat spanLengthRcp = 1.0f/spanLength;

    FxaaBool directionN = dstN < dstP;
    FxaaFloat dst = min(dstN, dstP);
    FxaaBool goodSpan = directionN ? goodSpanN : goodSpanP;
    FxaaFloat subpixG = subpixF * subpixF;
    FxaaFloat pixelOffset = (dst * (-spanLengthRcp)) + 0.5f;
    FxaaFloat subpixH = subpixG * fxaaQualitySubpix;

    FxaaFloat pixelOffsetGood = goodSpan ? pixelOffset : 0.0f;
    FxaaFloat pixelOffsetSubpix = max(pixelOffsetGood, subpixH);
    if(!horzSpan) posM.x += pixelOffsetSubpix * lengthSign;
    if( horzSpan) posM.y += pixelOffsetSubpix * lengthSign;
    
	return rsPackColorTo8888(get_rgb(posM));
}
