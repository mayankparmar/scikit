package kip.md;

import static java.lang.Math.*;
import static kip.util.MathPlus.*;
import static scikit.util.Utilities.periodicOffset;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

import kip.util.Random;
import kip.util.Vec3;
import scikit.graphics.Canvas;
import scikit.graphics.CircleGraphics;
import scikit.graphics.Graphics;
import scikit.graphics.RectangleGraphics;
import scikit.util.Bounds;
import scikit.util.Point;
import scikit.util.Utilities;


public class ParticleContext {
	public enum Type { Torus2D, Disk2D };
	public double L; // system length
	public Type type;
	
	public ParticleContext(double L, Type type) {
		this.L = L;
		this.type = type;
	}
	
	public static Type typeForString(String name) {
		if (name.equals("Disk"))
			return Type.Disk2D;
		else
			return Type.Torus2D;
	}
	
	public int dim() {
		return 2;
	}
	
	public boolean periodic() {
		switch (type) {
		case Torus2D: return true;
		case Disk2D: return false;
		}
		throw new IllegalStateException();
	}
	
	public void wrap(Point p) {
		if (periodic()) {
			p.x = (p.x+L)%L;
			p.y = (p.y+L)%L;
			p.z = (p.z+L)%L;
		}
	}
	
	public double systemArea() {
		switch (type) {
		case Disk2D:
			return PI*sqr(L/2.);
		case Torus2D:
			return L*L;
		}
		throw new IllegalStateException();
	}
	
	Vec3 tempVec = new Vec3(0,0,0);
	
	public Vec3 boundaryDistance(Point p) {
		switch (type) {
		case Disk2D:
			double boundaryRadius = L/2.;
			double xc = p.x - L/2.;
			double yc = p.y - L/2.;
			double distanceFromCenter = sqrt(xc*xc + yc*yc);
			tempVec.x = xc*(boundaryRadius/distanceFromCenter - 1);
			tempVec.y = yc*(boundaryRadius/distanceFromCenter - 1);
			tempVec.z = 0;
			return tempVec;
		default:
			return null;
		}
	}

	public Vec3 displacement(Point p1, Point p2) {
		if (periodic()) {
			tempVec.x = periodicOffset(L, p2.x-p1.x);
			tempVec.y = periodicOffset(L, p2.y-p1.y);
			tempVec.z = 0;
		}
		else {
			tempVec.x = p2.x-p1.x;
			tempVec.y = p2.y-p1.y;
			tempVec.z = 0;
		}
		return tempVec;
	}
	
	
	public void layOutParticles(Random rand, Particle<?>[] particles) {
		int N = particles.length;
		int[] indices = Utilities.integerSequence(N);
		rand.randomizeArray(indices);
		
		switch (type) {
		case Disk2D:
			// this value of R is such that the minimum distance from a particle to the wall is half
			// the minimum interparticle distance
			double R = L / (2 + sqrt(PI/N));
			for (int i = 0; i < N; i++) {
				// these expressions for radius and angle are chosen to give a spiral trajectory
				// in which the radial distance between loops has a fixed length, and the "velocity"
				// is constant. here the particle number, i, plays the role of "time".
				// it is somewhat surprising that the angle a(t) is independent of the bounding radius R
				// and total particle number N.
				double r = R * sqrt((double)(i+1) / N);
				double a = 2 * sqrt(PI * (i+1));
				particles[indices[i]].x = L/2. + r*cos(a);
				particles[indices[i]].y = L/2. + r*sin(a);
			}
			break;
		case Torus2D:
			int rootN = (int)ceil(sqrt(N));
			double dx = L/rootN;
			for (int i = 0; i < N; i++) {
				particles[indices[i]].x = (i%rootN + 0.5 + 0.01*rand.nextGaussian()) * dx;
				particles[indices[i]].y = (i/rootN + 0.5 + 0.01*rand.nextGaussian()) * dx;
			}
			break;
		}
	}

	
	
	public void addGraphicsToCanvas(Canvas canvas, final Particle<?>[] particles) {
		final GLU glu = new GLU();
		final GLUquadric quadric = glu.gluNewQuadric();
		
		Graphics graphics = new Graphics() {
			public void draw(GL gl, Bounds bounds) {
				for (Particle<?> p : particles) {
					gl.glColor4fv(p.tag.color.getComponents(null), 0);
					gl.glPushMatrix();		
					gl.glTranslated(p.x, p.y, 0);
					glu.gluDisk(quadric, 0, p.tag.radius, 12, 1);
					gl.glPopMatrix();
				}
			}
			public Bounds getBounds() {
				return new Bounds(0, L, 0, L);
			}			
		};
		canvas.addGraphics(graphics);
		switch (type) {
		case Disk2D:
			canvas.addGraphics(new CircleGraphics(L/2., L/2., L/2.));
			break;
		case Torus2D:
			canvas.addGraphics(new RectangleGraphics(0., 0., L, L));
			break;
		}
	}
}
