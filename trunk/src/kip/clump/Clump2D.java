package kip.clump;

import static java.lang.Math.*;
import kip.util.Random;
import scikit.jobs.Parameters;


public class Clump2D {
	PtsGrid pts;
	
	double L, R, T;	
	int t_cnt, numPts;
	double[] ptsX, ptsY;
	Random random = new Random();
	
	
	public Clump2D(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));

		R = params.fget("R");
		L = R*params.fget("L/R");
		T = params.fget("T");
		double dx = params.fget("dx");
		
		numPts = (int)(L*L);
		pts = new PtsGrid(L, R, dx);
		ptsX = new double[numPts];
		ptsY = new double[numPts];
		randomizePts();
		t_cnt = 0;
	}
	
	private void randomizePts() {
		for (int i = 0; i < numPts; i++) {
			ptsX[i] = random.nextDouble()*L;
			ptsY[i] = random.nextDouble()*L;			
			pts.add(ptsX[i], ptsY[i]);
		}
	}
	
	public void getParams(Parameters params) {
		T = params.fget("T");
	}
	
	double dist2(double dx, double dy) {
		dx = abs(dx);
		dx = min(dx, L-dx);
		dy = abs(dy);
		dy = min(dy, L-dy);
		return dx*dx + dy*dy;
	}
	
	int slowCount(double x, double y) {
		int acc = 0;
		for (int i = 0; i < numPts; i++) {
			if (dist2(ptsX[i]-x, ptsY[i]-y) < R*R) {
				acc++;
			}
		}
		return acc;
	}
	
	public void mcsTrial() {
		int i = random.nextInt(numPts);
		double x = ptsX[i];
		double y = ptsY[i];
		
		double xp = x + R*(2*random.nextDouble()-1);
		double yp = y + R*(2*random.nextDouble()-1);
		xp = (xp+L)%L;
		yp = (yp+L)%L;
//		assert(pts.countOverlaps(xp,yp) == slowCount(xp,yp));
//		assert(pts.countOverlaps(x,y) == slowCount(x,y));
		
		double dE = (pts.countOverlaps(xp,yp)-pts.countOverlaps(x,y))/(PI*R*R);
		if (dE < 0 || random.nextDouble() < exp(-dE/T)) {
			ptsX[i] = xp;
			ptsY[i] = yp;			
			pts.remove(x, y);
			pts.add(xp, yp);
		}
		t_cnt++;
	}
	
	public int[] coarseGrained() {
		return pts.rawElements;
	}
	
	public int numColumns() {
		return pts.gridCols;
	}
	
	public double time() {
		return (double)t_cnt/numPts;
	}
}
