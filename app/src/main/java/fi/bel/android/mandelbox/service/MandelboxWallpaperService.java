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
		private final GestureDetector gd = new GestureDetector(MandelboxWallpaperService.this, new GestureDetector.OnGestureListener() {
			@Override
			public boolean onDown(MotionEvent e) {
				return false;
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				return false;
			}

			@Override
			public void onLongPress(MotionEvent e) {
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
    			renderer.rotateByPixels(distanceX);
                glSurfaceView.requestRender();
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

        private GLSurfaceView glSurfaceView;

        private ProjectionRenderer renderer;

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
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		}

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (isPreview()) {
                //Log.i(TAG, "Touch event");
                gd.onTouchEvent(event);
            }
        }

        @Override
		public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            if (!isPreview()) {
                //Log.i(TAG, "Offset changed");
                renderer.setRotationByFraction(xOffset);
                glSurfaceView.requestRender();
            }
		}

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            Log.i(TAG, "Surface created");
            glSurfaceView.surfaceCreated(holder);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            Log.i(TAG, "Surface changed");
            glSurfaceView.surfaceChanged(holder, format, width, height);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            Log.i(TAG, String.format("Surface about to become visible: %s", visible));
            if (visible) {
                ViewUpdateService.aboutToView(MandelboxWallpaperService.this);
                renderer.reloadTextures();
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            Log.i(TAG, "Surface destroyed");
            glSurfaceView.surfaceDestroyed(holder);
        }
    }
}
