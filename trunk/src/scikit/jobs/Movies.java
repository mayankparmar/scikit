package scikit.jobs;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import scikit.jobs.params.ChoiceValue;
import scikit.jobs.params.DirectoryValue;
import scikit.jobs.params.DoubleValue;
import scikit.jobs.params.IntValue;
import scikit.util.Utilities;
import scikit.util.Window;

public class Movies {
	JTabbedPane tabbedPane; 
	JFrame frame;
	Control control;
	int count = 0;
	
	public Movies(Control c) {
		this.control = c;
        JComponent component = makeComponent();
        frame = Utilities.frame(component, "Movies");
	}
	
	public JFrame getFrame() {
		return frame;
	}
	
	public void removeAllMovies() {
		count = 0;
		tabbedPane.removeAll();
		addNewMovie();
	}
	
	public void saveImages() {
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			MovieConfig mc = (MovieConfig)tabbedPane.getComponentAt(i);
			mc.saveImage();
		}
	}
	
	private void addNewMovie() {
		tabbedPane.addTab("Movie "+(count++), new MovieConfig());		
	}
	
	private String[] getWindowTitles() {
		Window[] windows = control.getWindows();
		List<String> titles = new ArrayList<String>();
		titles.add(" - ");
		for (Window w : windows)
			titles.add(w.getTitle());
		return titles.toArray(new String[0]);
	}
	
	private Window getWindowFromTitle(String title) {
		Window[] windows = control.getWindows();
		for (Window w : windows)
			if (w.getTitle().equals(title))
				return w;
		return null;
	}
	
	private JComponent makeComponent() {
        tabbedPane = new JTabbedPane();        
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
     	tabbedPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
     	addNewMovie();
     	
    	JButton addButton = new JButton("Add Movie");
    	addButton.addActionListener(new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    			addNewMovie();
    		}
	    });
    	addButton.setEnabled(true);
    	
    	JButton removeButton = new JButton("Remove Movie");
    	removeButton.addActionListener(new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    			tabbedPane.remove(tabbedPane.getSelectedIndex());
    		}
	    });
    	removeButton.setEnabled(true);
    	
    	JPanel buttonPane = new JPanel();
     	buttonPane.add(addButton);
    	buttonPane.add(removeButton);
    	buttonPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(true);
        panel.add(tabbedPane, BorderLayout.CENTER);
        panel.add(buttonPane, BorderLayout.PAGE_END);
        return panel; 
	}
	
	
	private class MovieConfig extends JPanel {
		private static final long serialVersionUID = 1L;
		DecimalFormat fmt = new DecimalFormat("0000");
		
		int saveCount = 0;
		double lastSaveTime = Double.NEGATIVE_INFINITY;
		
		ChoiceValue window;
		DirectoryValue directory;
		IntValue width, height;
		DoubleValue startTime, endTime, delayTime;
		
		public MovieConfig() {
			super();
			
			GridLayout gl = new GridLayout(7, 2);
			gl.setHgap(4);
			gl.setVgap(4);
			setLayout(gl);
			
			window = new ChoiceValue(getWindowTitles());
			directory = new DirectoryValue();
			width = new IntValue(300);
			height = new IntValue(300);
			startTime = new DoubleValue(0);
			endTime = new DoubleValue(0);
			delayTime = new DoubleValue(0);
			
			add(new JLabel("Window to capture:"));
			add(window.getEditor());
			add(new JLabel("Output directory:"));
			add(directory.getEditor());
			add(new JLabel("Image width (pixels):"));
			add(width.getEditor());
			add(new JLabel("Image height (pixels):"));
			add(height.getEditor());
			add(new JLabel("Start time:"));
			add(startTime.getEditor());
			add(new JLabel("End time:"));
			add(endTime.getEditor());
			add(new JLabel("Capture delay time:"));
			add(delayTime.getEditor());
		}
		
		public void saveImage() {
			double time = control.getJob().sim().getTime();
			if (isValid(time)) {
				int w = width.getInt();
				int h = height.getInt();
				Window win = getWindowFromTitle(window.getValue());
				String dir = directory.getValue();
				File file = new File(dir+File.separator+fmt.format(saveCount)+".png");
				try {
					 ImageIO.write(win.getImage(w, h), "png", file);
				} catch (IOException e) {
				}
				lastSaveTime = time;
				saveCount += 1;
			}
		}

		private boolean isValid(double time) {
			return
				getWindowFromTitle(window.getValue()) != null &&
				new File(directory.getValue()).isDirectory() &&
				width.getInt() > 0 &&
				height.getInt() > 0 &&
				Double.isNaN(time) ||
				(time >= startTime.getDouble() &&
				time < endTime.getDouble() &&
				time - lastSaveTime > delayTime.getDouble());
		}
	}
}
