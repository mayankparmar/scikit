package scikit.graphics;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import scikit.util.Bounds;


public class Scene {
	protected Component _component;
	protected Bounds _curBounds = new Bounds();
	private List<Drawable> _drawables = new ArrayList<Drawable>();
	
	// when the user zooms out (double clicks to "resetViewWindow()"), the current
	// view bounds is set to topBounds (and then extended to fit data).
	protected Bounds _topBounds = new Bounds();
	// is the view zoomed in?  this will disable autoscale
	protected boolean _zoomed = false;
	// should the view autoscale in and out?
	protected boolean _autoScale = false;
	
	public Scene() {
		_component = createComponent();
	}
	
	public Scene(String title) {
		this();
		scikit.util.Utilities.frame(_component, title);
	}
	
	// returns an OpenGL hardware accelerated GLCanvas if it is available, otherwise an AWT backed Canvas.
	// uses reflection to avoid referring directly to the classes GLCapabities or GraphicsGL -- otherwise
	// we could get an uncatchable NoClassDefFoundError.
	private Component createComponent() {
		try {
			Class<?> c = Class.forName("javax.media.opengl.GLCapabilities");
			if ((Boolean)c.getMethod("getHardwareAccelerated").invoke(c.newInstance())) {
				c = Class.forName("scikit.graphics.GraphicsGL");
				return (Component)c.getMethod("createComponent", Scene.class).invoke(null, this);
			}
		}
		catch (Exception e) {}
		return GraphicsAWT.createComponent(this);				
	}
	
	
	/** Removes all drawables object from the scene leaving the state of the scene (such as
	 * view bounds) unmodified.
	 */
	public void clearDrawables() {
		_drawables.clear();
		animate();
	}
	
	/** Adds drawables objects to the scene. */
	public void addDrawables(Drawable... drawables) {
		_drawables.addAll(Arrays.asList(drawables));
		animate();
	}
	
	/** Sets the scene's drawable objects to be the specified list. */
	public void setDrawables(Drawable... drawables) {
		_drawables.clear();
		_drawables.addAll(Arrays.asList(drawables));
		animate();
	}

	/** Animates the scene by updating the view bounds and repainting the canvas component. */
	public void animate() {
		if (!_zoomed) {
			if (_autoScale)
				_curBounds = calculateDataBounds();
			else
				_curBounds = _curBounds.createUnion(calculateDataBounds());
		}
		_component.repaint();
	}
	
	/** Completely clears the scene to it's initial state by removing all drawables and
	 * resetting the view bounds. */
	public void clear() {
		_drawables.clear();
		_curBounds = _topBounds.clone();
		_zoomed = false;
		_component.repaint();
	}
	
	public Component getCanvas() {
		return _component;
	}
	
	public Bounds pixelBounds() {
		return new Bounds(0, _component.getWidth(), 0, _component.getHeight());
	}
	
	public Bounds dataBounds() {
		return _curBounds;
	}
	
	
	protected List<Drawable> allDrawables() {
		return _drawables;
	}
	
	protected Bounds calculateDataBounds() {
		Bounds bounds = new Bounds();
		for (Drawable d : allDrawables())
			bounds = (Bounds)bounds.createUnion(d.getBounds());
		
		// extend bounds a little bit
		double w = bounds.xmax - bounds.xmin;
		double h = bounds.ymax - bounds.ymin;
		bounds.xmin -= w/16;
		bounds.xmax += w/16;
		bounds.ymin -= h/16;
		bounds.ymax += h/16;
		return bounds;
	}
}
