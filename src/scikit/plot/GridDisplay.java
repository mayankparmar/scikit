package scikit.plot;


import static java.lang.Math.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;


public class GridDisplay extends JComponent implements Display {
    private BufferedImage _image;
    private int _w, _h;
    private double _min = 0, _max = 1.0;
    private double[] _data;
    private int[] _pixelArray;

    private double colors[][] = {
        {0,     255, 255, 255},
        {0.2,  235, 215, 80}, // yellow
        {0.4,   235, 71, 0}, // red
        {0.55,   190, 10, 90}, // solid red
        {0.75,    101, 0, 150}, // blue
        {0.85,    51, 0, 130}, // blue
        {0.95,    20, 0, 80},
        {0.98,    10, 0, 50},
        {1.0,     0, 0, 0}
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
    
    
    public void setData(int w, int h, double[] data) {
        _w = w;
        _h = h;
        _data = data;
        
        if (w*h != data.length)
            throw new IllegalArgumentException("Width and height don't match array size");
        
        _pixelArray = new int[w*h*3];
        _image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
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
//            wheel[i] = Color.HSBtoRGB(h, s, b);
        }
    }
    
    
    private int getColor(double v) {
        int i = (int) (WHEEL_SIZE*v);
        return wheel[min(max(i, 0), WHEEL_SIZE-1)];
    }
    
    
    public void animate() {
        if (_image != null) {
            int pixelArrayOffset = 0;
            for (int y = 0; y < _h; y++) {
                for (int x = 0; x < _w; x++) {
                    double v = _data[y*_w+x];
                    int rgb = getColor((v-_min) / (_max - _min));
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

