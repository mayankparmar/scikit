package scikit.graphics;

import java.awt.Canvas;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import scikit.util.Bounds;


public class Scene {
	protected Canvas _canvas;
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
		try {
			_canvas = GraphicsGL.createCanvas(this);
		} catch (Throwable t) {
			_canvas = GraphicsAWT.createCanvas(this);
		}
	}
	
	public Scene(String title) {
		this();
		scikit.util.Utilities.frame(_canvas, title);
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
		_canvas.repaint();
	}
	
	/** Completely clears the scene to it's initial state by removing all drawables and
	 * resetting the view bounds. */
	public void clear() {
		_drawables.clear();
		_curBounds = _topBounds.clone();
		_zoomed = false;
		_canvas.repaint();
	}
	
	public Canvas getCanvas() {
		return _canvas;
	}
	
	public Bounds canvasBounds() {
		return new Bounds(0, _canvas.getWidth(), 0, _canvas.getHeight());
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
