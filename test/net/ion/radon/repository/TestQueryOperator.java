package net.ion.radon.repository;

import net.ion.framework.util.DateUtil;
import net.ion.framework.util.Debug;
import net.ion.radon.core.PageBean;


public class TestQueryOperator extends TestBaseRepository {

	private void createSampleNode() {
		session.newNode().put("name", "bleu").put("age", 20);
		session.newNode().put("name",  "jin").put("age", 30).put("address", "seoul").put("path", "/zf/ziro");
		session.newNode().put("name", "hero").put("age", 40).put("address", null).put("path", "/hero");
		session.commit();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		createSampleNode();
	}

	public void testAllFind() throws Exception {
		createQuery().descending("Age").find().debugPrint(PageBean.ALL) ;
	}
	
	
	public void testOrExpression() throws Exception {

		NodeCursor nc = createQuery().or(PropertyQuery.create("name", "bleu"), PropertyQuery.create("name", "jin")).find();
		assertEquals(2, nc.count());
	}
	
	public void testOr2() throws Exception {
		session.newNode().put("color", "black").put("_age", 30);
		session.newNode().put("color", "white").put("_age", 40);
		session.newNode().put("color", "red").put("_age", 50);
		session.commit() ;

		NodeCursor nc = createQuery().or(PropertyQuery.create("color", "black").put("_age", 30), PropertyQuery.create("color", "white").put("_age", 40)).find();
		assertEquals(2, nc.count());
	}
	
	
	public void testOrAradonGroup() throws Exception {
		session.dropWorkspace() ;
		
		session.newNode().setAradonId("jonadan", "my1").put("name", "my1").put("color", "black") ;
		session.newNode().setAradonId("jonadan", "my2").put("name", "my2").put("color", "white") ;
		session.newNode().setAradonId("jonadan", "my4").put("name", "my4").put("color", "red") ;
		
		session.commit() ;
		
		PropertyQuery q1 = PropertyQuery.createByAradon("jonadan").put("__aradon.uid", "my1");
		PropertyQuery q2 = PropertyQuery.createByAradon("jonadan").put("__aradon.uid", "my4");

		createQuery().or(q1, q2).find().debugPrint(PageBean.ALL);
		assertEquals(2, createQuery().or(q1, q2).find().count()) ;
	}

	
	
	public void testInExpression() throws Exception {
		NodeCursor nc =  createQuery().in("name", new Object[] { "bleu", "jin" }).find();

		assertEquals(2, nc.count());
	}

	public void testGt() throws Exception {
		NodeCursor nc = createQuery().gt("age", 30).find();

		assertEquals(1, nc.count());
	}

	public void testGte() throws Exception {
		NodeCursor nc =  createQuery().gte("age", 30).ascending("age").find();

		assertEquals(2, nc.count());
		assertEquals(30, nc.next().getAsInt("age"));
		assertEquals(40, nc.next().getAsInt("age"));
	}

	public void testLt() throws Exception {
		NodeCursor nc = createQuery().lt("age", 30).find();

		assertEquals(1, nc.count());
	}

	public void testLte() throws Exception {
		NodeCursor nc = createQuery().lte("age", 30).ascending("age").find();

		assertEquals(2, nc.count());

		assertEquals(20, nc.next().getAsInt("age"));
		assertEquals(30, nc.next().getAsInt("age"));
	}

	public void testExist() throws Exception {
		NodeCursor nc = createQuery().isExist("address").ascending("age").find();

		assertEquals(2, nc.count());
		assertEquals(30, nc.next().getAsInt("age"));
	}

	public void testNotExist() throws Exception {
		NodeCursor nc =  createQuery().isNotExist("address").ascending("age").find();

		assertEquals(1, nc.count());
		assertEquals(20, nc.next().getAsInt("age"));
	}
	
	
	public void testRegExpression() throws Exception {
		SessionQuery query = createQuery().regEx("path", "^\\/\\w*$");
		Debug.debug(query) ;
		query.find().debugPrint(PageBean.ALL) ;
		
	}
	
	public void testBetween() throws Exception {
		session.dropWorkspace() ;
		session.newNode().put("age", 20) ;
		session.commit() ;
		
		assertEquals(1, session.createQuery().lt("age", 30).find().count()) ;
		assertEquals(0, session.createQuery().lt("age", 19).find().count()) ;
		
		assertEquals(0, session.createQuery().gt("age", 30).find().count()) ;
		assertEquals(1, session.createQuery().gt("age", 19).find().count()) ;

		assertEquals(0, session.createQuery().lte("age", 19).find().count()) ;
		assertEquals(1, session.createQuery().lte("age", 20).find().count()) ;
		assertEquals(1, session.createQuery().lte("age", 21).find().count()) ;
		
		assertEquals(1, session.createQuery().gte("age", 19).find().count()) ;
		assertEquals(1, session.createQuery().gte("age", 20).find().count()) ;
		assertEquals(0, session.createQuery().gte("age", 21).find().count()) ;
		
		assertEquals(1, session.createQuery().where("this.age < 25").find().count()) ;
		assertEquals(1, session.createQuery().where("this.age > 19").find().count()) ;
		// assertEquals(1, session.createQuery().where("this.age > 19 and this.age < 25").find().count()) ; .. is not permitted. use and operator

		
		assertEquals(1, session.createQuery().between("age", 20, 25).find().count()) ;
		assertEquals(1, session.createQuery().between("age", 19, 25).find().count()) ;
		assertEquals(0, session.createQuery().between("age", 17, 19).find().count()) ;
//		assertEquals(0, session.createQuery().between("age", 24, 25).find().count()) ;

	}
	
	public void testWhere() throws Exception {
		session.dropWorkspace() ;
		session.newNode().put("age", 20) ;
		session.commit() ;

		assertEquals(0, session.createQuery().where("this.age < 20").find().count()) ;
		assertEquals(0, session.createQuery().where("this.age < this.age").find().count()) ;
		assertEquals(1, session.createQuery().where("this.age <= this.age").find().count()) ;
		assertEquals(0, session.createQuery().where("this.age > this.age").find().count()) ;
		assertEquals(1, session.createQuery().where("this.age >= this.age").find().count()) ;

		assertEquals(0, session.createQuery().where("this.age != this.age").find().count()) ;
	}
	
	public void testBetween2() throws Exception {
		session.dropWorkspace() ;
		session.newNode().put("age", 20) ;
		session.commit() ;

		assertEquals(0, session.createQuery().between("age", 24, 25).find().count()) ;
	}
	
	public void testDate() throws Exception {
		session.dropWorkspace() ;
		session.newNode().put("birthDate", DateUtil.stringToDate("20100101-000000")) ;
		session.commit() ;
		
		session.createQuery().between("birthDate", DateUtil.stringToDate("20111111-000000"), DateUtil.stringToDate("20121111-000000")).find().debugPrint(PageBean.ALL) ;
		Debug.line() ;
		session.createQuery().gt("birthDate", DateUtil.stringToDate("20111111-000000")).lt("birthDate", DateUtil.stringToDate("20121111-000000")).find().debugPrint(PageBean.ALL) ;
	}

	public void testDateLessThen() throws Exception {
		session.dropWorkspace() ;
		session.newNode().put("birthDate", DateUtil.stringToDate("20120101-000000")) ;
		session.commit() ;
		
		assertEquals(0, session.createQuery().lt("birthDate", DateUtil.stringToDate("20111111-000000")).find().count()) ;
		assertEquals(1, session.createQuery().lt("birthDate", DateUtil.stringToDate("20131111-000000")).find().count()) ;
	}

	public void testDateGreaterThen() throws Exception {
		session.dropWorkspace() ;
		session.newNode().put("birthDate", DateUtil.stringToDate("20120101-000000")) ;
		session.commit() ;
		
		assertEquals(1, session.createQuery().gt("birthDate", DateUtil.stringToDate("20111111-000000")).find().count()) ;
		assertEquals(0, session.createQuery().gt("birthDate", DateUtil.stringToDate("20131111-000000")).find().count()) ;
		
	}

}
