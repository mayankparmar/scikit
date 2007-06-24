package scikit.graphics;

import static java.lang.Math.*;
import java.awt.Color;
import javax.media.opengl.GL;
import scikit.dataset.DataSet;
import scikit.util.Bounds;


public class Plot extends Canvas2D {
	enum Style {LINES, MARKS, BARS};
	Graphics _tickMarks = new TickMarks(_canvas);
	
	public Plot() {
		super();
	}
	
	public Plot(String title) {
		this();
		scikit.util.Utilities.frame(_canvas, title);
	}
		
	public void addPoints(DataSet data, Color color) {
		addDataset(data, color, Style.MARKS);
	}

	public void addLines(DataSet data, Color color) {
		addDataset(data, color, Style.LINES);
	}
	
	public void addBars(DataSet data, Color color) {
		addDataset(data, color, Style.BARS);
	}
	
	
	protected void drawAllGraphics(GL gl, Bounds bounds) {
		_tickMarks.draw(gl, bounds);
		super.drawAllGraphics(gl, bounds);
	}
	
	
	private void addDataset(DataSet data, Color color, Style style) {
		DatasetGraphics gfx = new DatasetGraphics(data, color, style);
		_drawables.add(gfx);
		// register gfx pulldown save dialog
	}
	
	
	private class DatasetGraphics implements Graphics {
		private DataSet _dataset;
		private Color _color;
		private Style _style;
		
		public DatasetGraphics(DataSet data, Color color, Style style) {
			_dataset = data;
			_color = color;
			_style = style;
		}
		
		public void draw(GL gl, Bounds bounds) {
			double data[] = _dataset.copyPartial(1000, bounds.xmin, bounds.xmax, bounds.ymin, bounds.ymax);
			
			gl.glColor4fv(_color.getComponents(null), 0);
			
			switch (_style) {
			case MARKS:
				gl.glPointSize(4.0f);
				gl.glBegin(GL.GL_POINTS);
				break;
			case LINES:
				gl.glLineWidth(1.0f);
				gl.glBegin(GL.GL_LINE_STRIP);
				break;
			case BARS:
				gl.glBegin(GL.GL_LINES);
				break;
			}
			for (int i = 0; i < data.length; i += 2) {
				gl.glVertex2d(data[i], data[i+1]);
				if (_style == Style.BARS)
					gl.glVertex2d(data[i], 0);
			}
			gl.glEnd();
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
}

