package scikit.plot;


import static java.lang.Math.*;
import java.util.Vector;
import javax.swing.JFrame;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;


public class Plot extends EmptyPlot implements Display {
	private static final int NUM_DATA_SETS = 8;
	private static final int MAX_DATA_POINTS = 100;
	private static final double AUTOSCALE_SLOP = 0.1;
	
	private boolean _autoScale = true;
	private boolean _invalidView = true;
	
	private double[][] _dataBuffer = new double[NUM_DATA_SETS][];
	protected DataSet[]  _dataSets = new DataSet[NUM_DATA_SETS];
	
	private Color[] _colors =
		{Color.BLACK, Color.BLUE, Color.RED, Color.GREEN,
		 Color.ORANGE, Color.MAGENTA, Color.PINK, Color.CYAN};
	
	private boolean[] _drawLines =
		{true, true, true, true, true, true, true, true};
	private boolean[] _drawMarks = new boolean[NUM_DATA_SETS];
	private boolean[] _drawBars  = new boolean[NUM_DATA_SETS];
	
	public enum Style {LINES, MARKS, BARS};
	
	
	
	public Plot(String title, boolean frame) {
		super(title, frame);
	}
	
	
	public void clear() {
		_dataSets   = new DataSet[NUM_DATA_SETS];
	}
	
	
	public void append(int i, double x, double y) {
		if (_dataSets[i] == null) {
			_dataSets[i] = new DynamicArray();
		}
		if (_dataSets[i] instanceof DynamicArray) {
			DynamicArray da = (DynamicArray)_dataSets[i];
			da.append(x);
			da.append(y);
		}
		else {
			throw new IllegalArgumentException("Can't append to " + _dataSets[i]);
		}
	}
	
	
	public void setDataSet(int i, DataSet dataSet) {
		_dataSets[i]   = dataSet;
	}
	
	
	synchronized public void animate() {
		int i = 0;
		for (DataSet dataSet : _dataSets)
			_dataBuffer[i++] = (dataSet == null ? null : dataSet.copyData());
		if (_autoScale || _invalidView) {
			autoScaleBounds();
		}
		repaint();
	}
	
	
	synchronized public void setStyle(int i, Style... styles) {
		_drawMarks[i] = _drawLines[i] = _drawBars[i] = false;
		for (Style style : styles) {
			switch (style) {
				case LINES: _drawLines[i] = true; break;
				case MARKS: _drawMarks[i] = true; break;
				case BARS:  _drawBars[i]  = true; break;
			}
		}
	}
	
	
	public void setColor(int i, Color color) {
		_colors[i] = color;
	}
	
	
	public void setAutoScale(boolean autoScale) {
		_autoScale = autoScale;
	}
	
	
	public void invalidateView() {
		_invalidView = true;
	}
	
	
	private void autoScaleBounds() {
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = hasBars() ? 0 : Double.POSITIVE_INFINITY;
		double maxY = hasBars() ? 0 : Double.NEGATIVE_INFINITY;
		
		for (DataSet dataSet : _dataSets) {
			if (dataSet == null) continue;
			double[] data = dataSet.copyData();
			for (int i = 0; i < data.length; i += 2) {
				minX = min(minX, data[i+0]);
				maxX = max(maxX, data[i+0]);
				minY = min(minY, data[i+1]);
				maxY = max(maxY, data[i+1]);
			}
		}
		
		double w = maxX - minX;
		double h = maxY - minY;
		boolean hasData = w > 0 && h > 0;
		if (hasData) {
			if (_invalidView) {
				setXRange(DEFAULT_MIN, DEFAULT_MAX);
				setYRange(DEFAULT_MIN, DEFAULT_MAX);
				_invalidView = false;
			}
			if (minX < _topMinX && minX < _minX) {
				_topMinX = minX - AUTOSCALE_SLOP*w;
				resetViewWindow();
			}
			if (maxX > _topMaxX && maxX > _maxX) {
				_topMaxX = maxX + AUTOSCALE_SLOP*w;
				resetViewWindow();
			}
			if (minY < _topMinY && minY < _minY) {
				_topMinY = minY - AUTOSCALE_SLOP*h;
				resetViewWindow();
			}
			if (maxY > _topMaxY && maxY > _maxY) {
				_topMaxY = maxY + AUTOSCALE_SLOP*h;
				resetViewWindow();
			}
		}
		else {
			_invalidView = _autoScale;
		}
	}
	
	
	private boolean hasBars() {
		for (boolean b : _drawBars)
			if (b) return true;
		return false;
	}
	
	
	private void drawMark(Graphics2D g, double x, double y) {
		g.fill(new Rectangle2D.Double(xToPix(x)-2, yToPix(y)-2, 5, 5));
	}
	
	
	private void drawLine(Graphics2D g, double x1, double y1, double x2, double y2) {
		if (x1 +x2 + y1 + y2 != Double.NaN)
			g.draw(new Line2D.Double(xToPix(x1), yToPix(y1), xToPix(x2), yToPix(y2)));
	}
	
	
	synchronized protected void paintData(Graphics2D g) {
		RenderingHints oldhints = g.getRenderingHints();
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
												  RenderingHints.VALUE_ANTIALIAS_ON);
		// hints.put(RenderingHints.KEY_RENDERING,	RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHints(hints);
		Stroke oldStroke = g.getStroke();
		g.setStroke(new BasicStroke(2f));
		
		int j = 0;
		for (double[] xy : _dataBuffer) {
			if (xy != null) {
				Rectangle2D.Double rect = new  Rectangle2D.Double
					(_minX, _minY, _maxX-_minX, _maxY-_minY);
				if (_drawBars[j]) {
					rect.y		= min(0, _minY);
					rect.height = max(0, _maxY) - rect.y;
				}
				xy = reducedData(xy, rect, MAX_DATA_POINTS);
				
				g.setColor(_colors[j]);
				
				for (int i = 0; i+1 < xy.length; i += 2) {
					if (_drawMarks[j])
						drawMark(g, xy[i], xy[i+1]);
					if (_drawBars[j])
						drawLine(g, xy[i], 0, xy[i], xy[i+1]);
					if (_drawLines[j] && i+3 < xy.length)
						drawLine(g, xy[i], xy[i+1], xy[i+2], xy[i+3]);
				}
			}
			
			j++;
		}

		g.setStroke(oldStroke);
		g.setRenderingHints(oldhints);
	}
	
	
	// Returns a copy of array, ignoring points outside of rect, and
	// "reducing" data so that at most maxPoints (x,y) values are included
	private double[] reducedData(double[] array, Rectangle2D.Double rect, int maxPoints) {
		double[] data = new double[array.length];
		System.arraycopy(array, 0, data, 0, array.length);
		
		int count = 0;
		
		// filter data to contain only points which fit inside rect, or neighbor
		// points which fit inside rect
		if (rect == null || data.length < 4)
			count = data.length;
		else {
			int i = 0;
			boolean wroteNaN = true;
			boolean c1, c2, c3;
			c2 = rect.contains(data[0], data[1]);
			c3 = rect.contains(data[2], data[3]);
			if (c2 || c3) {
				data[count++] = data[i+0];
				data[count++] = data[i+1];
				wroteNaN = false;
			}
			for (i = 2; i < data.length-3; i += 2) {
				c1 = c2;
				c2 = c3;
				c3 = rect.contains(data[i+2], data[i+3]);
				if (c1 || c2 || c3) {
					data[count++] = data[i+0];
					data[count++] = data[i+1];
					wroteNaN = false;
				}
				else if (!wroteNaN) {
					data[count++] = Double.NaN;
					data[count++] = Double.NaN;		
					wroteNaN = true;
				}
			}
			if (c2 || c3) {
				data[count++] = data[i+0];
				data[count++] = data[i+1];
			}
		}
		
		if (count == 0)
			return new double[0];
		
		// select points at intervals so that total data fits within size restriction
		// int available = 2*maxPoints;
		int desiredSize = min(2*maxPoints, count);
		int stepSize = count / desiredSize;
		int actualSize = count / stepSize;
		actualSize -= actualSize % 2;
		double[] ret = new double[actualSize];		
		for (int i = 0; i < ret.length-1; i += 2) {
			int j = i * stepSize;
			j -= j % 2;			
			ret[i+0] = data[j];
			ret[i+1] = data[j+1];
		}
		return ret;
	}	
}
