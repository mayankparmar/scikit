package scikit.params;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class FileValue extends GuiValue {
	JFileChooser chooser = new JFileChooser();
	
	public FileValue() {
		this(null);
	}
	
	public FileValue(String v) {
		super(defaultDirectory(v));
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		chooser.setSelectedFile(new File(getValue()));
	}

	protected boolean testValidity(String v) {
		return (new File(v)).isDirectory() || (new File(v)).isFile();
	}
	
	protected JComponent createEditor() {
		final JButton b = new JButton(getValue());
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				chooser.showDialog(null, "Select");
				File dir = chooser.getSelectedFile();
				if (dir != null)
					setValue(dir.toString());
			}
		});
		addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				chooser.setCurrentDirectory(new File(getValue()));
				b.setText(getValue());
			}
		});
		b.setPreferredSize(new Dimension(250, b.getPreferredSize().height));
		return b;
	}
	
	private static String defaultDirectory(String v) {
		if (v != null && new File(v).isDirectory() || new File(v).isFile())
			return v;
		else
			return System.getProperty("user.home");
	}	
}
