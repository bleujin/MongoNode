package net.ion.radon.repository;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.StringUtil;
import net.ion.radon.repository.innode.InListNodeImpl;
import net.ion.radon.repository.innode.InNodeImpl;
import net.ion.radon.repository.util.JSONUtil;

import org.bson.types.BasicBSONList;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class NodeObject implements Serializable, IPropertyFamily {
	private static final long serialVersionUID = -321332758287996204L;
	private DBObject inner;

	final static NodeObject BLANK_INNODE = NodeObject.load(new BasicDBList());

	private NodeObject() {
		inner = new BasicDBObject();
	}

	public static NodeObject load(DBObject dbo) {
		NodeObject newObject = new NodeObject();
		newObject.inner = dbo;
		return newObject;
	}

	public static NodeObject load(Map<String, ?> values) {
		NodeObject newObject = new NodeObject();
		for (Entry<String, ?> entry : values.entrySet()) {
			newObject.put(entry.getKey(), entry.getValue());
		}
		return newObject;
	}

	public final static NodeObject create() {
		return new NodeObject();
	}

	public static NodeObject create(String key, Object value) {
		NodeObject newObject = create();
		newObject.put(key, value);

		return newObject;
	}

	@Override
	public String toString() {
		return inner.toString();
	}

	public NodeObject put(String key, Object value) {

		String transKey = key.startsWith("$") ? key : key.toLowerCase();
		if (value instanceof IPropertyFamily) {
			inner.put(transKey, ((IPropertyFamily) value).getDBObject());
		} else if (value instanceof JsonObject) {
			inner.put(transKey, JSONUtil.toDBObject((JsonObject) value));
		} else {
			inner.put(transKey, value);
		}
		return this;
	}

	public void put(String key, String[] values) {
		BasicDBList list = new BasicDBList();
		for (int i = 0; i < values.length; i++) {
			list.add(values[i]);
		}
		put(key, list);
	}

	public void putProperty(PropertyId pid, Object value) {
		put(pid.getKeyString(), value);
	}

	public Object get(String key) {
		if (key.startsWith("$")) {
			DBObject value = (DBObject) inner.get(key);
			if (value == null) {

				inner.put(key, value);
			}
			return value;
		}

		String[] propIds = StringUtil.split(key, ".");

		int i = 0;

		Object result = null;
		DBObject about = inner;
		while (i < propIds.length) {
			result = about.get(propIds[i].toLowerCase());
			if (result != null && result instanceof DBObject) {
				about = (DBObject) result;
				i++;
				continue;
			} else
				break;
		}
		return result;
	}

	Serializable get(String propId, INode parent) {
		Object result = get(propId);
		if (result instanceof BasicDBList) {
			return inlist(propId, result, parent);
		} else if (result instanceof DBObject) {
			return inner(propId, result, parent);
		}
		return (Serializable) result;
	}

	public Serializable get(String propId, int index, INode parent) {
		Object result = get(propId, parent);
		if (result instanceof InListNode) {
			return (Serializable) ((InListNode) result).get(index);
		} else if (index == 0) {
			return (Serializable) result;
		} else {
			throw new IllegalArgumentException("element is not array");
		}
	}

	// public void inner(String inname, NodeObject inner) {
	// this.put(inname, inner.getDBObject()) ;
	// }

	public InNode inner(String name, INode parent) {
		Object result = get(name);
		return inner(name, result, parent);
	}

	private InNode inner(String name, Object result, INode parent) {

		if (!(result instanceof DBObject)) {
			DBObject value = new BasicDBObject();
			put(name, value);
			return InNodeImpl.create(value, name, parent);
		}
		return InNodeImpl.create((DBObject) result, name, parent);
	}

	public InListNode inlist(String name, INode parent) {
		Object result = get(name);
		// if (result == null) result = new BasicDBList();
		// put(name, result) ;
		return inlist(name, result, parent);
	}

	private InListNode inlist(String name, Object result, INode parent) {
		if (!(result instanceof BasicDBList)) {
			BasicDBList value = new BasicDBList();
			put(name, value);
			return InListNodeImpl.load(value, name, parent);
		}
		return InListNodeImpl.load((BasicDBList) result, name, parent);
	}

	public String getString(String path) {
		return StringUtil.toString(get(path));
	}

	public synchronized void appendProperty(PropertyId pid, Object val) {
		// putProperty( PropertyId.create("$addToSet"), new BasicDBObject(pid.getKeyString(), val)) ;

		if (containsField(pid.getKeyString())) {
			Object beforeVal = get(pid.getKeyString());
			if (beforeVal instanceof BasicBSONList) {
				((BasicBSONList) beforeVal).add(val);
			} else {
				BasicDBList list = new BasicDBList();
				list.add(beforeVal);
				list.add(val);
				putProperty(pid, list);
			}
		} else {
			BasicDBList list = new BasicDBList();
			list.add(val);
			putProperty(pid, list);
		}

	}

	public boolean containsField(String key) {
		return inner.containsField(key.toLowerCase());
	}

	public DBObject getDBObject() {
		return inner;
	}

	public int size() {
		return toMap().size();
	}

	public Map<String, ? extends Object> toMap() {
		return inner.toMap();
	}

	public Map<String, Object> toMap(INode parent) {
		Map<String, Object> result = new HashMap<String, Object>();
		for (String key : inner.keySet()) {
			result.put(key, get(key, parent));
		}
		return Collections.unmodifiableMap(result);
	}

	public Map<String, Object> toClone(INode parent) {
		Map<String, Object> result = new HashMap<String, Object>();
		for (String key : inner.keySet()) {
			if (NodeConstants.ID.equals(key)) continue ;
			result.put(key, get(key, parent));
		}
		return Collections.unmodifiableMap(result);
	}


	public Map<String, ? extends Object> toPropertyMap(INode parent) {
		Map<String, Object> result = new HashMap<String, Object>();
		
		for (String key : inner.keySet()) {
			if (NodeUtil.isReservedProperty(key))
				continue;
			result.put(key, get(key, parent));
		}
		return Collections.unmodifiableMap(result);
	}

	public Map<String, ? extends Object> toPropertyMap(NodeColumns cols, INode parent) {
		Map<String, Object> result = new HashMap<String, Object>();
		for (String key : inner.keySet()) {
			if (NodeUtil.isReservedProperty(key) && !cols.contains(key))
				continue;
			result.put(key, get(key, parent));
		}
		return Collections.unmodifiableMap(result);
	}

	public static PropertyId createPropId(String key) {
		return PropertyId.create(key);
	}

	public int hashCode() {
		return inner.hashCode();
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof NodeObject))
			return false;
		NodeObject that = (NodeObject) obj;
		return inner.equals(that.inner);
	}

}
