package kip.md2;

import java.awt.Color;

public class ParticleTag {
	public double mass, radius, charge;
	public double interactionRange;
	public Color color;
	public MolecularDynamics2D<?> md;
	
	public void initialize(MolecularDynamics2D<?> md) {
		this.md = md;
	}
}
