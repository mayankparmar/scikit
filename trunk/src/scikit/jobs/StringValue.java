package scikit.jobs;

public class StringValue extends GuiValue {
	public StringValue(Object v) {
		super(v);
	}
	
	public boolean testValidity(String v) {
		return true;
	}
	
	public Object valueForString(String v) {
		return v;
	}
}
