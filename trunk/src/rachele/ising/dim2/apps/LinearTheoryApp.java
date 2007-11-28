package rachele.ising.dim2.apps;

import java.awt.Color;

import rachele.ising.dim2.*;
import scikit.dataset.Accumulator;
import scikit.graphics.dim2.Grid;
import scikit.graphics.dim2.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import scikit.jobs.params.ChoiceValue;
import static java.lang.Math.floor;
import static scikit.util.Utilities.frameTogether;
import static java.lang.Math.*;
import static kip.util.MathPlus.j1;

public class LinearTheoryApp extends Simulation{
    Grid grid = new Grid("Phi(x)");
	Plot structurePeakV = new Plot("Ver Structure Factor");
	Plot structurePeakH = new Plot("Hor Structure factor");
	Plot sfPeakBoth = new Plot("Both Structure factors");
	Plot variance = new Plot("Variance");
	Plot meanPhi = new Plot("Mean Phi");
	StructureFactor sf;
    //IsingLangevin ising;
    IsingField2D ising;
	Accumulator sfTheoryAcc;
    Accumulator varianceAcc;
    Accumulator meanPhiAcc;
    
	public static void main(String[] args) {
		new Control(new LinearTheoryApp(), "Ising Field");
	}

	public LinearTheoryApp() {
		frameTogether("Plots", grid, meanPhi, structurePeakV, variance);
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("Interaction", new ChoiceValue("Circle", "Square"));
		params.addm("Noise", new ChoiceValue("Off","On"));
		params.addm("Dynamics?", new ChoiceValue("Langevin Conserve M", "Langevin No M Convervation"));
		params.add("Init Conditions", new ChoiceValue("Random Gaussian", 
				"Artificial Stripe 3", "Read From File", "Constant" ));
		params.addm("Approx", new ChoiceValue("Exact Stable",
				"Avoid Boundaries", "Exact SemiStable", "Exact", "Linear",  "Phi4"));
		//params.addm("Conserve M?", new ChoiceValue("Yes", "No"));
		params.addm("T", 0.05);
		params.addm("H", 0.0);
		params.add("Magnetization", 0.5);
		params.addm("dt", 1.0);
		params.addm("J", -1.0);
		params.addm("R", 1000000.0);
		params.add("L/R", 4.0);
		params.add("R/dx", 16.0);
		params.add("kR bin-width", 0.1);
		params.add("Random seed", 0);
		params.add("Max Time", 350);
		params.add("Time");
		params.add("Reps");
		params.add("Mean Phi");
		
	}
	
	public void animate() {
		ising.readParams(params);
		//params.set("Mean Phi", ising.mean(ising.phi));
		if (params.sget("Zoom").equals("Yes")) 	grid.setAutoScale();
		else grid.setScale(-1, 1);
		structurePeakV.setAutoScale(true);
		structurePeakH.setAutoScale(true);
		sfPeakBoth.setAutoScale(true);
		meanPhi.setAutoScale(true);
		grid.registerData(ising.Lp, ising.Lp, ising.phi);
		if(ising.circleInt() == true){
			structurePeakV.registerLines("Peak Value", sf.getPeakC(), Color.BLACK);
			structurePeakV.registerLines("Theory", sfTheoryAcc, Color.BLUE);
			variance.registerLines("Variance", varianceAcc, Color.BLACK);
			meanPhi.registerLines("Mean Phi", meanPhiAcc, Color.BLACK);
		}else{
			structurePeakV.registerLines("Vertical Peak", sf.getPeakV(), Color.CYAN);
		}	

			
	}

	public void clear() {
	}

	public void run() {
		ising = new IsingField2D(params);
		sfTheoryAcc = new Accumulator(ising.dt);
		sfTheoryAcc.setAveraging(true);
		double binWidth = params.fget("kR bin-width");
		binWidth = IsingLangevin.KR_SP / floor(IsingLangevin.KR_SP/binWidth);
        sf = new StructureFactor(ising.Lp, ising.L, ising.R, binWidth, ising.dt);
		sf.setBounds(0.1, 14);	
		double density = findMeanPhi();
		fillTheoryAccum(density);
		varianceAcc = new Accumulator(params.fget("dt"));
		varianceAcc.setAveraging(true);
		meanPhiAcc = new Accumulator(params.fget("dt"));
		meanPhiAcc.setAveraging(true);
		int reps = 0;
		while (true) {
			ising.randomizeField(density);
			for (double t = 0.0; t < params.fget("Max Time"); t = t + params.fget("dt")){
				params.set("Time", t);
				params.set("Mean Phi", ising.mean(ising.phi));
				ising.simulate();
				//accumTheoryPoint(t);
				sf.accumulateAll(t, ising.coarseGrained());
				varianceAcc.accum(t, ising.phiVariance());
				meanPhiAcc.accum(t,ising.mean(ising.phi));
				//System.out.println(t);
				Job.animate();
			}
			reps += 1;
			params.set("Reps", reps);
		}
	}
	
	public double findMeanPhi(){
		for (double t = 0.0; t < 50.0; t = t + params.fget("dt"))
		ising.simulate();
		return ising.mean(ising.phi);
	}
	
	public double linearTheory(double kR, double density, double time){
		//double V = ising.Lp*ising.Lp;
		double D = -circlePotential(kR) - ising.T/ (1-pow(ising.DENSITY,2));
		//double sf = (exp(2*time*D)*(V + ising.T/D)-ising.T/D)/V;
		double sf = exp(2*time*D);
		return sf;
	}
	
	public void fillTheoryAccum(double density){
		sfTheoryAcc = new Accumulator(ising.dt);
		//double kR = sf.circlekRValue();
		double kR = sf.getCircleKR();
		for(double time = 0.0; time < params.fget("Max Time"); time = time + params.fget("dt"))
		sfTheoryAcc.accum(time, linearTheory(kR, density, time));
	}
	
	public double circlePotential(double kR){
		return 2*j1(kR)/kR;
	}

}
