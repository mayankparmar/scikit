package kip.md;

import java.util.ArrayList;

import scikit.dataset.Accumulator;
import scikit.dataset.DataSet;
import scikit.dataset.DynamicArray;
import scikit.graphics.Scene;
import static kip.util.MathPlus.*;

public class StringAnalysis {
	private double _memoryTime;
	private ArrayList<Config> _history = new ArrayList<Config>();
	private Accumulator _dx2, _dx4;
	private ParticleContext _pc;
	
	public StringAnalysis(ParticleContext pc, double memoryTime, double dt) {
		_pc = pc;
		_memoryTime = memoryTime;
		
		_dx2 = new Accumulator(dt);
		_dx4 = new Accumulator(dt);
		_dx2.setAveraging(true);
		_dx4.setAveraging(true);
	}
	
	public void addConfiguration(double time, Particle[] ps) {
		Particle[] psCopy = new Particle[ps.length];
		for (int i = 0; i < ps.length; i++) {
			psCopy[i] = (Particle)ps[i].clone();
		}

		_history.add(new Config(time, psCopy));
		while (time - _history.get(0).time > _memoryTime)
			_history.remove(0);
		
		// average second and fourth moments
		Config last = _history.get(_history.size()-1);
		for (Config prev : _history) {
			double t = last.time - prev.time;
			if (t > 0) {
				for (int i = 0; i < prev.ps.length; i++) {
					double del2 = _pc.displacement(prev.ps[i],last.ps[i]).norm2();
					_dx2.accum(t, del2);
					_dx4.accum(t, del2*del2);
				}
			}
		}
	}
	
	public DataSet getAlpha() {
		DynamicArray ret = new DynamicArray();		
		for (double x : _dx2.keys()) {
			double dx2 = _dx2.eval(x);
			double dx4 = _dx4.eval(x);
			ret.append2(x, dx4/((1.0+2.0/_pc.dim())*sqr(dx2)) - 1.0);
		}
		return ret;
	}
	
	public void addGraphicsToCanvas(Scene canvas) {
//		int cnt = _history.size();
//		for (int i = 0; i < cnt; i++) {
//			_pc.addGraphicsToCanvas(canvas, _history.get(i).ps);
//		}
	}
	
	class Config {
		double time;
		Particle[] ps;
		public Config(double time, Particle[] ps) {
			this.time = time;
			this.ps = ps;
		}
	}
}