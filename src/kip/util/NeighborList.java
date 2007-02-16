package kip.util;

import static java.lang.Math.*;
import static kip.util.MathPlus.*;


/**
 * Utility class to calculate lattice neighbor lists.
 */

public class NeighborList {
	/**
	 * PERIODIC -- Indices on the edges have fewer neighbors.
	 * BORDERED -- Indices on the edges wrap around.
	 */
	public static final int BORDERED = 0, PERIODIC = 1;

	public NeighborList(int Nx, int Ny) {
		this(Nx, Ny, 1, PERIODIC);
	}

	public NeighborList(int Nx, int Ny, double r, int type) {
		this.Nx = Nx;
		this.Ny = Ny;
		this.r = r;
		this.type = type;
		int d = (int)r*2 + 1;
		list = new int[d*d];

		if (type != BORDERED && type != PERIODIC)
			throw new IllegalArgumentException();
	}

	/**
	 * Sets the range of the neighbor-list.
	 */
	public void setRange(double r) {
		this.r = r;
	}

	/**
	 * Returns a neighbor list array.
	 * For each lattice index i, neighbor[i] is an array containing all of i's neighbor
	 * indices.
	 */
	public int[] get(int i) {
		calculateNeighborList(i);
		int[] ret = new int[num];
		System.arraycopy(list, 0, ret, 0, num);
		return ret;
	}

	/**
	 * Returns a static neighbor list array, terminated by index -1.
	 * For each lattice index i, neighbor[i] is an array containing all of i's neighbor
	 * indices.
	 * The returned array will be reused for each call to unsafeGet()
	 */
	public int[] unsafeGet(int i) {
		calculateNeighborList(i);
		list[num] = -1;
		return list;
	}

	// -------------------------------------------------------------------------------------
	// Protected methods
	// 

	int Nx, Ny;    // lattice size
	double r;      // neighbor list range
	int type;      // type -- BORDERED | PERIODIC

	// These variables are set be calculateNeighborList()
	int[] list;    // most recently computed neighbor list
	int num;       // number of elements

	void calculateNeighborList(int i) {
		int _r = (int)ceil(r);
		int ix = i % Ny;
		int iy = i / Nx;
		num = 0;

		switch (type) {
		case BORDERED:
			for (int jy = Math.max(0, iy-_r); jy <= Math.min(Ny-1, iy+_r); jy++) {
				for (int jx = Math.max(0, ix-_r); jx <= Math.min(Nx-1, ix+_r); jx++) {
					if (sqr(jx-ix) + sqr(jy-iy) <= sqr(r))
						list[num++] = jy*Nx + jx;
				}
			}
			break;

		case PERIODIC:
			// take advantage of rounding down when Ny or Nx is even.  this way
			// we avoid double counting (-Ny/2 == +Ny/2).
			// for odd Ny or Nx, this ambiguity does not exist, but the subtraction
			// by one will not have an effect.
			int ylo = iy - min(_r, (Ny-1)/2);
			int yhi = iy + min(_r, Ny/2);
			int xlo = ix - min(_r, (Nx-1)/2);
			int xhi = ix + min(_r, Nx/2);
			
			for (int jy = ylo; jy <= yhi; jy++) {
				for (int jx = xlo; jx <= xhi; jx++) {
					if (sqr(jx-ix) + sqr(jy-iy) <= sqr(r)) {
						int _jy = (jy + Ny) % Ny;
						int _jx = (jx + Nx) % Nx;
						list[num++] = _jy*Nx + _jx;
					}
				}
			}
			break;
		}
	}
}
