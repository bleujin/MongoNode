package net.ion.radon.repository;

import static net.ion.radon.repository.NodeConstants.ID;

import java.io.Serializable;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.StringUtil;

import org.apache.commons.collections.map.LRUMap;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceOutput;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;
import com.mongodb.MapReduceCommand.OutputType;

public class Workspace {

	private DBCollection collection;
	private static Map<String, Workspace> wss = new ConcurrentHashMap<String, Workspace>(new LRUMap(30));

	protected Workspace() {
	}

	static Workspace load(DB db, String _wname, WorkspaceOption option) {
		String wName = _wname.toLowerCase();

		String key = db.getName() + "." + wName;
		if (!wss.containsKey(key)) {
			final Workspace workspace = createWorkspace(db, wName, option);
			wss.put(key, workspace);

			return workspace;
		} else {

			return wss.get(key);
		}
	}

	private static Workspace createWorkspace(DB db, String wname, WorkspaceOption option) {
		synchronized (Workspace.class) {
			if (!db.collectionExists(wname)) {
				db.createCollection(wname, option.getDBObject());
			}
		}

		Workspace result = new Workspace();
		DBCollection col = db.getCollection(wname);
		if (wname == null || wname.startsWith("_") || wname.startsWith("system.")) {
			result.collection = col;
			return result;
		}
		option.initWorkspace(col) ;
		result.collection = col;
		return result;
	}

	public long count() {
		return getCollection().count();
	}

	public String getName() {
		return getCollection().getName();
	}

	public void makeIndex(IPropertyFamily props, String indexName, boolean unique) {
		try {
			BasicDBObject options = new BasicDBObject();
			options.put("name", indexName);
			options.put("unique", unique);

			getCollection().ensureIndex(props.getDBObject(), options);
		} catch (MongoException me) {
			
			throw me ;
		}
	}

	public void drop() {
		wss.remove(getCollection().getFullName());
		getCollection().drop();
	}


	protected int mergeNodes(Session session, Map<String, Node> modified) {
		Node[] targets = modified.values().toArray(new Node[0]);

		List<Node> forInsert = ListUtil.newList();
		List<Node> forUpdate = ListUtil.newList();
		for (Node node : targets) {
			if (node.isNew()) {
				forInsert.add(node);
			} else {
				forUpdate.add(node);
			}
		}

		int affectedRows = 0;
		for (Node updateNode : forUpdate) {
			updateNode.put(NodeConstants.LASTMODIFIED, GregorianCalendar.getInstance().getTimeInMillis()) ;
			NodeResult nr = session.getWorkspace(updateNode.getWorkspaceName()).save(session, updateNode);
			// affectedRows += nr.getRow Count() ;
		}

		for (Node insertNode : forInsert) {
			insertNode.put(NodeConstants.LASTMODIFIED, GregorianCalendar.getInstance().getTimeInMillis()) ;
			NodeResult nr = session.getWorkspace(insertNode.getWorkspaceName()).append(session, insertNode);
			// affectedRows += nr.getRowCount() ;
		}

		return modified.size();
	}


	public NodeResult remove(Session session, PropertyQuery query) {
		NodeResult result = NodeResult.create(session, query, getCollection().remove(query.getDBObject()));
		return result;
	}


	public NodeCursor find(Session session, PropertyQuery iquery, Columns columns) {
		NodeCursorImpl result = NodeCursorImpl.create(session, iquery, this.getName(), getCollection().find(iquery.getDBObject(), columns.getDBOjbect()));
		session.setAttribute(Explain.class.getCanonicalName(), result.explain());
		return result;
	}
	
	public Node findOne(Session session, PropertyQuery iquery, Columns column) {
		NodeCursor nc = find(session, iquery, column).limit(1);

		Node result = (nc.hasNext()) ? nc.next() : null;
		return result;
	}

	void makeUniqueIndex(IPropertyFamily props, String indexName) {
		makeIndex(props, indexName, true);
	}

	Node newNode(Session session) {
		final String newId = new ObjectId().toString();
		return newNode(session, newId, PropertyFamily.create(ID, new ObjectId(newId)));
	}

	Node newNode(Session session, String name) {
		return newNode(session, name, PropertyFamily.create());
	}

	protected NodeCursor mapreduce(Session session, String mapFunction, String reduceFunction, String finalFunction, CommandOption options, PropertyQuery condition) {
		if (StringUtil.isNotBlank(options.getOutputCollection()) && session.getWorkspace(options.getOutputCollection(), WorkspaceOption.NONE).count() <= 0) {
			if (options.getOutputType() == OutputType.MERGE || options.getOutputType() == OutputType.REDUCE) {
				options.setOutputType(OutputType.REPLACE);
			}
		}

		MapReduceCommand command = new MapReduceCommand(getCollection(), mapFunction, reduceFunction, options.getOutputCollection(), options.getOutputType(), condition.getDBObject());
		if (StringUtil.isNotBlank(finalFunction))
			command.setFinalize(finalFunction);
		options.apply(command);

		MapReduceOutput out = getCollection().mapReduce(command);
		return ApplyCursor.create(session, condition, out);
	}

	protected Object applyMapReduce(Session session, String mapFunction, String reduceFunction, String finalFunction, CommandOption options, PropertyQuery condition, ApplyHander handler) {
		// MapReduceOutput out = collection.mapReduce(mapFunction, reduceFunction, null, MapReduceCommand.OutputType.INLINE, condition.getDBObject()) ;
		NodeCursor nc = mapreduce(session, mapFunction, reduceFunction, finalFunction, options, condition);

		Object result = handler.handle(nc);
		return result;
	}

	protected List<Node> group(Session session, IPropertyFamily keys, PropertyQuery condition, IPropertyFamily initial, String reduce) {
		BasicDBList list = (BasicDBList) getCollection().group(keys.getDBObject(), condition.getDBObject(), initial.getDBObject(), reduce);
		List<Node> nodes = ListUtil.newList();
		for (Object obj : list) {
			nodes.add(NodeImpl.load(session, condition, getName(), (DBObject) obj));
		}
		return nodes;

	}

	/* update start */

	public NodeResult merge(Session session, MergeQuery query, TempNode tnode) {
		Map<String, Serializable> map = MapUtil.newMap();
		map.putAll(tnode.toMap());

		Node found = findOne(session, PropertyQuery.load(query), Columns.append().add(NodeConstants.ID));
		if (found != null) {
			tnode.putProperty(PropertyId.reserved(NodeConstants.ID), found.getId());
		} else { // if newNode
			Map queryMap = query.data();
			for (Object key : queryMap.keySet()) {
				Object value = queryMap.get(key);
				if (value instanceof DBObject) {
					continue;
				}
				map.put(key.toString(), (Serializable) value);
			}
		}

		DBObject mod = new BasicDBObject();
		mod.put("$set", appendLastModified(map));

		return updateNode(session, PropertyQuery.load(query), mod, true, true);
	}

	protected NodeResult findAndOverwrite(Session session, PropertyQuery query, Map<String, ?> props) {
		DBObject find = getCollection().findOne(query.getDBObject());
		if (find == null)
			return NodeResult.NULL;

		NodeImpl findNode = NodeImpl.load(session, query, getCollection().getName(), find);
		findNode.clearProp(false);

		for (Entry<String, ?> entry : props.entrySet()) {
			find.put(entry.getKey(), entry.getValue());
		}

		// collection.findAndModify(query.getDBObject(), NodeObject.load(props).getDBObject()) ;

		return NodeResult.create(session, query, getCollection().save(find));
	}

	NodeResult findAndUpdate(Session session, PropertyQuery query, Map<String, ?> props) {
		DBObject mod = new BasicDBObject();
		mod.put("$set", appendLastModified(props));

		return updateNode(session, query, mod, false, true);
	}

	NodeResult inc(Session session, PropertyQuery query, String propId, long value) {
		DBObject mod = new BasicDBObject("$inc", new BasicDBObject(propId, value));
		return updateNode(session, query, mod, true, true);
	}

	NodeResult update(Session session, PropertyQuery query, Map<String, ?> values, boolean upset) {
		DBObject mod = new BasicDBObject();
		mod.put("$set", appendLastModified(values));

		return updateNode(session, query, mod, upset, true);
	}


	NodeResult unset(Session session, PropertyQuery query, BasicDBObject value) {
		DBObject mod = new BasicDBObject();
		mod.put("$unset", value);

		return updateNode(session, query, mod, false, true);
	}

	NodeResult pull(Session session, PropertyQuery query, Map<String, ?> values) {
		DBObject mod = new BasicDBObject();
		mod.put("$pull", NodeObject.load(values).getDBObject());

		return updateNode(session, query, mod, false, true);
	}

	NodeResult push(Session session, PropertyQuery query, Map<String, ?> values) {
		DBObject mod = new BasicDBObject();
		mod.put("$push", NodeObject.load(values).getDBObject());
		return updateNode(session, query, mod, false, true);
	}

	public List<NodeObject> getIndexInfo() {
		List<NodeObject> result = ListUtil.newList();
		for (DBObject index : getCollection().getIndexInfo()) {
			result.add(NodeObject.load(index));
		}
		return result;
	}

	public NodeResult updateNode(Session session, PropertyQuery query, DBObject values, boolean upset, boolean multi) {
		WriteResult wr = getCollection().update(query.getDBObject(), values, upset, multi);
		NodeResult result = NodeResult.create(session, query, wr);
		return result;
	}

	protected NodeResult updateInner(Session session, PropertyQuery query, DBObject values, boolean upset) {
		return this.updateNode(session, query, values, upset, true) ;
	}
	
	/* update end */
	protected DBCollection getCollection() {
		return collection;
	}

	private Node newNode(Session session, String name, PropertyFamily props) {
		return NodeImpl.create(session, this.getName(), NodeObject.load(props.getDBObject()), "/", name);
	}

	private NodeResult save(Session session, Node node) {
		DBObject inmod = node.getDBObject();
		// DBObject mod = new BasicDBObject("$set", inmod) ;
		// return NodeResult.create(collection.save(inmod)) ;

		return updateNode(session, PropertyQuery.createById(node.getIdentifier()), inmod, true, false);
	}

	private NodeResult append(Session session, Node node) {
		DBObject inmod = node.getDBObject();

		WriteResult wr = getCollection().insert(inmod);
		NodeResult result = NodeResult.create(session, node.getQuery(), wr);
		return result;
	}

	private DBObject appendLastModified(Map<String, ?> values) {
		DBObject inmod = NodeObject.load(values).getDBObject();
		inmod.put(NodeConstants.LASTMODIFIED, GregorianCalendar.getInstance().getTimeInMillis());
		return inmod;
	}

	public String toString() {
		return getCollection().getFullName();
	}

	DBCollection innerCollection() {
		return collection;
	}

}
