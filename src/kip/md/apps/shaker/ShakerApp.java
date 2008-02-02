package kip.md.apps.shaker;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import scikit.dataset.Accumulator;
import scikit.dataset.DynamicArray;
import scikit.util.FileUtil;
import scikit.util.Terminal;

class Particle {
	double radius, id;
	public DynamicArray t = new DynamicArray();
	public DynamicArray x = new DynamicArray();
	public DynamicArray y = new DynamicArray();
	
	public void clear() {
		t.clear();
		x.clear();
		y.clear();
	}
	
	public void acc(double x, double y, double t) {
		this.x.append(x);
		this.y.append(y);
		this.t.append(t);
	}
	
	public double dist2(int i1, int i2) {
		double dx = x(i1) - x(i2);
		double dy = y(i1) - y(i2);
		return dx*dx+dy*dy;
	}
	
	public int size() {return t.size();}
	public double x(int i) {return x.get(i); }
	public double y(int i) {return y.get(i); }
	
}

class Data {
	int maxCnt = Integer.MAX_VALUE;
	int cnt;
	FloatBuffer fb;
	
	public Data(String fname) {
		try {
			FileChannel channel = new FileInputStream(fname).getChannel();
			MappedByteBuffer bb = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
			bb.order(ByteOrder.LITTLE_ENDIAN);
			
			IntBuffer ib = bb.asIntBuffer();
		    ib.get(); // unknown meaning
		    int dim = ib.get(); // spatial dimensions
		    int n = ib.get(); // columns of data
		    int m = ib.get(); // rows of data
		    int prec = ib.get(); // precision (4 for float, 8 for double)
		    int size = ib.get(); // m*n
		    for (int i = 0; i < 6; i++)
		    	ib.get(); // these should be zero
		    assert(dim == 2);
		    assert(prec == 4);
		    assert(size == m*n);
		    
		    bb.position(4*ib.position());
		    
		    cnt = 0;
		    fb = bb.asFloatBuffer();
		    
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public void reset() {
		cnt = 0;
		fb.rewind();
	}
	
	public boolean hasRemaining() {
		return cnt < maxCnt && fb.hasRemaining();
	}
	
	public Particle nextParticle() {
		if (!hasRemaining())
			return null;
		
		Particle ret = new Particle();
		double x, y, radius, time, id;
    	x = fb.get();
    	y = fb.get();
    	fb.get(); // brightness
    	radius = fb.get();
    	time = fb.get();
    	id = fb.get();
    	ret.id = id;
    	ret.radius = radius;
   		ret.acc(x, y, time);
   		
    	while (fb.hasRemaining()) {
    		fb.mark();
        	x = fb.get();
        	y = fb.get();
        	fb.get(); // brightness
        	radius = fb.get();
        	time = fb.get();
        	id = fb.get();
        	if (id == ret.id)
        		ret.acc(x, y, time);
        	else {
        		fb.reset();
        		break;
        	}
    	}
		return ret;
	}
}

class Alpha {
	public Accumulator x2;
	public Accumulator x4;
	public Accumulator alpha;
}


class Commands {
	Terminal term;
	
	public Commands(Terminal term) {
		this.term = term;
	}
	
	public Data loadFile() {
		try {
			String fname = FileUtil.loadDialog(term.getConsole(), "Open Shaker Data");
			return new Data(fname);
		} catch(IOException e) {
			term.println(e);
			return null;
		} 
	}
	
	public Alpha analyze(Data data) {
		data.reset();
		
		Accumulator x2 = new Accumulator(1);
		Accumulator x4 = new Accumulator(1);
		x2.setAveraging(true);
		x4.setAveraging(true);
		
		while (data.hasRemaining()) {
			Particle p = data.nextParticle();
			for (int i = 0; i < p.size(); i++) {
				for (int j = i+1; j < p.size(); j++) {
					double d2 = p.dist2(i, j);
					x2.accum(j-i, d2);
					x4.accum(j-1, d2*d2);
				}
			}
		}
		
		Alpha ret = new Alpha();
		ret.x2 = x2;
		ret.x4 = x4;
		return ret;
	}
}

public class ShakerApp extends Terminal {
	public static void main(String[] args) {
		ShakerApp term = new ShakerApp();
		term.help = "Suggested command sequence:\n"+
			"\tdata = loadFile();\n"+
			"\tdata.maxCnt = 100; // # of particles to analyze\n"+
			"\ta = analyze(data);\n"+
			"\tplot(a.x2)\n"+
			"\tplot(a.alpha)\n"+
			"(Right click in the plot windows to save data)";
		term.importObject(new Commands(term));
		term.runApplication();
	}
}
