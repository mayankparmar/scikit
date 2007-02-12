package kip.clump;

import kip.util.Random;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.plot.GridDisplay;
import scikit.plot.Plot;
import static java.lang.Math.*;
import static java.lang.Integer.*;


public class Clump2DApp extends Job {
    GridDisplay grid = new GridDisplay("Grid", true);
    Plot plot = new Plot("Structure factor", true);
    
    QuadTree qt;
    StructureFactor sf;
    
    int L, R;
	double T, dx;
	
	int numPts;
	int[] ptsX, ptsY;
	Random random = new Random();
	
	
	public static void main(String[] args) {
		frame(new Control(new Clump2DApp()), "Nucleation");
	}

	public Clump2DApp() {
		params.add("R", 16);
		params.add("L/R", 16);
		params.add("R/dx", 8);
		params.addm("T", 0.14);
		params.add("Random seed", 0);
	}

	public void animate() {
		T = params.fget("T")*dx*dx;
	}
	
	void randomizePts() {
		for (int i = 0; i < numPts; i++) {
			ptsX[i] = (int) (random.nextDouble()*L);
			ptsY[i] = (int) (random.nextDouble()*L);
			qt.add(ptsX[i], ptsY[i]);
		}
	}
	
	void simStep() {
		int i = (int) (numPts*random.nextDouble());
		int x = ptsX[i];
		int y = ptsY[i];
		
		int xp = x + (int)(R*2*(random.nextDouble()-0.5));
		int yp = y + (int)(R*2*(random.nextDouble()-0.5));
		xp = (xp+L)%L;
		yp = (yp+L)%L;
		
		double dE = (qt.countOverlaps(xp,yp)-qt.countOverlaps(x,y))/(PI*R*R);
		if (dE < 0 || random.nextDouble() < exp(-dE/T)) {
			ptsX[i] = xp;
			ptsY[i] = yp;
			qt.remove(x, y);
			qt.add(xp, yp);
		}
	}
	
	
	public void run() {
		random.setSeed(params.iget("Random seed", 0));

		R = params.iget("R");
		L = R*params.iget("L/R");
        numPts = L*L;
        
		// adjust dx so that L/dx is a power of two
		dx = R/params.iget("R/dx");
		// int Lp = highestOneBit((int)round(L/dx));
		//dx = (double)L/Lp;
		//params.set("dx/R", dx/R);
		
		// rescale L,R,T so that dx->1 for simplicity
		L = (int)(L/dx);
		R = (int)(R/dx);
		T = params.fget("T")*dx*dx;
		if (L != highestOneBit(L))
			throw new IllegalArgumentException("L/dx must be a power of 2.");
		
		qt = new QuadTree(L, R);
        grid.setData(L, L, qt.rawElements);
        addDisplay(grid);
        
		ptsX = new int[numPts];
		ptsY = new int[numPts];
		randomizePts();
        
		sf = new StructureFactor(L, 0.25);
        plot.setDataSet(0, sf.calculate(qt.rawElements));
        addDisplay(plot);
        
        while (true) {
			for (int i = 0; i < numPts/100; i++) {
				simStep();
				yield();
			}
			sf.calculate(qt.rawElements);
		}
        // params.set("Random seed", params.iget("Random seed")+1);
	}
}
