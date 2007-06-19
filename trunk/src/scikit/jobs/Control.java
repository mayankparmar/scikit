package scikit.jobs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import scikit.params.GuiValue;


public class Control extends JPanel {
	private static final long serialVersionUID = 1L;
	private Job _job;
	private JPanel _buttonPanel;
	private JButton _startStopButton;
	private JButton _stepButton;	
	private JButton _resetButton;	
	
	public Control(Simulation sim) {
		_job = new Job(sim);
		
		JComponent paramPane = createParameterPane();
		createButtonPanel();
		
		setLayout(new BorderLayout());
		add(paramPane, BorderLayout.CENTER);
		add(_buttonPanel, BorderLayout.SOUTH);
	}
	
	public Control(Simulation sim, String title) {
		this(sim);
		JFrame frame = scikit.util.Utilities.frame(this, title);
		frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
	}
	
	public void addButton(String name, final String flag) {
		try {		
			final java.lang.reflect.Field field = _job.getClass().getField(flag);
			JButton b = new JButton(name);
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					try {
						field.setBoolean(_job, true);
					} catch (IllegalArgumentException e) {
						System.err.println("Flag '" + flag + "' is not of boolean type");
					} catch (IllegalAccessException e) {
						System.err.println("Insufficient privileges to write to flag '" + flag + "'");
					}
				}
			});
			_buttonPanel.add(b);
		} catch (NoSuchFieldException e) {
			System.err.println("Unable to access flag '" + flag + "' in object " + _job);
		} catch (SecurityException e) {
			System.err.println("Insufficient privileges to access flag '" + flag + "'");
		}
	}
	
	
	private ActionListener _actionListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String str = e.getActionCommand();
			if (str.equals("Start")) {
				_job.start();
				_job.params().setLocked(true);
				_startStopButton.setText("Stop");
				_resetButton.setText("Reset");
				_stepButton.setEnabled(false);
			}
			if (str.equals("Stop")) {
				_job.stop();
				_startStopButton.setText("Start");
				_stepButton.setEnabled(true);
			}
			if (str.equals("Step")) {
				_job.step();
				_job.params().setLocked(true);
				_resetButton.setText("Reset");
			}
			if (str.equals("Reset")) {
				_job.kill();
				_job.params().setLocked(false);				
				_startStopButton.setText("Start");
				_resetButton.setText("Defaults");
				_stepButton.setEnabled(true);
			}
			if (str.equals("Defaults")) {
				_job.params().setDefaults();
			}
		}
	};
	
	
	private void createButtonPanel() {
		_buttonPanel = new JPanel();
		JButton b1, b2, b3;
		b1 = new JButton("Start");
		b2 = new JButton("Step");
		b3 = new JButton("Defaults");
		b1.addActionListener(_actionListener);
		b2.addActionListener(_actionListener);
		b3.addActionListener(_actionListener);
		_buttonPanel.add(b1);
		_buttonPanel.add(b2);
		_buttonPanel.add(b3);
		_buttonPanel.setBorder(BorderFactory.createRaisedBevelBorder());
		
		// Extend size of "Reset" a little bit so that when it switches
		// to "Default", it won't have to expand
		Dimension d = b3.getPreferredSize();
		d.width = 110 * d.width / 100;
		b3.setPreferredSize(d);
		
		_startStopButton = b1;
		_stepButton = b2;
		_resetButton = b3;
	}
	
	
	private JComponent createParameterPane () {
		GridBagLayout grid = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		JPanel panel = new JPanel(grid);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.ipadx = 0;
  		c.anchor = GridBagConstraints.NORTH;
		c.gridy = 0;
		c.insets = new Insets(2, 2, 2, 2);
		
		// add parameters
		for (final String k : _job.params().keys()) {
			JLabel label = new JLabel(k + ":", SwingConstants.RIGHT);
			c.gridx = 0;
			c.weightx = 0;
			c.gridwidth = 1;
			grid.setConstraints(label, c);
			panel.add(label);
			
			GuiValue v = _job.params().getValue(k);
			JComponent field = v.createEditor();
			c.gridx = 1;
			c.weightx = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;
			grid.setConstraints(field, c);
			panel.add(field);
			
			JComponent slider = v.createAuxiliaryEditor();
			if (slider != null) {
				c.gridy++;
				grid.setConstraints(slider, c);
				panel.add(slider);
			}
			
			c.gridy++;
		}
		
		panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));	
		panel.setBackground(new Color(0.9f, 0.9f, 0.9f));
		JScrollPane scrollPane = new JScrollPane(panel);
		scrollPane.setBackground(Color.GRAY);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
/*
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
             BorderFactory.createEmptyBorder(10,10,10,10),
             BorderFactory.createBevelBorder(BevelBorder.RAISED)
		));
*/

		return scrollPane;		
	}
}

