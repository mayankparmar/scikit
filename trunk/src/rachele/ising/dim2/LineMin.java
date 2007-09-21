package rachele.ising.dim2;

//import static java.lang.Math.log;

abstract public class LineMin {
	static double GOLD = 1.618034;
	static double GOLDR = 0.61803399;
	static double GOLDC = 1-GOLDR;
	static double tolerance = 1e-16;

	// input values
	int N;
	double[] lineminPoint, lineminDirection;
	
	// return values
	double minValue;
	double xmin;
	
	abstract double freeEnergyCalc(double point[]);
	
	public LineMin(double lineminPoint[], double lineminDirection[]){
		N = lineminPoint.length;
		this.lineminPoint = lineminPoint;
		this.lineminDirection = lineminDirection;
		
    	double [] initBracket = new double [3]; 
     	// Make up two initial configurations and find an initial bracket
    	//be careful about input:  start at lambda_a = 0 amd lambda_b = positive so that it goes downhill 
    	initBracket = initialBracket(0.0, 0.1);
    	minValue = golden(initBracket);
    }
	
    private double [] initialBracket(double ax, double bx){
		double [] output = new double [3];
		//can't accept infinity as a bracketing parameter.
		
		//make sure bx is positive
		
		//bx = Math.abs(bx);
		
		double f_b = f1dim(bx);
		while(f_b == Double.POSITIVE_INFINITY){
			bx /= 2.0;
			f_b = f1dim(bx);
		}
		double f_a = f1dim(ax);
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
		while(f_c ==  Double.POSITIVE_INFINITY){
			move /= 2.0;
			cx = bx + move;
			f_c = f1dim(cx);
			//System.out.println("f_c loop = " + f_c);
		}
		//System.out.println("f_c = " + f_c);
		//repeat the following until we bracket
		int iterations = 0;
		while(f_b > f_c){
			u = 0.0;
			f_u = 0.0;
			//System.out.println("top: cx = "+ cx + " f_c = "+ f_c + " u = " + u + " f_u = " + f_u + " f_c= "+ tester + " f_b = "+ f_b);
			iterations ++;
			move =  GOLD*(cx-bx);
			//u = cx + GOLD*(cx-bx);
			u = cx + move;
			f_u = f1dim(u);
			//System.out.println("top: cx = "+ cx + " f_c = "+ f_c + " u = " + u + " f_u = " + f_u + " f_b = "+ f_b);
			if(f_u ==  Double.POSITIVE_INFINITY){
				int uIterations = 0;
				while(f_u ==  Double.POSITIVE_INFINITY){
					uIterations += 1;
					move /= 2.0;
					//System.out.println(f_u);
					u = cx + move;
					f_u = f1dim(u);
				}
			}
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
		System.out.println("init bracket finished after " + iterations + " iterations");
		System.out.println("f_a = " + f_a + " f_b = " + f_b + " f_c = " + f_c);
		return output;
	}
	
	
	private double golden(double [] input){
		
		double x0, x1, x2, x3;
		double ax = input[0];
		double bx = input[1];
		double cx = input[2];
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
		
		double f_1 = f1dim(x1);
		double f_2 = f1dim(x2);
		
		int iteration = 0;
		while(Math.abs(x3-x0) > tolerance*(Math.abs(x1) + Math.abs(x2))  && x1 != x2 && x2 != x3){
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
				
			}else{
				x3 = x2;
				x2 = x1;
				x1 = GOLDR*x2 + GOLDC*x0;
				// x1 = x0 + GOLDC*(x2-x0)
				//    = x0*GOLDR + x2*GOLDC
				
				f_2 = f_1;
				f_1 = f1dim(x1);
			}
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
		System.out.println("golden min = " + minValue + " lambda = " + xmin);
		//minReferenceDirection = delFreeEnergyCalc(minReferencePoint);
		return minValue;
	}
	
    private double f1dim(double lambda){
    	double newPoint [] = new double [N];
    	for(int i = 0; i < N; i++){
    		newPoint[i] = lineminPoint[i] + lambda*lineminDirection[i];
    	}
    	double ret = freeEnergyCalc(newPoint);
    	return ret;
    }

    /*
	public double [] steepestAscentCalc(double [] config){
		double steepestAscentDir [] = new double [N];
		convolveWithRange(config, phi_bar, R);
		for (int i = 0; i < Lp*Lp; i++) {
			steepestAscentDir[i] = -phi_bar[i] +T* kip.util.MathPlus.atanh(phi[i])- H;
		}
		return steepestAscentDir;		
	}
		public double freeEnergyCalc(double [] config){
		convolveWithRange(config, phi_bar, R);
		for (int i = 0; i < Lp*Lp; i++) {
			double entropy = -((1.0 + config[i])*log(1.0 + config[i]) +(1.0 - config[i])*log(1.0 - config[i]))/2.0;
			double potential = -(config[i]*phi_bar[i])/2.0;
			//System.out.println( i + " " + config[i] + " " + entropy + " " + potential);
			freeEnergy += potential - T*entropy - H*config[i];
		}
		
		return freeEnergy;
	}

	*/
}
