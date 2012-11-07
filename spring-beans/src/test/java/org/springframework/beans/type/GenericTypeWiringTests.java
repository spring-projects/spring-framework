/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.beans.type;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * @author Oliver Gierke
 */
public class GenericTypeWiringTests {

	DefaultListableBeanFactory factory;

	@Before
	public void setUp() {

		factory = new DefaultListableBeanFactory();
		factory.registerSingleton("stringType", new StringType());
		factory.registerSingleton("integerType", new IntegerType());

		AutowiredAnnotationBeanPostProcessor processor = new AutowiredAnnotationBeanPostProcessor();
		processor.setBeanFactory(factory);
		factory.addBeanPostProcessor(processor);
	}

	@Test
	public void injectsGenericProperties() {

		factory.registerBeanDefinition("client", new RootBeanDefinition(PropertyWiredClient.class));

		PropertyWiredClient client = factory.getBean(PropertyWiredClient.class);
		assertThat(client.integerType, is(instanceOf(IntegerType.class)));
		assertThat(client.stringType, is(instanceOf(StringType.class)));
		assertThat(client.stringTypes.size(), is(1));
	}

	@Test
	public void injectsGenericConstructorArguments() {

		factory.registerBeanDefinition("constructorClient", new RootBeanDefinition(ConstructorWiredClient.class));

		ConstructorWiredClient client = factory.getBean(ConstructorWiredClient.class);
		assertThat(client.integerType, is(instanceOf(IntegerType.class)));
		assertThat(client.stringType, is(instanceOf(StringType.class)));
		assertThat(client.stringTypes.size(), is(1));
	}

	@Test
	public void infersGenericPropertyFromSuperClass() {

		factory.registerBeanDefinition("client", new RootBeanDefinition(GenericStringClient.class));
		GenericStringClient client = factory.getBean(GenericStringClient.class);

		assertThat(client.property, is(instanceOf(StringType.class)));
		assertThat(client.subClassProperty, is(instanceOf(StringType.class)));
	}

	@Test
	public void injectsSubtypeButNotConreteReference() {

		factory.registerBeanDefinition("client", new RootBeanDefinition(GenericNumberClient.class));
		GenericNumberClient client = factory.getBean(GenericNumberClient.class);

		assertThat(client.subClassProperty, is(instanceOf(IntegerType.class)));
		assertThat(client.property, is(nullValue()));
	}
	
	@Test
	public void doesNotInjectSubTypeIntoGenericTypeWithParentTypeParameter() {

		factory.registerBeanDefinition("client", new RootBeanDefinition(GenericIntegerClient.class));
		GenericIntegerClient client = factory.getBean(GenericIntegerClient.class);

		assertThat(client.subClassProperty, is(instanceOf(IntegerType.class)));
		assertThat(client.property, is(instanceOf(IntegerType.class)));
	}

	interface GenericType<T> {

	}

	static class StringType implements GenericType<String> {

	}

	static class IntegerType implements GenericType<Integer> {

	}

	static abstract class GenericClient<T> {

		@Autowired
		GenericType<? extends T> subClassProperty;

		@Autowired(required = false)
		GenericType<T> property;
	}

	static class GenericStringClient extends GenericClient<String> {

	}

	static class GenericNumberClient extends GenericClient<Number> {

	}

	static class GenericIntegerClient extends GenericClient<Integer> {

	}

	static class PropertyWiredClient {

		@Autowired
		GenericType<String> stringType;

		@Autowired
		GenericType<Integer> integerType;

		@Autowired
		List<GenericType<String>> stringTypes;

		GenericType<GenericType<String>> wrappedStringType;
	}

	static class ConstructorWiredClient {

		GenericType<String> stringType;
		GenericType<Integer> integerType;
		List<GenericType<String>> stringTypes;

		@Autowired
		public ConstructorWiredClient(GenericType<String> stringType, GenericType<Integer> integerType,
				List<GenericType<String>> stringTypes) {

			this.integerType = integerType;
			this.stringType = stringType;
			this.stringTypes = stringTypes;
		}
	}
}
