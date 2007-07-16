package scikit.graphics;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;

import scikit.util.*;


public class Scene2D extends Scene {
	
	// when the user zooms out (double clicks to "resetViewWindow()"), the current
	// view bounds is set to topBounds (and then extended to fit data).
	protected Bounds _topBounds = new Bounds();
	// is the view zoomed in?  this will disable autoscale
	protected boolean _zoomed = false;
	
	// is the mouse selection active?
	protected boolean _selectionActive = false;
	protected Point _selectionStart = new Point(), _selectionEnd = new Point();
	
	
	public Scene2D() {
		super();
		_canvas.addMouseListener(_mouseListener);
		_canvas.addMouseMotionListener(_mouseMotionListener);		
	}
	
	public Scene2D(String title) {
		this();
		scikit.util.Utilities.frame(_canvas, title);
	}
	
	public void animate(Drawable... drawables) {
		super.animate(drawables);
		_drawables.add(_selectionGraphics);
	}
	
	public void clear() {
		super.clear();
		_curBounds = _topBounds.clone();
		_zoomed = false;
	}
	
	public void setXRange(double xmin, double xmax) {
		_topBounds.xmin = _curBounds.xmin = xmin;
		_topBounds.xmax = _curBounds.xmax = xmax;
		_zoomed = false;
	}
	
	public void setYRange(double ymin, double ymax) {
		_topBounds.ymin = _curBounds.ymin = ymin;
		_topBounds.ymax = _curBounds.ymax = ymax;
		_zoomed = false;
	}
	
	public void resetViewWindow() {
		if (!_zoomed)
			_curBounds = _topBounds.createUnion(super.calculateCurrentBounds());
	}
	
	protected Bounds calculateCurrentBounds() {
		if (_zoomed)
			return _curBounds;
		else
			return _curBounds.createUnion(_topBounds, super.calculateCurrentBounds());
	}
		
	protected Point pixToCoord(Point pix) {
		Bounds cb = _curBounds;
		double x = cb.xmin + (cb.xmax - cb.xmin) * pix.x / _canvas.getWidth();
		double y = cb.ymin + (cb.ymax - cb.ymin) * pix.y / _canvas.getHeight();
		return new Point(x, y);
	}
	
	protected Point coordToPix(Point coord) {
		Bounds cb = _curBounds;
		double x = ((coord.x - cb.xmin)/(cb.xmax - cb.xmin)) * _canvas.getWidth();
		double y = ((coord.y - cb.ymin)/(cb.ymax - cb.ymin)) * _canvas.getHeight();
		return new Point(x, y);		
	}
	
	private Point eventToPix(MouseEvent event) {
		return new Point(event.getX()-1, _canvas.getHeight()-event.getY()+1);
	}
	
	private MouseListener _mouseListener = new MouseAdapter() {
		public void mouseClicked(MouseEvent event) {
			if (event.getClickCount() > 1) {
				_zoomed = false;
				_selectionActive = false;
				resetViewWindow();
				_canvas.repaint();
			}
		}
		public void mousePressed(MouseEvent event) {
			_selectionStart = eventToPix(event);
			_selectionEnd = eventToPix(event);
			_selectionActive = true;
			_canvas.repaint();
		}
		public void mouseReleased(MouseEvent event) {
			if (_selectionActive) {
				Bounds bds = new Bounds(pixToCoord(_selectionStart), pixToCoord(_selectionEnd));
				if (bds.getWidth() > _curBounds.getWidth()/128 &&
					bds.getHeight() > _curBounds.getHeight()/128) {
					_zoomed = true;
					_curBounds = bds;
				}
				_selectionActive = false;
				_canvas.repaint();
			}
		}
	};
	
	private MouseMotionListener _mouseMotionListener = new MouseMotionAdapter() {
		public void mouseDragged(MouseEvent event) {
			_selectionEnd = eventToPix(event);
			_canvas.repaint();
		}
	};
	
	private Drawable _selectionGraphics = new Drawable() {
		public void draw(Graphics g) {
			if (_selectionActive) {
				g.projectOrtho2D(g.scene().canvasBounds());
				
				Bounds sel = new Bounds(_selectionStart, _selectionEnd);
				g.setColor(new Color(0.3f, 0.6f, 0.5f, 0.2f));
				g.fillRect(sel.xmin, sel.ymin, sel.getWidth(), sel.getHeight());
				g.setColor(new Color(0.2f, 0.2f, 0.2f, 0.7f));
				g.drawRect(sel.xmin, sel.ymin, sel.getWidth(), sel.getHeight());
				
				g.projectOrtho2D(g.scene().dataBounds());
			}
		}
		public Bounds getBounds() {
			return new Bounds();
		}
	};
}
