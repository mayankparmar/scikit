package scikit.jobs;


import java.util.Vector;
import java.util.HashMap;
import javax.swing.event.ChangeListener;


public class Parameters implements Cloneable {
	private Vector<String> keys = new Vector<String>();	
	private HashMap<String, GuiValue> map = new HashMap<String, GuiValue>();
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
	
	public GuiValue add(String key) {
		return add(key, new ReadonlyValue());
	}
	
	public GuiValue add(String key, Object val) {
		GuiValue v;
		if (val instanceof GuiValue)
			v = (GuiValue)val;
		else if (val instanceof Integer)
			v = new IntValue((Integer)val);
		else if (val instanceof Double)
			v = new DoubleValue((Double)val);
		else
			v = new StringValue(val);
		
		if (_listener != null)
			v.addChangeListener(_listener);
		keys.add(key);	
		map.put(key, v);
		return v;
	}
	
	public GuiValue addm(String key, Object val) {
		GuiValue v = add(key, val);
		v.setLockable(false);
		return v;
	}

	public void set(String key, Object value) {
		if (!getValue(key).testValidity(value.toString()))
			throw new IllegalArgumentException("Parameter '"+key+"' is incompatible with " + value);
		getValue(key).set(value);
	}
	
	public double fget(String key) {
		try {
			return (Double)getValue(key).get();
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Parameter '"+key+"' is not of type 'float'");
		}
	}
	public double fget(String key, double def) {
		try {
			getValue(key);
		} catch (Exception e) {
			return def;
		}
		return fget(key);
	}
	
	public int iget(String key) {
		try {
			return (Integer)(getValue(key).get());
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Parameter '"+key+"' is not of type 'int'");
		}
	}
	public int iget(String key, int def) {
		try {
			getValue(key);
		} catch (Exception e) {
			return def;
		}
		return iget(key);
	}
	
	public String sget(String key) {
		try {
			return (String)(getValue(key).get());
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Parameter '"+key+"' is not of type 'string'");
		}
	}
	public String sget(String key, String def) {
		try {
			getValue(key);
		} catch (Exception e) {
			return def;
		}
		return sget(key);
	}
	
	public void setLocked(boolean locked) {
		for (String k : keys) {
			getValue(k).setLocked(locked);
		}
	}
	
	public String[] keys() {
		return keys.toArray(new String[]{});
	}
	
	public GuiValue getValue(String key) {
		if (!map.containsKey(key))
			throw new IllegalArgumentException("Parameter '"+key+"' does not exist.");
		return map.get(key);
	}
}

