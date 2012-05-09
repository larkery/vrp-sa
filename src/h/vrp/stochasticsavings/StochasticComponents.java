package h.vrp.stochasticsavings;

import h.vrp.model.Instance;
import h.vrp.solcons.SavingsConstructor.IMoveSelector;
import h.vrp.solcons.SavingsConstructor.ISavingsCalculator;
import h.vrp.solcons.SavingsConstructor.SavingsMove;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Random;

public class StochasticComponents {
	/**
	 * Return a move selector which randomly moves through the savings options with probability p of choosing the current
	 * element at any time (this is the reverse of my prior option; more p = more randomness)
	 * @param random
	 * @param p
	 * @return
	 */
	public static IMoveSelector getBernoulliSelector(final Random random, final double p) {
		return new IMoveSelector() {
			ArrayList<SavingsMove> buffer = new ArrayList<SavingsMove>();
			@Override
			public SavingsMove selectMove(PriorityQueue<SavingsMove> currentQueue) {
				SavingsMove m = null;
				
				while (true) {
					m = currentQueue.poll();
					if (m == null) {
						if (buffer.isEmpty()) return m;
						else {
							m = buffer.get(buffer.size() - 1);
							currentQueue.addAll(buffer.subList(0, buffer.size()-1));
							break;
						}
					}
					if (!m.verify()) continue;
					if (random.nextDouble() < p) {
						buffer.add(m);
					} else {
						currentQueue.addAll(buffer);
						break;
					}
				}
				
				buffer.clear();
				
				return m;
			}
		};
	}
	
	/**
	 * Get a move selector which randomly chooses among the first k values at the front of the savings list
	 * @param random
	 * @param k
	 * @return
	 */
	public static IMoveSelector getWindowSelector(final Random random, final int window) {
		return new IMoveSelector() {
			SavingsMove[] buffer = new SavingsMove[window];
			@Override
			public SavingsMove selectMove(PriorityQueue<SavingsMove> currentQueue) {
				int pick = random.nextInt(Math.min(currentQueue.size(), window));
				for (int i = 0; i<pick; i++) {
					buffer[i] = currentQueue.poll();
				}
				
				SavingsMove m = currentQueue.poll();
				
				for (int i = 0; i<pick; i++) {
					currentQueue.add(buffer[i]);
				}
				
				return m;
			}
		};
	}
	
	/**
	 * Get a savings calculator which uniformly randomly adds +-p proportion to the true savings value calculated by base
	 * @param base
	 * @param random
	 * @param p
	 * @return
	 */
	public static ISavingsCalculator getUniformlyPerturbingCalculator(final ISavingsCalculator base, final Random random, final double p) {
		return new ISavingsCalculator() {
			final double q = 2*p;
			@Override
			public float calculateSavings(Instance instance, SavingsMove move) {
				return (float) (base.calculateSavings(instance, move) *
					  	(1 + (random.nextDouble() - 0.5) * q));
			}
		};
	}
	
	/**
	 * Get a savings calculator which almost-guassianly multiplies the given thingy by whatsit. blah.
	 * @param base
	 * @param random
	 * @param p
	 * @return
	 */
	public static ISavingsCalculator getGaussianPerturbingCalculator(final ISavingsCalculator base, final Random random, final double p) {
		return new ISavingsCalculator() {
			@Override
			public float calculateSavings(Instance instance, SavingsMove move) {
				final double d = 1 + Math.min(-1, random.nextGaussian() * p);
				//d has mean 0 and sd p, but is never less than zero
				return (float) (base.calculateSavings(instance, move) * d);
			}
		};
	}
}
