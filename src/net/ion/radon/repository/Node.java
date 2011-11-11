package net.ion.radon.repository;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.ion.framework.db.RepositoryException;
import net.ion.framework.db.procedure.IStringObject;
import net.ion.framework.util.ChainMap;
import net.ion.framework.util.ListUtil;
import net.ion.radon.core.PageBean;
import net.ion.radon.repository.innode.NormalInNode;
import net.ion.radon.repository.myapi.AradonQuery;
import net.ion.radon.repository.myapi.ICursor;
import net.ion.radon.repository.relation.IRelation;

import com.mongodb.DBObject;

public interface Node extends IPropertyFamily, INode {

	public Node put(String key, Object val) ;

	public Node append(String key, Object val) ;
	
	public Node putEncrypt(String key, String value)  throws RepositoryException;


	
//	public ReferenceObject toRef() ;
		
	public String getIdentifier() ;

	// public NodeResult save() ;
	// public Node merge(IPropertyFamily query) ;

	public Object getId() ;

	public String getName() ;

	public String getPath();

	public String getWorkspaceName();
	
	public Node setAradonId(String groupid, Object uid) ;
	
	// child
	public Node createChild(String name) ;
	
	public ICursor getChild() ;

	public Node getChild(String name);
	
//	public List<Node> removeChild() ;
//	
//	public List<Node> removeChild(String nameOrId) ;

//	public List<Node> removeDescendant() ;

	public Session getSession();

	public Node getParent();

	public AradonId getAradonId();

//	public boolean addReference(String refType, AradonQuery query);

//	public ReferenceTaragetCursor getReferencedNodes(String aradonGroup);

//	public int removeReference(String refType, AradonQuery query);

//	public boolean setReference(String refType, AradonQuery preQuery, AradonQuery newQuery);

	public long getLastModified();

	public NormalInNode inner(String name) ;
	
	public InListNode inlist(String name);

	public boolean isNew() ;

	public void notify(NodeEvent nevent);

	public Node toRelation(String relType, NodeRef aref);

	public IRelation relation(String relType);

	public PropertyQuery getQuery();

	public NodeRef selfRef();

}

class RootNode implements Node {

	private static final long serialVersionUID = -5004749388021801200L;
	private Session session ;
	RootNode(Session session){
		this.session = session ;
	}
	
	public boolean addReference(String refType, AradonQuery query) {
		throw new IllegalArgumentException("root node is read only") ;
	}

	public Node append(String key, Object val) {
		throw new IllegalArgumentException("root node is read only") ;
	}

	public Node createChild(String name) {
		return session.newNode(name);
	}

	public AradonId getAradonId() {
		return AradonId.EMPTY;
	}

	public ICursor getChild() {
		return session.createQuery().regEx(NodeConstants.PATH, "^\\/\\w*$").find() ;
	}

	public Node getChild(String name) {
		return session.createQuery().findByPath("/" + name);
	}

	public Object getId() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getIdentifier() {
		return "";
	}

	public String getName() {
		return "";
	}

	public String getPath() {
		return "/";
	}

	public PropertyQuery getQuery() {
		return PropertyQuery.EMPTY;
	}

	public ReferenceTaragetCursor getReferencedNodes(String aradonGroup) {
		// TODO Auto-generated method stub
		return null;
	}

	public Session getSession() {
		return session;
	}

	public String getWorkspaceName() {
		return session.getCurrentWorkspaceName();
	}

	public Node put(String key, Object val) {
		throw new IllegalArgumentException("root node is read only") ;
	}

	public Node putEncrypt(String key, String value) throws RepositoryException {
		throw new IllegalArgumentException("root node is read only") ;
	}

	public List<Node> removeChild() {
		List<Node> result = session.createQuery().find().toList(PageBean.ALL) ;
		session.dropWorkspace();
		return result ;
	}

	public List<Node> removeChild(String nameOrId) {
		return ListUtil.EMPTY ;
	}

	public List<Node> removeDescendant() {
		return removeChild();
	}

	public int removeReference(String refType, AradonQuery query) {
		; // no action 
		return 0;
	}

	public Node setAradonId(String groupid, Object uid) {
		return this;
	}

	public ReferenceObject toRef() {
		return null;
	}

	public DBObject getDBObject() {
		return NodeObject.BLANK_INNODE.getDBObject();
	}

	public Map<String, ? extends Object> toMap() {
		return Collections.EMPTY_MAP;
	}

	public void clearProp() {
	}

	public Serializable get(String propId) {
		return null;
	}

	public Serializable get(String propId, int index) {
		return null;
	}

	public int getAsInt(String propId) {
		return 0;
	}

	public Node getParent() {
		return null;
	}

	public String getString(String key) {
		return null;
	}

	public boolean hasProperty(String key) {
		return false;
	}

	public NormalInNode inner(String name) {
		throw new IllegalArgumentException("root node is read only") ;
	}

	public InListNode inlist(String name) {
		throw new IllegalArgumentException("root node is read only") ;
	}

	public boolean isMatchEncrypted(String key, String value) {
		return false;
	}

	public void putAll(Map<String, ? extends Object> props) {
		throw new IllegalArgumentException("root node is read only") ;
	}

	public void putAll(ChainMap<String, ? extends Object> chainMap) {
		throw new IllegalArgumentException("root node is read only") ;
	}

	public Map<String, ? extends Object> toPropertyMap() {
		return Collections.EMPTY_MAP;
	}

	public Map<String, ? extends Object> toPropertyMap(NodeColumns cols) {
		return Collections.EMPTY_MAP;
	}

	public boolean setReference(String refType, AradonQuery preQuery, AradonQuery newQuery) {
		// TODO Auto-generated method stub
		return false;
	}

	public long getLastModified() {
		return 0;
	}

	public void updateLastModified() {
		; // no action
	}

	public boolean isNew() {
		return false;
	}

	public Node put(String key, IStringObject val) {
		throw new IllegalArgumentException("root node is read only") ;
	}

	public void notify(NodeEvent nevent) {
	}

	public Node toRelation(String relType, NodeRef aref) {
		throw new IllegalArgumentException("root node is read only") ;
	}

	public IRelation relation(String relType) {
		throw new IllegalArgumentException("root node is read only") ;
	}

	public NodeRef selfRef() {
		return NodeRef.create(this);
	}

}
