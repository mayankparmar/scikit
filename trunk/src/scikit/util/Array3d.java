package scikit.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;


import scikit.numerics.Math2;
import scikit.numerics.fn.Function3D;

public class Array3d implements Cloneable {
	private double[] _a;
	private int _nx, _ny, _nz;
	private double _lx, _ly, _lz;
	
	
	public Array3d(int nx, int ny, int nz, double[] a) {
		_lx = _nx = nx;
		_ly = _ny = ny;
		_lz = _nz = nz;
		_a = a;
	}

	public Array3d(int nx, int ny, int nz) {
		this(nx, ny, nz, new double[nx*ny*nz]);
	}
	
	public Array3d(File file) {
		try {
			DataInputStream dis = FileUtil.disFromString(file.toString());
			_lx = _nx = dis.readInt();
			_ly = _ny = dis.readInt();
			_lz = _nz = dis.readInt();
			_a = new double[_nx*_ny*_nz];
			for (int i = 0; i < _nx*_ny*_nz; i++)
				_a[i] = dis.readDouble();
			dis.close();
		} catch(IOException e) {
			System.err.println("Could not read file: " + file);
		}
	}
	
	public Array3d clone() {
		try {
			Array3d ret = (Array3d)super.clone();
			ret._a = DoubleArray.clone(_a);
			return ret;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	public void setLengths(double lx, double ly, double lz) {
		_lx = lx;
		_ly = ly;
		_lz = lz;
	}
	
	public int nx() { return _nx; }
	public int ny() { return _ny; }
	public int nz() { return _nz; }
	public double lx() { return _lx; }
	public double ly() { return _ly; }
	public double lz() { return _lz; }
	public double[] array() { return _a; }

	public double get(int x, int y, int z) {
		return _a[_nx*_ny*z+_nx*y+x];
	}
	
	public void set(int x, int y, int z, double v) {
		_a[_nx*_ny*z+_nx*y+x] = v;
	}
	
	public void writeFile(File fname) {
		try {
			DataOutputStream dos = FileUtil.dosFromString(fname.toString());
			dos.writeInt(_nx);
			dos.writeInt(_ny);
			dos.writeInt(_nz);
			for (double v : _a)
				dos.writeDouble(v);
			dos.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public void build(Function3D f) {
		double dx = _lx/_nx;
		double dy = _ly/_ny;
		double dz = _lz/_nz;
		for (int k = 0; k < _nz; k++)
			for (int j = 0; j < _nx; j++)
				for (int i = 0; i < _ny; i++)
					set(i, j, k, f.eval(i*dx, j*dy, k*dz));
	}
	
	public void buildShell(final double k, final double width) {
		build(new Function3D() {
			public double eval(double x, double y, double z) {
				x = Math.min(x, _lx-x);
				y = Math.min(y, _ly-y);
				z = Math.min(z, _lz-z);
				double r = Math2.hypot(x,y,z);
				return (Math.abs(k-r) < width/2.) ? 1 : 0;
			}
		});
	}
	
	public void shift(double x, double y, double z) {
		int xs = (int)Math.round(x/(_lx/_nx));
		int ys = (int)Math.round(y/(_ly/_ny));
		int zs = (int)Math.round(z/(_lz/_nz));
		Array3d scratch = clone();
		for (int k = 0; k < _nz; k++) {
			int kp = (k+zs+_nz)%_nz;
			for (int j = 0; j < _ny; j++) { 
				int jp = (j+ys+_ny)%_ny;
				for (int i = 0; i < _nx; i++) {
					int ip = (i+xs+_nx)%_nx;
					scratch.set(i, j, k, get(ip, jp, kp));
				}
			}
		}
		DoubleArray.copy(scratch._a, _a);
	}
	
	public double[] sliceX(int y, int z) {
		double[] ret = new double[_nx];
		for (int i = 0; i < _nx; i++)
			ret[i] = get(i, y, z);
		return ret;
	}
}
