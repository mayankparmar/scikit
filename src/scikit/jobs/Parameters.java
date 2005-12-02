package scikit.jobs;


import java.util.Vector;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;


class Value {
	private Color lightGreen = new Color(0.85f, 1f, 0.7f);
	private Color lightRed   = new Color(1f, 0.7f, 0.7f);
	private Vector<ChangeListener> _listeners = new Vector<ChangeListener>();
	private boolean _lockable = false;
	private boolean _locked = false;
	protected String _v;
	
	
	public Value(String v, boolean lockable) {
		_v = v;
		_lockable = lockable;
	}
	
	public String sget() {
		return _v;
	}
	
	public int iget() {
		throw new IllegalArgumentException();
	}
	
	public double fget() {
		throw new IllegalArgumentException();	
	}
	
	public void set(String v) {
		if (isValid(v) && !_v.equals(v)) {
			_v = v;
			for (ChangeListener l : _listeners)
				l.stateChanged(null);
		}
	}
	
	public void setLocked(boolean locked) {
		_locked = _lockable && locked;
		for (ChangeListener l : _listeners)
			l.stateChanged(null);
	}
	
	public boolean isValid(String v) {
		return true;
	}
	
	public void addChangeListener(ChangeListener listener) {
		_listeners.add(listener);
	}
	
	
	public JComponent createEditor() {
		final JTextField field = new JTextField(sget());
		
		ActionListener action = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				set(field.getText());
				field.setText(_v);
				field.setBackground(Color.WHITE);				
			}
		};
		FocusListener focus = new FocusListener() {
			public void focusGained(FocusEvent e)  {}
			public void focusLost(FocusEvent e) {
				set(field.getText());
				field.setText(_v);
				field.setBackground(Color.WHITE);
			}
		};
		DocumentListener input = new DocumentListener() {
			private void textInput() {
				if (field.hasFocus()) {
					field.setBackground(isValid(field.getText()) ? lightGreen : lightRed);
				}
			}
			public void changedUpdate(DocumentEvent e)  {}
			public void insertUpdate(DocumentEvent e)  { textInput(); }
			public void removeUpdate(DocumentEvent e) { textInput(); }
		};
		field.addActionListener(action);
		field.addFocusListener(focus);
		field.getDocument().addDocumentListener(input);
		field.setHorizontalAlignment(JTextField.RIGHT);
		
		ChangeListener change = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!field.hasFocus())
					field.setText(sget());
				field.setEnabled(!_locked);
			}
		};
		addChangeListener(change);
		
		return field;	
	}
	
	public JComponent createAuxillaryEditor() {
		return null;
	}
}


class IntValue extends Value {
	private int lo = Integer.MIN_VALUE, hi = Integer.MAX_VALUE;
	
	public IntValue(String v, boolean lockable) {
		super(v, lockable);
	}
	
	public int iget() {
		return Integer.valueOf(_v);
	}
	
	public boolean isValid(String v) {
		try {
			int i = Integer.valueOf(v);
			return lo < i && i < hi;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}
}


class DoubleValue extends Value {
	private double _lo = Double.NEGATIVE_INFINITY, _hi = Double.POSITIVE_INFINITY;
	
	public DoubleValue(String v, boolean lockable) {
		super(v, lockable);
	}
	
	public DoubleValue(String v, double lo, double hi, boolean lockable) {
		super(v, lockable);
		_lo = lo;
		_hi = hi;
	}
	
	public double fget() {
		return Double.valueOf(_v);
	}
	
	public boolean isValid(String v) {
		try {
			double f = Double.valueOf(v);
			return _lo < f && f < _hi;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}
}



public class Parameters implements Cloneable {
	Vector<String> keys = new Vector<String>();
	HashMap<String, Value> map = new HashMap<String, Value>();
	
	
	synchronized public String[] keys() {
		return keys.toArray(new String[]{});
	}
	
	public String[] values() {
		String[] ret = new String[keys.size()];
		for (int i = 0; i < ret.length; i++)
			ret[i] = sget(keys.get(i));
		return ret;
	}
	
	
	public void add(String key, int value, boolean lockable) {
		keys.add(key);
		put(key, new IntValue(String.valueOf(value), lockable));
	}
	
	public void add(String key, double value, boolean lockable) {
		keys.add(key);
		put(key, new DoubleValue(String.valueOf(value), lockable));		
	}
	
	public void add(String key, String value, boolean lockable) {
		keys.add(key);
		put(key, new Value(value, lockable));		
	}
	
	public void set(String key, String value) {
		get(key).set(value);
	}
	
	public double fget(String key) {
		return get(key).fget();
	}
	
	public int iget(String key) {
		return get(key).iget();
	}
	
	public String sget(String key) {
		return get(key).sget();
	}
	
	public boolean isValidValue(String key, String value) {
		return get(key).isValid(value);
	}
	
	
	public void setLocked(boolean locked) {
		for (String k : keys) {
			get(k).setLocked(locked);
		}
	}
	
	
	synchronized private void put(String key, Value value) {
		map.put(key, value);
	}
	
	synchronized public Value get(String key) {
		if (!map.containsKey(key))
			throw new IllegalArgumentException("Parameter '"+key+"' does not exist.");
		return map.get(key);
	}
}

