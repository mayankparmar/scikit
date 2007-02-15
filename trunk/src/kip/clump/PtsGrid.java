package kip.clump;

import static java.lang.Math.sqrt;
import kip.util.NeighborList;


public class PtsGrid {
	static final int CELL_SIZE = 8;
	
	double L; // system length
	double R; // interaction range 
	int maxPoints;
	double[] xs, ys;
	int ptsCnt;
	
	int gridCols;  // columns in grid  
	double dx; // distance between grid elements
	int[][] grid;  // list of indices into x[], y[].
	int[] gridCnt; // number of elements in each grid.
	
	NeighborList nlist;
	int[][] neighbors;
	
	
	public PtsGrid(double L, double R, int maxPoints) {
		this.L = L;
		this.R = R;
		this.maxPoints = maxPoints;
		xs = new double[maxPoints];
		ys = new double[maxPoints];
		ptsCnt = 0;
		
		gridCols = (int)((sqrt(maxPoints)+1)/2);
		dx = L / gridCols;
		grid = new int[gridCols*gridCols][CELL_SIZE];
		gridCnt = new int[gridCols*gridCols]; // initialize to zero
		
		nlist = new NeighborList(gridCols, gridCols, (int)(R/dx+2), NeighborList.PERIODIC);
		neighbors = new int[gridCols*gridCols][];
	}
	
	
	private double dist2(double dx, double dy) {
		if (dx < -L/2) dx = L + dx;
		if (dx >  L/2) dx = L - dx;
		if (dy < -L/2) dy = L + dy;
		if (dy >  L/2) dy = L - dy;
		return dx*dx + dy*dy;
	}
	
	// rounding errors here are ok, as long as they occur in just this
	// one function
	private int gridIndex(double x, double y) {
		int i = (int)(x/dx);
		int j = (int)(y/dx);
		assert(i < gridCols && j < gridCols);
		return j*gridCols+i;
	}
	
	private void resizeGridElems(int j) {
		int[] cell = grid[j];
		int cellCnt = gridCnt[j];
		if (cellCnt < cell.length/4 && CELL_SIZE < cell.length) {
			System.out.println("shrinking " + j);
			grid[j] = new int[cell.length/2];
			System.arraycopy(cell, 0, grid[j], 0, cellCnt);
		}
		if (cellCnt == cell.length) {
			System.out.println("growing " + j);			
			grid[j] = new int[2*cell.length];
			System.arraycopy(cell, 0, grid[j], 0, cellCnt);
		}
	}
	
	int countOverlapsSlow(double x, double y) {
		int acc = 0;
		for (int i = 0; i < ptsCnt; i++) {
			if (dist2(x-xs[i], y-ys[i]) < R*R) {
				System.out.println("found1 " + i + " " + xs[i] + " " + ys[i]);
				acc++;
			}
		}
		return acc;
	}
	
	public int countOverlaps(double x, double y) {
		int j1 = gridIndex(x,y);
		if (neighbors[j1] == null)
			neighbors[j1] = nlist.get(j1);
		int acc = 0;
//		for (int j2 : neighbors[j1]) {
		for (int j2 = 0; j2 < grid.length; j2++) {
			for (int c = 0; c < gridCnt[j2]; c++) {
				int i = grid[j2][c];
				if (dist2(x-xs[i], y-ys[i]) < R*R) {
					System.out.println("found2 " + i + " " + xs[i] + " " + ys[i]);
					acc++;
				}
			}
		}
		int acc2 = countOverlapsSlow(x, y);
		assert (acc == acc2);
		return acc;
	}
	
	public void removeAll() {
		for (int i = 0; i < maxPoints; i++) {
			remove(xs[i], ys[i]);
		}
	}
	
	public void remove(double x, double y) {
		int j = gridIndex(x, y);
		int[] cell = grid[j];
		int cellCnt = gridCnt[j];
		
		for (int c = 0; c < cellCnt; c++) {
			int i = cell[c]; 
			if (xs[i] == x && ys[i] == y) {
				xs[i] = xs[ptsCnt-1];
				ys[i] = ys[ptsCnt-1];
				ptsCnt--;
				
				cell[c] = cell[cellCnt-1];
				gridCnt[j]--;
				resizeGridElems(j);
				return;
			}
		}
		throw new IllegalArgumentException();
	}
	
	public void add(double x, double y) {
		int j = gridIndex(x, y);
		int[] cell = grid[j];
		int cellCnt = gridCnt[j];
		
		xs[ptsCnt] = x;
		ys[ptsCnt] = y;
		ptsCnt++;
		
		cell[cellCnt] = ptsCnt-1;
		gridCnt[j]++;
		resizeGridElems(j);
	}
	
	
	public double x(int i) { return xs[i]; }
	public double y(int i) { return ys[i]; }
}
