/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.context.annotation;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import example.scannable.DefaultNamedComponent;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Chris Beams
 */
public class AnnotationBeanNameGeneratorTests {

	private AnnotationBeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();


	@Test
	public void testGenerateBeanNameWithNamedComponent() {
		BeanDefinitionRegistry registry = createMock(BeanDefinitionRegistry.class);
		replay(registry);

		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(ComponentWithName.class);
		String beanName = this.beanNameGenerator.generateBeanName(bd, registry);
		assertNotNull("The generated beanName must *never* be null.", beanName);
		assertTrue("The generated beanName must *never* be blank.", StringUtils.hasText(beanName));
		assertEquals("walden", beanName);

		verify(registry);
	}

	@Test
	public void testGenerateBeanNameWithDefaultNamedComponent() {
		BeanDefinitionRegistry registry = createMock(BeanDefinitionRegistry.class);
		replay(registry);

		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(DefaultNamedComponent.class);
		String beanName = this.beanNameGenerator.generateBeanName(bd, registry);
		assertNotNull("The generated beanName must *never* be null.", beanName);
		assertTrue("The generated beanName must *never* be blank.", StringUtils.hasText(beanName));
		assertEquals("thoreau", beanName);

		verify(registry);
	}

	@Test
	public void testGenerateBeanNameWithNamedComponentWhereTheNameIsBlank() {
		BeanDefinitionRegistry registry = createMock(BeanDefinitionRegistry.class);
		replay(registry);

		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(ComponentWithBlankName.class);
		String beanName = this.beanNameGenerator.generateBeanName(bd, registry);
		assertNotNull("The generated beanName must *never* be null.", beanName);
		assertTrue("The generated beanName must *never* be blank.", StringUtils.hasText(beanName));

		String expectedGeneratedBeanName = this.beanNameGenerator.buildDefaultBeanName(bd);

		assertEquals(expectedGeneratedBeanName, beanName);

		verify(registry);
	}

	@Test
	public void testGenerateBeanNameWithAnonymousComponentYieldsGeneratedBeanName() {
		BeanDefinitionRegistry registry = createMock(BeanDefinitionRegistry.class);
		replay(registry);

		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(AnonymousComponent.class);
		String beanName = this.beanNameGenerator.generateBeanName(bd, registry);
		assertNotNull("The generated beanName must *never* be null.", beanName);
		assertTrue("The generated beanName must *never* be blank.", StringUtils.hasText(beanName));

		String expectedGeneratedBeanName = this.beanNameGenerator.buildDefaultBeanName(bd);

		assertEquals(expectedGeneratedBeanName, beanName);

		verify(registry);
	}


	@Component("walden")
	private static class ComponentWithName {
	}


	@Component(" ")
	private static class ComponentWithBlankName {
	}


	@Component
	private static class AnonymousComponent {
	}

}
