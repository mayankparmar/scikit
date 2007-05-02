package kip.util;


import scikit.dataset.Accumulator;
import scikit.jobs.Control;
import scikit.jobs.Job;
import scikit.plot.Plot;


public class BugTest extends Job {
    Plot plot = new Plot("Structure factor", true);
    Accumulator acc;
	
	public static void main(String[] args) {
		frame(new Control(new BugTest()), "Nucleation");
	}

	public BugTest() {
		params.addm("bin-width", 0.025);
	}
	
	public void animate() {
		acc.setBinWidth(params.fget("bin-width"));
	}
	
	public void run() {
		acc = new Accumulator(params.fget("bin-width"));
		acc.setAveraging(true);
		plot.setDataSet(0, acc);
		plot.setDataSet(1, new scikit.dataset.Function(0, 1) {
			public double eval(double x) {
				return java.lang.Math.sin(x);
			}
		});
//		addDisplay(plot);
		while (true) {
			for (int i = 0; i < 10; i++)
				acc.accum(Math.random(), Math.random());			
			yield();
			plot.animate();
		}
	}
}
