package scikit.jobs;

import scikit.plot.Display;

import static java.lang.Math.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;


public class Control extends JPanel {
	private JFrame _frame;
	private Job _job;
	private JButton _startStopButton;
	private JButton _stepButton;	
	private JButton _newResetButton;
	private String[] _keys;
	private String[] _defaults;
	
	
	public Control(Job job) {
		_job = job;
		_keys = job.params.keys();
		_defaults = job.params.values();
		
		JComponent paramPane = createParameterPane();
		
		JPanel buttonPanel = new JPanel();
		JButton b1, b2, b3, b4;
		b1 = new JButton("Start");
		b2 = new JButton("Step");
		b3 = new JButton("New");
		b1.addActionListener(_actionListener);
		b2.addActionListener(_actionListener);
		b3.addActionListener(_actionListener);
		buttonPanel.add(b1);
		buttonPanel.add(b2);
		buttonPanel.add(b3);
		
		setLayout(new BorderLayout());
		add(paramPane, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);
		
		_startStopButton = b1;
		_stepButton = b2;
		_newResetButton = b3;
	}
	
	
	public Control(Job job, String title) {
		this(job);
		_frame = new JFrame();
		_frame.getContentPane().add(this);
		_frame.setSize(300, 300);
		_frame.setTitle(title);
		_frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		_frame.setVisible(true);
	}
	
	
	private ActionListener _actionListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String str = e.getActionCommand();
			if (str.equals("Start")) {
				_job.start();
				_startStopButton.setLabel("Stop");
				_newResetButton.setLabel("New");
				_stepButton.setEnabled(false);
			}
			if (str.equals("Stop")) {
				_job.stop();
				_startStopButton.setLabel("Start");
				_stepButton.setEnabled(true);
			}
			if (str.equals("Step")) {
				_newResetButton.setLabel("New");
				_job.step();
			}
			if (str.equals("New")) {
				_job.kill();
				_startStopButton.setLabel("Start");
				_newResetButton.setLabel("Reset");
				_stepButton.setEnabled(true);
			}
			if (str.equals("Reset")) {
				for (int i = 0; i < _keys.length; i++)
					_job.params.set(_keys[i], _defaults[i]);
			}
		}
	};
	
	
	private JComponent createParameterPane () {
		GridBagLayout grid = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		JPanel panel = new JPanel(grid);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.ipadx = 0;
		c.insets = new Insets(2, 2, 2, 2);
  		c.anchor = GridBagConstraints.NORTH;
		
		for (final String k : _job.params.keys()) {
			JLabel label = new JLabel(k + ":", SwingConstants.RIGHT);
			c.weightx = 0;
			c.gridwidth = 1;
			grid.setConstraints(label, c);
			panel.add(label);
			
			JComponent field = _job.params.get(k).createEditor();
			c.weightx = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;
			grid.setConstraints(field, c);
			panel.add(field);
		}
		
		return new JScrollPane(panel);
	}
}



