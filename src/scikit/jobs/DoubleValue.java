package scikit.jobs;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;


class DoubleValue extends Value {
	private double _x;	// the precise value approximated by string v
	private double _lo = Double.NEGATIVE_INFINITY, _hi = Double.POSITIVE_INFINITY;
	
	public DoubleValue(double x, boolean lockable) {
		super(format(x), lockable);
		_x = x;
	}
	
	public DoubleValue(double x, double lo, double hi, boolean lockable) {
		super(format(x), lockable);
		_x = x;
		_lo = lo;
		_hi = hi;
	}
	
	synchronized public double fget() {
		return _x;
	}
	
	synchronized public void set(String v) {
		if (!_v.equals(v)) {
			// _x needs to be set first, because super.set() will go call into
			// it's ChangeListeners which may ask about _x
			_x = Double.valueOf(v);
			super.set(v);
		}
	}
	
	synchronized public void set(double x) {
		if (_x != x) {
			// as before, _x needs to be set first
			_x = x;
			// it would be a bug to call super.set(x), because it would call this.set(v),
			// and the value of _x would be overwritten with a less accurate string
			// conversion number
			super.set(format(x));
		}
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
