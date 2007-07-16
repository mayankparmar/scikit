package scikit.graphics;

import static java.lang.Math.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import scikit.dataset.DataSet;
import scikit.util.Bounds;


public class Plot extends Scene2D {
	ArrayList<DatasetDw> _datas = new ArrayList<DatasetDw>();
	boolean _logScaleX = false, _logScaleY = false;
	
	public Plot() {
		super();
	}
	
	public Plot(String title) {
		this();
		scikit.util.Utilities.frame(_canvas, title);
	}
	
	public void setLogScale(boolean logScaleX, boolean logScaleY) {
		_logScaleX = logScaleX;
		_logScaleY = logScaleY;
	}
	
	public void animate(Drawable... drawables) {
		List<Drawable> ds = new ArrayList<Drawable>();
		ds.add(new TickMarks(_canvas));
		ds.addAll(Arrays.asList(drawables));
		ds.addAll(_datas);
		super.animate(ds.toArray(new Drawable[0]));
		
		// register pulldown save dialog for dataset?
	}
	
	public void addPoints(String name, DataSet data, Color color) {
		add(new DatasetDw(name, data, color, DatasetDw.Style.MARKS));
	}

	public void addLines(String name, DataSet data, Color color) {
		add(new DatasetDw(name, data, color, DatasetDw.Style.LINES));
	}
	
	public void addBars(String name, DataSet data, Color color) {
		add(new DatasetDw(name, data, color, DatasetDw.Style.BARS));
	}
	
	private void add(DatasetDw data) {
		// if the list contains an element with the same name as 'dataset',
		// replace that element with 'dataset'
		if (_datas.contains(data))
			_datas.set(_datas.indexOf(data), data);
		// otherwise, add 'dataset' to the end of the list
		else
			_datas.add(data);
		animate();
	}
}


class DatasetDw implements Drawable {
	enum Style {LINES, MARKS, BARS};
	
	String _name;
	DataSet _data;
	Color _color;
	Style _style;
	
	public DatasetDw(String name, DataSet data, Color color, Style style) {
		_name = name;
		_data = data;
		_color = color;
		_style = style;
	}		
	
	public void draw(Graphics g) {
		Bounds bounds = g.scene().dataBounds();
		g.setColor(_color);
		
		double pts[] = _data.copyPartial(1000, bounds.xmin, bounds.xmax, bounds.ymin, bounds.ymax);
		
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
	
	public Bounds getBounds() {
		double[] bds = _data.getBounds();
		if (_style == Style.BARS) {
			bds[2] = min(bds[2], 0);
			bds[3] = max(bds[3], 0);
		}
		return new Bounds(bds[0], bds[1], bds[2], bds[3]);
	}
	
	// implement a limited form of equality: two "dataset drawables" are equal
	// when their names are equal.
	public boolean equals(Object data) {
		if (data instanceof DatasetDw)
			return _name.equals(((DatasetDw)data)._name);
		else
			return false;
	}
}
