/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.xml;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.tests.sample.beans.TestBean;


/**
 * Tests for propagating enclosing beans element defaults to nested beans elements.
 *
 * @author Chris Beams
 */
public class NestedBeansElementAttributeRecursionTests {

	@Test
	public void defaultLazyInit() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("NestedBeansElementAttributeRecursionTests-lazy-context.xml", this.getClass()));

		BeanDefinition foo = bf.getBeanDefinition("foo");
		BeanDefinition bar = bf.getBeanDefinition("bar");
		BeanDefinition baz = bf.getBeanDefinition("baz");
		BeanDefinition biz = bf.getBeanDefinition("biz");
		BeanDefinition buz = bf.getBeanDefinition("buz");

		assertThat(foo.isLazyInit(), is(false));
		assertThat(bar.isLazyInit(), is(true));
		assertThat(baz.isLazyInit(), is(false));
		assertThat(biz.isLazyInit(), is(true));
		assertThat(buz.isLazyInit(), is(true));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void defaultMerge() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("NestedBeansElementAttributeRecursionTests-merge-context.xml", this.getClass()));

		TestBean topLevel = bf.getBean("topLevelConcreteTestBean", TestBean.class);
		// has the concrete child bean values
		assertThat((Iterable<String>) topLevel.getSomeList(), hasItems("charlie", "delta"));
		// but does not merge the parent values
		assertThat((Iterable<String>) topLevel.getSomeList(), not(hasItems("alpha", "bravo")));

		TestBean firstLevel = bf.getBean("firstLevelNestedTestBean", TestBean.class);
		// merges all values
		assertThat((Iterable<String>) firstLevel.getSomeList(),
				hasItems("charlie", "delta", "echo", "foxtrot"));

		TestBean secondLevel = bf.getBean("secondLevelNestedTestBean", TestBean.class);
		// merges all values
		assertThat((Iterable<String>)secondLevel.getSomeList(),
				hasItems("charlie", "delta", "echo", "foxtrot", "golf", "hotel"));
	}

	@Test
	public void defaultAutowireCandidates() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("NestedBeansElementAttributeRecursionTests-autowire-candidates-context.xml", this.getClass()));

		assertThat(bf.getBeanDefinition("fooService").isAutowireCandidate(), is(true));
		assertThat(bf.getBeanDefinition("fooRepository").isAutowireCandidate(), is(true));
		assertThat(bf.getBeanDefinition("other").isAutowireCandidate(), is(false));

		assertThat(bf.getBeanDefinition("barService").isAutowireCandidate(), is(true));
		assertThat(bf.getBeanDefinition("fooController").isAutowireCandidate(), is(false));

		assertThat(bf.getBeanDefinition("bizRepository").isAutowireCandidate(), is(true));
		assertThat(bf.getBeanDefinition("bizService").isAutowireCandidate(), is(false));

		assertThat(bf.getBeanDefinition("bazService").isAutowireCandidate(), is(true));
		assertThat(bf.getBeanDefinition("random").isAutowireCandidate(), is(false));
		assertThat(bf.getBeanDefinition("fooComponent").isAutowireCandidate(), is(false));
		assertThat(bf.getBeanDefinition("fRepository").isAutowireCandidate(), is(false));

		assertThat(bf.getBeanDefinition("aComponent").isAutowireCandidate(), is(true));
		assertThat(bf.getBeanDefinition("someService").isAutowireCandidate(), is(false));
	}

	@Test
	public void initMethod() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("NestedBeansElementAttributeRecursionTests-init-destroy-context.xml", this.getClass()));

		InitDestroyBean beanA = bf.getBean("beanA", InitDestroyBean.class);
		InitDestroyBean beanB = bf.getBean("beanB", InitDestroyBean.class);
		InitDestroyBean beanC = bf.getBean("beanC", InitDestroyBean.class);
		InitDestroyBean beanD = bf.getBean("beanD", InitDestroyBean.class);

		assertThat(beanA.initMethod1Called, is(true));
		assertThat(beanB.initMethod2Called, is(true));
		assertThat(beanC.initMethod3Called, is(true));
		assertThat(beanD.initMethod2Called, is(true));

		bf.destroySingletons();

		assertThat(beanA.destroyMethod1Called, is(true));
		assertThat(beanB.destroyMethod2Called, is(true));
		assertThat(beanC.destroyMethod3Called, is(true));
		assertThat(beanD.destroyMethod2Called, is(true));
	}

}

class InitDestroyBean {
	boolean initMethod1Called;
	boolean initMethod2Called;
	boolean initMethod3Called;

	boolean destroyMethod1Called;
	boolean destroyMethod2Called;
	boolean destroyMethod3Called;

	void initMethod1() { this.initMethod1Called = true; }
	void initMethod2() { this.initMethod2Called = true; }
	void initMethod3() { this.initMethod3Called = true; }

	void destroyMethod1() { this.destroyMethod1Called = true; }
	void destroyMethod2() { this.destroyMethod2Called = true; }
	void destroyMethod3() { this.destroyMethod3Called = true; }
}
