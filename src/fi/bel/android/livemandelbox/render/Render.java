package fi.bel.android.livemandelbox.render;

import java.security.SecureRandom;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.Sampler;
import android.util.Log;
import fi.bel.android.livemandelbox.R;

public class Render {
	private static final SecureRandom RANDOM = new SecureRandom();

	protected class Ready extends RenderScript.RSMessageHandler {
		@Override
		public void run() {
			switch (mID) {
			case 0: {
				float rotX = Float.intBitsToFloat(mData[0]);
				Log.i(TAG, String.format("Received rotation: %f", rotX));
				break;
			}
			case 1: {
				float x = Float.intBitsToFloat(mData[0]);
				float y = Float.intBitsToFloat(mData[1]);
				float z = Float.intBitsToFloat(mData[2]);
				Log.i(TAG, String.format("Received pos: %f %f %f", x, y, z));
				break;
			}
			case 2: {
				float exposure = Float.intBitsToFloat(mData[0]);
				Log.i(TAG, String.format("Received exposure: %f", exposure));
				break;
			}
			case 3: {
				float debug = Float.intBitsToFloat(mData[0]);
				Log.i(TAG, String.format("Received debug float: %f", debug));
				break;
			}
			default:
				Log.i(TAG, String.format("Received unknown message of %d bytes: %d", mLength, mID));
			}
		}
	}

	private static final String TAG = Render.class.getSimpleName();

	/*
	private static double clamp(double x, double min, double max) {
		if (x < min) {
			return min;
		}
		if (x > max) {
			return max;
		}
		return x;
	}

	public static double mb(Float3 pos, double scale) {
	    Double4 iter = new Double4(pos.x, pos.y, pos.z, 1);
	    Double4 pos0 = new Double4(pos.x, pos.y, pos.z, 1);

	    for (int i = 0; i < 18; i ++) {
	    	iter.x = clamp(iter.x, -1, 1) * 2 - iter.x;
	    	iter.y = clamp(iter.y, -1, 1) * 2 - iter.y;
	    	iter.z = clamp(iter.z, -1, 1) * 2 - iter.z;
	        double f = 1 / clamp(iter.x * iter.x + iter.y * iter.y + iter.z * iter.z, 0.25, 1.0);
	        iter.x = iter.x * scale * f + pos0.x;
	        iter.y = iter.y * scale * f + pos0.y;
	        iter.z = iter.z * scale * f + pos0.z;
	        iter.w = iter.w * scale * f + pos0.w;
	    }

	    double result = iter.x * iter.x + iter.y * iter.y + iter.z * iter.z;
	    return Math.sqrt(result) / iter.w;
	}*/

	private final int dim;
	private final RenderScript rs;
	private final ScriptC_render render;
	private final ScriptC_fxaa fxaa;

	public Render(Context context, float scale, int dim) {
		this.dim = dim;

		rs = RenderScript.create(context);
		rs.setMessageHandler(new Ready());

		render = new ScriptC_render(rs, context.getResources(), R.raw.render);
		render.set_invDim(1.0f / dim);
		render.set_scale(scale);
		render.set_seed(RANDOM.nextInt());
		Log.i(TAG, String.format("Render seed: %d", render.get_seed()));

		fxaa = new ScriptC_fxaa(rs, context.getResources(), R.raw.fxaa);
		fxaa.set_dim(dim);
		fxaa.set_pixWidth(1.0f / dim);
		fxaa.set_sampler(Sampler.CLAMP_LINEAR(rs));
	}

	public void prepare() {
		long time1 = System.currentTimeMillis();
		render.invoke_randomize_position();
		rs.finish();
		long time2 = System.currentTimeMillis();
		render.invoke_collect_exposure();
		rs.finish();
		long time3 = System.currentTimeMillis();

		Log.i(TAG, String.format("Starting position in %d ms, exposure in %d ms", time2 - time1, time3 - time2));
	}

	public Bitmap getImage(float angle) {
		long time1 = System.currentTimeMillis();
		Bitmap bm1 = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888);
		Allocation abm1 = Allocation.createFromBitmap(rs, bm1);
		render.invoke_adjust_rot(angle);
		render.forEach_root(abm1);
		render.invoke_adjust_rot(-angle);
		rs.finish();
		abm1.copyTo(bm1);

		long time2 = System.currentTimeMillis();
		Bitmap bm2 = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888);
		Allocation abm2 = Allocation.createFromBitmap(rs, bm2);
		fxaa.set_in(abm1);
		fxaa.forEach_root(abm2);
		abm2.copyTo(bm2);
		bm1.recycle();
		abm2.destroy();
		abm1.destroy();
		rs.finish();

		long time3 = System.currentTimeMillis();
		Log.i(TAG, String.format("Completed: %d ms for render, %d ms for fxaa", time2 - time1, time3 - time2));
		return bm2;
	}

	public int getDim() {
		return dim;
	}
}
