package kip.clump.dim3;
import static java.lang.Math.*;
import kip.util.Random;
import scikit.params.Parameters;


public class Clump3D {
	public static final double DENSITY = 1;
	public static final double KR_SP = 5.76345919689454979140645;
	public static final double T_SP = 0.08617089416190739793014991;
	
	public double L, R, T, dx;
	Random random = new Random();

	PtsGrid3D pts;
	int t_cnt, numPts;
	double dt;
	double[] ptsX, ptsY, ptsZ;

	public Clump3D(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));

		R = params.fget("R");
		L = R*params.fget("L/R");
		T = params.fget("T");
		dx = params.fget("dx");
		dt = params.fget("dt");
		
		numPts = (int)(L*L*L);
		pts = new PtsGrid3D(L, R, dx);
		ptsX = new double[numPts];
		ptsY = new double[numPts];
		ptsZ = new double[numPts];
		randomizePts();
		t_cnt = 0;
	}
	
	private void randomizePts() {
		for (int i = 0; i < numPts; i++) {
			ptsX[i] = random.nextDouble()*L;
			ptsY[i] = random.nextDouble()*L;
			ptsZ[i] = random.nextDouble()*L;
			pts.add(ptsX[i], ptsY[i], ptsZ[i]);
		}
	}
	
	public void readParams(Parameters params) {
		T = params.fget("T");
		dt = params.fget("dt");
	}
	
	double dist2(double dx, double dy, double dz) {
		dx = abs(dx);
		dx = min(dx, L-dx);
		dy = abs(dy);
		dy = min(dy, L-dy);
		dz = abs(dz);
		dz = min(dz, L-dz);
		return dx*dx + dy*dy + dz*dz;
	}
	
	int slowCount(double x, double y, double z) {
		int acc = 0;
		for (int i = 0; i < numPts; i++) {
			if (dist2(ptsX[i]-x, ptsY[i]-y, ptsZ[i]-z) < R*R) {
				acc++;
			}
		}
		return acc;
	}
	
	void mcsTrial() {
		int i = random.nextInt(numPts);
		double x = ptsX[i];
		double y = ptsY[i];
		double z = ptsZ[i];
		
		double xp = x + R*(2*random.nextDouble()-1);
		double yp = y + R*(2*random.nextDouble()-1);
		double zp = z + R*(2*random.nextDouble()-1);
		
		xp = (xp+L)%L;
		yp = (yp+L)%L;
		zp = (zp+L)%L;
		
//		assert(pts.countOverlaps(xp,yp,zp) == slowCount(xp,yp,zp));
//		assert(pts.countOverlaps(x,y,z) == slowCount(x,y,z));
		
		double dE = (pts.countOverlaps(xp,yp,zp)-pts.countOverlaps(x,y,z))/(4*PI*R*R*R/3);
		if (dE < 0 || random.nextDouble() < exp(-dE/T)) {
			ptsX[i] = xp;
			ptsY[i] = yp;
			ptsZ[i] = zp;
			pts.remove(x, y, z);
			pts.add(xp, yp, zp);
		}
		t_cnt++;
	}
	
	public void simulate() {
		for (int i = 0; i < numPts*dt; i++)
			mcsTrial();
	}
	
	public StructureFactor3D newStructureFactor(double binWidth) {
		// round binwidth down so that it divides KR_SP without remainder.
		binWidth = KR_SP / floor(KR_SP/binWidth);
		return new StructureFactor3D((int)(2*L), L, R, binWidth);
	}
	
	public void accumulateIntoStructureFactor(StructureFactor3D sf) {
		sf.accumulate(ptsX, ptsY, ptsZ);		
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
	
	public double potential(double kR) {
		return 3*(sin(kR)-kR*cos(kR))/(kR*kR*kR);		
	}
}