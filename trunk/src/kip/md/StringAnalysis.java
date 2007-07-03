package kip.md;

import java.util.ArrayList;

import scikit.dataset.Accumulator;
import static kip.util.MathPlus.*;

public class StringAnalysis<Pt extends Particle<Pt>> {
	private double _memoryTime;
	private double _dt;
	private ArrayList<Config> _history = new ArrayList<Config>();
	private Accumulator _alpha;
	
	public StringAnalysis(double memoryTime, double dt) {
		_memoryTime = memoryTime;
		_dt = dt;
		_alpha = new Accumulator(dt);
		_alpha.setAveraging(true);
	}
	
	public void addConfiguration(double time, Pt[] ps) {
		if (time - lastConfigurationTime() >= _dt) {
			System.out.println("add");
			_history.add(new Config(time, ps));
			while (time - firstConfigurationTime() > _memoryTime)
				_history.remove(0);
			
			// accumulate alpha(t) for each t
			Config last = _history.get(_history.size()-1);
			for (Config c : _history) {
				System.out.println("accum " + (last.time-c.time) + " " + calculateGaussianDeviation(c.ps, last.ps));
				_alpha.accum(last.time-c.time, calculateGaussianDeviation(c.ps, last.ps));
			}
		}
	}
	
	public Accumulator getAveragedAlpha() {
		return _alpha;
	}
	
	private double firstConfigurationTime() {
		int size = _history.size();
		return (size == 0) ? -Double.MAX_VALUE : _history.get(0).time;
	}
	
	private double lastConfigurationTime() {
		int size = _history.size();
		return (size == 0) ? -Double.MAX_VALUE : _history.get(size-1).time;
	}
	
	private double calculateGaussianDeviation(Pt[] ps1, Pt[] ps2) {
		MolecularDynamics2D<?> md = ps1[0].tag.md;
		double dx2_sum = 0, dx4_sum = 0;
		
		int cnt = ps1.length;
		for (int i = 0; i < cnt; i++) {
			double dx2 = md.displacement(ps1[i],ps2[i]).norm2();
			dx2_sum += dx2;
			dx4_sum += dx2*dx2;
		}
		
		return (dx4_sum/cnt) / (3 * sqr(dx2_sum/cnt));
	}

	class Config {
		double time;
		Pt[] ps;
		public Config(double time, Pt[] ps) {
			this.time = time;
			this.ps = ps;
		}
	}
}