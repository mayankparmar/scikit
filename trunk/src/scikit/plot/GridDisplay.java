package scikit.plot;


import static java.lang.Math.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;


public class GridDisplay extends JComponent implements Display {
    private BufferedImage _image;
    private int _w, _h;
    private boolean _autoAdjust;
    private double _min, _max;
    private double[] _data;
    private int[] _idata;
    private int[] _pixelArray;

    private double colors[][] = {
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
    
    
    public GridDisplay(String title, boolean inFrame) {
        if (inFrame) {
            scikit.jobs.Job.frame(this, title);
        }
        initColorWheel();
    }
    
    
    public Dimension getPreferredSize() {
        return new Dimension(300, 300);
    }
    
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (_image != null) {
            g.drawImage(_image, 0, 0, getWidth(), getHeight(), 0, 0, _image.getWidth(), _image.getHeight(), null);
        }
        else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    
    
    private void setDataHelper(int w, int h, double[] data, int[] idata) {
    	int len = data != null ? data.length : idata.length;
        if (w*h != len)
            throw new IllegalArgumentException("Width and height don't match array size");
        _w = w;
        _h = h;
        _data = data;
        _idata = idata;
        _autoAdjust = true;
        _pixelArray = new int[w*h*3];
        _image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);    
    }
    
    
    public void setData(int w, int h, double[] data) {
        setDataHelper(w, h, data, null);
    }
    
    
    public void setData(int w, int h, int[] data) {
        setDataHelper(w, h, null, data);
    }
    
    
    public void setBounds(double min, double max) {
    	_autoAdjust = false;
    	_min = min;
    	_max = max;
    }
    
    
    private void initColorWheel() {
        for (int i = 0; i < WHEEL_SIZE; i++) {
            double a = (double)i / WHEEL_SIZE;
            
            // get color for value 'a'
            int j = 0;
            while (a >= colors[j+1][0])
                j++;
            
            double v = (a - colors[j][0]) / (colors[j+1][0] - colors[j][0]);
            int r = (int) (colors[j][1]*(1-v) + colors[j+1][1]*v);
            int g = (int) (colors[j][2]*(1-v) + colors[j+1][2]*v);
            int b = (int) (colors[j][3]*(1-v) + colors[j+1][3]*v);
            wheel[i] = (r<<16) + (g<<8) + b;
        }
    }
    
    
    private double getValue(int i) {
    	return (_data != null) ? _data[i] : _idata[i];
    }
    
    
    private void findBounds() {
    	_min = _max = getValue(0);
    	for (int i = 1; i < _w*_h; i++) {
    		double v = getValue(i);
    		_min = min(_min, v);
    		_max = max(_max, v);
    	}
    }
    
    
    private int getColor(int i) {
        double v = getValue(i);
        double scaled = (v - _min) / (_max - _min);
        int c = (int) (WHEEL_SIZE*scaled);
        return wheel[min(max(c, 0), WHEEL_SIZE-1)];
    }
    
    
    public void animate() {
        if (_image != null) {
        	if (_autoAdjust)
        		findBounds();
            int pixelArrayOffset = 0;
            for (int y = 0; y < _h; y++) {
                for (int x = 0; x < _w; x++) {
                    int rgb = getColor(_w*y+x);
                    int r = (rgb & 0xff0000) >> 16;
                    int g = (rgb & 0x00ff00) >> 8;
                    int b = (rgb & 0x0000ff);
                    _pixelArray[pixelArrayOffset++] = r;
                    _pixelArray[pixelArrayOffset++] = g;
                    _pixelArray[pixelArrayOffset++] = b;
                }
            }
            WritableRaster raster = _image.getRaster();
            raster.setPixels(0, 0, _w, _h, _pixelArray);
        }
		repaint();        
    }
    
    
    public void clear() {
        _image = null;
        _data = null;
        _pixelArray = null;
    }
}

