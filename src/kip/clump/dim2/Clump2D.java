package kip.clump.dim2;

import static java.lang.Math.*;
import scikit.jobs.Job;
import scikit.params.Parameters;


public class Clump2D extends AbstractClump2D {	
	PtsGrid pts;
	int t_cnt, numPts;
	double dt;
	double[] ptsX, ptsY;

	public Clump2D(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));

		R = params.fget("R");
		L = params.fget("L");
		T = params.fget("T");
		dx = params.fget("dx");
		dt = params.fget("dt");
		
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
	
	public void readParams(Parameters params) {
		T = params.fget("T");
		dt = params.fget("dt");
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
	
	void mcsTrial() {
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
	
	public void simulate() {
		for (int i = 0; i < numPts*dt; i++) {
			mcsTrial();
			Job.yield();
		}
	}
	
	public StructureFactor newStructureFactor(double binWidth) {
		// round binwidth down so that it divides KR_SP without remainder.
		binWidth = KR_SP / floor(KR_SP/binWidth);
		return new StructureFactor((int)(2*L), L, R, binWidth);
	}
	
	public void accumulateIntoStructureFactor(StructureFactor sf) {
		sf.accumulate(ptsX, ptsY);		
	}
	
	public double[] coarseGrained() {
		return pts.rawElements;
	}
	
	public int numColumns() {
		return pts.gridCols;
	}
	
	public double time() {
		return (double)t_cnt/numPts;
	}
}
