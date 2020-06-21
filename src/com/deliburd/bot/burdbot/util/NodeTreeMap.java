package com.deliburd.bot.burdbot.util;

import java.util.HashMap;

public class NodeTreeMap<K, V> {
	/**
	 * The hashmap holding the nodes.
	 */
	private HashMap<K, Node<K, V>> nodeMap;
	
	/**
	 * A tree data structure where the leaves lead to a value given keys
	 */
	public NodeTreeMap() {
		nodeMap = new HashMap<K, Node<K, V>>();
	}
	
	/**
	 * Finds a value in the tree given the keys
	 * 
	 * @param keys The keys
	 * @return The value if found, or null if not
	 */
	public V FindValue(K[] keys) {
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
		
		V nodeValue = currentNode.getNodeValue();
		
		if(currentNode == null || nodeValue == null) {
			return null;
		} else {
			return nodeValue;
		}
	}
	
	/**
	 * Adds a value into the tree given keys
	 * 
	 * @param keys The keys
	 * @param value The value to add
	 */
	public void AddValue(K[] keys, V value) {
		if(keys == null || keys.length == 0) {
			throw new IllegalArgumentException("The provided array must contain keys to find a value.");
		}
		
		Node<K, V> currentNode = nodeMap.get(keys[0]);
		
		if(currentNode == null) {
			currentNode = new Node<K, V>();
			nodeMap.put(keys[0], currentNode);
		}
		
		for(int i = 1; i < keys.length; i++) {
			var newNode = currentNode.getChildNode(keys[i]);
			if(newNode == null) {
				newNode = new Node<K, V>();
				currentNode.addChildNode(keys[i], newNode);
			}
			
			currentNode = newNode;
		}
		
		currentNode.setNodeValue(value);
	}
}
