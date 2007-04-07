package scikit.params;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;


public class ReadonlyValue extends GuiValue {
	public ReadonlyValue() {
		super("-");
	}
	
	public boolean testValidity(String v) {
		return true;
	}
	
	public JComponent createEditor() {
		final JLabel label = new JLabel(get(), SwingConstants.RIGHT);
		addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) { label.setText(get()); }
		});
		Dimension d = label.getPreferredSize();
		d.width = Math.max(d.width, 80);
		label.setPreferredSize(d);
		return label;
	}
}
