package scikit.util;

import static java.lang.Math.*;

public class Bounds {
	private final double INF = Double.POSITIVE_INFINITY;
	public double xmin = INF, xmax = -INF;
	public double ymin = INF, ymax = -INF;
	public double zmin = INF, zmax = -INF;
	
	public Bounds() {}
	
	public Bounds(double xmin, double xmax, double ymin, double ymax) {
		this.xmin = xmin;
		this.xmax = xmax;
		this.ymin = ymin;
		this.ymax = ymax;
	}
	
	public Bounds(double xmin, double xmax, double ymin, double ymax, double zmin, double zmax) {
		this(xmin, xmax, ymin, ymax);
		this.zmin = zmin;
		this.zmax = zmax;
	}
	
	public Bounds(Point... pts) {
		for (Point pt : pts) {
			xmin = min(xmin, pt.x);
			xmax = max(xmax, pt.x);
			ymin = min(ymin, pt.y);
			ymax = max(ymax, pt.y);
			zmin = min(zmin, pt.z);
			zmax = max(zmax, pt.z);
		}
	}
	
	public Bounds clone() {
		return new Bounds(xmin, xmax, ymin, ymax, zmin, zmax);
	}
	
	public String toString() {
		return "["+xmin+"--"+xmax+" , "+ymin+"--"+ymax+" , "+zmin+"--"+zmax+"]";
	}
	
	public Bounds createUnion(Bounds b) {
		return new Bounds(min(xmin, b.xmin), max(xmax, b.xmax),
						 min(ymin, b.ymin), max(ymax, b.ymax),
						 min(zmin, b.zmin), max(zmax, b.zmax));	
	}
}
