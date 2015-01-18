package fi.bel.android.mandelbox.render;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.ETC1Util;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.util.Log;

import fi.bel.android.mandelbox.service.ViewUpdateService;

public class ProjectionRenderer implements GLSurfaceView.Renderer {
	protected static final String TAG = ProjectionRenderer.class.getSimpleName();
	private static final int QUALITY = 16;
	private static final int FOV = 90;

	private final Context context;

	private float upaxisAngle = 0;
	private float fovX = 180;
	private int width;

	private final ByteBuffer leftVertex, rightVertex;
	private final ByteBuffer leftTexCoord, rightTexCoord;
	private int[] texture;
    protected boolean needReload = true;

	public ProjectionRenderer(Context context) {
		this.context = context;

		leftVertex = ByteBuffer.allocateDirect(4 * 3 * (QUALITY + 1) * 2);
		leftVertex.order(ByteOrder.nativeOrder());
		rightVertex = ByteBuffer.allocateDirect(4 * 3 * (QUALITY + 1) * 2);
		rightVertex.order(ByteOrder.nativeOrder());
		leftTexCoord = ByteBuffer.allocateDirect(4 * 2 * (QUALITY + 1) * 2);
		leftTexCoord.order(ByteOrder.nativeOrder());
		rightTexCoord = ByteBuffer.allocateDirect(4 * 2 * (QUALITY + 1) * 2);
		rightTexCoord.order(ByteOrder.nativeOrder());

		makeCylinder(leftVertex.asFloatBuffer(), leftTexCoord.asFloatBuffer(), (float) -Math.PI / 2);
		makeCylinder(rightVertex.asFloatBuffer(), rightTexCoord.asFloatBuffer(), 0);
	}

	private void makeCylinder(FloatBuffer vertex, FloatBuffer texCoord, float offset) {
		/* Build a tesselation of a cylinder */
		for (int i = 0; i < QUALITY + 1; i ++) {
			float texCoordPos = (float) i / QUALITY;

			final float pi = (float) Math.PI;
			float vertexX = (float) Math.sin(texCoordPos * pi / 2 + offset);
			float vertexY = (float) Math.cos(texCoordPos * pi / 2 + offset);

			/* vertices go from:
			 * bottom left, bottom right, top left, top right
			 */
			vertex.put(new float[] {
					vertexX, vertexY, -1,
					vertexX, vertexY, 1,
			});
			texCoord.put(new float[] {
					texCoordPos, 1,
					texCoordPos, 0,
			});
		}
	}

	@Override
	public void onDrawFrame(GL10 gl) {
        if (needReload) {
            try {
                loadTexture(gl, texture[0], "0.pkm");
                loadTexture(gl, texture[1], "1.pkm");
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            needReload = false;
        }

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		gl.glLoadIdentity();
		gl.glRotatef(upaxisAngle, 0, 0, 1);
		drawTexture(gl, texture[0], leftVertex, leftTexCoord);
		drawTexture(gl, texture[1], rightVertex, rightTexCoord);
	}

	private void drawTexture(GL10 gl, int texNum, Buffer vertex, Buffer texCoord) {
	    gl.glBindTexture(GL10.GL_TEXTURE_2D, texNum);
	    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertex);
	    gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texCoord);
	    gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, (QUALITY + 1) * 2);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        texture = new int[2];
        gl.glGenTextures(2, texture, 0);
        Log.i(TAG, String.format("Loading new textures: %d and %d", texture[0], texture[1]));

        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    }

    public void reloadTextures() {
        needReload = true;
    }

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
	    this.width = width;

	    gl.glViewport(0, 0, width, height);
	    gl.glMatrixMode(GL10.GL_PROJECTION);
	    gl.glLoadIdentity();
	    float aspect = (float) width / height;
	    GLU.gluPerspective(gl, FOV, aspect, 0.5f, 2.0f);
	    GLU.gluLookAt(gl, 0, 0, 0, 0, 1, 0, 0, 0, 1);
	    gl.glMatrixMode(GL10.GL_MODELVIEW);
	    fovX = (float) (180 * (Math.atan(Math.tan(FOV / 180.0 * Math.PI / 2) * aspect) * 2) / Math.PI);

        reloadTextures();
	}

    /**
     * Load a texture from filesystem and bind it to texture
     *
     * @param gl
     * @param tex
     * @param name
     * @throws IOException
     */
	public void loadTexture(GL10 gl, int tex, String name) throws IOException {
		gl.glBindTexture(GL10.GL_TEXTURE_2D, tex);
	    gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
	    gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
	    gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
	    gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

	    InputStream textureStream;
	    if (context.getFileStreamPath(name).exists()) {
	    	textureStream = context.openFileInput(name);
	    } else {
	    	textureStream = context.getAssets().open(name);
	    }

	    ETC1Util.loadTexture(GL10.GL_TEXTURE_2D, 0, 0, GL10.GL_RGB, GL10.GL_UNSIGNED_SHORT_5_6_5, textureStream);
	    textureStream.close();
	}

    /**
     * Rotate view angle
     *
     * @param dx
     */
	public void rotateByPixels(float dx) {
		upaxisAngle += dx / width * fovX;
		upaxisAngle = Math.max(Math.min(90 - fovX/2, upaxisAngle), -90 + fovX / 2);
	}

    /**
     * Rotate view angle
     *
     * @param xOffset
     */
	public void setRotationByFraction(float xOffset) {
		float range = 90 - fovX/2;
		upaxisAngle = (xOffset - 0.5f) * 2f * range;
	}
}