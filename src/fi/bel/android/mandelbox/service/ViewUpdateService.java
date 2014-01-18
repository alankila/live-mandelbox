
package fi.bel.android.mandelbox.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.opengl.ETC1Util;
import android.opengl.ETC1Util.ETC1Texture;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import fi.bel.android.mandelbox.render.Render;

public class ViewUpdateService extends IntentService {
	protected static final String TAG = ViewUpdateService.class.getSimpleName();

	public static int getUpdateFrequency(Context ctx) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		return Integer.valueOf(prefs.getString("update_frequency", null));
	}

	public static long getViewTime(Context ctx) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		return prefs.getLong("view_time", 0);
	}

	public static void setViewTime(Context ctx, long time) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		Editor edit = prefs.edit();
		edit.putLong("view_time", time);
		edit.commit();
	}

	public static void aboutToView(final Context ctx) {
		/* Image never been displayed yet? */
		if (getViewTime(ctx) == 0) {
			Log.i(TAG, "First view time of image set");
			setViewTime(ctx, System.currentTimeMillis());
		}

		final int keepInterval = getUpdateFrequency(ctx);
		if (keepInterval == -1) {
			Log.i(TAG, "User asked no update.");
			return;
		}

		long desiredChangeTime = getViewTime(ctx) + keepInterval * 1000;
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		PendingIntent alarmIntent = PendingIntent.getService(ctx, 0, new Intent(ctx, ViewUpdateService.class), 0);
		am.cancel(alarmIntent);
		am.set(AlarmManager.RTC, desiredChangeTime, alarmIntent);
		Log.i(TAG, "Update scheduled to occur at " + (desiredChangeTime - System.currentTimeMillis()) + " ms");
	}

	private WakeLock wake;

	public ViewUpdateService() {
		super(ViewUpdateService.class.getSimpleName());
	}

	@Override
	public void onCreate() {
		super.onCreate();
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MandelBox render");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		render();
	}

	private void render() {
		/* Don't recalculate if the file was just earlier done (multiple intents may get sent). */
		long updateFrequency = getUpdateFrequency(this);
		if (updateFrequency == -1) {
			return;
		}

		long viewTime = getViewTime(this);
		if (viewTime == 0 || viewTime + updateFrequency * 1000 > System.currentTimeMillis()) {
			Log.i(TAG, "Update triggered, but vetoing: view recent enough.");
			return;
		}

		try {
			wake.acquire();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			float scale = Float.valueOf(prefs.getString("scale", null));
			int dim = Integer.valueOf(prefs.getString("dim", null));

            Render render = new Render(this, scale, dim);
			makeImage(render, (float) (-Math.PI/8), "0.tmp");
			makeImage(render, (float) (Math.PI/8), "1.tmp");

			File left = getFileStreamPath("0.tmp");
			File right = getFileStreamPath("1.tmp");
			File leftReal = getFileStreamPath("0.pkm");
			File rightReal = getFileStreamPath("1.pkm");
			left.renameTo(leftReal);
			right.renameTo(rightReal);
			setViewTime(this, 0);
		}
		catch (IOException ioe) {
			Log.e(TAG, "Failed to save image", ioe);
		}
		finally {
			wake.release();
		}
	}

	private void makeImage(Render render, float angle, String name) throws IOException {
		Bitmap bitmap = render.getImage(angle);
		int dim = bitmap.getWidth();

		ByteBuffer bb = ByteBuffer.allocateDirect(dim * dim * 3);
		for (int y = 0; y < dim; y += 1) {
			for (int x = 0; x < dim; x += 1) {
				int pixel = bitmap.getPixel(x, y);
				bb.put((byte) (pixel >> 16));
				bb.put((byte) (pixel >> 8));
				bb.put((byte) (pixel >> 0));
			}
		}
		bb.rewind();
		ETC1Texture compressed = ETC1Util.compressTexture(bb, dim, dim, 3, dim*3);

		FileOutputStream outFile = openFileOutput(name, Context.MODE_PRIVATE);
		ETC1Util.writeTexture(compressed, outFile);
		outFile.close();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
}
