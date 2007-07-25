package scikit.params;

import javax.swing.*;
import javax.swing.event.*;
import static scikit.util.Utilities.*;


public class DoubleValue extends StringValue {

	private double _lo = Double.NEGATIVE_INFINITY, _hi = Double.POSITIVE_INFINITY;
	
	public DoubleValue(double x) {
		super(x);
	}
	
	public DoubleValue(double x, double lo, double hi) {
		super(x);
		_lo = lo;
		_hi = hi;
	}
	
	public DoubleValue withSlider() {
		useAuxiliaryEditor();
		return this;
	}
	
	protected boolean testValidity(String v) {
		try {
			double f = Double.valueOf(v);
			return _lo <= f && f <= _hi;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}
	
	protected JComponent createAuxiliaryEditor() {
		if (_hi - _lo == Double.POSITIVE_INFINITY)
			return null;
		
		final JSlider slider = new JSlider(0, 1000, toRangedInt(Double.valueOf(getValue())));
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (slider.hasFocus()) {
					setValue(fromRangedInt(slider.getValue()));
				}
			}
		});
		
		addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!slider.hasFocus())
					slider.setValue(toRangedInt(Double.valueOf(getValue())));
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
