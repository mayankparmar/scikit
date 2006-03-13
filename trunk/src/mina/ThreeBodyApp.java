/* 
 * The org.opensourcephysics.sip.ch10 package contains classes for Chapter 10
 * of the book, Introduction to Computer Simulation Methods.
 * Copyright (c) 2004, H. Gould, J. Tobochnik, and W. Christian.
 */

package mina;
import org.opensourcephysics.controls.*;
import org.opensourcephysics.frames.*;
 
public class ThreeBodyApp extends AbstractSimulation {   
   PlotFrame plot = new PlotFrame("x", "y", "planets");
   
   double m1 = 1, m2 = 1, m3 = 1;
   
   double G, dt;
   double x1, y1, vx1, vy1;
   double x2, y2, vx2, vy2;
   double x3, y3, vx3, vy3;
   
   
   public void doStep() {
		for (int i = 0; i < 100; i++) {
			double r, Fx, Fy;
			
			// force between planets 1 and 2
			r = Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
			Fx = G*m1*m2*(x1-x2) / (r*r*r);
			Fy = G*m1*m2*(y1-y2) / (r*r*r);
			vx1 -= (Fx/m1) * dt;
			vy1 -= (Fy/m1) * dt;
			vx2 += (Fx/m2) * dt;
			vy2 += (Fy/m2) * dt;
			
			// force between planets 1 and 3
			r = Math.sqrt((x1-x3)*(x1-x3) + (y1-y3)*(y1-y3));
			Fx = G*m1*m3*(x1-x3) / (r*r*r);
			Fy = G*m1*m3*(y1-y3) / (r*r*r);
			vx1 -= (Fx/m1) * dt;
			vy1 -= (Fy/m1) * dt;
			vx3 += (Fx/m3) * dt;
			vy3 += (Fy/m3) * dt;
			
			// force between planets 2 and 3
			r = Math.sqrt((x2-x3)*(x2-x3) + (y2-y3)*(y2-y3));
			Fx = G*m2*m3*(x2-x3) / (r*r*r);
			Fy = G*m2*m3*(y2-y3) / (r*r*r);
			vx2 -= (Fx/m2) * dt;
			vy2 -= (Fy/m2) * dt;
			vx3 += (Fx/m3) * dt;
			vy3 += (Fy/m3) * dt;
			
			// update planet 1 position
			x1 += vx1 * dt;
			y1 += vy1 * dt;
			
			// update planet 2 position
			x2 += vx2 * dt;
			y2 += vy2 * dt;
			
			// update planet 3 position
			x3 += vx3 * dt;
			y3 += vy3 * dt;
			
			
			plot.clearData();
			plot.append(1, x1, y1);
			plot.append(2, x2, y2);
			plot.append(3, x3, y3);
		}
   }
	
   
   public void initializeAnimation() {
		G = control.getDouble("G");
		dt = control.getDouble("dt");
		
		x1 = control.getDouble("x1");
		y1 = control.getDouble("y1");
		vx1 = control.getDouble("vx1");
		vy1 = control.getDouble("vy1");
		
		x2 = control.getDouble("x2");
		y2 = control.getDouble("y2");
		vx2 = control.getDouble("vx2");
		vy2 = control.getDouble("vy2");

		x3 = control.getDouble("x3");
		y3 = control.getDouble("y3");
		vx3 = control.getDouble("vx3");
		vy3 = control.getDouble("vy3");

		plot.setPreferredMinMax(-10, 10, -10, 10);  		
   }
	
	
   public void resetAnimation() {
		control.setValue("G", 20);
		control.setValue("dt", 0.004);
	  control.setValue("  ", "");
		
		double r = 6;
		double pi = Math.PI;
		double t = 0;
		
      control.setValue("x1", r * Math.cos(t));
      control.setValue("y1", r * Math.sin(t));
      control.setValue("vx1", Math.cos(t + pi/2));
      control.setValue("vy1", Math.sin(t + pi/2));
	  control.setValue("", "");

		t += 2*pi/3;
      control.setValue("x2", r * Math.cos(t));
      control.setValue("y2", r * Math.sin(t));
      control.setValue("vx2", Math.cos(t + pi/2));
      control.setValue("vy2", Math.sin(t + pi/2));
	  control.setValue(" ", "");
	  
		t += 2*pi/3;
      control.setValue("x3", r * Math.cos(t));
      control.setValue("y3", r * Math.sin(t));
      control.setValue("vx3", Math.cos(t + pi/2));
      control.setValue("vy3", Math.sin(t + pi/2));

      initializeAnimation();
   }

  
   public ThreeBodyApp() {
   }
   
   
   public static void main (String[] args) {
		SimulationControl.createApp(new ThreeBodyApp());
   }   
}
