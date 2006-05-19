package scikit.jobs;

import static java.lang.Math.*;
import java.text.DecimalFormat;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;


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
	
	public String sget() {
		return _v;
	}
	
	public int iget() {
		throw new IllegalArgumentException();
	}
	
	public double fget() {
		throw new IllegalArgumentException();	
	}
	
	public void set(String v) {
		if (!testValidity(v))
			throw new IllegalArgumentException();
		
		if (!_v.equals(v)) {
			_v = v;
			for (ChangeListener l : _listeners)
				l.stateChanged(null);
		}
	}
	
	public void set(int v) {
		set(""+v);
	}
	
	public void set(double v) {
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
		final JLabel label = new JLabel(sget(), SwingConstants.RIGHT);
		addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				label.setText(sget());
			}
		});
		Dimension d = label.getPreferredSize();
		d.width = Math.max(d.width, 80);
		label.setPreferredSize(d);
		return label;
	}
	
	public JComponent createEditor() {
		final JTextField field = new JTextField(sget());
		
		ActionListener action = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				set(field.getText());
				field.setText(_v);
				field.setBackground(Color.WHITE);				
			}
		};
		FocusListener focus = new FocusListener() {
			public void focusGained(FocusEvent e)  {}
			public void focusLost(FocusEvent e) {
				set(field.getText());
				field.setText(_v);
				field.setBackground(Color.WHITE);
			}
		};
		DocumentListener input = new DocumentListener() {
			private void textInput() {
				if (field.hasFocus()) {
					field.setBackground(testValidity(field.getText()) ? lightGreen : lightRed);
				}
			}
			public void changedUpdate(DocumentEvent e)  {}
			public void insertUpdate(DocumentEvent e)  { textInput(); }
			public void removeUpdate(DocumentEvent e) { textInput(); }
		};
		field.addActionListener(action);
		field.addFocusListener(focus);
		field.getDocument().addDocumentListener(input);
		field.setHorizontalAlignment(JTextField.RIGHT);
		
		ChangeListener change = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!field.hasFocus())
					field.setText(sget());
				field.setEnabled(!_locked);
			}
		};
		addChangeListener(change);
		
		Dimension d = field.getPreferredSize();
		d.width = Math.max(d.width, 80);
		field.setPreferredSize(d);
		
		return field;
	}
	
	public JComponent createAuxiliaryEditor() {
		return null;
	}
}
