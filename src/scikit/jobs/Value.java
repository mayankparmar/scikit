package scikit.jobs;

import static java.lang.Math.*;
import java.text.DecimalFormat;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;


// it is necessary to be careful with threads in this class because the GUI thread 
// may try to change a value at the same time the simulation thread is reading a
// value.  therefore all reads and writes to values must be synchronized.

public class Value {
	private Color lightGreen = new Color(0.85f, 1f, 0.7f);
	private Color lightRed   = new Color(1f, 0.7f, 0.7f);
	private Vector<ChangeListener> _listeners = new Vector<ChangeListener>();
	private boolean _lockable = false;
	
	protected boolean _locked = false;
	protected boolean _auxiliaryEditor = false;
	protected String _v;
	protected String _default;
	
	static DecimalFormat df1 = new DecimalFormat("0.######");
	static DecimalFormat df2 = new DecimalFormat("0.######E0");
	static String format(double x) {
		return (abs(x) > 0.001 && abs(x) < 1000 || x == 0 ? df1 : df2).format(x);
	}
	
	
	public Value(String v, boolean lockable) {
		_v = _default = v;
		_lockable = lockable;
	}
	
	public String getDefault() {
		return _default;
	}
	
	synchronized public String sget() {
		return _v;
	}
	
	synchronized public int iget() {
		throw new IllegalArgumentException();
	}
	
	synchronized public double fget() {
		throw new IllegalArgumentException();	
	}
	
	synchronized public void set(String v) {
		if (!_v.equals(v)) {
			if (!testValidity(v))
				throw new IllegalArgumentException();
			_v = v;
			for (ChangeListener l : _listeners) {
				l.stateChanged(null);
			}
		}
	}
	
	synchronized public void set(int v) {
		set(""+v);
	}
	
	synchronized public void set(double v) {
		set(format(v));
	}
	
	
	public void setLocked(boolean locked) {
		_locked = _lockable && locked;
		for (ChangeListener l : _listeners)
			l.stateChanged(null);
	}
	
	public boolean testValidity(String v) {
		return true;
	}
	
	public void addChangeListener(ChangeListener listener) {
		_listeners.add(listener);
	}
	
	
	public void enableAuxiliaryEditor() {
		_auxiliaryEditor = true;
	}
	
	public JComponent createView() {
		final JLabel label = new JLabel(_v, SwingConstants.RIGHT);
		addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				label.setText(_v);
			}
		});
		Dimension d = label.getPreferredSize();
		d.width = Math.max(d.width, 80);
		label.setPreferredSize(d);
		return label;
	}
	
	public JComponent createAuxiliaryEditor() {
		return null;
	}

	public JComponent createEditor() {
		final JTextField field = new JTextField(_v);
		
		field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { fieldTextEvaluated(field); }
		});
		
		field.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e)  {}
			public void focusLost(FocusEvent e) { fieldTextEvaluated(field); }
		});
		
		field.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e)  {}
			public void insertUpdate(DocumentEvent e)  { fieldTextInput(field); }
			public void removeUpdate(DocumentEvent e) { fieldTextInput(field); }
		});
		
		addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) { fieldStateChanged(field); }
		});
		
		Dimension d = field.getPreferredSize();
		d.width = Math.max(d.width, 80);
		field.setPreferredSize(d);
		field.setHorizontalAlignment(JTextField.RIGHT);
		return field;
	}
	
	synchronized private void fieldTextEvaluated(JTextField field) {
		if (testValidity(field.getText()))
			set(field.getText());
		field.setText(_v);
		field.setBackground(Color.WHITE);	
	}
	
	synchronized private void fieldTextInput(JTextField field) {
		if (field.getText().equals(_v))
			field.setBackground(Color.WHITE);
		else
			field.setBackground(testValidity(field.getText()) ? lightGreen : lightRed);
	}
	
	synchronized private void fieldStateChanged(JTextField field) {
		if (!field.getText().equals(_v))
			field.setText(_v);
		field.setEnabled(!_locked);	
	}
}
