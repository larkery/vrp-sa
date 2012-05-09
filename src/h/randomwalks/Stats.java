package h.randomwalks;

public class Stats {
	double sampleMean, varianceEstimator, sampleVariance;
	int sampleSize;
	double samples, sampleSquares;
	public Stats() {
		
	}
	
	public void addSample(double d) {
		samples += d;
		sampleSquares += d*d;
		sampleSize++;
	}
	
	public void update() {
		sampleMean = samples / sampleSize;
		sampleVariance = sampleSquares / sampleSize - Math.pow(sampleMean, 2);
		varianceEstimator = sampleSquares/(sampleSize - 1.0) - (sampleSize) * Math.pow(sampleMean,2) / (sampleSize - 1.0);
	}

	public double getSampleMean() {
		return sampleMean;
	}

	public double getVarianceEstimator() {
		return varianceEstimator;
	}
	
	public double getSEM() {
		return Math.sqrt(sampleVariance)/ Math.sqrt(sampleSize);
	}
	public String toString() {
		return "mean = " + sampleMean + ", var = " + varianceEstimator + ", s^2 = " + sampleVariance + ", mse = " + getSEM(); 
	}
}
