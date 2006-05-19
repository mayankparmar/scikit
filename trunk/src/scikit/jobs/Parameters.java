package scikit.jobs;


import java.util.Vector;
import java.util.HashMap;
import javax.swing.event.*;


public class Parameters implements Cloneable {
	private Job _job;
	private Vector<String> keys = new Vector<String>();
	private HashMap<String, Value> map = new HashMap<String, Value>();
	
	
	public Parameters(Job job) {
		_job = job;
	}
	
	public void setDefaults() {
		for (String k : keys) {
			set(k, getValue(k).getDefault());
		}
	}
	
	public void add(String key) {
		_addValue(key, new Value("-", true));
	}
	
	public void add(String key, boolean lockable, String... choices) {
		_addValue(key, new ChoiceValue(choices, lockable));
	}
	
	public void add(String key, int value, boolean lockable) {
		_addValue(key, new IntValue(value, lockable));
	}
	
	public void add(String key, double value, boolean lockable) {
		_addValue(key, new DoubleValue(value, lockable));		
	}
	
	public void add(String key, int value, int lo, int hi, boolean lockable) {
		_addValue(key, new IntValue(value, lo, hi, lockable));
	}
	
	public void add(String key, double value, double lo, double hi, boolean lockable) {
		_addValue(key, new DoubleValue(value, lo, hi, lockable));		
	}
	
	public void add(String key, String value, boolean lockable) {
		_addValue(key, new Value(value, lockable));		
	}
	
	public void set(String key, String value) {
		getValue(key).set(value);
	}
	
	public void set(String key, double value) {
		Value v = getValue(key);
		try {
			v.set(value);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Parameter '"+key+"' can't store a 'float'");
		}
	}
	
	public void set(String key, int value) {
		Value v = getValue(key);
		try {
			v.set(value);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Parameter '"+key+"' can't store a 'int'");
		}
	}
	
	public double fget(String key) {
		Value v = getValue(key);
		try {
			return v.fget();
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Parameter '"+key+"' is not of type 'float'");
		}
	}
	
	public int iget(String key) {
		Value v = getValue(key);
		try {
			return v.iget();
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Parameter '"+key+"' is not of type 'int'");
		}
	}
	
	public String sget(String key) {
		return getValue(key).sget();
	}
	
	public void enableSlider(String key) {
		getValue(key).enableAuxiliaryEditor();
	}
	
	public void setLocked(boolean locked) {
		for (String k : keys) {
			getValue(k).setLocked(locked);
		}
	}
	
	
	synchronized public String[] keys() {
		return keys.toArray(new String[]{});
	}
	
	synchronized public Value getValue(String key) {
		if (!map.containsKey(key))
			throw new IllegalArgumentException("Parameter '"+key+"' does not exist.");
		return map.get(key);
	}
	
	synchronized private void _addValue(String key, Value value) {
		value.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (_job != null)
					_job.wakeProcess();
			}
		});
		keys.add(key);	
		map.put(key, value);
	}
}

