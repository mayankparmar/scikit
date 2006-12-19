package scikit.jobs;

import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;


abstract public class GuiValue {
	private String _v, _default;
	
	abstract public boolean testValidity(String v);
	abstract public Object valueForString(String v);
	
	
	public GuiValue(Object v) {
		_v = _default = v.toString();
	}
	
	public Object get() {
		return valueForString(getStringRep());
	}
	
	public Object getDefault() {
		return valueForString(_default);
	}
	
	public void set(Object v) {
		setStringRep(v.toString());
	}
	
	
	// be careful with threads in this class because the GUI thread may try 
	// to change a value at the same time the simulation thread is reading a
	// value
	synchronized protected void setStringRep(String v) {
		if (!_v.equals(v) && testValidity(v)) {
			_v = v;
			notifyListeners();
		}
	}
	
	synchronized protected String getStringRep() {
		return _v;
	}
	
	
	// ----------------------------------------------- Listeners ---------------------------------------------

	private Vector<ChangeListener> _listeners = new Vector<ChangeListener>();

	public void addChangeListener(ChangeListener listener) {
		_listeners.add(listener);
	}
	
	protected void notifyListeners() {
		for (ChangeListener l : _listeners) {
			l.stateChanged(null);
		}	
	}
	
	
	// ----------------------------------------------- Locks ---------------------------------------------
	
	protected boolean _locked = false, _lockable = true;

	public void setLocked(boolean locked) {
		_locked = _lockable && locked;
		notifyListeners();
	}
	
	public void setLockable(boolean lockable) {
		_lockable = lockable;
	}
	
	
	// ----------------------------------------------- GUI ---------------------------------------------
	
	final private Color lightGreen = new Color(0.85f, 1f, 0.7f);
	final private Color lightRed   = new Color(1f, 0.7f, 0.7f);
	protected boolean _auxiliaryEditor = false;
	
	
	public void enableAuxiliaryEditor() {
		_auxiliaryEditor = true;
	}
	
	public JComponent createAuxiliaryEditor() {
		return null;
	}

	public JComponent createEditor() {
		final JTextField field = new JTextField(getStringRep());
		
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
			public void stateChanged(ChangeEvent e) {
				String s = getStringRep();
				if (!field.getText().equals(s))
					field.setText(s);
				field.setEnabled(!_locked);
			}
		});
		
		Dimension d = field.getPreferredSize();
		d.width = Math.max(d.width, 80);
		field.setPreferredSize(d);
		field.setHorizontalAlignment(JTextField.RIGHT);
		return field;
	}
	
	
	// called when the user has entered a final string value
	private void fieldTextEvaluated(JTextField field) {
		setStringRep(field.getText());
		field.setText(getStringRep());
		field.setBackground(Color.WHITE);	
	}
	
	// called while the user is inputting the string value
	private void fieldTextInput(JTextField field) {
		if (field.getText().equals(getStringRep()))
			field.setBackground(Color.WHITE);
		else
			field.setBackground(testValidity(field.getText()) ? lightGreen : lightRed);
	}
}
