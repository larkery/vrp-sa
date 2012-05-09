package h.math;

import java.math.BigInteger;

public class StirlingTable {
	static CacheTable<Integer, StirlingTable> cache = new CacheTable<Integer, StirlingTable>();
	
	BigInteger[] values;

	private int n;
	
	public StirlingTable(int n) {
		values = new BigInteger[n-1];
		this.n = n;
	}

	public static StirlingTable getTable(int n) {
		StirlingTable t = cache.get(n);
		if (t == null) {
			t = new StirlingTable(n);
			cache.store(n, t);
		}
		return t;
	}

	public BigInteger get(int k) {
		if (k == 0) return BigInteger.ZERO;
		if (k == n) return BigInteger.ONE;
		if (values[k-1] == null) {
			StirlingTable Snmo = getTable(n-1);
			values[k-1] = 
				Snmo.get(k-1).add(BigInteger.valueOf(k).multiply(Snmo.get(k)));
		}
		return values[k-1];
	}
}
