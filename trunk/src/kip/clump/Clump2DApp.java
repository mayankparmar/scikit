package kip.clump;

import kip.util.Random;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.plot.DataSet;
import scikit.plot.GridDisplay;
import scikit.plot.Plot;
import scikit.plot.PointSet;
import static java.lang.Math.*;
import static kip.util.MathPlus.*;


class CoarseGrained {
	double max;
	int L;
	double[] gridData;
	
	public CoarseGrained(double max, int L) {
		this.max = max;
		this.L = L;
		gridData = new double[L*L];
	}
	
	int pointToIndex(double x, double y) {
		int i = (int) (L*x/max);
		int j = (int) (L*y/max);
		assert (i < L && j < L);
		return j*L+i;
	}
	
	public void addPoint(double x, double y) {
		gridData[pointToIndex(x,y)] += 1;
	}
	
	public void removePoint(double x, double y) {
		gridData[pointToIndex(x,y)] -= 1;
	}
}


public class Clump2DApp extends Job {
    GridDisplay grid = new GridDisplay("Grid", true);
    CoarseGrained cg;
    int L, R;
	double T;
	
	int numPts;
	double[] ptsX, ptsY;
	Random random = new Random();
	
	
	public static void main(String[] args) {
		frame(new Control(new Clump2DApp()), "Nucleation");
	}

	public Clump2DApp() {
		params.add("R", 10);
		params.add("L", 100);
		params.addm("T", 1.0);
		params.add("R/dx", 4);
		params.add("Random seed", 0);
	}

	public void animate() {
		T = params.fget("T");
	}
	
	void randomizePts() {
		for (int i = 0; i < numPts; i++) {
			ptsX[i] = random.nextDouble()*L;
			ptsY[i] = random.nextDouble()*L;
			cg.addPoint(ptsX[i], ptsY[i]);
		}
	}
	
	int countInteractions(double x, double y) {
		int acc = 0;
		for (int i = 0; i < numPts; i++) {
			double dx = abs(x-ptsX[i]);
			double dy = abs(y-ptsY[i]);
			if (dx > L/2) dx = L - dx;
			if (dy > L/2) dy = L - dy;
			if (dx*dx+dy*dy < R*R) acc++;
		}
		return acc;
	}
	
	void simStep() {
		int i = (int) (numPts*random.nextDouble());
		double x = ptsX[i];
		double y = ptsY[i];
		
		double xp = x + R*2*(random.nextDouble()-0.5);
		double yp = y + R*2*(random.nextDouble()-0.5);
		xp = (xp+L)%L;
		yp = (yp+L)%L;
		
		double dE = (countInteractions(xp,yp)-countInteractions(x,y))/(PI*R*R);
		if (dE < 0 || random.nextDouble() < exp(-dE/T)) {
			ptsX[i] = xp;
			ptsY[i] = yp;
			cg.removePoint(x, y);
			cg.addPoint(xp, yp);
		}
	}
	
	
	public void run() {
		L = params.iget("L");
		R = params.iget("R");
		T = params.fget("T");
		random.setSeed(params.iget("Random seed", 0));
		
        numPts = L*L;
		ptsX = new double[numPts];
		ptsY = new double[numPts];
 
        int cgL = L*params.iget("R/dx")/R;		// # coarse grained cells per side
		cg = new CoarseGrained(L, cgL);
        grid.setData(cgL, cgL, cg.gridData);
        addDisplay(grid);
        
        randomizePts();
        
        while (true) {
			yield();
			
			for (int i = 0; i < 100; i++) {
				simStep();
			}
		}
        // params.set("Random seed", params.iget("Random seed")+1);
	}
}
