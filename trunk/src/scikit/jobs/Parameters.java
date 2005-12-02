package scikit.jobs;


import java.util.Vector;
import java.util.HashMap;


class Value {
	protected String _v;
	
	public Value(String v) {
		_v = v;
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
		if (isValid(v))
			_v = v;
	}
	
	public boolean isValid(String v) {
		return true;
	}
}


class IntValue extends Value {
	private int lo = Integer.MIN_VALUE, hi = Integer.MAX_VALUE;
	
	public IntValue(String v) {
		super(v);
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
	
	public DoubleValue(String v) {
		super(v);
	}
	
	public DoubleValue(String v, double lo, double hi) {
		super(v);
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
	
	public void add(String key, int value) {
		keys.add(key);
		put(key, new IntValue(String.valueOf(value)));		
	}
	
	public void add(String key, double value) {
		keys.add(key);
		put(key, new DoubleValue(String.valueOf(value)));		
	}
	
	public void add(String key, String value) {
		keys.add(key);
		put(key, new Value(value));		
	}
	
	public void set(String key, String value) {
		get(key).set(value);
	}
	
	public boolean isValidValue(String key, String value) {
		return get(key).isValid(value);
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
	
	public Object clone() {
		try {
			Parameters c = (Parameters) super.clone();
			c.keys = (Vector<String>) keys.clone();
			c.map = (HashMap<String, Value>) map.clone();
			return c;
		 } catch (CloneNotSupportedException e) {
			return null;
		 }
	}
	

	synchronized private void put(String key, Value value) {
		map.put(key, value);
	}
	
	synchronized private Value get(String key) {
		if (!map.containsKey(key))
			throw new IllegalArgumentException("Parameter '"+key+"' does not exist.");
		return map.get(key);
	}
}

