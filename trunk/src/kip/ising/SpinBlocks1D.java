package kip.ising;


public class SpinBlocks1D {
	SpinBlockIndexer indexer;
	int[] indices;
	int[][] blocks;
	int L, R;
	
	public SpinBlocks1D(int L, int R) {
		this.L = L;
		this.R = R;
		indexer = new SpinBlockIndexer(L, R);
		int maxScale = indexer.maxScale();
		indices = indexer.newArray();
		blocks = new int[maxScale+1][];
		
		for (int scale = 0; scale <= maxScale; scale++) {
			int blockLen = 1 << scale;
			blocks[scale] = new int[L/blockLen];
			for (int x = 0; x < L/blockLen; x++) {
				blocks[scale][x] = blockLen;
			}
		}
	}
	
	
	public int sumInRange(int x) {
		return sumInRange(x-R, x+R);
	}
	
	
	public int sumInRange(int xlo, int xhi) {
		indexer.fillArray(xlo, xhi, indices);
		int sum = 0;
		for (int i = 0; indices[i] >= 0; i += 2) {
			sum += blocks[indices[i]][indices[i+1]];
		}
		// assert(sum == slowSumInRange(x));
		return sum;
	}
	
	
	public int slowSumInRange(int xlo, int xhi) {
		int sum = 0;
		for (int xp = xlo; xp <= xhi; xp++) {
			int i = (xp + L)%L;
			sum += blocks[0][i];
		}
		return sum;
	}
	
	
	public void flip(int x) {
		int dm = -2*blocks[0][x]; // change in magnetization after flipping spin i
		for (int scale = 0; scale < blocks.length; scale++) {
			int b = x >> scale;
			blocks[scale][b] += dm;
		}
	}
	
	public int get(int x) {
		return blocks[0][x];
	}	
}
