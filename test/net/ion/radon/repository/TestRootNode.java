package net.ion.radon.repository;

import net.ion.radon.repository.myapi.ICursor;

public class TestRootNode extends TestBaseRepository{

	public void testGet() throws Exception {
		
		Node root = session.getRoot() ;
		
		assertEquals("", root.getIdentifier()) ;
		assertEquals("", root.getName()) ;
		assertEquals("/", root.getPath()) ;
	}
	
	public void testChild() throws Exception {
		Node bigboy = session.newNode("bleujin") ;
		Node child = bigboy.createChild("child") ;
		
		session.commit() ;
		assertEquals(true, bigboy.getParent() == session.getRoot()) ;
		
		
		Node root = session.getRoot() ;
		
		ICursor cursor = root.getChild() ;
		assertEquals(1, cursor.count()) ;
		assertEquals(bigboy.getIdentifier(), cursor.next().getIdentifier()) ;
	}

	
	public void testPath() throws Exception {
		Node node = session.newNode("bleujin") ;
		assertEquals("/bleujin", node.getPath()) ;
	}
	
}
