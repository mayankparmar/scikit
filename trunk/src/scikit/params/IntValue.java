package scikit.params;

public class IntValue extends GuiValue {
	private int _lo = Integer.MIN_VALUE, _hi = Integer.MAX_VALUE;
	
	public IntValue(int x) {
		super(x);
	}
	
	public IntValue(int x, int lo, int hi) {
		super(x);
		_lo = lo;
		_hi = hi;		
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
