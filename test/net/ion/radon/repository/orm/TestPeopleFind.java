package net.ion.radon.repository.orm;

import java.lang.reflect.Constructor;
import java.util.List;

import net.ion.framework.util.Debug;
import net.ion.radon.core.PageBean;
import net.ion.radon.repository.Node;
import net.ion.radon.repository.PropertyQuery;
import net.ion.radon.repository.TestBaseRepository;

import org.apache.commons.beanutils.ConstructorUtils;

public class TestPeopleFind extends TestBaseRepository {

	private PeopleManager<People> pm;

	@Override
	protected void setUp() throws Exception {
		// TODO Auto-generated method stub
		super.setUp();
		session.changeWorkspace("peoples");
		session.dropWorkspace();
		pm = new PeopleManager<People>(session);
		pm.removeAll() ;
	}

	public void xtestConstructor() throws Exception {
		Constructor[] cons = People.class.getConstructors() ;
		for (Constructor con : cons) {
			Class[] paramclz = con.getParameterTypes() ;
			for (Class clz : paramclz) {
				
			}
		}
		boolean acce = ConstructorUtils.getAccessibleConstructor(People.class, new Class[0]) != null;
		assertEquals(true, acce) ;
	}
	
	public void testToNode() throws Exception {
		pm.save(People.create("bleu", 20, "seoul", "white"));
		pm.save(People.create("hero", 30, "busan", "black"));
		pm.save(People.create("jin", 40, "busan", "red"));
		pm.save(People.create("bee", 29, "busan", "red"));

		People bleu = pm.findById("bleu");
		assertEquals("bleu", bleu.getId());
		assertEquals(20, bleu.getAge());
		assertEquals("seoul", bleu.getAddress());
		assertEquals("white", bleu.getFavoriateColor());

		Node node = pm.toNode(bleu);
		assertEquals("bleu", node.get("userId"));
		assertEquals("seoul", node.get("address"));
		assertEquals("white", node.get("fcolor"));
		assertEquals(20, node.get("age"));

		List<People> peoples = pm.findByAddress("seoul");
		assertEquals(1, peoples.size());
		People f = peoples.get(0);

		assertEquals("bleu", f.getId());
		assertEquals(20, f.getAge());

		List<People> busanPeople = pm.find(PropertyQuery.create().eq("address", "busan").gt("age", 20)).ascending("age").descending("userId").toList(PageBean.create(2, 1));
		for (People peo : busanPeople) {
			Debug.debug(peo);
		}
	}
}