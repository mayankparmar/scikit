package scikit.jobs;

import static java.lang.Math.*;
import java.text.DecimalFormat;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;


class DoubleValue extends Value {
	private double _lo = Double.NEGATIVE_INFINITY, _hi = Double.POSITIVE_INFINITY;
	DecimalFormat df1 = new DecimalFormat("0.####");
	DecimalFormat df2 = new DecimalFormat("0.####E0");
	
	public DoubleValue(String v, boolean lockable) {
		super(v, lockable);
	}
	
	public DoubleValue(String v, double lo, double hi, boolean lockable) {
		super(v, lockable);
		_lo = lo;
		_hi = hi;
	}
	
	public void set(double x) {
		set ((abs(x) > 0.001 && abs(x) < 1000 || x == 0 ? df1 : df2).format(x));
	}
	
	public double fget() {
		return Double.valueOf(_v);
	}
	
	public boolean testValidity(String v) {
		try {
			double f = Double.valueOf(v);
			return _lo <= f && f <= _hi;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}
	
	public JComponent createAuxiliaryEditor() {
		final double range = _hi - _lo;
		
		if (!_auxiliaryEditor || range == Double.POSITIVE_INFINITY)
			return null;
		
		final JSlider slider = new JSlider(0, 1000, toRangedInt(fget()));
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (slider.hasFocus()) {
					set(fromRangedInt(slider.getValue()));
				}
			}
		});
		
		addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!slider.hasFocus())
					slider.setValue(toRangedInt(fget()));
				slider.setEnabled(!_locked);
			}
		});
		
		return slider;
	}
	
	private double fromRangedInt(int i) {
		return _lo + (_hi - _lo) * i / 1000;
	}
	
	private int toRangedInt(double x) {
		return (int) (1000 * (x - _lo) / (_hi-_lo));	
	}
}
