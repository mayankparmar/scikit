package scikit.graphics;

import static scikit.util.Utilities.OPTIMAL_FRAME_SIZE;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import scikit.util.Bounds;
import scikit.util.Window;
import scikit.util.Utilities;


abstract public class Scene<T> implements Window {
	protected Component _component; // contains scene and possible other GUI objects
	protected Component _canvas;    // the canvas on which scene is drawn
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

	private String _title;
	
	public Scene(String title) {
		_canvas = createCanvas();
		_canvas.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) { maybeShowPopup(e); }
			public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }
		});
		_component = createComponent(_canvas);
		_component.setPreferredSize(new Dimension(OPTIMAL_FRAME_SIZE, OPTIMAL_FRAME_SIZE));
		_title = title;
	}
	
	public String getTitle() {
		return _title;
	}

	/**
	 * Gets the GUI component object for this scene 
	 */
	public Component getComponent() {
		return _component;
	}
	
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
		if (Utilities.isComponentShowing(_canvas)) {
			_canvas.repaint();
		}
	}
	
	/** Completely clears the scene to it's initial state by removing all drawables and
	 * resetting the view bounds. */
	public void clear() {
		_drawables.clear();
		_curBounds = new Bounds();
		_zoomed = false;
		_canvas.repaint();
	}
	
	/**
	 * Returns the current viewing bounds for the same, in data coordinates.
	 * @return current scene view bounds
	 */
	public Bounds viewBounds() {
		return _curBounds.clone();
	}
	
	/**
	 * Draws all objects in the scene
	 * @param g the graphics engine
	 */
	abstract protected void drawAll(T g);
	
	/**
	 * Creates the canvas GUI component on which the scene will be drawn.  This object
	 * may display a pop-up menu when requested.
	 * @return the canvas GUI component
	 */
	abstract protected Component createCanvas();
	
	/**
	 * Creates a wrapper around the canvas component which may contain additional GUI
	 * content.
	 * @param canvas
	 * @return a component which wraps the canvas object
	 */
	protected Component createComponent(Component canvas) {
		JPanel component = new JPanel(new BorderLayout());
		component.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(4, 4, 4, 4),
				BorderFactory.createLineBorder(Color.GRAY)));
		component.add(canvas);
		return component;
	}
	
	/**
	 * Gets a list of all drawable objects contained in the scene.
	 * @return the list of all drawable objects
	 */
	protected List<Drawable<T>> getAllDrawables() {
		return _suppressDrawables ? new ArrayList<Drawable<T>>() : _drawables;
	}
	
	/**
	 * Calculates the bounds for all data contained in the scene.
	 * @return the total data bounds
	 */
	protected Bounds calculateDataBounds() {
		Bounds bounds = new Bounds();
		for (Drawable<T> d : getAllDrawables())
			bounds = (Bounds)bounds.union(d.getBounds());
		return bounds;
	}
	
	/**
	 * Gets all menu items to be included when the user opens a popup menu from the GUI canvas
	 * @return the list of popup menu items
	 */
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
