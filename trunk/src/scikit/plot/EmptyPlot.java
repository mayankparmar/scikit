package scikit.plot;

import static java.lang.Math.*;
import java.util.Vector;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import javax.swing.JComponent;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;


public class EmptyPlot extends JComponent {	
	private static final long serialVersionUID = 1L;
	private static int FONT_SIZE = 12;
	private static int MARGIN = 6;
	private static double TICKS_PER_PIXEL = 1.0/60.0;
	private static Color PANEL_COLOR = new Color(0.96f, 0.95f, 0.99f);
	
	protected double DEFAULT_MIN = Double.POSITIVE_INFINITY;
	protected double DEFAULT_MAX = Double.NEGATIVE_INFINITY;
	
	// the "top" bounds provide a minimum size of the view.
	// when the user double clicks to "resetViewWindow()", the view size
	// will be guaranteed to include the "top" bounds as well as any data
	// which extends further.
	// the infinite defaults guarantee this condition is initially satisfied
	protected double _topMinX = DEFAULT_MIN, _topMaxX = DEFAULT_MAX;
	protected double _topMinY = DEFAULT_MIN, _topMaxY = DEFAULT_MAX;
	// the current view bounds
	protected double _minX, _maxX, _minY, _maxY;
	// is the view zoomed in?  this will disable autoscale
	protected boolean _zoomed = false;
	
	// the pixel bounds of the view rectangle
	private Rectangle _bound = new Rectangle();
	// the pixel bounds of the selection rectangle
	private Rectangle _selection = new Rectangle();
	private boolean _selectionActive = false;
	
//	private boolean _logscaleX = false, _logscaleY = false;
	
	
	public EmptyPlot(String title, boolean inFrame) {
		addMouseListener(_mouseListener);
		addMouseMotionListener(_mouseMotionListener);
		setOpaque(true);
		
		_minX = _topMinX; _maxX = _topMaxX;
		_minY = _topMinY; _maxY = _topMaxY;
		
		if (inFrame) {
            scikit.util.Utilities.frame(this, title);
		}
	}
	
	
	public Dimension getPreferredSize() {
		return new Dimension(300, 300);
	}
	
	public void setXRange(double minX, double maxX) {
		_topMinX = _minX = minX;
		_topMaxX = _maxX = maxX;
		_zoomed = false;
	}
	
	public void setYRange(double minY, double maxY) {
		_topMinY = _minY = minY;
		_topMaxY = _maxY = maxY;
		_zoomed = false;
	}
	
	public void resetViewWindow() {
		_minX = _topMinX;
		_maxX = _topMaxX;
		_minY = _topMinY;
		_maxY = _topMaxY;
		_zoomed = false;
	}
	
		
	public void setTitle(String title) {
	}
	
	
	public void setLogScale(boolean logscaleX, boolean logscaleY) {
//		_logscaleX = logscaleX;
//		_logscaleY = logscaleY;
	}
	
	
	protected double xToPix(double x) {
		return _bound.x + _bound.width * (x - _minX) / (_maxX - _minX);
	}
	protected double yToPix(double y) {
		return _bound.y+_bound.height - _bound.height * (y - _minY) / (_maxY - _minY);
	}
	
	protected double pixToX(double p) {
		return _minX + (_maxX - _minX) * (p - _bound.x) / _bound.width;
	}
	protected double pixToY(double p) {
		return _minY - (_maxY - _minY) * (p - _bound.y-_bound.height) / _bound.height;	
	}
	
	
	
	private static String tickToString(double tick, double length) {
		StringBuffer str;
		if (length > 0.00001 && length < 100000) {
			NumberFormat nf = new DecimalFormat("0.######");
			str = new StringBuffer(nf.format(tick));
		}
		else {
			NumberFormat nf = new DecimalFormat("0.######E0");
			str = new StringBuffer(nf.format(tick));
			int i = str.indexOf("E");
			str.replace(i, i+1, " E");
			//if (abs(tick) >= 1)
			//	str.insert(i+1, ' ');
			if (tick == 0)
				str = new StringBuffer("0");
		}
		
		if (tick >= 0)
			str.insert(0, ' ');
			
		return str.toString();
	}
	
	
	private static double log10(double x) {
		return log(x) / log(10);
	}
	
	private static Vector<Double> getTicks(double lo, double hi, double desiredTickNum) {
		// find "step" between ticks:  s = i * 10^p, where i is one of 1, 2, 5, 8,
		// and x is an integer.  desired number of ticks is about 10.
		
		double p1 = log10((hi-lo) / (1*desiredTickNum));
		double p2 = log10((hi-lo) / (2*desiredTickNum));
		double p5 = log10((hi-lo) / (5*desiredTickNum));
		// double p8 = log10((hi-lo) / 80);
		
		double d1 = abs(p1 - round(p1));
		double d2 = abs(p2 - round(p2));
		double d5 = abs(p5 - round(p5));
		// double d8 = abs(p8 - round(p8));
		
		double i = 1, p = p1, d = d1;
		if (d2 < d) { i = 2; p = p2; d = d2; }
		if (d5 < d) { i = 5; p = p5; d = d5; }
		// if (d8 < d) { i = 8; p = p8; d = d8; }
		p = round(p);
		
		double step = i * pow(10, p);		
		
		Vector<Double> ret = new Vector<Double>();
		double mult = floor(lo / step) + 1;
		while (mult*step < hi) {
			ret.add((Double)(mult*step));
			mult++;
		}
		return ret;
	}
	
	
	private void paintTicks(Graphics2D g) {
		Color lightgray = new Color(0.82f, 0.82f, 0.87f);
		g.setColor(lightgray);
		
		// draw lines
		for (double tick : getTicks(_minX, _maxX, _bound.width*TICKS_PER_PIXEL)) {
			int x = (int)round(xToPix(tick));
			int y = (int)round(yToPix(_minY));
			int y2 = (int)round(yToPix(_maxY));
			g.drawLine(x, y, x, y2);
		}
		for (double tick : getTicks(_minY, _maxY, _bound.height*TICKS_PER_PIXEL)) {
			int x = (int)round(xToPix(_minX));
			int y = (int)round(yToPix(tick));
			int x2 = (int)round(xToPix(_maxX));
			g.drawLine(x, y, x2, y);
		}
	}
	
	
	// BUG: there is a very weird bug where if this font object is allocated,
    // and g.drawString() is called, then memory is leaked.  See example
    // MemoryApp, test1.
	Font f = new Font("Monospaced", Font.PLAIN, FONT_SIZE);
	
	private void paintLabels(Graphics2D g) {
		g.setColor(Color.BLACK);
		g.setFont(f);
		
		for (double tick : getTicks(_minX, _maxX, _bound.width*TICKS_PER_PIXEL)) {
			if ((tick - _minX) / (_maxX - _minX) < 0.05)
				continue;
			int x = (int)round(xToPix(tick));
			int y = (int)round(yToPix(_minY));
			g.rotate(-PI/2);
			g.drawString(tickToString(tick, _maxX-_minX), -y-(MARGIN-FONT_SIZE/2-1), x+4);
			g.rotate(PI/2);
		}
		for (double tick : getTicks(_minY, _maxY, _bound.height*TICKS_PER_PIXEL)) {
			if ((tick - _minY) / (_maxY - _minY) < 0.05)
				continue;
			int x = (int)round(xToPix(_minX));
			int y = (int)round(yToPix(tick));
			g.drawString(tickToString(tick, _maxY-_minY), x-(MARGIN-FONT_SIZE/2-1), y+4);
		}
	}
	
	
	protected void autoScaleBounds() {
	}
	
	protected void paintData(Graphics2D g) {
	}
	
	private void paintBound(Graphics2D g) {
		g.setColor(PANEL_COLOR);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(Color.WHITE);
		g.fill(_bound);
		g.setColor(new Color(0.5f, 0.5f, 0.5f));
		g.draw(_bound);
	}
	
	private void paintSelection(Graphics2D g) {
		if (_selectionActive) {
			g.setColor(new Color(0.2f, 0.2f, 0.2f, 0.5f));
			g.draw(fixRectangle(_selection));
			g.setColor(new Color(0.3f, 0.6f, 0.5f, 0.1f));
			g.fill(fixRectangle(_selection));
		}
	}
	
	
	protected void paintComponent(Graphics g1) {
		Graphics2D g = (Graphics2D)g1;

		int w = getWidth(), h = getHeight();
		_bound.x = MARGIN;
		_bound.y = MARGIN;
		_bound.width = max(w - 2*MARGIN, 0);
		_bound.height = max(h - 2*MARGIN, 0);
		paintBound(g);
		g.setClip(_bound.x+1, _bound.y+1, _bound.width-1, _bound.height-1);
		
		if (!_zoomed)
			autoScaleBounds();
		paintTicks(g);
		
		paintData(g);
		paintSelection(g);
		paintLabels(g);
		g.setClip(0, 0, w, h);
	}
	
	
	
	private Rectangle fixRectangle(Rectangle rect) {
		rect = new Rectangle(rect);
		if (rect.width < 0) {
			rect.width *= -1;
			rect.x -= rect.width;
		}
		if (rect.height < 0) {
			rect.height *= -1;
			rect.y -= rect.height;
		}
		return rect;
	}
	
	
	private boolean rangeIsReasonable(double minX, double maxX) {
		return (maxX-minX) / max(abs(minX), abs(maxX)) > 1e-8;
	}
	
	
	private void changeViewWindow(Rectangle r) {
		if (r.width > 6 && r.height > 6) {
			double minX = pixToX(r.x);
			double maxX = pixToX(r.x + r.width);
			double minY = pixToY(r.y + r.height);
			double maxY = pixToY(r.y);
			
			if (rangeIsReasonable(minX, maxX) && rangeIsReasonable(minY, maxY)) {
				_minX = minX; _maxX = maxX;
				_minY = minY; _maxY = maxY;
				_zoomed = true;
			}
		}
	}
	
	
	protected boolean maybeShowPopup(MouseEvent e) {
		return false;
	}
	
	
	private MouseListener _mouseListener = new MouseAdapter() {
		private boolean withinBounds(MouseEvent event) {
			return _bound.contains(event.getX()-1, event.getY()-1);
		}
		
		public void mouseClicked(MouseEvent event) {			
			if (withinBounds(event) && event.getClickCount() > 1) {
				resetViewWindow();
				_selectionActive = false;
				repaint();
			}
		}
		public void mousePressed(MouseEvent event) {
			if (maybeShowPopup(event))
				return;
			if (withinBounds(event)) {
				_selection.x = event.getX()-1;
				_selection.y = event.getY()-1;
				_selection.width = _selection.height = 0;
				_selectionActive = true;
				repaint();
			}
		}
		public void mouseReleased(MouseEvent event) {
			if (maybeShowPopup(event))
				return;
			if (_selectionActive) {
				changeViewWindow(fixRectangle(_selection));
				_selectionActive = false;
				repaint();
			}
		}
	};
	private MouseMotionListener _mouseMotionListener = new MouseMotionAdapter() {
		public void mouseDragged(MouseEvent event) {
			_selection.width  = event.getX() - _selection.x - 1;
			_selection.height = event.getY() - _selection.y - 1;
			repaint();
		}
	};

}
