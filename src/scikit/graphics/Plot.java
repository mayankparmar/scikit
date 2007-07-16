package scikit.graphics;

import static java.lang.Math.*;
import java.awt.Color;
import scikit.dataset.DataSet;
import scikit.util.Bounds;


public class Plot extends Scene2D {
	Drawable _tickMarks = new TickMarks(_canvas);
	
	public Plot() {
		super();
	}
	
	public Plot(String title) {
		this();
		scikit.util.Utilities.frame(_canvas, title);
	}
	
	public void animate(Drawable... drawables) {
		super.animate(drawables);
		_drawables.add(0, _tickMarks);
		
		// register pulldown save dialog for dataset?
	}
	
	
	public static Drawable points(DataSet data, Color color) {
		return new DatasetGraphics(data, color, DatasetGraphics.Style.MARKS);
	}

	public static Drawable lines(DataSet data, Color color) {
		return new DatasetGraphics(data, color, DatasetGraphics.Style.LINES);
	}
	
	public static Drawable bars(DataSet data, Color color) {
		return new DatasetGraphics(data, color, DatasetGraphics.Style.BARS);
	}
}


class DatasetGraphics implements Drawable {
	enum Style {LINES, MARKS, BARS};

	private DataSet _dataset;
	private Color _color;
	private Style _style;
	
	public DatasetGraphics(DataSet data, Color color, Style style) {
		_dataset = data;
		_color = color;
		_style = style;
	}		
	
	public void draw(Graphics g) {
		Bounds bounds = g.scene().dataBounds();
		g.setColor(_color);
		
		double data[] = _dataset.copyPartial(1000, bounds.xmin, bounds.xmax, bounds.ymin, bounds.ymax);
		
		for (int i = 0; i < data.length; i += 2) {
			switch (_style) {
			case MARKS:
				g.drawPoint(data[i], data[i+1]);
				break;
			case LINES:
				if (i >= 2)
					g.drawLine(data[i-2], data[i-1], data[i], data[i+1]);
				break;
			case BARS:
				g.drawLine(data[i], data[i+1], data[i], 0);
				break;
			}
		}
	}
	
	public Bounds getBounds() {
		double[] bds = _dataset.getBounds();
		if (_style == Style.BARS) {
			bds[2] = min(bds[2], 0);
			bds[3] = max(bds[3], 0);
		}
		return new Bounds(bds[0], bds[1], bds[2], bds[3]);
	}
}	
