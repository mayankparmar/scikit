package scikit.jobs;

import java.text.DecimalFormat;
import static java.lang.Math.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;


public class DoubleValue extends GuiValue {
	static DecimalFormat df1 = new DecimalFormat("0.####");
	static DecimalFormat df2 = new DecimalFormat("0.####E0");
	static public String format(double x) {
		return (abs(x) > 0.001 && abs(x) < 1000 || x == 0 ? df1 : df2).format(x);
	}
	
	private double _lo = Double.NEGATIVE_INFINITY, _hi = Double.POSITIVE_INFINITY;
	
	public DoubleValue(double x) {
		super(x);
	}
	
	public DoubleValue(double x, double lo, double hi) {
		super(x);
		_lo = lo;
		_hi = hi;
	}
	
	public Double valueForString(String v) {
		return Double.valueOf(v);
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
		
		final JSlider slider = new JSlider(0, 1000, toRangedInt((Double)get()));
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
					slider.setValue(toRangedInt((Double)get()));
				slider.setEnabled(!_locked);
			}
		});
		
		return slider;
	}
	
	private String fromRangedInt(int i) {
		return format(_lo + (_hi - _lo) * i / 1000);
	}
	
	private int toRangedInt(double x) {
		return (int) (1000 * (x - _lo) / (_hi-_lo));	
	}
}
