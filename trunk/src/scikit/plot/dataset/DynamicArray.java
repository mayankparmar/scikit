package scikit.dataset;


public class DynamicArray extends DataSet {
	private int _length;
	private double[] _data;
	
	public DynamicArray() {
		_data = new double[64];
		_length = 0;
	}
	
	/**
	* Returns the number of elements in the dynamic array
	*/
	public int size() {
		return _length;
	}
	
	/**
	* Appends a value to the end of the dynamic array
	*
	* @param x
	*/
	public void append(double x) {
		if (_length >= _data.length)
		increaseCapacity();
		_data[_length++] = x;
	}
	
	/**
	* Gets an indexed value
	*
	* @param i
	* @return array[i]
	*/   
	public double get(int i) {
		if (i >= _length)
			throw new ArrayIndexOutOfBoundsException();
		return _data[i];
	}
	
	/**
	* Sets value v to index i.  Grows the array by one if i=length()
	*
	* @param i
	* @param v
	*/   
	public void set(int i, double v) {
		if (i >= _length)
			throw new ArrayIndexOutOfBoundsException();
		_data[i] = v;
	}
	
	/**
	* Returns a copy of the dynamic array as a static array
	*/
	public double[] copyData() {
		double[] ret = new double[_length];
		System.arraycopy(_data, 0, ret, 0, _length);
		// System.out.println(ret.length);		
		return ret;
	}
	
	
	private void increaseCapacity() {
		double[] temp = new double[2 * _length];
		System.arraycopy(_data, 0, temp, 0, _length);
		_data = temp;
	}
}