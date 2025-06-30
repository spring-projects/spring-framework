/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.context.support;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

/**
 * Unit tests cornering SPR-7502.
 *
 * @author Chris Beams
 */
class SerializableBeanFactoryMemoryLeakTests {

	/**
	 * Defensively zero-out static factory count - other tests
	 * may have misbehaved before us.
	 */
	@BeforeAll
	@AfterAll
	static void zeroOutFactoryCount() throws Exception {
		getSerializableFactoryMap().clear();
	}

	@Test
	void genericContext() throws Exception {
		assertFactoryCountThroughoutLifecycle(new GenericApplicationContext());
	}

	@Test
	void abstractRefreshableContext() throws Exception {
		assertFactoryCountThroughoutLifecycle(new ClassPathXmlApplicationContext());
	}

	@Test
	void genericContextWithMisconfiguredBean() throws Exception {
		GenericApplicationContext ctx = new GenericApplicationContext();
		registerMisconfiguredBeanDefinition(ctx);
		assertFactoryCountThroughoutLifecycle(ctx);
	}

	@Test
	void abstractRefreshableContextWithMisconfiguredBean() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext() {
			@Override
			protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
				super.customizeBeanFactory(beanFactory);
				registerMisconfiguredBeanDefinition(beanFactory);
			}
		};
		assertFactoryCountThroughoutLifecycle(ctx);
	}

	private void assertFactoryCountThroughoutLifecycle(ConfigurableApplicationContext ctx) throws Exception {
		assertThat(serializableFactoryCount()).isEqualTo(0);
		try {
			ctx.refresh();
			assertThat(serializableFactoryCount()).isEqualTo(1);
			ctx.close();
		}
		catch (BeanCreationException ex) {
			// ignore - this is expected on refresh() for failure case tests
		}
		finally {
			assertThat(serializableFactoryCount()).isEqualTo(0);
		}
	}

	private void registerMisconfiguredBeanDefinition(BeanDefinitionRegistry registry) {
		registry.registerBeanDefinition("misconfigured",
			rootBeanDefinition(Object.class).addPropertyValue("nonexistent", "bogus")
				.getBeanDefinition());
	}

	private int serializableFactoryCount() throws Exception {
		Map<?, ?> map = getSerializableFactoryMap();
		return map.size();
	}

	private static Map<?, ?> getSerializableFactoryMap() throws Exception {
		Field field = DefaultListableBeanFactory.class.getDeclaredField("serializableFactories");
		field.setAccessible(true);
		return (Map<?, ?>) field.get(DefaultListableBeanFactory.class);
	}

}
