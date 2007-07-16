package scikit.graphics;

import java.awt.Canvas;
import java.awt.Color;

import scikit.util.Bounds;

public class GraphicsAWT implements Graphics {
	Scene scene;
	
	public GraphicsAWT(Scene scene) {
		this.scene = scene;
	}
	
	public Object engine() {
		return null;
	}

	public Scene scene() {
		return scene;
	}
	
	
	public static Canvas createCanvas(Scene scene) {
		return new Canvas();
	}

	public void projectOrtho2D(Bounds bds) {
	}

	public void setColor(Color color) {
	}
	
	public void drawPoint(double x, double y) {
	}
	
	public void drawLine(double x1, double y1, double x2, double y2) {
	}

	public void drawLines(double[] xys) {
	}

	public void drawRect(double x, double y, double w, double h) {
	}

	public void fillRect(double x, double y, double w, double h) {
	}
	
	public void drawCircle(double x, double y, double r) {
	}

	public void fillCircle(double x, double y, double r) {
	}

	public double stringWidth(String str) {
		return -1;
	}
	
	public double stringHeight(String str) {
		return -1;
	}

	public void drawString(String str, double x, double y) {
	}

}
