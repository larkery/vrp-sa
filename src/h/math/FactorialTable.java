package h.math;

import java.math.BigInteger;

public class FactorialTable {
	static CacheTable<Integer, BigInteger> cache = new CacheTable<Integer, BigInteger>();
	public static BigInteger get(int n) {
		if (n == 0) return BigInteger.ONE;
		if (n == 1) return BigInteger.ONE;
		
		BigInteger r = cache.get(n);
		if (r == null) {
			r = get(n-1).multiply(BigInteger.valueOf(n));
			cache.store(n, r);
		}
		return r;
	}
}
