package scikit.params;

public class StringValue extends GuiValue {
	public StringValue(Object v) {
		super(v);
	}
	
	public boolean testValidity(String v) {
		return true;
	}
}
