package scikit.plot;


public class Histogram extends Plot {
	private double _defaultBinWidth;
	
	public Histogram(String title, double defaultBinWidth, boolean frame) {
		super(title, frame);
		_defaultBinWidth = defaultBinWidth;
	}
	
	public void accum(int i, double x) {
		getAccumulator(i).accum(x, 1);
	}
	
	public void accum(int i, double x, double v) {
		getAccumulator(i).accum(x, v);
	}
	
	public void setBinWidth(int i, double binWidth) {
		if (binWidth != getBinWidth(i)) {
			_defaultBinWidth = binWidth;	
			Accumulator acc = getAccumulator(i);
			acc.setBinWidth(binWidth);
			// invalidateView();
			resetViewWindow();
		}
	}
	
	private double getBinWidth(int i) {
		if (_dataSets[i] != null && _dataSets[i] instanceof Accumulator)
			return ((Accumulator)_dataSets[i]).getBinWidth();
		else
			return _defaultBinWidth;
	}
	
	private Accumulator getAccumulator(int i) {
		if (_dataSets[i] == null) {
			_dataSets[i] = new Accumulator(_defaultBinWidth);
			setStyle(i, Style.BARS);
		}
		if (_dataSets[i] instanceof Accumulator) {
			return (Accumulator)_dataSets[i];
		}
		else {
			throw new IllegalArgumentException("Object at index " + i + " is not an Accumulator.");
		}
	}
}
