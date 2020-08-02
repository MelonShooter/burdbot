package com.deliburd.util;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.deliburd.bot.burdbot.Constant;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ServerConfig {
	private static final Set<String> tables = ConcurrentHashMap.newKeySet();
	private static final ConcurrentHashMap<Long, File> serverFileMap = new ConcurrentHashMap<Long, File>();
	private static final ObjectMapper objectMapper = new ObjectMapper();
	
	public static void registerTable(String tableName) {
		if(containsTable(tableName)) {
			throw new IllegalArgumentException("This table has already been registered.");
		}
		
		tables.add(tableName);
	}
	
	public static boolean containsTable(String tableName) {
		return tables.contains(tableName);
	}
	
	public static boolean writeToConfig(String tableName, long serverID, String value, String... keys) {
		NodeInfo nodeInfo;
		
		try {
			nodeInfo = getKeyNode(tableName, serverID, true, keys);
		} catch (IOException e) {
			ErrorLogger.LogException(e);
			return false;
		}
		
		JsonGenerator generator = nodeInfo.getGenerator();
		
		try(generator) {
			nodeInfo.getKeyNode().put(keys[keys.length - 1], value);
			objectMapper.writeTree(generator, nodeInfo.getRootNode());
			return true;
		} catch(IOException e) {
			ErrorLogger.LogException(e);
			return false;
		}
	}
	
	public static boolean writeToConfig(String tableName, long serverID, long number, String... keys) {
		NodeInfo nodeInfo;
		
		try {
			nodeInfo = getKeyNode(tableName, serverID, true, keys);
		} catch (IOException e) {
			ErrorLogger.LogException(e);
			return false;
		}
		
		JsonGenerator generator = nodeInfo.getGenerator();
		
		try(generator) {
			nodeInfo.getKeyNode().put(keys[keys.length - 1], number);
			objectMapper.writeTree(generator, nodeInfo.getRootNode());
			return true;
		} catch(IOException e) {
			ErrorLogger.LogException(e);
			return false;
		}
	}
	
	public static boolean writeToConfig(String tableName, long serverID, double number, String... keys) {
		NodeInfo nodeInfo;
		
		try {
			nodeInfo = getKeyNode(tableName, serverID, true, keys);
		} catch (IOException e) {
			ErrorLogger.LogException(e);
			return false;
		}
		
		JsonGenerator generator = nodeInfo.getGenerator();
		
		try(generator) {
			nodeInfo.getKeyNode().put(keys[keys.length - 1], number);
			objectMapper.writeTree(generator, nodeInfo.getRootNode());
			return true;
		} catch(IOException e) {
			ErrorLogger.LogException(e);
			return false;
		}
	}
	
	public static boolean writeToConfig(String tableName, long serverID, boolean bool, String... keys) {
		NodeInfo nodeInfo;
		
		try {
			nodeInfo = getKeyNode(tableName, serverID, true, keys);
		} catch (IOException e) {
			ErrorLogger.LogException(e);
			return false;
		}
		
		JsonGenerator generator = nodeInfo.getGenerator();
		
		try(generator) {
			nodeInfo.getKeyNode().put(keys[keys.length - 1], bool);
			objectMapper.writeTree(generator, nodeInfo.getRootNode());
			return true;
		} catch(IOException e) {
			ErrorLogger.LogException(e);
			return false;
		}
	}
	
	public static boolean writeToConfig(String tableName, long serverID, Object POJO, String... keys) {
		NodeInfo nodeInfo;
		
		try {
			nodeInfo = getKeyNode(tableName, serverID, true, keys);
		} catch (IOException e) {
			ErrorLogger.LogException(e);
			return false;
		}
		
		JsonGenerator generator = nodeInfo.getGenerator();
		
		try(generator) {
			nodeInfo.getKeyNode().putPOJO(keys[keys.length - 1], POJO);
			objectMapper.writeTree(generator, nodeInfo.getRootNode());
			return true;
		} catch(IOException e) {
			ErrorLogger.LogException(e);
			return false;
		}
	}
	
	public static boolean writeToConfig(String tableName, long serverID, String[] stringArray, String... keys) {
		NodeInfo nodeInfo;
		try {
			nodeInfo = getKeyNode(tableName, serverID, true, keys);
		} catch (IOException e1) {
			ErrorLogger.LogException(e1);
			return false;
		}
		
		JsonGenerator generator = nodeInfo.getGenerator();
		
		try(generator) {
			String lastKey = keys[keys.length - 1];
			
			JsonNode currentNode = nodeInfo.getKeyNode().get(lastKey);
			ArrayNode arrayNode;
			
			if(currentNode == null) {
				arrayNode = nodeInfo.getKeyNode().putArray(lastKey);
			} else {
				if(currentNode.isArray()) {
					arrayNode = ((ArrayNode) currentNode).removeAll();
				} else {
					return false;
				}
			}
			
			for(var string : stringArray) {
				arrayNode.add(string);
			}
			
			objectMapper.writeTree(generator, nodeInfo.getRootNode());
			return true;
		} catch(IOException e) {
			ErrorLogger.LogException(e);
			return false;
		}
	}
	
	public static boolean writeToConfig(String tableName, long serverID, long[] numberArray, String... keys) {
		NodeInfo nodeInfo;
		try {
			nodeInfo = getKeyNode(tableName, serverID, true, keys);
		} catch (IOException e1) {
			ErrorLogger.LogException(e1);
			return false;
		}
		
		JsonGenerator generator = nodeInfo.getGenerator();
		
		try(generator) {
			String lastKey = keys[keys.length - 1];
			
			JsonNode currentNode = nodeInfo.getKeyNode().get(lastKey);
			ArrayNode arrayNode;
			
			if(currentNode == null) {
				arrayNode = nodeInfo.getKeyNode().putArray(lastKey);
			} else {
				if(currentNode.isArray()) {
					arrayNode = ((ArrayNode) currentNode).removeAll();
				} else {
					return false;
				}
			}
			
			for(var number : numberArray) {
				arrayNode.add(number);
			}
			
			objectMapper.writeTree(generator, nodeInfo.getRootNode());
			return true;
		} catch(IOException e) {
			ErrorLogger.LogException(e);
			return false;
		}
	}
	
	public static boolean writeToConfig(String tableName, long serverID, double[] numberArray, String... keys) {
		NodeInfo nodeInfo;
		try {
			nodeInfo = getKeyNode(tableName, serverID, true, keys);
		} catch (IOException e1) {
			ErrorLogger.LogException(e1);
			return false;
		}
		
		JsonGenerator generator = nodeInfo.getGenerator();
		
		try(generator) {
			String lastKey = keys[keys.length - 1];
			
			JsonNode currentNode = nodeInfo.getKeyNode().get(lastKey);
			ArrayNode arrayNode;
			
			if(currentNode == null) {
				arrayNode = nodeInfo.getKeyNode().putArray(lastKey);
			} else {
				if(currentNode.isArray()) {
					arrayNode = ((ArrayNode) currentNode).removeAll();
				} else {
					return false;
				}
			}
			
			for(var number : numberArray) {
				arrayNode.add(number);
			}
			
			objectMapper.writeTree(generator, nodeInfo.getRootNode());
			return true;
		} catch(IOException e) {
			ErrorLogger.LogException(e);
			return false;
		}
	}
	
	public static boolean writeToConfig(String tableName, long serverID, boolean[] booleanArray, String... keys) {
		NodeInfo nodeInfo;
		try {
			nodeInfo = getKeyNode(tableName, serverID, true, keys);
		} catch (IOException e1) {
			ErrorLogger.LogException(e1);
			return false;
		}
		
		JsonGenerator generator = nodeInfo.getGenerator();
		
		try(generator) {
			String lastKey = keys[keys.length - 1];
			
			JsonNode currentNode = nodeInfo.getKeyNode().get(lastKey);
			ArrayNode arrayNode;
			
			if(currentNode == null) {
				arrayNode = nodeInfo.getKeyNode().putArray(lastKey);
			} else {
				if(currentNode.isArray()) {
					arrayNode = ((ArrayNode) currentNode).removeAll();
				} else {
					return false;
				}
			}
			
			for(var bool : booleanArray) {
				arrayNode.add(bool);
			}
			
			objectMapper.writeTree(generator, nodeInfo.getRootNode());
			return true;
		} catch(IOException e) {
			ErrorLogger.LogException(e);
			return false;
		}
	}
	
	public static boolean writeToConfig(String tableName, long serverID, Object[] objectArray, String... keys) {
		NodeInfo nodeInfo;
		try {
			nodeInfo = getKeyNode(tableName, serverID, true, keys);
		} catch (IOException e1) {
			ErrorLogger.LogException(e1);
			return false;
		}
		
		JsonGenerator generator = nodeInfo.getGenerator();
		
		try(generator) {
			String lastKey = keys[keys.length - 1];
			
			JsonNode currentNode = nodeInfo.getKeyNode().get(lastKey);
			ArrayNode arrayNode;
			
			if(currentNode == null) {
				arrayNode = nodeInfo.getKeyNode().putArray(lastKey);
			} else {
				if(currentNode.isArray()) {
					arrayNode = ((ArrayNode) currentNode).removeAll();
				} else {
					return false;
				}
			}
			
			for(var object : objectArray) {
				arrayNode.addPOJO(object);
			}
			
			objectMapper.writeTree(generator, nodeInfo.getRootNode());
			return true;
		} catch(IOException e) {
			ErrorLogger.LogException(e);
			return false;
		}
	}
	
	public static String getServerConfigNodeValueAsString(String tableName, long serverID, String... keys) throws IOException {
		NodeInfo nodeInfo = getKeyNode(tableName, serverID, false, keys);
		JsonNode node;
		
		if(nodeInfo != null) {
			node = nodeInfo.getKeyNode().path(keys[keys.length - 1]);
		} else {
			return null;
		}

		if(node.isValueNode()) {
			return node.asText();
		} else {
			return null;
		}
	}
	
	public static Long getServerConfigNodeValueAsNumber(String tableName, long serverID, String... keys) throws IOException {
		NodeInfo nodeInfo = getKeyNode(tableName, serverID, false, keys);
		JsonNode node;
		
		if(nodeInfo != null) {
			node = nodeInfo.getKeyNode().path(keys[keys.length - 1]);
		} else {
			return null;
		}

		if(node.canConvertToLong()) {
			return node.asLong();
		} else {
			return null;
		}
	}
	
	public static Boolean getServerConfigNodeValueAsBoolean(String tableName, long serverID, String... keys) throws IOException {
		NodeInfo nodeInfo = getKeyNode(tableName, serverID, false, keys);
		JsonNode node;
		
		if(nodeInfo != null) {
			node = nodeInfo.getKeyNode().path(keys[keys.length - 1]);
		} else {
			return null;
		}

		if(node.isBoolean()) {
			return node.asBoolean();
		} else {
			return null;
		}
	}
	
	public static Double getServerConfigNodeValueAsDecimal(String tableName, long serverID, String... keys) throws IOException {
		NodeInfo nodeInfo = getKeyNode(tableName, serverID, false, keys);
		JsonNode node;
		
		if(nodeInfo != null) {
			node = nodeInfo.getKeyNode().path(keys[keys.length - 1]);
		} else {
			return null;
		}

		if(node.isFloatingPointNumber()) {
			return node.asDouble();
		} else {
			return null;
		}
	}
	
	public static <T> T getServerConfigNodeValueAsPOJO(String tableName, long serverID, Class<T> classObject, String... keys) throws IOException {
		NodeInfo nodeInfo = getKeyNode(tableName, serverID, false, keys);
		JsonNode node;
		
		if(nodeInfo != null) {
			node = nodeInfo.getKeyNode().path(keys[keys.length - 1]);
		} else {
			return null;
		}

		if(node.isPojo()) {
			return objectMapper.treeToValue(node, classObject);
		} else {
			return null;
		}
	}
	
	public static String[] getServerConfigNodeValueAsArray(String tableName, long serverID, String... keys) throws IOException {
		NodeInfo nodeInfo = getKeyNode(tableName, serverID, false, keys);
		JsonNode node;
		
		if(nodeInfo != null) {
			node = nodeInfo.getKeyNode().path(keys[keys.length - 1]);
		} else {
			return null;
		}

		if(node.isContainerNode()) {
			String[] values = new String[node.size()];
			int position = 0;
			
			for(JsonNode valueNode : node) {
				if(valueNode.isValueNode()) {
					values[position] = valueNode.asText();
					position++;
				} else {
					return null;
				}
			}
			
			return values;
		} else {
			return null;
		}
	}
	
	public static JsonNode getNode(String tableName, long serverID, String... keys) throws IOException {
		NodeInfo nodeInfo = getKeyNode(tableName, serverID, false, keys);
		
		if(nodeInfo == null) {
			return null;
		}
		
		JsonNode node = nodeInfo.getKeyNode().path(keys[keys.length - 1]);
		
		if(node.isMissingNode()) {
			return null;
		} else {
			return node;
		}
	}
	
	public static JsonNode getTableData(String tableName, long serverID) throws IOException {
		File serverFile = getConfigFile(tableName, serverID);
		
		if(!serverFile.exists()) {
			return null;
		}
		
		JsonNode serverJsonNode = objectMapper.readTree(serverFile);
		
		if(serverJsonNode == null || serverJsonNode.isMissingNode()) {
			return null;
		}
		
		return serverJsonNode.get(tableName);
	}
	
	public static void removeNode(String tableName, long serverID, String... keys) throws IOException {
		NodeInfo nodeInfo = getKeyNode(tableName, serverID, true, keys);
		
		JsonGenerator generator = nodeInfo.getGenerator();
		
		try(generator) {
			JsonNode node = nodeInfo.getKeyNode();
			String fieldName = keys[keys.length - 1];
			
			if(node.has(fieldName)) {
				((ObjectNode) node).remove(fieldName);
				generator.writeTree(nodeInfo.getRootNode());
			}
		}
	}
	
	private static NodeInfo getKeyNode(String tableName, long serverID, boolean writable, String... keys) throws IOException {
		if (keys == null || keys.length == 0) {
			throw new IllegalArgumentException("The keys cannot be null or blank.");
		}
		
		File serverFile = getConfigFile(tableName, serverID);
		serverFile.getParentFile().mkdirs();
		serverFile.createNewFile();
		
		JsonNode serverJsonNode = objectMapper.readTree(serverFile);
		ObjectNode serverNode;
		
		if(serverJsonNode.isMissingNode()) {
			if(writable) {
				serverNode = objectMapper.createObjectNode();
			} else {
				return null;
			}
		} else {
			serverNode = (ObjectNode) serverJsonNode;
		}
		
		ObjectNode tableNode = getOrCreateObjectNode(serverNode, tableName, writable);
		
		if(tableNode == null) {
			return null;
		}
		
		for(int i = 0; i < keys.length - 1; i++) {
			if(keys[i] == null) {
				throw new IllegalArgumentException("No key can be null.");
			}
			
			tableNode = getOrCreateObjectNode(tableNode, keys[i], writable);
			
			if(tableNode == null) {
				return null;
			}
		}
		
		JsonGenerator generator;
		
		if(writable) {
			generator = objectMapper.getFactory().createGenerator(serverFile, JsonEncoding.UTF8);
		} else {
			generator = null;
		}
		
		return new NodeInfo(generator, serverNode, tableNode);
	}
	
	private static ObjectNode getOrCreateObjectNode(ObjectNode parentNode, String field, boolean createIfMissing) {
		JsonNode tableNode = parentNode.get(field);
		ObjectNode tableObjectNode;
		
		if(tableNode == null && createIfMissing) {
			tableObjectNode = parentNode.putObject(field);
		} else if(tableNode == null) {
			return null;
		} else {
			if(tableNode.isObject()) {
				tableObjectNode = (ObjectNode) tableNode;
			} else {
				throw new IllegalArgumentException("This node already exists and its not an object node.");
			}
		}
		
		return tableObjectNode;
	}
	
	private static File getConfigFile(String tableName, long serverID) {
		if(!containsTable(tableName)) {
			throw new IllegalArgumentException("The table '" + tableName + "' has not been registered yet.");
		}
		
		File serverFile = serverFileMap.computeIfAbsent(serverID, (id) -> {
			String filePath = new StringBuilder(Constant.CONFIG_PATH)
					.append(serverID)
					.append(".json")
					.toString();
			
			return new File(filePath);
		});
		
		return serverFile;
	}
	
	private static class NodeInfo {
		private final JsonGenerator generator;
		private final ObjectNode rootNode;
		private final ObjectNode keyNode;
		
		public NodeInfo(JsonGenerator generator, ObjectNode rootNode, ObjectNode keyNode) {
			if(rootNode == null) {
				throw new IllegalArgumentException("Root node can't be null.");
			} else if(keyNode == null) {
				throw new IllegalArgumentException("Key node can't be null.");
			}
			
			this.generator = generator;
			this.rootNode = rootNode;
			this.keyNode = keyNode;
		}
		public JsonGenerator getGenerator() {
			return generator;
		}
		public ObjectNode getRootNode() {
			return rootNode;
		}
		public ObjectNode getKeyNode() {
			return keyNode;
		}
	}
}
