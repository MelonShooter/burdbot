package com.deliburd.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class NodeTreeMap<K, V> {
	/**
	 * The hashmap holding the nodes.
	 */
	private final ConcurrentHashMap<K, Node<K, V>> nodeMap;
	private final ReentrantLock writeCopyLock;
	
	/**
	 * A tree data structure where the leaves lead to a value given keys
	 */
	public NodeTreeMap() {
		nodeMap = new ConcurrentHashMap<K, Node<K, V>>();
		writeCopyLock = new ReentrantLock();
	}
	
	/**
	 * A tree data structure where the leaves lead to a value given keys
	 * 
	 * @param initialSize The size to intialize the node map with
	 */
	public NodeTreeMap(int initialSize) {
		nodeMap = new ConcurrentHashMap<K, Node<K, V>>(initialSize);
		writeCopyLock = new ReentrantLock();
	}
	
	/**
	 * Finds a value in the tree given the keys
	 * 
	 * @param keys The keys
	 * @return The value if found, or null if not
	 */
	public V findValue(K[] keys) {
		if(keys == null || keys.length == 0) {
			throw new IllegalArgumentException("The provided array must contain keys to find a value.");
		}
		
		var currentNode = nodeMap.get(keys[0]);
		
		for(int i = 1; i < keys.length; i++) {
			if(currentNode == null) {
				return null;
			}

			currentNode = currentNode.getChildNode(keys[i]);
		}
		
		if(currentNode == null) {
			return null;
		}
		
		V nodeValue = currentNode.getNodeValue();
		
		if(nodeValue == null) {
			return null;
		} else {
			return nodeValue;
		}
	}
	
	/**
	 * Finds a node in the tree given the keys
	 * 
	 * @param keys The keys
	 * @return The value if found, or null if not
	 */
	public Node<K, V> findNode(K[] keys) {
		if(keys == null || keys.length == 0) {
			throw new IllegalArgumentException("The provided array must contain keys to find a value.");
		}
		
		var currentNode = nodeMap.get(keys[0]);
		
		for(int i = 1; i < keys.length; i++) {
			if(currentNode == null) {
				return null;
			}

			currentNode = currentNode.getChildNode(keys[i]);
		}
		
		return currentNode;
	}
	
	/**
	 * Adds a value into the tree given keys
	 * 
	 * @param keys The keys
	 * @param value The value to add
	 */
	public void addValue(K[] keys, V value) {
		if(keys == null || keys.length == 0) {
			throw new IllegalArgumentException("The provided array must contain keys to find a value.");
		}
		
		Node<K, V> currentNode = nodeMap.get(keys[0]);
		
		writeCopyLock.lock();
		
		try {
			if(currentNode == null) {
				currentNode = new Node<K, V>(null);
				nodeMap.put(keys[0], currentNode);
			}
			
			for(int i = 1; i < keys.length; i++) {
				var newNode = currentNode.getChildNode(keys[i]);
				if(newNode == null) {
					newNode = new Node<K, V>(currentNode);
					currentNode.addChildNode(keys[i], newNode);
				}
				
				currentNode = newNode;
			}
			
			currentNode.setNodeValue(value);
		} finally {
			writeCopyLock.unlock();
		}
	}
	
	/**
	 * Copies the NodeTreeMap and all of its nodes copying references to any stored keys or values.
	 * 
	 * @param sizeIncrease The amount to increase each internal Map stored within each node by.
	 * @return The copied NodeTreeMap
	 */
	public NodeTreeMap<K, V> copy(int sizeIncrease) {
		if(sizeIncrease < 0) {
			throw new IllegalArgumentException("The size increase must be at least 0.");
		}
		
		int newSize = nodeMap.size();
		
		if(newSize <= Integer.MAX_VALUE - sizeIncrease) {
			newSize += sizeIncrease;
		}
		
		NodeTreeMap<K, V> newNodeTreeMap = new NodeTreeMap<K, V>(newSize);

		writeCopyLock.lock();
		
		try {
			nodeMap.forEach(1, (key, node) -> {
				newNodeTreeMap.nodeMap.put(key, node.copy(sizeIncrease, null));
			});
		} finally {
			writeCopyLock.unlock();
		}
		
		return newNodeTreeMap;
	}
}
