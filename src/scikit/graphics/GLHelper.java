package scikit.graphics;

import java.awt.Dimension;

import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.swing.SwingUtilities;

public class GLHelper {
	public static GLCanvas createComponent(GLEventListener listener) {
		GLCapabilities capabilities = new GLCapabilities();
		
		// For some unknown reason, enabling GL "sample buffers" actually make anti-aliased lines
		// look much worse on OS X. I guess it disables custom anti-aliasing code. It's better
		// to use only glEnable(GL.GL_LINE_SMOOTH).
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
		
		canvas.addGLEventListener(listener);
		canvas.setPreferredSize(new Dimension(300, 300));
		return canvas;
	}
}
