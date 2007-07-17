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
	
	public void displayAll(Drawable... drawables) {
		_drawables = Arrays.asList(drawables);
		display();
	}

	public void display() {
		if (!_zoomed) {
			if (_autoScale)
				_curBounds = calculateDataBounds();
			else
				_curBounds = _curBounds.createUnion(calculateDataBounds());
		}
		_canvas.repaint();
	}
	
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
