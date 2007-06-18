package scikit.util;

public class Point {
	public double x = 0, y = 0, z = 0;
	
	public Point(double x, double y, double z) {
		set(x,y,z);
	}
	
	public Point(double x, double y) {
		set(x,y);
	}
	
	public Point() {
	}
	
	public String toString() {
		return "("+x+","+y+","+z+")";
	}
	
	public Point clone() {
		return new Point(x, y, z);
	}
	
	public void set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void set(double x, double y) {
		this.x = x;
		this.y = y;
	}
}
