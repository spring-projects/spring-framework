package org.springframework.beans;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

public class Spr6972Tests {
	@Test
	public void repro() {
		BeanFactory bf = new XmlBeanFactory(new ClassPathResource("Spr6972Tests-context.xml", this.getClass()));
		TestSpringBean bean = bf.getBean(TestSpringBean.class);
		assertTrue(bean.bool);
		assertNotNull(bean.map);
	}
}

class TestSpringBean {
	boolean bool;
	Map<String, String> map;

	public TestSpringBean(boolean bool, Map<String, String> map) {
		this.bool = bool;
		this.map = map;
	}

	public TestSpringBean(Map<String, String> map) {
		this(true, map);
	}
}
