package kip.clump;
import static java.lang.Math.*;
import static java.lang.Integer.*;


public class QuadTree {
	int[] elements, rawElements;
	int L, R;
	
	// - system length = L, interaction range = R, lattice spacing = 1
	// - points are separated by at least latticeSpacing distance
	// - the coordinates of points range from 0 to L-1
	// - (x,y) position of quads are specified by the center point.
	//   for quads of non-zero length, the coordinates are rounded
	//   up to integer values 
	
	// offsets in array corresponding to the following variables:
	// X,Y: center coordinates of quad, rounded up
	// CNT: number of points within quad
	// LEN: distance from leftmost to rightmost points of a quad
	static final int X=0, Y=1, CNT=2, LEN=3, NITEMS=4;
	// four children: bottom-left, top-left, bottom-right, top-right
	static final int BL=1, TL=2, BR=3, TR=4;
	
	static final double sqrt2 = sqrt(2);
	
	// # elements: 1 + 4 + ... 4^n = (4^n-1) / (4-1)
	// layout:
	// within a quad, the layout is bl, tl, br, tr, so:
	//  {_} {bl  tl   br   tr} {bl.bl bl.tl   .. tl.bl  ..  tr.tr} {bl.bl.bl ...}
	//  {0} {(0*4+1)..(0*4+4)} {(1*4+1) (1*4+2)..(2*4+1)..(4*4+4)} {(5*4+1) ... }
	//
	// children are addressed as:
	// bl(i) = i*4+1
	// tl(i) = i*4+2
	// br(i) = i*4+3
	// tr(i) = i*4+4
	// 
	// maximum index = number of elements:
	// verify by substitution: ((0*4+4)*4+4)*4+4... (n times) = (4^n-1)/3
	// 
	// each element has four numbers: x, y, count, length, so a final factor of
	// four (NITEMS) is applied when addressing an element in the array
	//
	
	public QuadTree(int L, int R) {
		this.L = L;
		this.R = R;
		assert(R < L/4);
		// n: the number of levels of the quadtree. n=1 is just the root.
		int n = 1+numberOfTrailingZeros(L);  			// L = 2^(n-1)
		assert (L == 1 << (n-1));
		int nelems = ((1<<2*n)-1)/3; 					// (4^n-1)/(4-1)
		elements = new int[nelems*NITEMS];
		rawElements = new int[L*L];
		fillElements(0, L/2, L/2, L);
	}
	
	private void fillElements(int i, int x, int y, int len) {
		elements[i*NITEMS+X] = x;
		elements[i*NITEMS+Y] = y;
		elements[i*NITEMS+CNT] = 0;
		elements[i*NITEMS+LEN] = len;
		if (len > 1) {
			// usually (x,y) is the center of quad plus (1/2,1/2).  however, when
			// the quad contains just one point, (x,y) should be exactly its coords.
			// we therefore introduce an asymmetry when divide by 4 introduces
			// rounding.
			int add = len/4;
			int sub = (len+2)/4;
			fillElements(i*4+BL, x-sub,	y-sub,	len/2);
			fillElements(i*4+TL, x-sub,	y+add,	len/2);
			fillElements(i*4+BR, x+add,	y-sub,	len/2);
			fillElements(i*4+TR, x+add,	y+add,	len/2);
		}
	}
	
	private int childIndex(int i, int x, int y) {
		int centerX = elements[i*NITEMS+X];
		int centerY = elements[i*NITEMS+Y];
		// points which fall exactly on centerX or centerY are in the rightmost
		// or topmost child, respectively.
		boolean x_lt = x < centerX;
		boolean y_lt = y < centerY;
		return i*4 + (x_lt ? (y_lt ? BL : TL) : (y_lt ? BR : TR));
	}
	
	public void addOrRemove(int x, int y, int cnt) {		
		int i = 0;
		while (elements[i*NITEMS+LEN] > 1) {
			elements[i*NITEMS+CNT] += cnt;
			i = childIndex(i, x, y);
		}
		elements[i*NITEMS+CNT] += cnt;
		
		rawElements[L*y+x] += cnt;
	}
	
	int elems = 0;
	public void add(int x, int y) {
		elems++;
		addOrRemove(x, y, 1);
	}
	
	public void remove(int x, int y) {
		addOrRemove(x, y, -1);
	}
	
	public int countOverlaps(int x, int y) {
		// explicitly choose periodicity of coordinates before visiting each of
		// the four top quadrants.  this will enable simpler calculation of
		// distances.  we make sure that if (x,y) is not within a quadrant, then
		// at least it is represented as being as close as possible to the quadrant.  
		int xl = (x < L*3/4) ? x : x-L;
		int xr = (x > L*1/4) ? x : x+L;
		int yb = (y < L*3/4) ? y : y-L;
		int yt = (y > L*1/4) ? y : y+L;
		int c1 = countOverlapsAux(BL, xl, yb) + countOverlapsAux(TL, xl, yt)
			+ countOverlapsAux(BR, xr, yb) + countOverlapsAux(TR, xr, yt);
//		int c2 = countOverlapsAux2(x,y);
//		assert(c1 == c2);
		return c1;
	}
	
	public int countOverlapsAux(int i, int x, int y) {
		int len = elements[i*NITEMS+LEN];
		int cnt = elements[i*NITEMS+CNT];
		int xp = elements[i*NITEMS+X];
		int yp = elements[i*NITEMS+Y];
		
		if (len == 1) {
			int dx = x - xp;
			int dy = y - yp;
			return (dx*dx + dy*dy <= R*R) ? cnt : 0;
		}
		else {
			double dx = x - (xp-0.5);
			double dy = y - (yp-0.5);
			double d2 = dx*dx + dy*dy;
			
			// - this quad has "len" points on a side, so the distance from the
			//   center to the side is (len-1)/2
			// - "a" is the length from the center of the quad to the corner
			// - it is extended slightly to make complete inclusion or exclusion stricter
			double a = ((len-1)/2.0)*(sqrt2+1e-8); // 
			
//			int slow = countOverlapsAux(4*i+BL, x, y) + countOverlapsAux(4*i+TL, x, y)
//				+ countOverlapsAux(4*i+BR, x, y) + countOverlapsAux(4*i+TR, x, y);
			
			if (a < R && d2 < (R-a)*(R-a)) { // d+a < R, entire region within interaction range
//				assert (slow == cnt);
				return cnt;
			}
			else if (d2 > (R+a)*(R+a)) { // d-a > R, none of region in range
//				assert (slow == 0);
				return 0;
			}
			else {
				return countOverlapsAux(4*i+BL, x, y) + countOverlapsAux(4*i+TL, x, y)
					+ countOverlapsAux(4*i+BR, x, y) + countOverlapsAux(4*i+TR, x, y);
			}
		}
	}
	
	public int countOverlapsAux2(int x, int y) {
		int acc = 0;
		for (int yp = 0; yp < L; yp++) {
			for (int xp = 0; xp < L; xp++) {
				int dx = abs(x - xp);
				int dy = abs(y - yp);
				dx = min(dx, L-dx);
				dy = min(dy, L-dy);
				if (dx*dx + dy*dy <= R*R)
					acc += rawElements[L*yp + xp];
			}
		}
		return acc;
	}
}

