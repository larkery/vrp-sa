package h.math;

import h.util.random.HintonRandom;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

public class SolutionDistribution {
	protected double[] p;
	protected double[] pc;
	
	protected SolutionDistribution(int n) {
		BigInteger[] values = new BigInteger[n];
		p = new double[n];
		pc = new double[n];
		BigInteger nF = FactorialTable.get(n);
//		StirlingTable Sn = StirlingTable.getTable(n);
		BigInteger sum = FactorialTable.get(n);
		values[0] = sum;
		for (int k = 2; k<n; k++) {			
//			BigInteger value = nF.divide(FactorialTable.get(k)).multiply(Sn.get(k));
			
			BigInteger breaks = FactorialTable.get(n-1).divide(FactorialTable.get(k-1).multiply(FactorialTable.get(n-k)));
			BigInteger value = nF.divide(FactorialTable.get(k)).multiply(breaks);
			sum = sum.add(value);
			values[k-1] = value;
		}
		values[n-1] = BigInteger.ONE;
		sum = sum.add(BigInteger.ONE);
		BigDecimal decimalSum = new BigDecimal(sum, 25);
		for (int i = 0; i<n; i++) {
			p[i] = (new BigDecimal(values[i], 25)).divide(decimalSum, BigDecimal.ROUND_HALF_UP).doubleValue();
			pc[i] = ((i == 0) ? 0 : pc[i-1]) + p[i];
		}
	}

	static CacheTable<Integer, SolutionDistribution> cache = new CacheTable<Integer, SolutionDistribution>();
	
	public static SolutionDistribution getInstance(int size) {
		SolutionDistribution d = cache.get(size);
		if (d == null) {
			d = new SolutionDistribution(size);
			cache.store(size, d);
		}
		return d;
	}
	
	public static void main(String [] args) {
		SolutionDistribution d = getInstance(Integer.parseInt(args[0]));
		HintonRandom random = new HintonRandom();
		System.out.println(Arrays.toString(d.p));
		for (int i = 0; i<10; i++) {
			System.out.println(d.pick(random));
		}
	}
	
	public int pick(Random random) {
		double d = random.nextDouble();
		for (int i = 0; i<pc.length; i++) {
			if (pc[i] >= d) {
				return i+1;
			}
		}
		return pc.length;
	}
}
