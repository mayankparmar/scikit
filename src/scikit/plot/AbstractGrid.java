package scikit.plot;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.*;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.*;


abstract class AbstractGrid extends JComponent implements Display {
	protected static final long serialVersionUID = 1L;
	protected BufferedImage _image;
	protected int _w, _h;
    protected int[] _pixelArray;
    protected double _dx = 1;
    protected JPopupMenu _popup = new JPopupMenu();

	
    public AbstractGrid(String title, boolean inFrame) {
		addMouseListener(_mouseListener);
        if (inFrame) {
            scikit.jobs.Job.frame(this, title);
        }
    }
    
    
    public void setDeltaX(double dx) {
    	_dx = dx;
    }
 

    public void animate() {
        if (_image != null) {
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
        _pixelArray = null;
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
    
    
    abstract protected double[] copyData();
    abstract protected int getColor(int i);
    
    protected void setImageSize(int w, int h) {
        _w = w;
        _h = h;
        _pixelArray = new int[w*h*3];
        _image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);    

    }
    

    private boolean hasData() {
    	return _image != null;
    }
    
    
	private void saveData(String str) {
		try {
			PrintWriter pw = scikit.util.Dump.pwFromDialog(this, str);
			scikit.util.Dump.writeOctaveGrid(pw, copyData(), _w, _dx);
		} catch (IOException e) {}
	}
	
	
	private void fillPopup() {
		_popup.removeAll();
		JMenuItem menuItem = new JMenuItem("Save data...");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveData("grid.txt");
			}
		});
		_popup.add(menuItem);
	}
	
	
	private MouseListener _mouseListener = new MouseAdapter() {
		public void mousePressed(MouseEvent e) {
			if (e.isPopupTrigger() && hasData()) {
				fillPopup();
				_popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
		public void mouseReleased(MouseEvent e) {
			if (e.isPopupTrigger() && hasData()) {
				fillPopup();
				_popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	};
}

