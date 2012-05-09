package h.vrp.model;

import java.util.AbstractList;
import java.util.Arrays;

public class Route extends AbstractList<Integer> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7961939156223403476L;
	final public int index;
	
//	ArrayList<Integer> proxy = new ArrayList<Integer>();
	private float cost;
	
	private int[] contents;
	private int size;
	
	public Route(int i, int capacity) {
		index = i;
		size = 0;
		contents = new int[capacity];
	}

	public Route(int i) {
		this(i, 4);
	}

	public Integer get(int x) {
		return at(x);
	}
	
	final int fold(int x) {
		if (x >= 0) {
			if (x >= size) x-= size;
		} else {
			x = size+x;
		}
		return x;
	}
	
	public final int at(int x) {
		return contents[fold(x)];
	}
	
	public Integer set(int x, Integer element) {
		int y = contents[fold(x)];
		contents[fold(x)] = element;
		return y;
	}

	public void add(int index, Integer element) {
//		proxy.add(index, element);
		add(index, element.intValue());
	}
	
	public void add(int index, int element) {
		expand();
		shiftRight(index);
		contents[index] = element;
	}

	public void add(int x) {
		expand();
		contents[size] = x;
		size++;
	}
	
	private final void expand() {
		if (size == contents.length) {
			contents = Arrays.copyOf(contents, contents.length * 2);
		}
	}

	public boolean add(Integer e) {
//		return proxy.add(e);
		add(e.intValue());
		return true;
	}

	public void clear() {
//		proxy.clear();
		size = 0;
	}
	
	void shiftRight(int pos) {
		for (int i = size; i>pos; i--) {
			contents[i] = contents[i-1];
		}
		size++;
	}
	
	void shiftLeft(int pos) {
		for (int i = pos; i<size-1; i++) {
			contents[i] = contents[i+1];
		}
		size--;
	}
	
	@Override
	protected void removeRange(int fromIndex, int toIndex) {
		for (int i = 0; (i+toIndex)<size ; i++) {
			contents[fromIndex + i] = contents[toIndex + i];
		}
		size -= (toIndex - fromIndex);
	}

	public Integer remove(int index) {
//		System.err.println("remove contents[" + index + "] = " + contents[index]);
		int x = contents[index];
		shiftLeft(index);
//		return proxy.remove(index);
		return x;
	}

	public boolean isEmpty() {
		return size == 0;
//		return proxy.isEmpty();
	}

	public String toString() {
		return "(" + index + ") " + super.toString();
	}

	@Override
	public int size() {
		return size;
	}

	public float getCost() {
		return cost;
	}
	public void setCost(float cost) {
		this.cost = cost;
	}
}
