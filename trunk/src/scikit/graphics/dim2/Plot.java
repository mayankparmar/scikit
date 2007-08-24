package scikit.graphics.dim2;

import static java.lang.Math.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;

import scikit.dataset.DataSet;
import scikit.graphics.Drawable;
import scikit.util.Bounds;


public class Plot extends Scene2D {
	ArrayList<RegisteredData> _datas = new ArrayList<RegisteredData>();
	// log-scale drawing is handled as follows:
	//  - all registered Datasets are reinterpreted (x,y)->(log x,log y)
	//  - the viewbounds are recalculated in log space
	//  - the TickMarks Drawable changes its mode
	//  - Drawables which are not Datasets are hidden, since non-linear warping
	//    can't be accurately represented
	boolean _logScaleX = false, _logScaleY = false;
	
	public Plot() {
		super();
		_visibleBoundsBufferScale = 1.1;
	}
	
	public Plot(String title) {
		this();
		scikit.util.Utilities.frame(_component, title);
	}
	
	public void animate() {
		// if it is invalid to display the system on a log scale then use a linear scale.
		// in this case, clear current view bounds so that super.animate() can start fresh.
		Bounds bds = calculateDataBounds();
		if (bds.xmin == Double.NEGATIVE_INFINITY) {
			_logScaleX = false;
			_curBounds = new Bounds();
		}
		if (bds.ymin == Double.NEGATIVE_INFINITY) {
			_logScaleY = false;
			_curBounds = new Bounds();
		}
		// do not draw geometric primitives on a log scale, since they won't be correctly
		// transformed
		_suppressDrawables = _logScaleX || _logScaleY;
		// ready to go!
		super.animate();
	}
	
	public void clear() {
		// remove data sets first because super.clear() will cause a drawAll() operation
		_datas.clear();
		super.clear();
	}
	
	/**
	 * Sets the plot view to optionally display the x and/or y coordinates on a logarithmic
	 * scale. Also animates the display.
	 * 
	 * @param logScaleX True if the x coordinate should be displayed on a logarithmic scale 
	 * @param logScaleY True if the y coordinate should be displayed on a logarithmic scale
	 */
	public void setLogScale(boolean logScaleX, boolean logScaleY) {
		if (logScaleX != _logScaleX || logScaleY != _logScaleY) {
			_logScaleX = logScaleX;
			_logScaleY = logScaleY;
			// clear current view bounds
			_curBounds = new Bounds();
			// calculate new view bounds and redisplay
			animate();
		}
	}
	
	/**
	 * Registers the dataset corresponding to <code>name</code> to display points. If a dataset
	 * with the same name is already registered, it will be replaced by this one. Also animates
	 * the display.
	 * 
	 * @param name The name of the dataset
	 * @param data The dataset to be registered
	 * @param color The color of the dataset
	 */
	public void registerPoints(String name, DataSet data, Color color) {
		registerDataset(name, data, color, RegisteredData.Style.MARKS);
	}

	/**
	 * Registers the dataset corresponding to <code>name</code> to display lines. If a dataset
	 * with the same name is already registered, it will be replaced by this one. Also animates
	 * the display.
	 * 
	 * @param name The name of the dataset
	 * @param data The dataset to be registered
	 * @param color The color of the dataset
	 */
	public void registerLines(String name, DataSet data, Color color) {
		registerDataset(name, data, color, RegisteredData.Style.LINES);
	}
	
	/**
	 * Registers the dataset corresponding to <code>name</code> to display bars. If a dataset
	 * with the same name is already registered, it will be replaced by this one. Also animates
	 * the display.
	 * 
	 * @param name The name of the dataset
	 * @param data The dataset to be registered
	 * @param color The color of the dataset
	 */
	public void registerBars(String name, DataSet data, Color color) {
		registerDataset(name, data, color, RegisteredData.Style.BARS);
	}
	
	protected List<Drawable<Gfx2D>> getAllDrawables() {
		List<Drawable<Gfx2D>> ds = new ArrayList<Drawable<Gfx2D>>();
		ds.add(new TickMarks(this));
		ds.addAll(_datas);
		ds.addAll(super.getAllDrawables());
		return ds;
	}
	
	protected List<JMenuItem> getAllPopupMenuItems() {
		List<JMenuItem> ret = new ArrayList<JMenuItem>(super.getAllPopupMenuItems());
		
		// add log/linear scale menu items
		JMenuItem itemX = new JMenuItem(_logScaleX ? "Set Linear in X" : "Set Logarithmic in X");
		JMenuItem itemY = new JMenuItem(_logScaleY ? "Set Linear in Y" : "Set Logarithmic in Y");
		itemX.setEnabled(_logScaleX || calculateDataBounds().xmin > 0);
		itemY.setEnabled(_logScaleY || calculateDataBounds().ymin > 0);
		itemX.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setLogScale(!_logScaleX, _logScaleY);
			}
		});
		itemY.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setLogScale(_logScaleX, !_logScaleY);
			}
		});
		ret.add(itemX);
		ret.add(itemY);
		
		// add save dataset menu items
		for (final RegisteredData d : _datas) {
			JMenuItem menuItem = new JMenuItem("Save '" + d._name + "' ...");
			menuItem.setForeground(d._color);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveDataset(d._data, d._name);
				}
			});
			ret.add(menuItem);
		}
		return ret;
	}
	
	private void registerDataset(String name, DataSet data, Color color, RegisteredData.Style style) {
		RegisteredData dw = new RegisteredData(this, name, data, color, style);
		// if the list contains an element with the same name as 'dataset',
		// replace that element with 'dataset'
		if (_datas.contains(dw))
			_datas.set(_datas.indexOf(dw), dw);
		// otherwise, add 'dataset' to the end of the list
		else
			_datas.add(dw);
		
		animate();
	}
	
	private void saveDataset(DataSet data, String str) {
		try {
			PrintWriter pw = scikit.util.Dump.pwFromDialog(_component, str);
			if (pw != null)
				scikit.util.Dump.writeColumns(pw, data.copyData(), 2);
		} catch (IOException e) {}
	}
}

class RegisteredData implements Drawable<Gfx2D> {
	enum Style {LINES, MARKS, BARS};
	
	Plot _plot;
	String _name;
	DataSet _data;
	Color _color;
	Style _style;

	public RegisteredData(Plot plot, String name, DataSet data, Color color, Style style) {
		_plot = plot;
		_name = name;
		_data = data;
		_color = color;
		_style = style;
	}

	public void draw(Gfx2D g) {
		Bounds bds = expBounds(g.scene().viewBounds());
		double pts[] = _data.copyPartial(1000, bds.xmin, bds.xmax, bds.ymin, bds.ymax);
		g.setColor(_color);
		
		for (int i = 0; i < pts.length; i += 2) {
			if (_plot._logScaleX)
				pts[i+0] = log10(pts[i+0]);
			if (_plot._logScaleY)
				pts[i+1] = log10(pts[i+1]);
			
			switch (_style) {
			case MARKS:
				g.drawPoint(pts[i+0], pts[i+1]);
				break;
			case LINES:
				if (i >= 2)
					g.drawLine(pts[i-2], pts[i-1], pts[i+0], pts[i+1]);
				break;
			case BARS:
				g.drawLine(pts[i+0], pts[i+1], pts[i+0], 0);
				break;
			}
		}
	}
	
	public Bounds getBounds() {
		Bounds bds = logBounds(_data.getBounds());
		if (_style == Style.BARS) {
			bds.ymin = min(bds.ymin, 0);
			bds.ymax = max(bds.ymax, 0);
		}
		if (_plot._logScaleY && _style == Style.BARS)
			throw new IllegalArgumentException("Can't draw bars with vertical logscale.");
		return bds;
	}
	
	// implement a special form of equality: two "dataset drawables" are equal
	// when their names are equal.
	public boolean equals(Object data) {
		if (data instanceof RegisteredData)
			return _name.equals(((RegisteredData)data)._name);
		else
			return false;
	}

	private Bounds expBounds(Bounds in) {
		double xmin = _plot._logScaleX ? pow(10, in.xmin) : in.xmin;
		double xmax = _plot._logScaleX ? pow(10, in.xmax) : in.xmax;
		double ymin = _plot._logScaleY ? pow(10, in.ymin) : in.ymin;
		double ymax = _plot._logScaleY ? pow(10, in.ymax) : in.ymax;
		return new Bounds(xmin, xmax, ymin, ymax);
	}
	
	private Bounds logBounds(Bounds in) {
		// we use (xmin,xmax == +inf,-inf) to represent the absence of bounds;
		// taking, e.g., max(_,0) preserves this convention in log space.
		// furthermore, using max(_,0) guarantees bounds will not be NaN.
		double xmin = _plot._logScaleX ? log10(max(in.xmin,0)) : in.xmin;
		double xmax = _plot._logScaleX ? log10(max(in.xmax,0)) : in.xmax;
		double ymin = _plot._logScaleY ? log10(max(in.ymin,0)) : in.ymin;
		double ymax = _plot._logScaleY ? log10(max(in.ymax,0)) : in.ymax;
		return new Bounds(xmin, xmax, ymin, ymax);		
	}
}

