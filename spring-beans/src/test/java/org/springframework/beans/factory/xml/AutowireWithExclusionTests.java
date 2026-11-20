/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
class AutowireWithExclusionTests {

	@Test
	void byTypeAutowireWithAutoSelfExclusion() {
		CountingFactory.reset();
		DefaultListableBeanFactory beanFactory = getBeanFactory("autowire-with-exclusion.xml");
		beanFactory.preInstantiateSingletons();
		TestBean rob = (TestBean) beanFactory.getBean("rob");
		TestBean sally = (TestBean) beanFactory.getBean("sally");
		assertThat(rob.getSpouse()).isEqualTo(sally);
		assertThat(CountingFactory.getFactoryBeanInstanceCount()).isEqualTo(1);
	}

	@Test
	void byTypeAutowireWithExclusion() {
		CountingFactory.reset();
		DefaultListableBeanFactory beanFactory = getBeanFactory("autowire-with-exclusion.xml");
		beanFactory.preInstantiateSingletons();
		TestBean rob = (TestBean) beanFactory.getBean("rob");
		assertThat(rob.getSomeProperties().getProperty("name")).isEqualTo("props1");
		assertThat(CountingFactory.getFactoryBeanInstanceCount()).isEqualTo(1);
	}

	@Test
	void byTypeAutowireWithExclusionInParentFactory() {
		CountingFactory.reset();
		DefaultListableBeanFactory parent = getBeanFactory("autowire-with-exclusion.xml");
		parent.preInstantiateSingletons();
		DefaultListableBeanFactory child = new DefaultListableBeanFactory(parent);
		RootBeanDefinition robDef = new RootBeanDefinition(TestBean.class);
		robDef.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		robDef.getPropertyValues().add("spouse", new RuntimeBeanReference("sally"));
		child.registerBeanDefinition("rob2", robDef);
		TestBean rob = (TestBean) child.getBean("rob2");
		assertThat(rob.getSomeProperties().getProperty("name")).isEqualTo("props1");
		assertThat(CountingFactory.getFactoryBeanInstanceCount()).isEqualTo(1);
	}

	@Test
	void byTypeAutowireWithPrimaryInParentFactory() {
		CountingFactory.reset();
		DefaultListableBeanFactory parent = getBeanFactory("autowire-with-exclusion.xml");
		parent.getBeanDefinition("props1").setPrimary(true);
		parent.preInstantiateSingletons();
		DefaultListableBeanFactory child = new DefaultListableBeanFactory(parent);
		RootBeanDefinition robDef = new RootBeanDefinition(TestBean.class);
		robDef.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		robDef.getPropertyValues().add("spouse", new RuntimeBeanReference("sally"));
		child.registerBeanDefinition("rob2", robDef);
		RootBeanDefinition propsDef = new RootBeanDefinition(PropertiesFactoryBean.class);
		propsDef.getPropertyValues().add("properties", "name=props3");
		child.registerBeanDefinition("props3", propsDef);
		TestBean rob = (TestBean) child.getBean("rob2");
		assertThat(rob.getSomeProperties().getProperty("name")).isEqualTo("props1");
		assertThat(CountingFactory.getFactoryBeanInstanceCount()).isEqualTo(1);
	}

	@Test
	void byTypeAutowireWithPrimaryOverridingParentFactory() {
		CountingFactory.reset();
		DefaultListableBeanFactory parent = getBeanFactory("autowire-with-exclusion.xml");
		parent.preInstantiateSingletons();
		DefaultListableBeanFactory child = new DefaultListableBeanFactory(parent);
		RootBeanDefinition robDef = new RootBeanDefinition(TestBean.class);
		robDef.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		robDef.getPropertyValues().add("spouse", new RuntimeBeanReference("sally"));
		child.registerBeanDefinition("rob2", robDef);
		RootBeanDefinition propsDef = new RootBeanDefinition(PropertiesFactoryBean.class);
		propsDef.getPropertyValues().add("properties", "name=props3");
		propsDef.setPrimary(true);
		child.registerBeanDefinition("props3", propsDef);
		TestBean rob = (TestBean) child.getBean("rob2");
		assertThat(rob.getSomeProperties().getProperty("name")).isEqualTo("props3");
		assertThat(CountingFactory.getFactoryBeanInstanceCount()).isEqualTo(1);
	}

	@Test
	void byTypeAutowireWithPrimaryInParentAndChild() {
		CountingFactory.reset();
		DefaultListableBeanFactory parent = getBeanFactory("autowire-with-exclusion.xml");
		parent.getBeanDefinition("props1").setPrimary(true);
		parent.preInstantiateSingletons();
		DefaultListableBeanFactory child = new DefaultListableBeanFactory(parent);
		RootBeanDefinition robDef = new RootBeanDefinition(TestBean.class);
		robDef.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		robDef.getPropertyValues().add("spouse", new RuntimeBeanReference("sally"));
		child.registerBeanDefinition("rob2", robDef);
		RootBeanDefinition propsDef = new RootBeanDefinition(PropertiesFactoryBean.class);
		propsDef.getPropertyValues().add("properties", "name=props3");
		propsDef.setPrimary(true);
		child.registerBeanDefinition("props3", propsDef);
		TestBean rob = (TestBean) child.getBean("rob2");
		assertThat(rob.getSomeProperties().getProperty("name")).isEqualTo("props3");
		assertThat(CountingFactory.getFactoryBeanInstanceCount()).isEqualTo(1);
	}

	@Test
	void byTypeAutowireWithInclusion() {
		CountingFactory.reset();
		DefaultListableBeanFactory beanFactory = getBeanFactory("autowire-with-inclusion.xml");
		beanFactory.preInstantiateSingletons();
		TestBean rob = (TestBean) beanFactory.getBean("rob");
		assertThat(rob.getSomeProperties().getProperty("name")).isEqualTo("props1");
		assertThat(CountingFactory.getFactoryBeanInstanceCount()).isEqualTo(1);
	}

	@Test
	void byTypeAutowireWithSelectiveInclusion() {
		CountingFactory.reset();
		DefaultListableBeanFactory beanFactory = getBeanFactory("autowire-with-selective-inclusion.xml");
		beanFactory.preInstantiateSingletons();
		TestBean rob = (TestBean) beanFactory.getBean("rob");
		assertThat(rob.getSomeProperties().getProperty("name")).isEqualTo("props1");
		assertThat(CountingFactory.getFactoryBeanInstanceCount()).isEqualTo(1);
	}

	@Test
	void constructorAutowireWithAutoSelfExclusion() {
		DefaultListableBeanFactory beanFactory = getBeanFactory("autowire-constructor-with-exclusion.xml");
		TestBean rob = (TestBean) beanFactory.getBean("rob");
		TestBean sally = (TestBean) beanFactory.getBean("sally");
		assertThat(rob.getSpouse()).isEqualTo(sally);
		TestBean rob2 = (TestBean) beanFactory.getBean("rob");
		assertThat(rob2).isEqualTo(rob);
		assertThat(rob2).isNotSameAs(rob);
		assertThat(rob2.getSpouse()).isEqualTo(rob.getSpouse());
		assertThat(rob2.getSpouse()).isNotSameAs(rob.getSpouse());
	}

	@Test
	void constructorAutowireWithExclusion() {
		DefaultListableBeanFactory beanFactory = getBeanFactory("autowire-constructor-with-exclusion.xml");
		TestBean rob = (TestBean) beanFactory.getBean("rob");
		assertThat(rob.getSomeProperties().getProperty("name")).isEqualTo("props1");
	}

	private DefaultListableBeanFactory getBeanFactory(String configPath) {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource(configPath, getClass()));
		return bf;
	}

}
