package scikit.plot;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class FieldDisplay extends AbstractGrid {
	private static final long serialVersionUID = 1L;
	private double _data[];
	private boolean _autoScale = true;
	private double _min, _max;

	private double _colors[][] = {
			{1-1.0,     0, 0, 0},
			{1-0.98,    10, 0, 50},
			{1-0.95,    20, 0, 80},
			{1-0.85,    61, 0, 130}, // blue
			{1-0.7,    121, 20, 150}, // blue
			{1-0.5,    190, 40, 90}, // solid red
			{1-0.35,   215, 90, 40}, // red
			{1-0.15,   235, 195, 80}, // yellow
			{1-0,      255, 255, 255}
	};
	private int WHEEL_SIZE = 512;
	private int wheel[] = new int[WHEEL_SIZE];

	public FieldDisplay(String title, boolean inFrame) {
		super(title, inFrame);
		initColorWheel();
	}

	public void setData(int w, int h, double[] data) {
		if (w*h != data.length)
			throw new IllegalArgumentException("Width and height don't match array size");
		_data = data;
		setImageSize(w, h);
	}

    
    public void setScale(double min, double max) {
    	_autoScale = false;
    	_min = min;
    	_max = max;
    }
    
    
    public void setAutoScale() {
    	_autoScale = true;
    }
    
    
    private void initColorWheel() {
         for (int i = 0; i < WHEEL_SIZE; i++) {
             double a = (double)i / WHEEL_SIZE;
             
             // get color for value 'a'
             int j = 0;
             while (a >= _colors[j+1][0])
                 j++;
             
             double v = (a - _colors[j][0]) / (_colors[j+1][0] - _colors[j][0]);
             int r = (int) (_colors[j][1]*(1-v) + _colors[j+1][1]*v);
             int g = (int) (_colors[j][2]*(1-v) + _colors[j+1][2]*v);
             int b = (int) (_colors[j][3]*(1-v) + _colors[j+1][3]*v);
             wheel[i] = (r<<16) + (g<<8) + b;
         }
     }
    
    
    private void findBounds() {
    	_min = _max = _data[0];
    	for (int i = 1; i < _w*_h; i++) {
    		double v = _data[i];
    		_min = min(_min, v);
    		_max = max(_max, v);
    	}
    }
    
    
    protected int getColor(int i) {
        double v = _data[i];
        double scaled = (v - _min) / (_max - _min);
        int c = (int) (WHEEL_SIZE*scaled);
        return wheel[min(max(c, 0), WHEEL_SIZE-1)];
    }
    
    
    protected double[] copyData() {
    	double[] ret = new double[_w*_h];
    	System.arraycopy(_data, 0, ret, 0, _w*_h);
    	return ret;
    }
    
    
    public void animate() {    	 
    	if (_autoScale)
    		findBounds();
    	super.animate();
    }

}
