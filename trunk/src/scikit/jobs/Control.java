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
	private Parameters _defaultParams;
	private Vector<JTextField> _fields = new Vector<JTextField>();
	private boolean _ignoreTextChanges = false;
	
	
	public Control(Job job) {
		_job = job;
		_defaultParams = (Parameters)job.params.clone();
		
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
		
		_job.addDisplay(new Display() {
			public void animate() { setTextFields(); }
			public void clear() {}
		});
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
				_job.params = (Parameters)_defaultParams.clone();
				setTextFields();
			}
		}
	};
	
	
	private void setTextFields() {
		_ignoreTextChanges = true;
		String[] keys = _job.params.keys();
		int i = 0;
		for (JTextField field : _fields) {
			String v = _job.params.sget(keys[i++]);
			if (field.getBackground() == Color.WHITE &&  !field.getText().equals(v))
				field.setText(v);
		}
		_ignoreTextChanges = false;
	}
	
	
	private JTextField createParameterTextField(final String k) {
		final Color lightGreen = new Color(0.85f, 1f, 0.7f);
		final Color lightRed   = new Color(1f, 0.7f, 0.7f);
		final JTextField field = new JTextField(_job.params.sget(k));
		
		ActionListener action = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (_job.params.isValidValue(k, field.getText()))
					_job.params.set(k, field.getText());
				else
					field.setText(_job.params.sget(k));
				field.setBackground(Color.WHITE);
			}
		};
		FocusListener focus = new FocusListener() {
			public void focusGained(FocusEvent e)  {}
			public void focusLost(FocusEvent e) {
				if (_job.params.isValidValue(k, field.getText()))
					_job.params.set(k, field.getText());
				else
					field.setText(_job.params.sget(k));
				field.setBackground(Color.WHITE);
			}
		};
		DocumentListener input = new DocumentListener() {
			public void changedUpdate(DocumentEvent e)  {}
			public void insertUpdate(DocumentEvent e)  {
				if (!_ignoreTextChanges) {
					field.setBackground(
						_job.params.isValidValue(k, field.getText()) ? lightGreen : lightRed
					);
				}
			}
			public void removeUpdate(DocumentEvent e) {
				if (!_ignoreTextChanges) {
					field.setBackground(
						_job.params.isValidValue(k, field.getText()) ? lightGreen : lightRed
					);
				}
			}
		};
		
		field.addActionListener(action);
		field.addFocusListener(focus);
		field.getDocument().addDocumentListener(input);
		field.setHorizontalAlignment(JTextField.RIGHT);
		return field;
	}
	
	
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
			
			JTextField field = createParameterTextField(k);
			c.weightx = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;
			grid.setConstraints(field, c);
			panel.add(field);
			_fields.add(field);
		}
		
		return new JScrollPane(panel);
	}
}



