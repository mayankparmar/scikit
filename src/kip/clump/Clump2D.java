package kip.clump;

import static java.lang.Math.*;
import kip.util.Random;
import scikit.jobs.Parameters;


public class Clump2D {
    double L, R, T;	
	PtsGrid pts;
	int t_cnt, numPts;
	Random random = new Random();
	
	
	public Clump2D(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));

		R = params.fget("R");
		L = R*params.fget("L/R");
		T = params.fget("T");

		numPts = (int)(L*L);
		pts = new PtsGrid(L, R, numPts);
		randomizePts();
		t_cnt = 0;

		pts.remove(pts.xs[2], pts.ys[2]);
//		for (int i = 0; i < numPts; i++) {
//			pts.countOverlaps(pts.xs[i], pts.ys[i]);
//		}
		pts.add(1, 1);
//		for (int i = 0; i < numPts; i++) {
		int i = 0;
			pts.countOverlaps(pts.xs[i], pts.ys[i]);
//		}
	}
	
	private void randomizePts() {
		for (int i = 0; i < pts.maxPoints; i++) {
			double x = random.nextDouble()*L;
			double y = random.nextDouble()*L;			
			pts.add(x, y);
		}
	}
	
	public void getParams(Parameters params) {
		T = params.fget("T");
	}
	
	public void mcsTrial() {
		int i = random.nextInt(pts.maxPoints);
		double x = pts.xs[i];
		double y = pts.ys[i];
		
		double xp = x + R*(2*random.nextDouble()-1);
		double yp = y + R*(2*random.nextDouble()-1);
		xp = (xp+L)%L;
		yp = (yp+L)%L;
		
		double dE = (pts.countOverlaps(xp,yp)-pts.countOverlaps(x,y))/(PI*R*R);
		if (dE < 0 || random.nextDouble() < exp(-dE/T)) {
			pts.remove(x, y);
			for (i = 0; i < numPts; i++) {
				pts.countOverlaps(pts.xs[i], pts.ys[i]);
			}
			pts.add(xp, yp);
			for (i = 0; i < numPts; i++) {
				pts.countOverlaps(pts.xs[i], pts.ys[i]);
			}
		}
		t_cnt++;
	}
	
	public int[] coarseGrained() {
		return pts.gridCnt;
	}
	
	public int numColumns() {
		return pts.gridCols;
	}
	
	public double time() {
		return (double)t_cnt/pts.ptsCnt;
	}
}
