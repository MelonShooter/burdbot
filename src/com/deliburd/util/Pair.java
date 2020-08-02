package com.deliburd.util;

import java.util.Objects;

public class Pair<K, V> {
	private final K key;
	private final V value;
	
	public Pair(K key, V value) {
		this.key = key;
		this.value = value;
	}
	
	public K getKey() {
		return key;
	}
	
	public V getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Pair<?, ?>)) {
			return false;
		}
		
		var pairObject = (Pair<?, ?>) obj;
		
		return Objects.equals(key, pairObject.getKey()) &&
				Objects.equals(value, pairObject.getValue());
	}
}
