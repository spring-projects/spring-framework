package org.springframework.beans.factory.xml;

import junit.framework.TestCase;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.TestBean;

import java.util.List;
import java.util.Set;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Rob Harrop
 */
public class CollectionsWithDefaultTypesTests extends TestCase {

	private XmlBeanFactory beanFactory;

	protected void setUp() throws Exception {
		this.beanFactory = new XmlBeanFactory(new ClassPathResource("collectionsWithDefaultTypes.xml", getClass()));
	}

	public void testListHasDefaultType() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		List list = bean.getSomeList();
		for (int i = 0; i < list.size(); i++) {
			Object o = list.get(i);
			assertEquals("Value type is incorrect", Integer.class, o.getClass());
		}
	}

	public void testSetHasDefaultType() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		Set set = bean.getSomeSet();
		Iterator iterator = set.iterator();
		while (iterator.hasNext()) {
			Object o =  iterator.next();
			assertEquals("Value type is incorrect", Integer.class, o.getClass());
		}
	}

	public void testMapHasDefaultKeyAndValueType() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		assertMap(bean.getSomeMap());
	}

	public void testMapWithNestedElementsHasDefaultKeyAndValueType() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean2");
		assertMap(bean.getSomeMap());
	}

	private void assertMap(Map map) {
		for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			assertEquals("Key type is incorrect", Integer.class, entry.getKey().getClass());
			assertEquals("Value type is incorrect", Boolean.class, entry.getValue().getClass());
		}
	}
}
