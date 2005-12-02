package scikit.plot;


public interface DataSet {
	/** Returns the size of this dataset */
	public int size();
	
	/** Returns a copy of this dataset */
	public double[] copyData();
}
