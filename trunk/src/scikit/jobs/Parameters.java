package scikit.jobs;


import java.util.Vector;
import java.util.HashMap;
import javax.swing.event.ChangeListener;


public class Parameters implements Cloneable {
	private Vector<String> keys = new Vector<String>();	
	private HashMap<String, Value> map = new HashMap<String, Value>();
	private ChangeListener _listener = null;
	
	
	public Parameters() {}

	public Parameters(Object... keyvals) {
		for (int i = 0; i < keyvals.length; i += 2)
			add((String)keyvals[i], keyvals[i+1]);
	}

	public void setChangeListener(ChangeListener listener) {
		_listener = listener;
	}

	public void setDefaults() {
		for (String k : keys) {
			set(k, getValue(k).getDefault());
		}
	}
	
	public Value add(String key) {
		return add(key, "-");
	}
	
	public Value add(String key, Object val) {
		Value v;
		if (val instanceof Value)
			v = (Value)val;
		else if (val instanceof Integer)
			v = new IntValue((Integer)val);
		else if (val instanceof Double)
			v = new DoubleValue((Double)val);
		else
			v = new Value(val.toString());

		v.addChangeListener(_listener);
		keys.add(key);	
		map.put(key, v);
		return v;
	}
	
	public Value addm(String key, Object val) {
		Value v = add(key, val);
		v.setLockable(false);
		return v;
	}

/*	
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
*/	
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
	
	public void setLocked(boolean locked) {
		for (String k : keys) {
			getValue(k).setLocked(locked);
		}
	}
	
	public String[] keys() {
		return keys.toArray(new String[]{});
	}
	
	public Value getValue(String key) {
		if (!map.containsKey(key))
			throw new IllegalArgumentException("Parameter '"+key+"' does not exist.");
		return map.get(key);
	}
}

