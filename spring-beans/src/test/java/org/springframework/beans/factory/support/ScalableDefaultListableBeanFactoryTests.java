/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.AbstractDefaultListableBeanFactoryTests;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test class for the {@link ScalableDefaultListableBeanFactory}.
 * Tests for properties population and autowire behavior
 *
 * @author Rahul Shinde
 */
public class ScalableDefaultListableBeanFactoryTests extends AbstractDefaultListableBeanFactoryTests {

	public DefaultListableBeanFactory getInstance() {
		return new ScalableDefaultListableBeanFactory();
	}

	public DefaultListableBeanFactory getInstance(@Nullable BeanFactory parentBeanFactory) {
		return new ScalableDefaultListableBeanFactory(parentBeanFactory);
	}

	interface IA {
	}

	interface IB {
	}

	interface IAB extends IA, IB {
	}

	private class ClassA implements IA {
	}

	private class ClassAB implements IAB {
	}

	private class ClassAWithB implements IA, IB {
	}

	@Test
	public void testGetSuperTypes() {
		Function<Class<?>, Set<Class<?>>> superTypeMapper = ScalableDefaultListableBeanFactory::getSuperTypes;

		Set<Class<?>> clazzes;
		clazzes = superTypeMapper.apply(IA.class);
		assertEquals(1, clazzes.size());
		assertTrue(clazzes.contains(Object.class));

		clazzes = superTypeMapper.apply(ClassA.class);
		assertEquals(2, clazzes.size());
		assertTrue(clazzes.contains(IA.class));

		clazzes = superTypeMapper.apply(ClassAB.class);
		assertEquals(4, clazzes.size());
		List<Class<?>> resultAB = new java.util.ArrayList<>();
		Collections.addAll(resultAB, IA.class, IB.class, IAB.class);
		assertTrue(clazzes.containsAll(resultAB));

		clazzes = superTypeMapper.apply(ClassAWithB.class);
		assertEquals(3, clazzes.size());
		List<Class<?>> resultAWB = new java.util.ArrayList<>();
		Collections.addAll(resultAWB, IA.class, IB.class, Object.class);
		assertTrue(clazzes.containsAll(resultAWB));
	}
}
