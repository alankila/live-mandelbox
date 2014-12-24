package fi.bel.android.mandelbox.service;

import android.opengl.GLSurfaceView;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import fi.bel.android.mandelbox.R;
import fi.bel.android.mandelbox.render.ProjectionRenderer;

public class MandelboxWallpaperService extends WallpaperService {
	protected static final String TAG = MandelboxWallpaperService.class.getSimpleName();

	@Override
	public Engine onCreateEngine() {
		PreferenceManager.setDefaultValues(this, R.xml.prefs, true);
		return new MandelboxWallpaperServiceEngine();
	}

	private class MandelboxWallpaperServiceEngine extends WallpaperService.Engine {
        private final GLSurfaceView glSurfaceView;

		private final ProjectionRenderer renderer;

		private final GestureDetector gd = new GestureDetector(MandelboxWallpaperService.this, new GestureDetector.OnGestureListener() {
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
                    glSurfaceView.invalidate();
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

        public MandelboxWallpaperServiceEngine() {
			Log.i(TAG, "Creating wallpaper engine");
            glSurfaceView = new GLSurfaceView(MandelboxWallpaperService.this) {
                @Override
                public SurfaceHolder getHolder() {
                    return MandelboxWallpaperServiceEngine.this.getSurfaceHolder();
                }
            };
			renderer = new ProjectionRenderer(MandelboxWallpaperService.this);
            glSurfaceView.setRenderer(renderer);
		}

		@Override
		public void onTouchEvent(MotionEvent event) {
			gd.onTouchEvent(event);
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			super.onSurfaceCreated(holder);
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset,
				float xOffsetStep, float yOffsetStep, int xPixelOffset,
				int yPixelOffset) {
			renderer.setRotationByFraction(xOffset);
            glSurfaceView.invalidate();
		}
	}
}
