package scikit.graphics;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import scikit.util.Bounds;


abstract public class Scene<T> {
	protected Component _component;
	protected Bounds _curBounds = new Bounds();
	protected List<Drawable<T>> _drawables = new ArrayList<Drawable<T>>();
	protected JPopupMenu _popup = new JPopupMenu();
	
	// if true, suppress inclusion of _drawables in the return value of getAllDrawables()
	protected boolean _suppressDrawables = false;
	// view bounds can be scaled to include additional buffer space
	protected double _visibleBoundsBufferScale = 1;
	// is the view zoomed in?  this will disable autoscale
	protected boolean _zoomed = false;
	// if false, bounds will zoom out to fit data; if true, will zoom both in and out
	protected boolean _autoScale = false;
	
	
	public Scene() {
		_component = createComponent();
		_component.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) { maybeShowPopup(e); }
			public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }
		});
	}
	
	public Scene(String title) {
		this();
		scikit.util.Utilities.frame(_component, title);
	}
	
	abstract protected Component createComponent(); 
	
	/** Removes all drawables object from the scene leaving the state of the scene (such as
	 * view bounds) unmodified.
	 */
	public void clearDrawables() {
		_drawables.clear();
		animate();
	}
	
	/** Adds drawables objects to the scene. */
	public void addDrawable(Drawable<T> drawable) {
		_drawables.add(drawable);
		animate();
	}
	
	/** Sets the scene's drawable objects to be the specified list. */
	public void setDrawables(List<Drawable<T>> drawables) {
		_drawables.clear();
		_drawables.addAll(drawables);
		animate();
	}

	/** Animates the scene by updating the view bounds and repainting the canvas component. */
	public void animate() {
		if (!_zoomed) {
			_curBounds = calculateVisibleBounds(_autoScale ? new Bounds() : _curBounds);
		}
		_component.repaint();
	}
	
	/** Completely clears the scene to it's initial state by removing all drawables and
	 * resetting the view bounds. */
	public void clear() {
		_drawables.clear();
		_curBounds = new Bounds();
		_zoomed = false;
		_component.repaint();
	}
	
	public Component getCanvas() {
		return _component;
	}
	
	public Bounds pixelBounds() {
		return new Bounds(0, _component.getWidth(), 0, _component.getHeight());
	}
	
	public Bounds viewBounds() {
		return _curBounds.clone();
	}
	
	abstract protected void drawAll(T g);
	
	protected List<Drawable<T>> getAllDrawables() {
		return _suppressDrawables ? new ArrayList<Drawable<T>>() : _drawables;
	}
	
	/**
	 * Calculates the bounds for all data contained in the scene.
	 * @return
	 */
	protected Bounds calculateDataBounds() {
		Bounds bounds = new Bounds();
		for (Drawable<T> d : getAllDrawables())
			bounds = (Bounds)bounds.union(d.getBounds());
		return bounds;
	}
	
	protected List<JMenuItem> getAllPopupMenuItems() {
		return new ArrayList<JMenuItem>();
	}
	
	/* Calculates the visible bounds for the scene. These bounds are big enough
	 * to contain all data in the scene, as well as possibly some buffer space.
	 */
	private Bounds calculateVisibleBounds(Bounds oldBds) {
		Bounds datBds = calculateDataBounds();
		double eps = 0.001;
		double s = _visibleBoundsBufferScale;
		return oldBds.scale(1/(s+eps)).union(datBds).scale(s).union(oldBds);
	}
	
	private void maybeShowPopup(MouseEvent e) {
		List<JMenuItem> items = getAllPopupMenuItems();
		if (e.isPopupTrigger() && items.size() > 0) {
			_popup.removeAll();
			for (JMenuItem item : items)
				_popup.add(item);
			_popup.show(e.getComponent(), e.getX(), e.getY());
		}
	}
}
