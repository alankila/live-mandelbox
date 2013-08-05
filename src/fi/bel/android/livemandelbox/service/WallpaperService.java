package fi.bel.android.livemandelbox.service;

import net.rbgrn.android.glwallpaperservice.GLWallpaperService;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import fi.bel.android.livemandelbox.R;
import fi.bel.android.livemandelbox.render.ProjectionRenderer;

public class WallpaperService extends GLWallpaperService {
	protected static final String TAG = WallpaperService.class.getSimpleName();

	@Override
	public Engine onCreateEngine() {
		PreferenceManager.setDefaultValues(this, R.xml.prefs, true);
		return new WallpaperServiceEngine();
	}

	private class WallpaperServiceEngine extends GLEngine {
		private final ProjectionRenderer renderer;

		private final GestureDetector gd = new GestureDetector(WallpaperService.this, new GestureDetector.OnGestureListener() {
			@Override
			public boolean onDown(MotionEvent e) {
				return false;
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {
				return false;
			}

			@Override
			public void onLongPress(MotionEvent e) {
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2,
					float distanceX, float distanceY) {
				if (isPreview()) {
					renderer.rotateByPixels(distanceX);
					requestRender();
				}
				return true;
			}

			@Override
			public void onShowPress(MotionEvent e) {
			}

			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				return false;
			}
		});

		public WallpaperServiceEngine() {
			Log.i(TAG, "Creating wallpaper engine");
			renderer = new ProjectionRenderer(WallpaperService.this);
			setRenderer(renderer);
		}

		@Override
		public void onTouchEvent(MotionEvent event) {
			gd.onTouchEvent(event);
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			super.onSurfaceCreated(holder);
			if (! isPreview()) {
				setRenderMode(GLEngine.RENDERMODE_WHEN_DIRTY);
			}
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset,
				float xOffsetStep, float yOffsetStep, int xPixelOffset,
				int yPixelOffset) {
			renderer.setRotationByFraction(xOffset);
			requestRender();
		}
	}
}
