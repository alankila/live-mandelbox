package fi.bel.android.mandelbox.render;

import java.security.SecureRandom;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Sampler;
import android.renderscript.Script;
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

    private final Bitmap bm2;

    private final Allocation abm1, abm2;

	public Render(Context context, float scale, int dim) {
		rs = RenderScript.create(context);
		rs.setMessageHandler(new Ready());

		render = new ScriptC_render(rs);
		render.set_seed(RANDOM.nextInt());
		render.set_invDim(1.0f / dim);
		Log.i(TAG, String.format("Render seed: %d", render.get_seed()));

		fxaa = new ScriptC_fxaa(rs);
		fxaa.set_sampler(Sampler.CLAMP_LINEAR(rs));
		fxaa.set_dim(dim);
		fxaa.set_pixWidth(1.0f / dim);

        if (scale != 0) {
            render.set_scale(scale);
        } else {
            float randomScale = RANDOM.nextFloat() * 5 + (1.5f * 1.5f);
            randomScale = (float) Math.sqrt(randomScale);
            randomScale *= RANDOM.nextBoolean() ? -1 : 1;
            Log.i(TAG, String.format("Random scale value: %.3f", randomScale));
            render.set_scale(randomScale);
        }
		long time1 = System.currentTimeMillis();
		render.invoke_randomize_position();
		rs.finish();
		long time2 = System.currentTimeMillis();
		render.invoke_collect_exposure();
		rs.finish();
		long time3 = System.currentTimeMillis();

		Log.i(TAG, String.format("Starting position in %d ms, exposure in %d ms", time2 - time1, time3 - time2));

        bm2 = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888);
		abm1 = Allocation.createFromBitmap(rs, bm2);
        abm2 = Allocation.createFromBitmap(rs, bm2);
	}

	public Bitmap getImage(float angle) {
		long time1 = System.currentTimeMillis();
		render.invoke_adjust_rot(angle);
        Script.LaunchOptions lc = new Script.LaunchOptions();
        /* Using 256k pixel budget per invocation -- experimentally determined from Nexus 5 */
        int pixelBudget = Math.min(bm2.getHeight(), 256 * 1024 / bm2.getWidth());
        for (int y = 0; y < bm2.getHeight(); y += pixelBudget) {
            lc.setY(y, y + pixelBudget);
            Log.i(TAG, String.format("Rendering chunk from %d to %d", lc.getYStart(), lc.getYEnd()));
            render.forEach_root(abm1, lc);
            rs.finish();
        }
		render.invoke_adjust_rot(-angle);

		long time2 = System.currentTimeMillis();
		fxaa.set_in(abm1);
		fxaa.forEach_root(abm2);
        abm2.copyTo(bm2);
        rs.finish();
		long time3 = System.currentTimeMillis();

		Log.i(TAG, String.format("Completed: %d ms for render, %d ms for fxaa", time2 - time1, time3 - time2));
		return bm2;
	}
}
