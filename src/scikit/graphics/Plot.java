package scikit.graphics;

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
import scikit.dataset.Transformer;
import scikit.util.Bounds;
import scikit.util.Point;


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
	
	protected List<Drawable> allDrawables() {
		List<Drawable> ds = new ArrayList<Drawable>();
		ds.add(new TickMarks(this));
		ds.addAll(_datas);
		ds.addAll(super.allDrawables());
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


class DatasetDw implements Drawable {
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

	public void draw(Graphics g) {
		Bounds bounds = g.scene().dataBounds();
		g.setColor(_color);

		double pts[] = transformedData().copyPartial(1000, bounds.xmin, bounds.xmax, bounds.ymin, bounds.ymax);

		for (int i = 0; i < pts.length; i += 2) {
			switch (_style) {
			case MARKS:
				g.drawPoint(pts[i], pts[i+1]);
				break;
			case LINES:
				if (i >= 2)
					g.drawLine(pts[i-2], pts[i-1], pts[i], pts[i+1]);
				break;
			case BARS:
				g.drawLine(pts[i], pts[i+1], pts[i], 0);
				break;
			}
		}
	}

	private DataSet transformedData() {
		return new Transformer(_data) {
			public void transform(Point p) {
				if (_plot._logScaleX) p.x = log10(p.x);
				if (_plot._logScaleY) p.y = log10(p.y);
			}
		};
	}
	
	public Bounds getBounds() {
		double[] bds = transformedData().getBounds();
		if (_style == Style.BARS) {
			bds[2] = min(bds[2], 0);
			bds[3] = max(bds[3], 0);
		}
		return new Bounds(bds[0], bds[1], bds[2], bds[3]);
	}

	// implement a special form of equality: two "dataset drawables" are equal
	// when their names are equal.
	public boolean equals(Object data) {
		if (data instanceof DatasetDw)
			return _name.equals(((DatasetDw)data)._name);
		else
			return false;
	}
}

