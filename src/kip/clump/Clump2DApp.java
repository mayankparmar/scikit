package kip.clump;

import kip.util.Random;
import static kip.util.MathPlus.*;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.plot.GridDisplay;
import scikit.plot.Plot;
import scikit.plot.Function;
import static java.lang.Math.*;
import static java.lang.Integer.*;


public class Clump2DApp extends Job {
    GridDisplay grid = new GridDisplay("Grid", true);
    Plot plot = new Plot("Structure factor", true);
    
    QuadTree qt;
    StructureFactor sf;
    
    // unscaled parameters
    int L, R, dx;
    double T;
	// parameters with dx scaled out, for implementation purposes
	int Lp, Rp;
	double Tp;
	
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
		params.addm("T", 1.0);
		params.addm("kR bin-width", 0.02);
		params.add("Random seed", 0);
		params.add("Time");
	}
	
	public void animate() {
		T = params.fget("T");
		Tp = T*dx*dx;
		sf.getAccumulator().setBinWidth(params.fget("kR bin-width"));
	}
	
	void randomizePts() {
		for (int i = 0; i < numPts; i++) {
			ptsX[i] = (int) (random.nextDouble()*Lp);
			ptsY[i] = (int) (random.nextDouble()*Lp);
			qt.add(ptsX[i], ptsY[i]);
		}
	}
	
	void simStep() {
		int i = (int) (numPts*random.nextDouble());
		int x = ptsX[i];
		int y = ptsY[i];
		
		int xp = x + (int)(Rp*2*(random.nextDouble()-0.5));
		int yp = y + (int)(Rp*2*(random.nextDouble()-0.5));
		xp = (xp+Lp)%Lp;
		yp = (yp+Lp)%Lp;
		
		double dE = (qt.countOverlaps(xp,yp)-qt.countOverlaps(x,y))/(PI*Rp*Rp);
		if (dE < 0 || random.nextDouble() < exp(-dE/Tp)) {
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
		dx = R/params.iget("R/dx");
		T = params.fget("T");
        numPts = L*L;
        if (highestOneBit(R)!=R || highestOneBit(L)!=L || highestOneBit(dx)!=dx)
			throw new IllegalArgumentException("All parameters must be powers of 2.");
        
		// rescale L,R,T so that dx->1 for simplicity
		Lp = L/dx;
		Rp = R/dx;
		Tp = T*dx*dx;
		
		qt = new QuadTree(Lp, Rp);
        grid.setData(Lp, Lp, qt.rawElements);
        addDisplay(grid);
        
		ptsX = new int[numPts];
		ptsY = new int[numPts];
		randomizePts();
        
		sf = new StructureFactor(Lp, L, R, params.fget("kR bin-width"));
        plot.setDataSet(0, sf.getAccumulator());
        plot.setDataSet(1, new Function(sf.kRmin, sf.kRmax) {
        	public double eval(double kR) {
        		double V = 2*j1(kR)/kR;
        		return 1/(V/T+1);
        	}
        });
        addDisplay(plot);
        
        double t = 0;
        while (true) {
			params.set("Time", t);
			for (int i = 0; i < numPts/8; i++) {
				simStep();
				yield();
			}
			sf.accumulate(qt.rawElements);
			t += 1./8.;
		}
        // params.set("Random seed", params.iget("Random seed")+1);
	}
}
