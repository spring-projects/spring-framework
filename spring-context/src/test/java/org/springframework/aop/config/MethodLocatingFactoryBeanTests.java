/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void isSingleton() {
		assertThat(factory.isSingleton()).isTrue();
	}

	@Test
	void getObjectType() {
		assertThat(factory.getObjectType()).isEqualTo(Method.class);
	}

	@Test
	void withNullTargetBeanName() {
		factory.setMethodName("toString()");
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
	}

	@Test
	void withEmptyTargetBeanName() {
		factory.setTargetBeanName("");
		factory.setMethodName("toString()");
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
	}

	@Test
	void withNullTargetMethodName() {
		factory.setTargetBeanName(BEAN_NAME);
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
	}

	@Test
	void withEmptyTargetMethodName() {
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("");
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
	}

	@Test
	void whenTargetBeanClassCannotBeResolved() {
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("toString()");
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
		verify(beanFactory).getType(BEAN_NAME);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void sunnyDayPath() throws Exception {
		given(beanFactory.getType(BEAN_NAME)).willReturn((Class)String.class);
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("toString()");
		factory.setBeanFactory(beanFactory);
		Object result = factory.getObject();
		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Method.class);
		Method method = (Method) result;
		assertThat(method.invoke("Bingo")).isEqualTo("Bingo");
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void whereMethodCannotBeResolved() {
		given(beanFactory.getType(BEAN_NAME)).willReturn((Class)String.class);
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("loadOfOld()");
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setBeanFactory(beanFactory));
	}

}
