/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aop.config;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
class MethodLocatingFactoryBeanTests {

	private static final String BEAN_NAME = "string";
	private MethodLocatingFactoryBean factory = new MethodLocatingFactoryBean();
	private BeanFactory beanFactory = mock();


	@Test
	void testIsSingleton() {
		assertThat(factory.isSingleton()).isTrue();
	}

	@Test
	void testGetObjectType() {
		assertThat(factory.getObjectType()).isEqualTo(Method.class);
	}

	@Test
	void testWithNullTargetBeanName() {
		factory.setMethodName("toString()");
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
	}

	@Test
	void testWithEmptyTargetBeanName() {
		factory.setTargetBeanName("");
		factory.setMethodName("toString()");
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
	}

	@Test
	void testWithNullTargetMethodName() {
		factory.setTargetBeanName(BEAN_NAME);
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
	}

	@Test
	void testWithEmptyTargetMethodName() {
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("");
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
	}

	@Test
	void testWhenTargetBeanClassCannotBeResolved() {
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("toString()");
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
		verify(beanFactory).getType(BEAN_NAME);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void testSunnyDayPath() throws Exception {
		given(beanFactory.getType(BEAN_NAME)).willReturn((Class)String.class);
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("toString()");
		factory.setBeanFactory(beanFactory);
		Object result = factory.getObject();
		assertThat(result).isNotNull();
		boolean condition = result instanceof Method;
		assertThat(condition).isTrue();
		Method method = (Method) result;
		assertThat(method.invoke("Bingo")).isEqualTo("Bingo");
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void testWhereMethodCannotBeResolved() {
		given(beanFactory.getType(BEAN_NAME)).willReturn((Class)String.class);
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("loadOfOld()");
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
	}

}
