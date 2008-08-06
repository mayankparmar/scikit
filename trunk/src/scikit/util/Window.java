package scikit.util;

import java.awt.Component;
import java.awt.image.BufferedImage;

public interface Window {
	public String getTitle();
	public Component getComponent();
	public BufferedImage getImage(int width, int height);
	public void clear();
	public void animate();
}
