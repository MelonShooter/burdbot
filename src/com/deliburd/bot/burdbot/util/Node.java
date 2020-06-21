package com.deliburd.bot.burdbot.util;

import java.util.HashMap;

public class Node<K, V> {
	/**
	 * The value associated with the node
	 */
	private V value;
	
	/**
	 * The children node that the node holds
	 */
	private HashMap<K, Node<K, V>> nodeChildren;

	/**
	 * Creates a node with children but no value associated with it
	 */
	public Node() {
		nodeChildren = new HashMap<K, Node<K,V>>();
	}
	
	/**
	 * Creates a node either with or without children but with a value associated with it
	 * 
	 * @param value The value associated with the node
	 * @param hasChildren Whether the node will have children
	 */
	public Node(V value, boolean hasChildren) {
		this.value = value;
		
		if(!hasChildren) {
			nodeChildren = new HashMap<K, Node<K,V>>();
		}
	}
	
	/**
	 * Get the value associated with the node
	 * 
	 * @return The value associated with the node. Will be null if not assigned one.
	 */
	public V getNodeValue() {
		return value;
	}
	
	/**
	 * Sets the node's associated value
	 * 
	 * @param value The value
	 */
	public void setNodeValue(V value) {
		this.value = value;
	}
	
	/**
	 * Adds a child node to the current node given an identifier
	 * 
	 * @param nodeIdentifier The identifier
	 * @param childNode The child node
	 */
	public void addChildNode(K nodeIdentifier, Node<K, V> childNode) {
		nodeChildren.put(nodeIdentifier, childNode);
	}
	
	/**
	 * Gets a child node given the identifier
	 * @param nodeIdentifier The node identifier
	 * @return The node. Null if not found.
	 */
	public Node<K, V> getChildNode(K nodeIdentifier) {
		return nodeChildren.get(nodeIdentifier);
	}
}
