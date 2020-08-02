package com.deliburd.util;

import java.util.concurrent.ConcurrentHashMap;

public class Node<K, V> {	
	/**
	 * The children node that the node holds
	 */
	private final ConcurrentHashMap<K, Node<K, V>> nodeChildren;
	
	/**
	 * The parent node of this node.
	 */
	private final Node<K, V> parentNode;
	
	/**
	 * The value associated with the node
	 */
	private volatile V value;
	
	/**
	 * Creates a node without a value associated with it
	 */
	Node(Node<K, V> parent) {
		nodeChildren = new ConcurrentHashMap<K, Node<K,V>>();
		parentNode = parent;
	}
	
	/**
	 * Creates a node without a value associated with it.
	 * The internal ConcurrentHashMap will have the given initial size
	 */
	Node(Node<K, V> parent, int initialSize) {
		nodeChildren = new ConcurrentHashMap<K, Node<K,V>>(initialSize);
		parentNode = parent;
	}
	
	/**
	 * Gets a child node given the identifier
	 * @param nodeIdentifier The node identifier
	 * @return The node. Null if not found.
	 */
	public Node<K, V> getChildNode(K nodeIdentifier) {
		return nodeChildren.get(nodeIdentifier);
	}
	
	/**
	 * Gets the parent node
	 * 
	 * @return The parent node of this node. Null if it's a top-level node.
	 */
	public Node<K, V> getParentNode() {
		return parentNode;
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
	void setNodeValue(V value) {
		this.value = value;
	}
	
	/**
	 * Adds a child node to the current node given an identifier
	 * 
	 * @param nodeIdentifier The identifier
	 * @param childNode The child node
	 */
	void addChildNode(K nodeIdentifier, Node<K, V> childNode) {
		nodeChildren.put(nodeIdentifier, childNode);
	}

	/**
	 * Creates a copy of the current node
	 * 
	 * @param sizeIncrease The size to increase the internal ConcurrentHashMap by
	 * @param newParent The new node to parent the copied node to
	 * @return A copy of the node
	 */
	Node<K, V> copy(int sizeIncrease, Node<K, V> newParent) {
		if(sizeIncrease < 0) {
			throw new IllegalArgumentException("The size increase must be at least 0.");
		}
		
		int newSize = nodeChildren.size();
		
		if(newSize <= Integer.MAX_VALUE - sizeIncrease) {
			newSize += sizeIncrease;
		}
		
		Node<K, V> newNode = new Node<K, V>(newParent, newSize);
		newNode.setNodeValue(value);

		nodeChildren.forEach(1, (key, node) -> {
			newNode.addChildNode(key, node.copy(sizeIncrease, newNode));
		});
		
		return newNode;
	}
}