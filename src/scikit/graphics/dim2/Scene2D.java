package scikit.graphics.dim2;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

import scikit.graphics.Drawable;
import scikit.graphics.Scene;
import scikit.util.*;


public class Scene2D extends Scene<Gfx2D> {	
	// is the mouse selection active?
	protected boolean _selectionActive = false;
	protected Point _selectionStart = new Point(), _selectionEnd = new Point();
	
	public Scene2D(String title) {
		super(title);
		_canvas.addMouseListener(_mouseListener);
		_canvas.addMouseMotionListener(_mouseListener);
	}
	
	public void setAutoScale(boolean autoScale) {
		_autoScale = autoScale;
	}
	
	// returns an OpenGL hardware accelerated GLCanvas if it is available, otherwise an AWT backed Canvas.
	// uses reflection to avoid referring directly to the classes GLCapabilities or Gfx2DGL, otherwise
	// an uncatchable NoClassDefFoundError may be thrown when the Scene2D class is loaded.
	protected Component createCanvas() {
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
		g.setProjection(g.pixelBounds());
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, g.pixelBounds().getWidth(), g.pixelBounds().getHeight());
	}
	
	protected void drawAll(Gfx2D g) {
		drawBackground(g);
		g.setProjection(viewBounds());
		for (Drawable<Gfx2D> d : getAllDrawables())
			d.draw(g);
	}
	 
	protected List<Drawable<Gfx2D>> getAllDrawables() {
		List<Drawable<Gfx2D>> ds = new ArrayList<Drawable<Gfx2D>>();
		ds.addAll(super.getAllDrawables());
		ds.add(_selectionGraphics);
		return ds;
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
	
	protected List<JMenuItem> getAllPopupMenuItems() {
		List<JMenuItem> ret = super.getAllPopupMenuItems();
		JMenuItem item = new JMenuItem("Zoom to Fit");
		item.setEnabled(boundsIsValid());
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				zoomToFitData();
			}
		});
		ret.add(item);
		return ret;
	}
	
	private boolean boundsIsValid() {
		return _curBounds.getWidth() > 0 && _curBounds.getHeight() > 0;
	}
	
	private void zoomToFitData() {
		_zoomed = false;
		_curBounds = new Bounds();
		animate();
	}
	
	private Point eventToPix(MouseEvent event) {
		return new Point(event.getX()-1, _canvas.getHeight()-event.getY()+1);
	}
	
	private MouseInputListener _mouseListener = new MouseInputAdapter() {
		public void mouseClicked(MouseEvent event) {
			if (event.getClickCount() > 1) {
				_selectionActive = false;
				zoomToFitData();
			}
		}
		public void mousePressed(MouseEvent event) {
			if (boundsIsValid() && !event.isPopupTrigger()) {
				_selectionStart = eventToPix(event);
				_selectionEnd = eventToPix(event);
				_selectionActive = true;
				_canvas.repaint();
			}
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
		public void mouseDragged(MouseEvent event) {
			_selectionEnd = eventToPix(event);
			_canvas.repaint();
		}
	};
	
	private Drawable<Gfx2D> _selectionGraphics = new Drawable<Gfx2D>() {
		public void draw(Gfx2D g) {
			if (_selectionActive) {
				g.setLineSmoothing(false);
				g.setProjection(g.pixelBounds());
				
				Bounds sel = new Bounds(_selectionStart, _selectionEnd);
				g.setColor(new Color(0.6f, 0.9f, 0.8f, 0.25f));
				g.fillRect(sel.xmin, sel.ymin, sel.getWidth(), sel.getHeight());
				g.setColor(new Color(0f, 0f, 0f, 0.25f));
				g.drawRect(sel.xmin, sel.ymin, sel.getWidth(), sel.getHeight());
				
				g.setProjection(g.viewBounds());
				g.setLineSmoothing(true);
			}
		}
		public Bounds getBounds() {
			return new Bounds();
		}
	};
}
