package scikit.params;

import javax.swing.*;
import javax.swing.event.*;
import static scikit.util.Utilities.*;


public class DoubleValue extends GuiValue {

	private double _lo = Double.NEGATIVE_INFINITY, _hi = Double.POSITIVE_INFINITY;
	
	public DoubleValue(double x) {
		super(x);
	}
	
	public DoubleValue(double x, double lo, double hi) {
		super(x);
		_lo = lo;
		_hi = hi;
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
		
		final JSlider slider = new JSlider(0, 1000, toRangedInt(Double.valueOf(get())));
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
					slider.setValue(toRangedInt(Double.valueOf(get())));
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
