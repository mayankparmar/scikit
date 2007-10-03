package rachele.ising.dim2;

import scikit.dataset.Accumulator;

//import static java.lang.Math.log;

abstract public class LineMin {
	static double GOLD = 1.618034;
	static double GOLDR = 0.61803399;
	static double GOLDC = 1-GOLDR;
	static double tolerance = 1e-15;
	public int feInfCt = 0;
	public int bracketPts = 0;
	public int accumPts = 0;

	
	// input values
	int N;
	double[] lineminPoint, lineminDirection;
	Accumulator landscape;
	Accumulator bracketLandscape;
	
	// return values
	double minValue;
	double xmin;
	
	abstract double freeEnergyCalc(double point[]);
	
	public LineMin(double lineminPoint[], double lineminDirection[]){
		N = lineminPoint.length;
		this.lineminPoint = lineminPoint;
		this.lineminDirection = lineminDirection;
		landscape = new Accumulator (tolerance);
		landscape.setAveraging(true);
		bracketLandscape = new Accumulator (tolerance);
		bracketLandscape.setAveraging(true);
		
    	double [] initBracket = new double [3]; 
     	// Make up two initial configurations and find an initial bracket
    	//be careful about input:  start at lambda_a = 0 amd lambda_b = positive so that it goes downhill 
    	initBracket = initialBracket(0.0, 0.5);
    	minValue = golden(initBracket);
    }
	
    private double [] initialBracket(double ax, double bx){
		double [] output = new double [3];

		double f_b = f1dim(bx);
		double f_a = f1dim(ax);
		bracketLandscape.accum(bx, f_b);
		bracketLandscape.accum(ax, f_a);
		bracketPts = 2;
		
		double u, f_u;
		
		//Check to see if f(ax) has a higher value than f(bx)
		//If not, make bx smaller and smaller until it does
		
		//while (f_b > f_a){
			//bx = .5*bx;
			//f_b = f1dim(bx);
//			if(bx < tolerance){
//				System.out.print("bx less than tolerence");
//			}
		//}
		//If not, switch the sign of bx and check
//		if (f_b > f_a){
//			bx = -bx;
//			f_b = f1dim(bx);
//		}
//		
		//Check to see if f(ax) has a higher value than f(bx)
		//If not, switch roles of ax and bx so that we can 
		//go downhill in the direction from ax to bx

		if (f_b > f_a){
			double tempA = f_a;
			f_a = f_b;
			f_b = tempA;
			tempA = ax;
			ax = bx;
			bx = tempA;
		}
		
		//First guess for midpoint
		double move = GOLD*(bx-ax);
		//double cx =	bx + GOLD*(bx-ax);
		double cx = bx + move;
		double f_c = f1dim(cx);
		bracketLandscape.accum(cx, f_c);
		bracketPts += 1;
//		while(f_c ==  Double.POSITIVE_INFINITY){
//			move *= GOLDC;
//			//move /= 2.0;
//			cx = bx + move;
//			f_c = f1dim(cx);
//			//System.out.println("f_c loop = " + f_c);
//		}
		//System.out.println("f_c = " + f_c);
		//repeat the following until we bracket
		int iterations = 0;
		while(f_b > f_c){
			iterations ++;
			u = 0.0;
			f_u = 0.0;
			move =  GOLD*(cx-bx);
			u = cx + move;
			f_u = f1dim(u);
			bracketLandscape.accum(u, f_u);
			bracketPts += 1;
			
//			if(f_u ==  Double.POSITIVE_INFINITY){
//				int uIterations = 0;
//				while(f_u ==  Double.POSITIVE_INFINITY){
//					uIterations += 1;
//					move /= 2.0;
//					u = cx + move;
//					f_u = f1dim(u);
//				}
//			}
			ax = bx;
			f_a = f_b;
			bx = cx;
			f_b = f_c;
			cx = u;
			f_c = f_u;
		}
		
		output[0] = ax;
		output[1] = bx;
		output[2] = cx;
		System.out.println("no of init pts = " + bracketPts);
		//double brSize = cx-ax;
		//double feDiff1 = f_c-f_b;
		//double feDiff2 = f_a-f_b;
		//System.out.print(" ");
		//System.out.println("init bracket finished after " + iterations + " iterations");
		//System.out.println("f_a = " + f_a + " f_b = " + f_b + " f_c = " + f_c);
		//System.out.print("bracket size = " + brSize + " FE diff = " + feDiff1 + " and " + feDiff2);
		//System.out.println(" ");
		return output;
	}
	
	
	private double golden(double [] input){
		
		double x0, x1, x2, x3;
		double ax = input[0];
		double bx = input[1];
		double cx = input[2];
		double smallest, smallestTol;
		
		
		//SOME TESTING
		
		double bracketSize = Math.abs(cx-ax);
		if(2*bracketSize < tolerance *(Math.abs(ax)+Math.abs(cx))){
			System.out.println("bracket too small");
		}
		double f_a = f1dim(ax);
		double f_c = f1dim(cx);
		double bracketRange = Math.abs(f_c-f_a);
		if(2*bracketRange < tolerance *(Math.abs(f_a)+Math.abs(f_c))){
			System.out.println("f range too small");
		}
		System.out.println("bracketSize = " + bracketSize + " range = " + bracketRange);
		
//		if(bracketRange < tolerance){
//			double xmin = bx;
//			for(int i = 0; i < N; i ++){
//				lineminPoint[i] = lineminPoint[i] + xmin*lineminDirection[i];
//			}
//			System.out.println("stop");
//			return xmin;
//		}
		
		//END TESTING
		
		x0 = ax;
		x3 = cx;
		
		//put the new test point in the longer segment
		//if bc is the longer segment, then x2 is the 
		//new test point and goes in between b and c
		
		if(Math.abs(cx - bx) > Math.abs(bx - ax)){
			x1 = bx;
			x2 = bx + GOLDC*(cx - bx);
		}else{
			x2 = bx;
			x1 = bx - GOLDC*(bx - ax);
		}


		double d01 = Math.abs(x1-x0);
		double tol01 = Math.abs(x1)+ Math.abs(x0);
		double d12 = Math.abs(x2-x1);
		double tol12 = Math.abs(x1)+ Math.abs(x2);
		double d23 = Math.abs(x3-x2);
		double tol23 = Math.abs(x2)+ Math.abs(x3);
		double d03 = Math.abs(x3-x0);
		
		smallest = d01;
		smallestTol = tol01;
		if (smallest > d12)
			smallest = d12;
			smallestTol = tol12;
		if (smallest > d23)
			smallest = d23;
			smallestTol = tol23;
		
		double f_1 = f1dim(x1);
		double f_2 = f1dim(x2);
		landscape.accum(x1, f_1);
		landscape.accum(x2, f_2);
		accumPts = 2;
		
		int iteration = 0;
		//while(Math.abs(x3-x0) > tolerance  && Math.abs(f_2-f_1) > tolerance && x1 != x2 && x2 != x3){
		//while(Math.abs(x3-x0) > tolerance*(Math.abs(x1) + Math.abs(x2))  && x1 != x2 && x2 != x3){
		while(2*Math.abs(f_2-f_1) > tolerance*(Math.abs(f_1) + Math.abs(f_2))  
				&& 2*smallest > tolerance*smallestTol){
				//&& x1 != x2 && x2 != x3){
			iteration ++;
//			System.out.println(x1 + " " + x2 + " " + x3);
//			System.out.println(Math.abs(x3-x0) + " " + tolerance*(Math.abs(x1) + Math.abs(x2)));
			
			if(f_2 < f_1){
				// choose new triplet as x1, x2, x3
				// shift variables x0, x1, x2 (new test point
				// which is chosen to go inside the larger segment
				x0 = x1;
				x1 = x2;
				x2 = GOLDR*x1 + GOLDC*x3; 
				// x2 goes a distance GOLDC*(x3-x1) inside the x3 x1 segment:
				// so x2 = x1 + GOLDC*(x3-x1)
				//       = x1*(1-GOLDC) + GOLDC*x3
				//       = x1*GOLDR + x3*GOLDC
				
				f_1 = f_2;
				f_2 = f1dim(x2);
				accumPts += 1;
				landscape.accum(x2, f_2);
				
			}else{
				x3 = x2;
				x2 = x1;
				x1 = GOLDR*x2 + GOLDC*x0;
				// x1 = x0 + GOLDC*(x2-x0)
				//    = x0*GOLDR + x2*GOLDC
				
				f_2 = f_1;
				f_1 = f1dim(x1);
				accumPts += 1;
				landscape.accum(x1, f_1);				
			}
			d01 = Math.abs(x1-x0);
			tol01 = Math.abs(x1)+ Math.abs(x0);
			d12 = Math.abs(x2-x1);
			tol12 = Math.abs(x1)+ Math.abs(x2);
			d23 = Math.abs(x3-x2);
			tol23 = Math.abs(x2)+ Math.abs(x3);
			d03 = Math.abs(x3-x0);
			//double tol03 = Math.abs(x1)+ Math.abs(x0);
			smallest = d01;
			smallestTol = tol01;
			if (smallest > d12){
				smallest = d12;
				smallestTol = tol12;}
			if (smallest > d23){
				smallest = d23;
				smallestTol = tol23;}
		}
		
		//System.out.println("Golden Min finished after " + iteration + " iterations");
		
		double xmin;
		double minValue;
		if(f_1 < f_2){
			xmin = x1;
			minValue = f_1;
		}else{
			xmin = x2;
			minValue = f_2;
		}
		for(int i = 0; i < N; i ++){
			lineminPoint[i] = lineminPoint[i] + xmin*lineminDirection[i];
		}
		//double finalBracket = x3-x2;
		//System.out.println("golden min = " + minValue + " lambda = " + xmin + " finalBracket = " + finalBracket);
		//minReferenceDirection = delFreeEnergyCalc(minReferencePoint);
		double decrease = f_2-f_1;

		
		System.out.println("no of golden pts = " + accumPts + " decrease = " + decrease + " " + d01 + " " + d12 + " " + d23 + " " + d03);
		return minValue;
	}
	
    private double f1dim(double lambda){
    	double newPoint [] = new double [N];
    	//accumPts += 1;
    	for(int i = 0; i < N; i++){
    		newPoint[i] = lineminPoint[i] + lambda*lineminDirection[i];
//    		if(Math.abs(newPoint[i]) >= 1.0){
//    			System.out.println("not allowed");
//    		}
    	}
    	double ret = freeEnergyCalc(newPoint);
    	if(ret == Double.POSITIVE_INFINITY){
    		System.out.println("inf at lambda = " + lambda);
    		feInfCt += 1;
    		//landscape.accum(lambda, 0);
    	}else{
    		//landscape.accum(lambda, ret);
   			//System.out.println("f1dim " + lambda + " " + ret);
    	}
    	return ret;
    }

    
    public Accumulator getLandscape(){
    	return landscape;
    }
    
    public Accumulator getBracketLandscape(){
    	return bracketLandscape;
    }
}
