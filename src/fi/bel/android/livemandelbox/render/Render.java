package fi.bel.android.livemandelbox.render;

import java.security.SecureRandom;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Sampler;
import android.util.Log;

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

	private final RenderScript rs;
	private final ScriptC_render render;
	private final ScriptC_fxaa fxaa;

	private int dim;

	private float scale;

	public Render(Context context) {
		rs = RenderScript.create(context);
		rs.setMessageHandler(new Ready());

		render = new ScriptC_render(rs);
		render.set_seed(RANDOM.nextInt());
		Log.i(TAG, String.format("Render seed: %d", render.get_seed()));

		fxaa = new ScriptC_fxaa(rs);
		fxaa.set_sampler(Sampler.CLAMP_LINEAR(rs));
	}

	public void prepare() {
		Log.i(TAG, "Calling RS prepare()");
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
		rs.finish();
		abm2.copyTo(bm2);
		bm1.recycle();
		abm2.destroy();
		abm1.destroy();

		long time3 = System.currentTimeMillis();
		Log.i(TAG, String.format("Completed: %d ms for render, %d ms for fxaa", time2 - time1, time3 - time2));
		return bm2;
	}

	public int getDim() {
		return dim;
	}
	public void setDim(int dim) {
		this.dim = dim;
		render.set_invDim(1.0f / dim);
		fxaa.set_dim(dim);
		fxaa.set_pixWidth(1.0f / dim);
	}

	public float getScale() {
		return scale;
	}
	public void setScale(float scale) {
		this.scale = scale;
		render.set_scale(scale);
	}
}
