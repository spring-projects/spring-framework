package org.springframework.context.support;

import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author Juergen Hoeller
 * @since 25.04.2004
 */
public class ResourceMapFactoryBeanTests extends TestCase {

	public void testResourceMapFactoryBeanWithoutContext() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		Properties props = new Properties();
		props.setProperty("test1", "classpath:org/springframework/context/support/contextA.xml");
		props.setProperty("test2", "classpath:org/springframework/context/support/contextB.xml");
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", props);
		RootBeanDefinition bd = new RootBeanDefinition(ResourceMapFactoryBean.class, pvs);
		beanFactory.registerBeanDefinition("resourceMap", bd);
		Map result = (Map) beanFactory.getBean("resourceMap");
		assertEquals(2, result.size());
		assertTrue(result.get("test1") instanceof ClassPathResource);
		assertTrue(((Resource) result.get("test1")).getDescription().indexOf("contextA.xml") != -1);
		assertTrue(((Resource) result.get("test2")).getDescription().indexOf("contextB.xml") != -1);
	}

	public void testResourceMapFactoryBeanWithContext() {
		StaticApplicationContext context = new StaticApplicationContext() {
			public Resource getResource(String location) {
				return super.getResource("classpath:org/springframework/context/support/context" + location);
			}
		};
		DefaultListableBeanFactory beanFactory = context.getDefaultListableBeanFactory();
		Properties props = new Properties();
		props.setProperty("test1", "A.xml");
		props.setProperty("test2", "B.xml");
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", props);
		RootBeanDefinition bd = new RootBeanDefinition(ResourceMapFactoryBean.class, pvs);
		beanFactory.registerBeanDefinition("resourceMap", bd);
		context.refresh();
		Map result = (Map) beanFactory.getBean("resourceMap");
		assertEquals(2, result.size());
		assertTrue(result.get("test1") instanceof ClassPathResource);
		assertTrue(((Resource) result.get("test1")).getDescription().indexOf("contextA.xml") != -1);
		assertTrue(((Resource) result.get("test2")).getDescription().indexOf("contextB.xml") != -1);
	}

	public void testResourceMapFactoryBeanWithResourceBasePath() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		Properties props = new Properties();
		props.setProperty("test1", "A.xml");
		props.setProperty("test2", "B.xml");
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", props);
		pvs.addPropertyValue("resourceBasePath", "classpath:org/springframework/context/support/context");
		RootBeanDefinition bd = new RootBeanDefinition(ResourceMapFactoryBean.class, pvs);
		beanFactory.registerBeanDefinition("resourceMap", bd);
		Map result = (Map) beanFactory.getBean("resourceMap");
		assertEquals(2, result.size());
		assertTrue(result.get("test1") instanceof ClassPathResource);
		assertTrue(((Resource) result.get("test1")).getDescription().indexOf("contextA.xml") != -1);
		assertTrue(((Resource) result.get("test2")).getDescription().indexOf("contextB.xml") != -1);
	}

}
