package scikit.graphics.dim2;

import static java.lang.Math.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import scikit.dataset.DataSet;
import scikit.graphics.Drawable;
import scikit.util.Bounds;


public class Plot extends Scene2D {
	ArrayList<DatasetDw> _datas = new ArrayList<DatasetDw>();
	boolean _logScaleX = false, _logScaleY = false;
	protected JPopupMenu _popup = new JPopupMenu();
	
	
	public Plot() {
		super();
		_component.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) { maybeShowPopup(e); }
			public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }
		});
	}
	
	public Plot(String title) {
		this();
		scikit.util.Utilities.frame(_component, title);
	}
	
	public void clear() {
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
			_curBounds = _topBounds.clone();
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
		registerDataset(name, data, color, DatasetDw.Style.MARKS);
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
		registerDataset(name, data, color, DatasetDw.Style.LINES);
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
		registerDataset(name, data, color, DatasetDw.Style.BARS);
	}
	
	protected List<Drawable<Gfx2D>> getAllDrawables() {
		List<Drawable<Gfx2D>> ds = new ArrayList<Drawable<Gfx2D>>();
		ds.add(new TickMarks(this));
		ds.addAll(_datas);
		ds.addAll(super.getAllDrawables());
		return ds;
	}

	private void registerDataset(String name, DataSet data, Color color, DatasetDw.Style style) {
		DatasetDw dw = new DatasetDw(this, name, data, color, style);
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
			scikit.util.Dump.writeColumns(pw, data.copyData(), 2);
		} catch (IOException e) {}
	}
	
	private void maybeShowPopup(MouseEvent e) {
		if (e.isPopupTrigger() && _datas.size() > 0) {
			_popup.removeAll();
			for (final DatasetDw d : _datas) {
				JMenuItem menuItem = new JMenuItem("Save '" + d._name + "' ...");
				menuItem.setForeground(d._color);
				menuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						saveDataset(d._data, d._name);
					}
				});
				_popup.add(menuItem);
			}
			_popup.show(e.getComponent(), e.getX(), e.getY());
		}
	}
}


class DatasetDw implements Drawable<Gfx2D> {
	enum Style {LINES, MARKS, BARS};
	
	Plot _plot;
	String _name;
	DataSet _data;
	Color _color;
	Style _style;

	public DatasetDw(Plot plot, String name, DataSet data, Color color, Style style) {
		_plot = plot;
		_name = name;
		_data = data;
		_color = color;
		_style = style;
	}		

	public void draw(Gfx2D g) {
		Bounds bds = expBounds(g.scene().dataBounds());
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
		if (data instanceof DatasetDw)
			return _name.equals(((DatasetDw)data)._name);
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
		double xmin = _plot._logScaleX ? log10(max(in.xmin,0)) : in.xmin;
		double xmax = _plot._logScaleX ? log10(max(in.xmax,0)) : in.xmax;
		double ymin = _plot._logScaleY ? log10(max(in.ymin,0)) : in.ymin;
		double ymax = _plot._logScaleY ? log10(max(in.ymax,0)) : in.ymax;
		return new Bounds(xmin, xmax, ymin, ymax);		
	}
}

