package scikit.graphics.dim2;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import scikit.graphics.Drawable;
import scikit.graphics.Scene;
import scikit.util.*;


public class Scene2D extends Scene<Gfx2D> {	
	// is the mouse selection active?
	protected boolean _selectionActive = false;
	protected Point _selectionStart = new Point(), _selectionEnd = new Point();
	
	
	public Scene2D() {
		super();
		_component.addMouseListener(_mouseListener);
		_component.addMouseMotionListener(_mouseMotionListener);
	}
	
	public Scene2D(String title) {
		this();
		scikit.util.Utilities.frame(_component, title);
	}
	
	// returns an OpenGL hardware accelerated GLCanvas if it is available, otherwise an AWT backed Canvas.
	// uses reflection to avoid referring directly to the classes GLCapabities or GraphicsGL -- otherwise
	// we could get an uncatchable NoClassDefFoundError.
	protected Component createComponent() {
		try {
			Class<?> c = Class.forName("javax.media.opengl.GLCapabilities");
			if ((Boolean)c.getMethod("getHardwareAccelerated").invoke(c.newInstance())) {
				c = Class.forName("scikit.graphics.dim2.Gfx2DGL");
				return (Component)c.getMethod("createComponent", Scene2D.class).invoke(null, this);
			}
		}
		catch (Exception e) {}
		return Gfx2DSwing.createComponent(this);				
	}
	
	protected void drawBackground(Gfx2D g) {
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, _component.getWidth(), _component.getHeight());
	}
	
	protected void drawAll(Gfx2D g) {
		drawBackground(g);
		g.projectOrtho2D(dataBounds());
		for (Drawable<Gfx2D> d : getAllDrawables())
			d.draw(g);
	}
	 
	protected List<Drawable<Gfx2D>> getAllDrawables() {
		List<Drawable<Gfx2D>> ds = new ArrayList<Drawable<Gfx2D>>();
		ds.addAll(super.getAllDrawables());
		ds.add(_selectionGraphics);
		return ds;
	}
	
	public void setAutoScale(boolean autoScale) {
		_autoScale = autoScale;
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
	
	protected Point pixToCoord(Point pix) {
		Bounds cb = _curBounds;
		double x = cb.xmin + (cb.xmax - cb.xmin) * pix.x / _component.getWidth();
		double y = cb.ymin + (cb.ymax - cb.ymin) * pix.y / _component.getHeight();
		return new Point(x, y);
	}
	
	protected Point coordToPix(Point coord) {
		Bounds cb = _curBounds;
		double x = ((coord.x - cb.xmin)/(cb.xmax - cb.xmin)) * _component.getWidth();
		double y = ((coord.y - cb.ymin)/(cb.ymax - cb.ymin)) * _component.getHeight();
		return new Point(x, y);		
	}
	
	private Point eventToPix(MouseEvent event) {
		return new Point(event.getX()-1, _component.getHeight()-event.getY()+1);
	}
	
	private MouseListener _mouseListener = new MouseAdapter() {
		public void mouseClicked(MouseEvent event) {
			if (event.getClickCount() > 1) {
				_zoomed = false;
				_selectionActive = false;
				_curBounds = _topBounds.createUnion(calculateDataBounds());
				_component.repaint();
			}
		}
		public void mousePressed(MouseEvent event) {
			_selectionStart = eventToPix(event);
			_selectionEnd = eventToPix(event);
			_selectionActive = true;
			_component.repaint();
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
				_component.repaint();
			}
		}
	};
	
	private MouseMotionListener _mouseMotionListener = new MouseMotionAdapter() {
		public void mouseDragged(MouseEvent event) {
			_selectionEnd = eventToPix(event);
			_component.repaint();
		}
	};
	
	private Drawable<Gfx2D> _selectionGraphics = new Drawable<Gfx2D>() {
		public void draw(Gfx2D g) {
			if (_selectionActive) {
				g.setLineSmoothing(false);
				g.projectOrtho2D(g.scene().pixelBounds());
				
				Bounds sel = new Bounds(_selectionStart, _selectionEnd);
				g.setColor(new Color(0.6f, 0.9f, 0.8f, 0.25f));
				g.fillRect(sel.xmin, sel.ymin, sel.getWidth(), sel.getHeight());
				g.setColor(new Color(0f, 0f, 0f, 0.25f));
				g.drawRect(sel.xmin, sel.ymin, sel.getWidth(), sel.getHeight());
				
				g.projectOrtho2D(g.scene().dataBounds());
				g.setLineSmoothing(true);
			}
		}
		public Bounds getBounds() {
			return new Bounds();
		}
	};
}
