package kip.clump;
import static java.lang.Math.abs;


public class QuadTree {
	double halfDiagonalCutoff;
	double L;
	double R2;
	
	double dist2(double x, double y) {
		x = abs(x);
		y = abs(y);
		if (x > L/2) x = L-x;
		if (y > L/2) y = L-y;
		return x*x + y*y;
	}
	
	abstract class Node {
		double x, y;
		int ptsCount;
		
		abstract int overlapCount(double x, double y);
		abstract Node add(double x, double y);
		abstract Node remove(double x, double y);		
	}
	class Quad extends Node {
			double halfDiagonal;
			Node tl, tr, bl, br;
			public int overlapCount(double xp, double yp) {
				return 0;
			}
			public Node add(double x, double y) {
				// TODO Auto-generated method stub
				return null;
			}
			public Node remove(double x, double y) {
				// TODO Auto-generated method stub
				return null;
			}
			
	}
	class Leaf extends Node {
		public int overlapCount(double xp, double yp) {
			return (ptsCount == 0 || dist2(xp-x, yp-y) > R2) ? 0 : ptsCount;
		}
		public Node add(double x, double y) {
			// TODO Auto-generated method stub
			return null;
		}
		public Node remove(double x, double y) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}

