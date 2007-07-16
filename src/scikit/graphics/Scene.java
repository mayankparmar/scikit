package scikit.graphics;

import java.awt.Canvas;
import java.util.ArrayList;
import java.util.List;

import scikit.util.Bounds;


public class Scene {
	protected Canvas _canvas;
	protected List<Drawable> _drawables = new ArrayList<Drawable>();
	protected Bounds _curBounds = new Bounds();
	
	
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
	
	public Canvas getCanvas() {
		return _canvas;
	}
	
	public void animate(Drawable... drawables) {
		_drawables.clear();
		for (Drawable d : drawables)
			_drawables.add(d);
		_curBounds = calculateCurrentBounds();
		_canvas.repaint();
	}
	
	public void clear() {
		_drawables.clear();
		_canvas.repaint();
	}
	
	public Bounds canvasBounds() {
		return new Bounds(0, _canvas.getWidth(), 0, _canvas.getHeight());
	}
	
	public Bounds dataBounds() {
		return _curBounds;
	}
	
	protected Bounds calculateCurrentBounds() {
		Bounds bounds = new Bounds();
		for (Drawable d : _drawables)
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
