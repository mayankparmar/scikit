package scikit.jobs;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;



public class ChoiceValue extends GuiValue {
	private String[] _choices;
	
	public ChoiceValue(String... choices) {
		super(choices[0]);
		_choices = choices;
	}
	
	public boolean testValidity(String v) {
		return itemIndex(v) >= 0;
	}
	
	public JComponent createEditor() {
		final JComboBox choice = new JComboBox(_choices);
		
		choice.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				set((String)choice.getSelectedItem());
			}
		});
		
		addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!choice.hasFocus())
					choice.setSelectedIndex(itemIndex(get()));
				choice.setEnabled(!_locked);
			}
		});
		
		return choice;
	}
	
	private int itemIndex(String v) {
		for (int i = 0; i < _choices.length; i++)
			if (v.equals(_choices[i]))
				return i;
		return -1;
	}
}