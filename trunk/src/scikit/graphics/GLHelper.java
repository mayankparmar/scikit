package scikit.graphics;

import java.awt.Dimension;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.swing.SwingUtilities;

public class GLHelper {
	public interface DisplayListener {
		public void init(GLAutoDrawable gl);
		public void display(GLAutoDrawable gl);
	}

	public static GLCanvas createComponent(final DisplayListener listener) {
		GLCapabilities capabilities = new GLCapabilities();
		
		// For some unknown reason, enabling GL "sample buffers" actually make anti-aliased lines
		// look much worse on OS X. I guess it disables custom anti-aliasing code.
		//
		// capabilities.setSampleBuffers(true);
		// capabilities.setNumSamples(4);
		
		// could use a GLJPanel instead of GLCanvas -- this would avoid some rendering
		// errors related to mixing lightweight (swing) and heavyweight (GL) components,
		// but would also be slower
		final GLCanvas canvas = new GLCanvas(capabilities) {
			private static final long serialVersionUID = 1L;
			// mystery: for some reason, repaint() doesn't work, so we must override to
			// explicitly call display() from GUI thread.
			public void repaint() {
				// N.B.: can't call display() from this thread, since its a blocking call, but
				// the GUI call is already blocked waiting for us!
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						display();
					}
				});
			}
		};
		
		canvas.addGLEventListener(new GLEventListener() {
			public void display(GLAutoDrawable glDrawable) {
				GL gl = glDrawable.getGL();
				gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
				listener.display(glDrawable);
			}

			public void displayChanged(GLAutoDrawable gLDrawable, boolean modeChanged, boolean deviceChanged) {
				// do nothing
			}

			public void init(GLAutoDrawable glDrawable) {
				GL gl = glDrawable.getGL();
				gl.glClearColor(1f, 1f, 1f, 0.0f);
				gl.glEnable(GL.GL_BLEND);
				gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
				gl.glEnable(GL.GL_LINE_SMOOTH);
				// gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
				gl.glLineWidth(1.0f);
				gl.glPointSize(4.0f);
				listener.init(glDrawable);
			}

			public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) {
				GL gl = glDrawable.getGL();
				gl.glViewport(0, 0, width, height);
			}
		});

		canvas.setPreferredSize(new Dimension(300, 300));
		return canvas;
	}
}
