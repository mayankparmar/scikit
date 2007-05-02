package scikit.plot;

import java.util.Hashtable;

public class GridDisplay extends AbstractGrid {
	private static final long serialVersionUID = 1L;
	private int _data[];
	private Hashtable<Integer,Integer> _colors = new Hashtable<Integer,Integer>(); 
    
    public GridDisplay(String title, boolean inFrame) {
    	super(title, inFrame);
    }

    public void setData(int w, int h, int[] data) {
        if (w*h != data.length)
            throw new IllegalArgumentException("Width and height don't match array size");
        _data = data;
        setImageSize(w, h);
    }
    
    public void setColor(int v, int r, int g, int b) {
    	_colors.put(v, (r<<16) + (g<<8) + b);
    }
    
    protected double[] copyData() {
    	double[] ret = new double[_w*_h];
    	for (int i = 0; i < _w*_h; i++)
    		ret[i] = _data[i];
    	return ret;
    }
    
    protected int getColor(int i) {
    	if (_colors.containsKey(_data[i]))
    		return _colors.get(_data[i]);
    	else
    		return 0; // black
    }

}

