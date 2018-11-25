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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ResolvableType;

import static org.junit.Assert.assertEquals;

/**
 * @author Melnikov Nikita
 */
public class SPR17524Tests {

	@Test
	public void testNonLazyWithGeneric() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Configuration.class);
		ResolvableType type = ResolvableType.forClassWithGenerics(List.class, String.class);
		assertEquals(context.getBeanNamesForType(type).length, 1);
	}

	@Test
	public void testNonLazyWithGenericMultipleCalls() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Configuration.class);
		ResolvableType type = ResolvableType.forClassWithGenerics(List.class, String.class);
		assertEquals(context.getBeanNamesForType(type).length, 1);
		assertEquals(context.getBeanNamesForType(type).length, 1);
		assertEquals(context.getBeanNamesForType(type).length, 1);
	}

	@Test
	public void testLazyWithGenericMultipleCalls() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Configuration.class);
		ResolvableType type = ResolvableType.forClassWithGenerics(List.class, Integer.class);
		assertEquals(context.getBeanNamesForType(type).length, 1);
		assertEquals(context.getBeanNamesForType(type).length, 1);
		assertEquals(context.getBeanNamesForType(type).length, 1);
	}

	static class Configuration {

		@Bean
		public List<String> stringListBean() {
			return new ArrayList<>();
		}

		@Bean
		@Lazy
		public List<Integer> lazyIntegerListBean() {
			return new ArrayList<>();
		}
	}
}
