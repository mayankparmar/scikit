package kip.md2;

import java.awt.Color;

import scikit.util.Point;

public abstract class ParticleTag {
	public double mass, radius;
	public Color color;
	
	abstract public double interactionRange();	
	abstract public double[] displacement(Point p1, Point p2);
	abstract public double[] boundaryDistance(Point p);
}
