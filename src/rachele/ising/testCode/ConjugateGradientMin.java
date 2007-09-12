package rachele.ising.testCode;

public class ConjugateGradientMin {
	static double GOLD = 1.618034;
	static double GOLDR = 0.61803399;
	static double GOLDC = 1-GOLDR;
	static double maxInterations = 100000;
	static double ZEPS = 0.00000001;  //small number that protects against
	//trying to achieve fractional accuracy for a minimum that happens to be
	// exactly zero
		
	public double function(double x) {
		double func = Math.pow(x+0.0,4)- 10*Math.pow(x,2);
		return func;
	}
	/**
	* initial bracket method takes two random points, ax and bx,
	* where f(ax) > f(bx). The point ax is one end point of the 
	* bracketing procedure and bx is the midpoint.  We wish to find
	* another point cx such that f(cx) > f(bx).  These will be our 
	* first 3 input points for the actual minimization procedure. 
	*/
	public double [] initialBracket(double ax, double bx){
		double [] output = new double [3];
		double f_b = function(bx);
		double f_a = function(ax);
		double u, f_u;
		System.out.println("initial a and b points:");
		System.out.println(ax + " " + bx);
		System.out.println("initial a and b values:");
		System.out.println(f_a + " " + f_b);
		
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
			System.out.println("switched: ax = " + ax + " bx = " + bx);			
		}
		
		//First guess for midpoint
		double cx =	bx + GOLD*(bx-ax);
		double f_c = function(cx);
		
		//repeat the following until we bracket
		int iterations = 0;
		while(f_b > f_c){
			iterations ++;
			u = cx + GOLD*(cx-bx);
			f_u = function(u);
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
		System.out.println("finished after " + iterations + " iterations");
		return output;
	}
	
	public double [] goldenMin(double [] input, double tolerence){
		double [] output = new double [2];
		double x0, x1, x2, x3;
		double ax = input[0];
		double bx = input[1];
		double cx = input[2];
		System.out.println("initial points: " + ax + " " + bx + " " + cx);
		x0 = ax;
		x3 = cx;
		
		//put the new test point in the longer segment
		//if bc is the longer segment, then x2 is the 
		//new test point and goes in between b and c
		
		if(Math.abs(cx - bx) > Math.abs(bx - ax)){
			System.out.println("bc larger");
			x1 = bx;
			x2 = bx + GOLDC*(cx - bx);
		}else{
			x2 = bx;
			x1 = bx - GOLDC*(bx - ax);
		}
		
		double f_1 = function(x1);
		double f_2 = function(x2);
		
		while(Math.abs(x3-x0) > tolerence*(Math.abs(x1) + Math.abs(x2))){
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
				f_2 = function(x2);
				
			}else{
				x3 = x2;
				x2 = x1;
				x1 = GOLDR*x2 + GOLDC*x0;
				// x1 = x0 + GOLDC*(x2-x0)
				//    = x0*GOLDR + x2*GOLDC
				
				f_2 = f_1;
				f_1 = function(x1);
			}
		}
		
		double xmin;
		double minValue;
		if(f_1 < f_2){
			xmin = x1;
			minValue = f_1;
		}else{
			xmin = x2;
			minValue = f_2;
		}
		

		output[0] = xmin;
		output[1] = minValue;
		return output;
	}
	
    public double [] brent(double [] input, double tolerence){
		double [] output = new double [2];
    	double xm; //midpoint
		
		double ax = input[0];
		double bx = input[1];
		double cx = input[2];		
		
		double a = (ax < cx ? ax : cx);
		double b = (ax > cx ? ax : cx);
		double x = bx;
		//double w = bx;
		//double v = bx;
		
		//double f_x = function(bx);
		//double f_w = f_x;
		//double f_v = f_x;
		
		for(int iteration = 1; iteration <= maxInterations; iteration ++){
			xm = 0.5*(a + b);
			double tol1 = tolerence*Math.abs(x) + ZEPS;
			double tol2 = 2.0*(tol1);
			//test for done here
			if (Math.abs(x-xm) <= (tol2 - 0.5*(b-a)) ){
				//xmin = x;
			}
			//construct a trial parabolic fit
			//if(){
				
			//}else{
				
			//}
		}//done with housekeeping.  back for another interation
		
		
		//output[0] = xmin;
		//output[1] = minValue;    	
		return output;
    }
	
}
