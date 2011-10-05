package net.ion.radon.repository;

import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;

public class TestGetPath extends TestBaseRepository{

	
	public void testNormalPath() throws Exception {
		Session session = createSampleNode();

		Node find = session.createQuery().findByPath("/root/bleujin") ;
		assertEquals("bleujin", find.getString("name")) ;
	}
	
	public void testname() throws Exception {
		Session session = createSampleNode();
		Node find = session.createQuery().findByPath("/root/bleujin") ;
		
		
	}
	
	

	private Session createSampleNode() {
		Node root = session.newNode("root").put("name", "root").put("depth", 1) ;
		Node bleujin = root.createChild("bleujin").put("name", "bleujin").put("address", "seoul").inner("loc").put("x", 1).put("y", 1).getParent() ;
		
		for (int i = 0; i < 10 ; i++) {
			bleujin.inlist("comments").push(MapUtil.<String, Object>chainMap().put("greeting", "hello").put("index", i)) ;
		}
		session.commit() ;
		return session ;
	}
	
	
	
	
	
}