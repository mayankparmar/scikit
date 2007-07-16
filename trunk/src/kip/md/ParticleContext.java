package kip.md;

import static java.lang.Math.PI;
import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static kip.util.MathPlus.sqr;
import static scikit.util.Utilities.periodicOffset;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import kip.util.Random;
import kip.util.Vec3;
import scikit.graphics.Graphics;
import scikit.graphics.CircleGraphics;
import scikit.graphics.Drawable;
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
	
	
	public void layOutParticles(Random rand, Particle[] particles) {
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
	
	
	public static void dumpParticles(String filename, Particle[] particles) {
	  	try {
	  		DataOutputStream dos = new DataOutputStream(new FileOutputStream(filename));
	  		
	  		// write context
	  		ParticleContext pc = particles[0].tag.pc;
	  		dos.writeDouble(pc.L);
	  		dos.writeBoolean(pc.type == Type.Torus2D);
	  		
	  		// write tags
	  		Set<ParticleTag> tags = new HashSet<ParticleTag>();
	  		for (Particle p : particles)
	  			tags.add(p.tag);
	  		dos.writeInt(tags.size());
	  		for (ParticleTag tag : tags) {
	  			dos.writeInt(tag.id);
	  			dos.writeDouble(tag.mass);
	  			dos.writeDouble(tag.radius);
	  			dos.writeInt(tag.color.getRed());
	  			dos.writeInt(tag.color.getGreen());
	  			dos.writeInt(tag.color.getBlue());
	  		}
	  		
	  		// write particles
	  		dos.writeInt(particles.length);
	  		for (Particle p : particles) {
	  			dos.writeInt(p.tag.id);
	  			dos.writeDouble(p.x);
	  			dos.writeDouble(p.y);
	  			dos.writeDouble(p.z);
	  		}
	  		dos.close();
	  	}
    	catch (IOException e) {}
	}
	
	public static Particle[] readParticles(String filename) {
		Particle[] ret = null;
		try {
		    DataInputStream dis = new DataInputStream(new FileInputStream(filename));
		    
		    // read context
		    double L = dis.readDouble();
		    Type type = dis.readBoolean() ? Type.Torus2D : Type.Disk2D;
		    ParticleContext pc = new ParticleContext(L, type);
		    
		    // read tags
		    Map<Integer, ParticleTag> tags = new TreeMap<Integer, ParticleTag>();
		    int ntags = dis.readInt();
		    for (int i = 0; i < ntags; i++) {
		    	ParticleTag tag = new ParticleTag(dis.readInt());
		    	tag.pc = pc;
		    	tag.mass = dis.readDouble();
		    	tag.radius = dis.readDouble();
		    	tag.color = new Color(dis.readInt(), dis.readInt(), dis.readInt());
		    	tags.put(tag.id, tag);
		    }
		    
		    // read particles
			ret = new Particle[dis.readInt()];
			for (int i = 0; i < ret.length; i++) {
				ret[i] = new Particle();
			    ret[i].tag = tags.get(dis.readInt());
			    ret[i].x = dis.readDouble();
			    ret[i].y = dis.readDouble();
			    ret[i].z = dis.readDouble();
			}
		    dis.close();
		}
		catch (IOException e) {
			System.err.println ("Unable to read from '" + filename + "'");
		}
		return ret;
	}
	
	
	public Drawable[] getDrawables(final Particle[] particles) {
		ArrayList<Drawable> drawables = new ArrayList<Drawable>();
		
		drawables.add(new Drawable() {
			public void draw(Graphics g) {
				for (Particle p : particles) {
					g.setColor(p.tag.color);
					g.fillCircle(p.x, p.y, p.tag.radius);
				}
			}
			public Bounds getBounds() {
				return new Bounds(0, L, 0, L);
			}			
		});
		switch (type) {
		case Disk2D:
			drawables.add(new CircleGraphics(L/2., L/2., L/2.));
			break;
		case Torus2D:
			drawables.add(new RectangleGraphics(0., 0., L, L));
			break;
		}
		return drawables.toArray(new Drawable[0]);
	}
}
