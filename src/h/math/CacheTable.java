package h.math;

import java.lang.ref.SoftReference;
import java.util.HashMap;

public class CacheTable<K, V> {
	private HashMap<K, SoftReference<V>> map;

	public CacheTable() {
		this.map = new HashMap<K, SoftReference<V>>();
	}
	
	public void store(K key, V value) {
		map.put(key, new SoftReference<V>(value));
	}
	
	public V get(K key) {
		if (map.containsKey(key)) {
			return map.get(key).get();
		} else {
			return null;
		}
	}
}
