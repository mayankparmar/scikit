package kip.clump;

import static java.lang.Integer.highestOneBit;
import static java.lang.Math.PI;
import static java.lang.Math.exp;
import kip.util.Random;
import scikit.jobs.Parameters;

public class Clump2DLattice {
    QuadTree qt;
    
    // unscaled parameters
    int L, R, dx;
    double T;
	// parameters with dx scaled out, for implementation purposes
	int Lp, Rp;
	
	int t_cnt, numPts;
	int[] ptsX, ptsY;
	Random random = new Random();
	
	
	public Clump2DLattice(Parameters params) {
		random.setSeed(params.iget("Random seed", 0));

		R = params.iget("R");
		L = R*params.iget("L/R");
		dx = R/params.iget("R/dx");
		T = params.fget("T");
        numPts = L*L;
        if (highestOneBit(R)!=R || highestOneBit(L)!=L || highestOneBit(dx)!=dx)
			throw new IllegalArgumentException("All parameters must be powers of 2.");
        
		// rescale L,R so that dx->1 for simplicity
		Lp = L/dx;
		Rp = R/dx;
		
		qt = new QuadTree(Lp, Rp);
		ptsX = new int[numPts];
		ptsY = new int[numPts];
		randomizePts();
		t_cnt = 0;
	}
	
	
	private void randomizePts() {
		for (int i = 0; i < numPts; i++) {
			ptsX[i] = (int) (random.nextDouble()*Lp);
			ptsY[i] = (int) (random.nextDouble()*Lp);
			qt.add(ptsX[i], ptsY[i]);
		}
	}
	
	public void getParams(Parameters params) {
		T = params.fget("T");
	}
	
	public void mcsTrial() {
		int i = random.nextInt(numPts);
		int x = ptsX[i];
		int y = ptsY[i];
		
		int xp = x + random.nextInt(2*Rp+1) - Rp;
		int yp = y + random.nextInt(2*Rp+1) - Rp;
		xp = (xp+Lp)%Lp;
		yp = (yp+Lp)%Lp;
		
		double dE = (qt.countOverlaps(xp,yp)-qt.countOverlaps(x,y))/(PI*R*R);
		if (dE < 0 || random.nextDouble() < exp(-dE/T)) {
			ptsX[i] = xp;
			ptsY[i] = yp;
			qt.remove(x, y);
			qt.add(xp, yp);
		}
		t_cnt++;
	}
	
	public int[] coarseGrained() {
		return qt.rawElements;
	}
	
	public int numColumns() {
		return Lp;
	}
	
	public double time() {
		return (double)t_cnt/numPts;
	}
}