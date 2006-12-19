package scikit.jobs;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;


public class ReadonlyValue extends GuiValue {
	public ReadonlyValue() {
		super("-");
	}
	
	public boolean testValidity(String v) {
		return true;
	}
	
	public Object valueForString(String v) {
		return v;
	}
	
	public JComponent createEditor() {
		final JLabel label = new JLabel(getStringRep(), SwingConstants.RIGHT);
		addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) { label.setText(getStringRep()); }
		});
		Dimension d = label.getPreferredSize();
		d.width = Math.max(d.width, 80);
		label.setPreferredSize(d);
		return label;
	}
}
