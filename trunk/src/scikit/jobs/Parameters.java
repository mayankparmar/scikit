package scikit.jobs;


import java.util.Vector;
import java.util.HashMap;


public class Parameters implements Cloneable {
	Vector<String> keys = new Vector<String>();
	HashMap<String, Object> map = new HashMap<String, Object>();
	
	
	synchronized public String[] keys() {
		return keys.toArray(new String[]{});
	}
	
	public void add(String key, int value) {
		keys.add(key);
		put(key, value);		
	}
	
	public void add(String key, double value) {
		keys.add(key);
		put(key, value);		
	}
	
	public void add(String key, String value) {
		keys.add(key);
		put(key, value);		
	}
	
	public void set(String key, String value) {
		Object v = get(key);
		if		(v instanceof String)	put(key, value);
		else if (v instanceof Integer)	put(key, Integer.valueOf(value));
		else if (v instanceof Double)	put(key, Double.valueOf(value));
		else
			throw new IllegalArgumentException("Parameter '"+key+"' does not exist.");
	}
	
	public void set(String key, double value) {
		Object v = get(key);
		if		(v instanceof String)	put(key, ((Double)value).toString());
		else if (v instanceof Integer)	put(key, (int)value);
		else if (v instanceof Double)	put(key, value);
		else
			throw new IllegalArgumentException("Parameter '"+key+"' does not exist.");
	}
	
	public boolean isValidValue(String key, String value) {
		Object v = get(key);
		try {
			if (v instanceof Double)	Double.valueOf(value);
			if (v instanceof Integer)	Integer.valueOf(value);
			return true;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}
	
	public double fget(String key) {
		try {
			return ((Double) get(key)).doubleValue();
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Parameter '"+key+"' is not a double.");
		}
	}
	
	public int iget(String key) {
		try {
			return ((Integer) get(key)).intValue();
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Parameter '"+key+"' is not an integer.");
		}
	}
	
	public String sget(String key) {
		return get(key).toString();
	}
	
	public Object clone() {
		try {
			Parameters c = (Parameters) super.clone();
			c.keys = (Vector<String>) keys.clone();
			c.map = (HashMap<String, Object>) map.clone();
			return c;
		 } catch (CloneNotSupportedException e) {
			return null;
		 }
	}
	

	synchronized private void put(String key, Object value) {
		map.put(key, value);
	}
	
	synchronized private Object get(String key) {
		if (!map.containsKey(key))
			throw new IllegalArgumentException("Parameter '"+key+"' does not exist.");
		return map.get(key);
	}
}

