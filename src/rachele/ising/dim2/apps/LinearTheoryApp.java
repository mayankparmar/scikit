package rachele.ising.dim2.apps;

import java.awt.Color;

import rachele.ising.dim2.IsingLangevin;
import rachele.ising.dim2.StructureFactor;
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

public class LinearTheoryApp extends Simulation{
    Grid grid = new Grid("Phi(x)");
	Plot structurePeakV = new Plot("Ver Structure Factor");
	Plot structurePeakH = new Plot("Hor Structure factor");
	Plot sfPeakBoth = new Plot("Both Structure factors");
	Plot variance = new Plot("Variance");
	StructureFactor sf;
    IsingLangevin ising;
    Accumulator sfTheoryAcc;
    Accumulator varianceAcc;
    
	public static void main(String[] args) {
		new Control(new LinearTheoryApp(), "Ising Field");
	}

	public LinearTheoryApp() {
		frameTogether("Plots", grid, structurePeakH, structurePeakV, variance);
		params.addm("Zoom", new ChoiceValue("Yes", "No"));
		params.addm("Interaction", new ChoiceValue("Circle", "Square"));
		params.addm("Noise", new ChoiceValue("Off","On"));
		params.addm("Conserve M?", new ChoiceValue("Yes", "No"));
		params.addm("T", 0.1);
		params.addm("H", 0.0);
		params.add("Magnetization", 0.0);
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
		
	}
	
	public void animate() {
		ising.readParams(params);
		if (params.sget("Zoom").equals("Yes")) 	grid.setAutoScale();
		else grid.setScale(-1, 1);
		structurePeakV.setAutoScale(true);
		structurePeakH.setAutoScale(true);
		sfPeakBoth.setAutoScale(true);
		grid.registerData(ising.Lp, ising.Lp, ising.phi);
		if(ising.circleInt() == true){
			structurePeakV.registerLines("Peak Value", sf.getPeakC(), Color.BLACK);
			structurePeakV.registerLines("Theory", sfTheoryAcc, Color.BLUE);
			variance.registerLines("Variance", varianceAcc, Color.BLACK);
		}else{
			structurePeakV.registerLines("Vertical Peak", sf.getPeakV(), Color.CYAN);
			//sfPeakBoth.registerLines("Hortizontal Peak", sf.getPeakH(), Color.ORANGE);
			//sfPeakBoth.registerLines("Vertical Peak", sf.getPeakV(), Color.CYAN);
			structurePeakH.registerLines("Horizontal Peak", sf.getPeakH(), Color.ORANGE);
		}	

			
	}

	public void clear() {
	}

	public void run() {
		ising = new IsingLangevin(params);
		double binWidth = params.fget("kR bin-width");
		binWidth = IsingLangevin.KR_SP / floor(IsingLangevin.KR_SP/binWidth);
        sf = new StructureFactor(ising.Lp, ising.L, ising.R, binWidth, ising.dt);
		sf.setBounds(0.1, 14);
		fillTheoryAccum();
		varianceAcc = new Accumulator(params.fget("dt"));
		int reps = 0;
		while (true) {
			ising.randomizeField(params.fget("Magnetization"));
			for (double t = 0.0; t < params.fget("Max Time"); t = t + params.fget("dt")){
				params.set("Time", t);
				ising.simulate();
				sf.accumulateAll(t,ising.coarseGrained());
				varianceAcc.accum(t, ising.phiVariance());
				//System.out.println(t);
				Job.animate();
			}
			reps += 1;
			params.set("Reps", reps);
		}
	}
	
	public double linearTheory(double kR, double time){
		double V = ising.Lp*ising.Lp;
		double D = -ising.circlePotential(kR) - ising.T/ pow(1-pow(params.fget("Magnetization"),2),2);
		double sf = (exp(2*time*D)*(V + ising.T/D)-ising.T/D)/V;
		return sf;
	}
	
	public void fillTheoryAccum(){
		sfTheoryAcc = new Accumulator(ising.dt);
		for(double time = 0.0; time < params.fget("Max Time"); time = time + params.fget("dt"))
		sfTheoryAcc.accum(time, linearTheory(sf.circlekRValue(),time));
	}

}
