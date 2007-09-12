package rachele.ising.testCode.apps;

import java.awt.Color;

import rachele.ising.testCode.ConjugateGradientMin;
import scikit.dataset.Accumulator;
import scikit.graphics.dim2.Plot;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
import static scikit.util.Utilities.frame;

public class ConjugateGradientMinApp extends Simulation{
	Plot function = new Plot ("Function");
	Accumulator acc = new Accumulator(1.0);
	ConjugateGradientMin conjugateGrad;

	public ConjugateGradientMinApp() {
		frame(function);

		acc.setAveraging(true);
	}
	
	public static void main(String[] args) {
		new Control(new ConjugateGradientMinApp(), "Conjugate Grad Min");
	}

	public void animate() {
		//plot the function:
		function.registerLines("function", acc, Color.BLACK);
	}

	public void clear() {
	}

	public void run() {
		double [] minSearchInput = new double [3];
		double [] minSearchOutput = new double [2];
		conjugateGrad = new ConjugateGradientMin();
		System.out.println(conjugateGrad.function(1.0));
		//plot the function:
		for(int i = -5; i <= 5; i++){
			acc.accum(i,conjugateGrad.function((double)i));
		}
		minSearchInput = conjugateGrad.initialBracket(1000, 999);
		System.out.println(minSearchInput[0] + " " + minSearchInput[1] + " " + minSearchInput[2]);
		minSearchOutput = conjugateGrad.goldenMin(minSearchInput, 0.0001);
		System.out.println("min = " + minSearchOutput[1] + " at " + minSearchOutput[0]);
		Job.animate();
		acc.clear();
	}
	
	public Accumulator getAcc(){
		return acc;
	}
	
}