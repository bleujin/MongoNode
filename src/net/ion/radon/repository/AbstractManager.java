package net.ion.radon.repository;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.ion.framework.db.RepositoryException;
import net.ion.framework.util.HashFunction;
import net.ion.radon.repository.orm.AbstractORM;
import net.ion.radon.repository.orm.IDMethod;

import org.apache.commons.beanutils.ConstructorUtils;

public abstract class AbstractManager<T extends AbstractORM> {

	private String wsname;
	private Session session;

	public void init(Session session, String wsname) {
		this.session = session;
		this.wsname = wsname;
	}

	public T findById(T p) {
		try {
			IDRow<T> idRow = getIDRow(p);
			
			Node node = session.getWorkspace(wsname).findOne(session, idRow.getAradonQuery(), Columns.ALL);
			
			Class<? extends AbstractORM> clz = p.getClass();
			
			AbstractORM result = clz.cast(ConstructorUtils.invokeConstructor(clz, new Object[]{idRow.getValue()}));

			return (T) result.load(node);
		} catch (IllegalAccessException e) {
			throw RepositoryException.throwIt(e) ;
		} catch (NoSuchMethodException e) {
			throw RepositoryException.throwIt(e) ;
		} catch (InvocationTargetException e) {
			throw RepositoryException.throwIt(e) ;
		} catch (InstantiationException e) {
			throw RepositoryException.throwIt(e) ;
		}
	}

	public NodeResult save(T p) {
		IDRow<T> idRow = getIDRow(p);
		NodeResult result = session.getWorkspace(wsname).setMerge(session, idRow.aradonId(), p.getNodeObject().toPropertyMap(session.getRoot())) ;
		return result ;
	}

	public Node toNode(T p) {
		IDRow<T> idRow = getIDRow(p);
		return NodeImpl.load(session, idRow.getAradonQuery(), wsname, p.getNodeObject());
	}

	protected Session getSession() {
		return session;
	}

	protected IDRow<T> getIDRow(T p) {
		try {
			IDRow<T> idrow = null;
			Method[] methods = p.getClass().getDeclaredMethods();
			for (Method method : methods) {
				IDMethod idm = method.getAnnotation(IDMethod.class);
				if (idm != null) {
					idrow = IDRow.create(p, idm.nodeName(), method.invoke(p, new Object[0]));
				}
			}

			if (idrow == null) {
				throw new IllegalArgumentException(p + " has not id object");
			}

			return idrow;
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e.getCause());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getCause());
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e.getCause());
		}
	}

	
	public NodeCursor find(PropertyQuery query){
		return session.getWorkspace(wsname).find(session, query, Columns.ALL) ;
	}
	
	public void drop(){
		session.getWorkspace(wsname).drop() ;
	}

}

class IDRow<T extends AbstractORM> {

	private final T obj;
	private final String key;
	private final Object value;

	private IDRow(T obj, String key, Object value) {
		this.obj = obj;
		this.key = key;
		this.value = value;
	}

	public final static <T extends AbstractORM> IDRow<T> create(T obj, String key, Object value) {
		return new IDRow<T>(obj, key, value);
	}

	public String getGroupNm() {
		return obj.getClass().getSimpleName();
	}

	public String getKey() {
		return key;
	}

	public Object getValue() {
		return value;
	}

	public String getGroup(){
		return obj.getClass().getSimpleName() ;
	}
	
	public PropertyQuery getAradonQuery() {
		return PropertyQuery.createByAradon(getGroup(), value);
	}

	PropertyQuery aradonId(){
		return PropertyQuery.create().eq(NodeConstants.ARADON_GROUP, new String[]{getGroup()}).eq(NodeConstants.ARADON_UID, value).eq(NodeConstants.ARADON_GHASH, HashFunction.hashGeneral(getGroup())) ;
	}
}
