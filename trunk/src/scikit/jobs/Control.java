package scikit.jobs;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.*;
import java.awt.event.*;
import scikit.params.GuiValue;


public class Control {
	private JPanel _panel;
	private Job _job;
	private JButton _startStopButton;
	private JButton _stepButton;	
	private JButton _resetButton;	
	
	public Control(Simulation sim) {
		_panel = new JPanel();
		_job = new Job(sim);
		
		JComponent paramPane = createParameterPane();
		JPanel buttonPanel = createButtonPanel();
		
		_panel.setLayout(new BorderLayout());
		_panel.add(paramPane, BorderLayout.CENTER);
		_panel.add(buttonPanel, BorderLayout.SOUTH);
	}
	
	public Control(Simulation sim, String title) {
		this(sim);
		JFrame frame = scikit.util.Utilities.frame(_panel, title);
		frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
	}
	
	/**
	 * Returns the Job corresponding to this control
	 * @return the corresponding job
	 */
	public Job getJob() {
		return _job;
	}
	
	private ActionListener _actionListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String str = e.getActionCommand();
			if (str.equals("Start")) {
				_job.start();
				_job.sim().params.setLocked(true);
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
				_job.sim().params.setLocked(true);
				_resetButton.setText("Reset");
			}
			if (str.equals("Reset")) {
				_job.kill();
				_job.sim().params.setLocked(false);				
				_startStopButton.setText("Start");
				_resetButton.setText("Defaults");
				_stepButton.setEnabled(true);
			}
			if (str.equals("Defaults")) {
				_job.sim().params.setDefaults();
			}
		}
	};
	
	
	private JPanel createButtonPanel() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setBorder(BorderFactory.createRaisedBevelBorder());
		buttonPanel.setLayout(new FlowLayout());
		
		JButton b1, b2, b3;
		b1 = new JButton("Start");
		b2 = new JButton("Step");
		b3 = new JButton("Defaults");
		b1.addActionListener(_actionListener);
		b2.addActionListener(_actionListener);
		b3.addActionListener(_actionListener);
		buttonPanel.add(b1);
		buttonPanel.add(b2);
		buttonPanel.add(b3);
		_startStopButton = b1;
		_stepButton = b2;
		_resetButton = b3;
		
		for (final String s : _job.sim().flags) {
			JButton b = new JButton(s);
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					_job.sim().flags.add(s);
					_job.wake();
				}
			});
			buttonPanel.add(b);
		}
		_job.sim().flags.clear();
		
		return buttonPanel;
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
		for (final String k : _job.sim().params.keys()) {
			JLabel label = new JLabel(k + ":", SwingConstants.RIGHT);
			c.gridx = 0;
			c.weightx = 0;
			c.gridwidth = 1;
			grid.setConstraints(label, c);
			panel.add(label);
			
			// add primary editor for parameter
			GuiValue v = _job.sim().params.getValue(k);
			JComponent field = v.createEditor();
			c.gridx = 1;
			c.weightx = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;
			grid.setConstraints(field, c);
			panel.add(field);
			
			// possible add auxiliary editor
			JComponent slider = v.createAuxiliaryEditor();
			if (slider != null) {
				c.gridy++;
				grid.setConstraints(slider, c);
				panel.add(slider);
			}
			
			// wake job when parameter value has changed
			v.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent arg0) {
					_job.wake();
				}
			});
			
			c.gridy++;
		}
		
		panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));	
		panel.setBackground(new Color(0.9f, 0.9f, 0.9f));
		JScrollPane scrollPane = new JScrollPane(panel);
		scrollPane.setBackground(Color.GRAY);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
		return scrollPane;		
	}
}

