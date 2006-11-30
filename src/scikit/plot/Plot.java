package scikit.plot;


import static java.lang.Math.*;

import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

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
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.FileDialog;


public class Plot extends EmptyPlot implements Display {
	private static final int NUM_DATA_SETS = 8;
	private static final int MAX_DATA_POINTS = 300;
	private static final double AUTOSCALE_SLOP = 0.10;		// extend view 10% on rescale
	private static final double AUTOSCALE_NEARNESS = 0.02;	// extend view when data within 2% of bounds
	
//	private boolean _autoScale = true;		// automatically expand view bounds to fit data
	
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
	
	protected JPopupMenu _popup;
	
	
	public Plot(String title, boolean frame) {
		super(title, frame);
		_popup = new JPopupMenu();
	}
	
	
	public void clear() {
		_dataSets = new DataSet[NUM_DATA_SETS];
		resetViewWindow();
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
		if (!_zoomed)
			autoScaleBounds();
		for (DataSet dataSet : _dataSets)
			_dataBuffer[i++] = dataSet == null ? null : dataSet.copyPartial(256, _minX, _maxX, _minY, _maxY);
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
	
	private void mergeBounds(double[] b1, double[] b2) {
		b1[0] = min(b1[0], b2[0]);
		b1[1] = max(b1[1], b2[1]);
		b1[2] = min(b1[2], b2[2]);
		b1[3] = max(b1[3], b2[3]);
	}
	
	private void extendBounds(double[] b, double slop) {
		double w = b[1]-b[0];
		double h = b[3]-b[2];
		b[0] -= w*AUTOSCALE_SLOP;
		b[1] += w*AUTOSCALE_SLOP;
		b[2] -= h*AUTOSCALE_SLOP;
		b[3] += h*AUTOSCALE_SLOP;
	}
	
	private void autoScaleBounds() {
		double[] bounds = null;
		
		// if bounds got corrupted, completely reset them to "top"
		double dx = _maxX - _minX;
		double dy = _maxY - _minY;
		if (Double.isInfinite(dx) || Double.isNaN(dx) ||
			Double.isInfinite(dy) || Double.isNaN(dy)) {
			_minX = _topMinX;
			_maxX = _topMaxX;
			_minY = _topMinY;
			_maxY = _topMaxY;
		}
		
		// grow bounds to include all data
		for (DataSet dataSet : _dataSets) {
			if (dataSet == null) continue;
			double[] test = dataSet.getBounds();
			if (bounds == null)
				bounds = test;
			else
				mergeBounds(bounds, test);
		}
		if (bounds == null)
			return;
		
		// add a little extra "slop" for viewing (fixme)
		extendBounds(bounds, AUTOSCALE_SLOP);
		
		_minX = min(bounds[0], _minX);
		_maxX = max(bounds[1], _maxX);
		_minY = min(bounds[2], _minY);
		_maxY = max(bounds[3], _maxY);
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
	
	
	protected void paintData(Graphics2D g) {
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
	
	
	private int countDataSets() {
		int cnt = 0;
		for (DataSet dataSet : _dataSets)
			if (dataSet != null)
				cnt++;
		return cnt;
	}
	
	private void saveDataset(DataSet data, String str) {
		scikit.util.Dump.saveDialog(this, str, data.copyData(), 2);
	}
	
	private void fillPopup() {
		_popup.removeAll();
		int cnt = 1;
		for (int i = 0; i < _dataSets.length; i++) {
			final DataSet dataSet = _dataSets[i];
			if (dataSet != null) {
				final String str = "Dataset" + cnt;
				JMenuItem menuItem = new JMenuItem("Save " + str + "...");
				menuItem.setForeground(_colors[i]);
				final Plot p = this;
				menuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						saveDataset(dataSet, str);
					}
				});
				_popup.add(menuItem);
				cnt++;
			}
		}
	}
	
	
	protected boolean maybeShowPopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			if (countDataSets() > 0) {
				fillPopup();
				_popup.show(e.getComponent(), e.getX(), e.getY());
			}
			return true;
		}
		return false;
	}
}

