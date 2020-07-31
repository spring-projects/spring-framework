/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

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

		assertLazyInits(bf);
	}

	@Test
	public void defaultLazyInitWithNonValidatingParser() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(bf);
		xmlBeanDefinitionReader.setValidating(false);
		xmlBeanDefinitionReader.loadBeanDefinitions(
				new ClassPathResource("NestedBeansElementAttributeRecursionTests-lazy-context.xml", this.getClass()));

		assertLazyInits(bf);
	}

	private void assertLazyInits(DefaultListableBeanFactory bf) {
		BeanDefinition foo = bf.getBeanDefinition("foo");
		BeanDefinition bar = bf.getBeanDefinition("bar");
		BeanDefinition baz = bf.getBeanDefinition("baz");
		BeanDefinition biz = bf.getBeanDefinition("biz");
		BeanDefinition buz = bf.getBeanDefinition("buz");

		assertThat(foo.isLazyInit()).isFalse();
		assertThat(bar.isLazyInit()).isTrue();
		assertThat(baz.isLazyInit()).isFalse();
		assertThat(biz.isLazyInit()).isTrue();
		assertThat(buz.isLazyInit()).isTrue();
	}

	@Test
	public void defaultMerge() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("NestedBeansElementAttributeRecursionTests-merge-context.xml", this.getClass()));

		assertMerge(bf);
	}

	@Test
	public void defaultMergeWithNonValidatingParser() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(bf);
		xmlBeanDefinitionReader.setValidating(false);
		xmlBeanDefinitionReader.loadBeanDefinitions(
				new ClassPathResource("NestedBeansElementAttributeRecursionTests-merge-context.xml", this.getClass()));

		assertMerge(bf);
	}

	@SuppressWarnings("unchecked")
	private void assertMerge(DefaultListableBeanFactory bf) {
		TestBean topLevel = bf.getBean("topLevelConcreteTestBean", TestBean.class);
		// has the concrete child bean values
		assertThat((Iterable<String>) topLevel.getSomeList()).contains("charlie", "delta");
		// but does not merge the parent values
		assertThat((Iterable<String>) topLevel.getSomeList()).doesNotContain("alpha", "bravo");

		TestBean firstLevel = bf.getBean("firstLevelNestedTestBean", TestBean.class);
		// merges all values
		assertThat((Iterable<String>) firstLevel.getSomeList()).contains(
				"charlie", "delta", "echo", "foxtrot");

		TestBean secondLevel = bf.getBean("secondLevelNestedTestBean", TestBean.class);
		// merges all values
		assertThat((Iterable<String>)secondLevel.getSomeList()).contains(
				"charlie", "delta", "echo", "foxtrot", "golf", "hotel");
	}

	@Test
	public void defaultAutowireCandidates() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource("NestedBeansElementAttributeRecursionTests-autowire-candidates-context.xml", this.getClass()));

		assertAutowireCandidates(bf);
	}

	@Test
	public void defaultAutowireCandidatesWithNonValidatingParser() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(bf);
		xmlBeanDefinitionReader.setValidating(false);
		xmlBeanDefinitionReader.loadBeanDefinitions(
				new ClassPathResource("NestedBeansElementAttributeRecursionTests-autowire-candidates-context.xml", this.getClass()));

		assertAutowireCandidates(bf);
	}

	private void assertAutowireCandidates(DefaultListableBeanFactory bf) {
		assertThat(bf.getBeanDefinition("fooService").isAutowireCandidate()).isTrue();
		assertThat(bf.getBeanDefinition("fooRepository").isAutowireCandidate()).isTrue();
		assertThat(bf.getBeanDefinition("other").isAutowireCandidate()).isFalse();

		assertThat(bf.getBeanDefinition("barService").isAutowireCandidate()).isTrue();
		assertThat(bf.getBeanDefinition("fooController").isAutowireCandidate()).isFalse();

		assertThat(bf.getBeanDefinition("bizRepository").isAutowireCandidate()).isTrue();
		assertThat(bf.getBeanDefinition("bizService").isAutowireCandidate()).isFalse();

		assertThat(bf.getBeanDefinition("bazService").isAutowireCandidate()).isTrue();
		assertThat(bf.getBeanDefinition("random").isAutowireCandidate()).isFalse();
		assertThat(bf.getBeanDefinition("fooComponent").isAutowireCandidate()).isFalse();
		assertThat(bf.getBeanDefinition("fRepository").isAutowireCandidate()).isFalse();

		assertThat(bf.getBeanDefinition("aComponent").isAutowireCandidate()).isTrue();
		assertThat(bf.getBeanDefinition("someService").isAutowireCandidate()).isFalse();
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

		assertThat(beanA.initMethod1Called).isTrue();
		assertThat(beanB.initMethod2Called).isTrue();
		assertThat(beanC.initMethod3Called).isTrue();
		assertThat(beanD.initMethod2Called).isTrue();

		bf.destroySingletons();

		assertThat(beanA.destroyMethod1Called).isTrue();
		assertThat(beanB.destroyMethod2Called).isTrue();
		assertThat(beanC.destroyMethod3Called).isTrue();
		assertThat(beanD.destroyMethod2Called).isTrue();
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
