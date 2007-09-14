package rachele.ising.testCode.apps;

import java.awt.Color;

import rachele.ising.testCode.ConjugateGradientMin;
import scikit.dataset.Accumulator;
import scikit.graphics.dim2.Geom2D;
import scikit.graphics.dim2.Grid;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.jobs.Simulation;
//import static scikit.util.Utilities.asList;
import static scikit.util.Utilities.frame;

public class ConjugateGradientMinApp extends Simulation{
	Grid function = new Grid ("Function");
	Accumulator acc = new Accumulator(1.0);
	ConjugateGradientMin conjugateGrad;
	public double conjGradOutput [] = new double [4];

	public ConjugateGradientMinApp() {
		frame(function);

		acc.setAveraging(true);
	}
	
	public static void main(String[] args) {
		new Control(new ConjugateGradientMinApp(), "Conjugate Grad Min");
	}

	public void animate() {
		//plot the function:
		//function.registerLines("function", acc, Color.BLACK);
		double [] point = new double [2];
		double [] funcData = new double [21*21];
		int index;
		for (int x = 0; x <= 20; x++){
			for(int y = 0; y <= 20; y++){
				point[0] = x;
				point[1] = y;
				index = 21*(y)+(x);
				funcData[index] = conjugateGrad.functionMultiDim(point);
			}
		}
		function.registerData(21, 21, funcData);
		acc.clear();
		System.out.println(conjugateGrad.initPoint[0] + " " + conjugateGrad.initPoint[1] + " " + conjugateGrad.finPoint[0] + " " + conjugateGrad.finPoint[1]);
		double x1 = conjugateGrad.initPoint[0]/21;
		double y1 = conjugateGrad.initPoint[1]/21;
		double x2 = conjugateGrad.finPoint[0]/21;
		double y2 = conjugateGrad.finPoint[1]/21;
		function.addDrawable(Geom2D.line(x1, y1, x2, y2, Color.GREEN));
	}

	public void clear() {
	}

	public void run() {
		conjugateGrad = new ConjugateGradientMin();
		System.out.println(conjugateGrad.function(1.0));

		
		
		//minSearchInput = conjugateGrad.initialBracket(-1000,-996);
		//System.out.println(minSearchInput[0] + " " + minSearchInput[1] + " " + minSearchInput[2]);
		//minSearchOutput = conjugateGrad.goldenMin(minSearchInput, 1e-16);
		//minSearchOutput = conjugateGrad.brent(minSearchInput, 1e-16);
		//System.out.println("min = " + minSearchOutput[1] + " at " + minSearchOutput[0]);
		
		
		// Give some initial point for the conjugate grad minimization
		// Take (0,0) for the 2 D example
		double [] initialPoint = new double[conjugateGrad.N];
		//double [] minPoint = new double[conjugateGrad.N];
		for(int i = 0; i < conjugateGrad.N; i++)
			initialPoint[i] = 0;
		conjugateGrad.conjuageGradMin(initialPoint);
		//conjugateGrad.steepestDecent(initialPoint);
		//System.out.println("min = " + min);
		//for (int i = 0; i < conjugateGrad.N; i++){
		//System.out.println("min " + i + " = " + minPoint[i]);
		//}
		Job.animate();
		acc.clear();
	}
	
	public Accumulator getAcc(){
		return acc;
	}
	
}