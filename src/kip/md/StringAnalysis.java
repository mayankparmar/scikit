package kip.md;

import java.util.ArrayList;

import scikit.dataset.Accumulator;
import scikit.util.Point;
import static kip.util.MathPlus.*;

public class StringAnalysis {
	private double _memoryTime;
	private ArrayList<Config> _history = new ArrayList<Config>();
	private Accumulator _alpha;
	private MolecularDynamics2D<?> _md;
	
	public StringAnalysis(MolecularDynamics2D<?> md, double memoryTime, double dt) {
		_memoryTime = memoryTime;
		_alpha = new Accumulator(dt);
		_alpha.setAveraging(true);
		_md = md;
	}
	
	public <T extends Point> void addConfiguration(double time, T[] ps) {
		Point[] psCopy = new Point[ps.length];
		for (int i = 0; i < ps.length; i++) {
			psCopy[i] = ps[i].clone();
		}

		_history.add(new Config(time, psCopy));
		while (time - _history.get(0).time > _memoryTime)
			_history.remove(0);
		
		// accumulate alpha(t) for each t
		Config last = _history.get(_history.size()-1);
		for (Config c : _history) {
			double t = last.time - c.time;
			if (t > 0) {
				_alpha.accum(t, calculateGaussianDeviation(c.ps, last.ps));
			}
		}
	}
	
	public Accumulator getAveragedAlpha() {
		return _alpha;
	}
	
	private double calculateGaussianDeviation(Point[] ps1, Point[] ps2) {
		double dx2_sum = 0, dx4_sum = 0;
		
		int cnt = ps1.length;
		for (int i = 0; i < cnt; i++) {
			double dx2 = _md.displacement(ps1[i],ps2[i]).norm2();
			dx2_sum += dx2;
			dx4_sum += dx2*dx2;
		}
		
		return (dx4_sum/cnt) / (3 * sqr(dx2_sum/cnt));
	}

	class Config {
		double time;
		Point[] ps;
		public Config(double time, Point[] ps) {
			this.time = time;
			this.ps = ps;
		}
	}
}