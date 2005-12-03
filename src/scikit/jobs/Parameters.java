package scikit.jobs;


import java.util.Vector;
import java.util.HashMap;



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
		put(key, new IntValue(value, lockable));
	}
	
	public void add(String key, double value, boolean lockable) {
		keys.add(key);
		put(key, new DoubleValue(value, lockable));		
	}
	
	public void add(String key, int value, int lo, int hi, boolean lockable) {
		keys.add(key);
		put(key, new IntValue(value, lo, hi, lockable));
	}
	
	public void add(String key, double value, double lo, double hi, boolean lockable) {
		keys.add(key);
		put(key, new DoubleValue(value, lo, hi, lockable));		
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
	
	public void enableSlider(String key) {
		get(key).enableAuxiliaryEditor();
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

