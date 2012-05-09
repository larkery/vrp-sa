package h.vrp.search;

public final class EpochalGeometricTF implements ITemperatureFunction {
	private int epochs;
	private float alpha;
	private float T0;

	public EpochalGeometricTF(float T0, float Tmin, int epochs) {
		this.T0 = T0;
		epochs--;
		alpha = (float) Math.pow(Tmin / T0, 1.0/epochs);
		
		this.epochs = epochs;
	}
	
	@Override
	public float getTemperature(float progress) {
		int epoch = (int) Math.floor(epochs * progress);
		return (float) (T0* Math.pow(alpha, epoch));
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer( "EpochalGeometricTF [T0=" + T0 + ", alpha=" + alpha
				+ ", epochs=" + epochs + 
				" epoch values = [");
		for (int i = 0; i<epochs; i++) {
			sb.append(getTemperature(i / (float) epochs));
			sb.append(", ");
		}
		sb.append("]]");
		return sb.toString();
	}
}
