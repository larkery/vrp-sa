package h.vrp.stochasticsavings;


public class Histogram {
	int unders, overs;
	int[] bins;
	double min, max, binsize;
	private double width;
	
	public Histogram(double min, double max, double binsize) {
		this.min = min;
		this.max = max;
		this.binsize = binsize;
		this.bins = new int[(int) ((max - min) / binsize)];
		this.width = max - min;
		this.binsize = width / bins.length;
	}

	public void add(float quality) {
		if (quality < min) {
			unders++;
		} else if (quality > max) {
			overs++;
		} else {
			final int bin = (int) ((bins.length - 1) * (quality - min) / width);
			bins[bin]++;
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		double binStart = min;
		for (int i = 0; i<bins.length; i++) {
			if (bins[i] > 0) {
				if (sb.length() > 0) sb.append(", ");
				sb.append("(");
				sb.append(Math.round(binStart / binsize) * binsize);
				sb.append(", ");
				sb.append(Math.round((binStart + binsize) /binsize) * binsize );
				sb.append(", ");
				sb.append(bins[i]);
				sb.append(")");
			}
			binStart += binsize;
		}
		
		return "H(under=(" + min + ", " + unders + "), over=(" +max+ ", "+ overs+"), bins=[" + sb.toString() + "])";
		
	}
}
