package scikit.jobs;

public class IntValue extends Value {
	private int _lo = Integer.MIN_VALUE, _hi = Integer.MAX_VALUE;
	
	public IntValue(int v, boolean lockable) {
		super(String.valueOf(v), lockable);
	}
	
	public IntValue(int v, int lo, int hi, boolean lockable) {
		super(String.valueOf(v), lockable);
		_lo = lo;
		_hi = hi;		
	}
	
	public int iget() {
		return Integer.valueOf(_v);
	}
	
	public boolean testValidity(String v) {
		try {
			int i = Integer.valueOf(v);
			return _lo <= i && i <= _hi;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}
}
