/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.serviceloader;

import java.util.List;
import java.util.ServiceLoader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class ServiceLoaderTests {

	@BeforeAll
	static void assumeDocumentBuilderFactoryCanBeLoaded() {
		assumeThat(ServiceLoader.load(DocumentBuilderFactory.class).iterator()).hasNext();
	}

	@Test
	void serviceLoaderFactoryBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition bd = new RootBeanDefinition(ServiceLoaderFactoryBean.class);
		bd.getPropertyValues().add("serviceType", DocumentBuilderFactory.class.getName());
		bf.registerBeanDefinition("service", bd);
		ServiceLoader<?> serviceLoader = (ServiceLoader<?>) bf.getBean("service");
		assertThat(serviceLoader).element(0).isInstanceOf(DocumentBuilderFactory.class);
	}

	@Test
	void serviceFactoryBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition bd = new RootBeanDefinition(ServiceFactoryBean.class);
		bd.getPropertyValues().add("serviceType", DocumentBuilderFactory.class.getName());
		bf.registerBeanDefinition("service", bd);
		assertThat(bf.getBean("service")).isInstanceOf(DocumentBuilderFactory.class);
	}

	@Test
	void serviceListFactoryBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		RootBeanDefinition bd = new RootBeanDefinition(ServiceListFactoryBean.class);
		bd.getPropertyValues().add("serviceType", DocumentBuilderFactory.class.getName());
		bf.registerBeanDefinition("service", bd);
		List<?> serviceList = (List<?>) bf.getBean("service");
		assertThat(serviceList).element(0).isInstanceOf(DocumentBuilderFactory.class);
	}

}
