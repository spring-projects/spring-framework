/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import example.scannable.DefaultNamedComponent;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AnnotationBeanNameGenerator}.
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Chris Beams
 * @author Sam Brannen
 */
public class AnnotationBeanNameGeneratorTests {

	private AnnotationBeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();


	@Test
	public void generateBeanNameWithNamedComponent() {
		BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(ComponentWithName.class);
		String beanName = this.beanNameGenerator.generateBeanName(bd, registry);
		assertThat(beanName).as("The generated beanName must *never* be null.").isNotNull();
		assertThat(StringUtils.hasText(beanName)).as("The generated beanName must *never* be blank.").isTrue();
		assertThat(beanName).isEqualTo("walden");
	}

	@Test
	public void generateBeanNameWithDefaultNamedComponent() {
		BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(DefaultNamedComponent.class);
		String beanName = this.beanNameGenerator.generateBeanName(bd, registry);
		assertThat(beanName).as("The generated beanName must *never* be null.").isNotNull();
		assertThat(StringUtils.hasText(beanName)).as("The generated beanName must *never* be blank.").isTrue();
		assertThat(beanName).isEqualTo("thoreau");
	}

	@Test
	public void generateBeanNameWithNamedComponentWhereTheNameIsBlank() {
		BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(ComponentWithBlankName.class);
		String beanName = this.beanNameGenerator.generateBeanName(bd, registry);
		assertThat(beanName).as("The generated beanName must *never* be null.").isNotNull();
		assertThat(StringUtils.hasText(beanName)).as("The generated beanName must *never* be blank.").isTrue();
		String expectedGeneratedBeanName = this.beanNameGenerator.buildDefaultBeanName(bd);
		assertThat(beanName).isEqualTo(expectedGeneratedBeanName);
	}

	@Test
	public void generateBeanNameWithAnonymousComponentYieldsGeneratedBeanName() {
		BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(AnonymousComponent.class);
		String beanName = this.beanNameGenerator.generateBeanName(bd, registry);
		assertThat(beanName).as("The generated beanName must *never* be null.").isNotNull();
		assertThat(StringUtils.hasText(beanName)).as("The generated beanName must *never* be blank.").isTrue();
		String expectedGeneratedBeanName = this.beanNameGenerator.buildDefaultBeanName(bd);
		assertThat(beanName).isEqualTo(expectedGeneratedBeanName);
	}

	@Test
	public void generateBeanNameFromMetaComponentWithStringValue() {
		BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(ComponentFromStringMeta.class);
		String beanName = this.beanNameGenerator.generateBeanName(bd, registry);
		assertThat(beanName).isEqualTo("henry");
	}

	@Test
	public void generateBeanNameFromMetaComponentWithNonStringValue() {
		BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(ComponentFromNonStringMeta.class);
		String beanName = this.beanNameGenerator.generateBeanName(bd, registry);
		assertThat(beanName).isEqualTo("annotationBeanNameGeneratorTests.ComponentFromNonStringMeta");
	}

	@Test
	public void generateBeanNameFromComposedControllerAnnotationWithoutName() {
		// SPR-11360
		BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(ComposedControllerAnnotationWithoutName.class);
		String beanName = this.beanNameGenerator.generateBeanName(bd, registry);
		String expectedGeneratedBeanName = this.beanNameGenerator.buildDefaultBeanName(bd);
		assertThat(beanName).isEqualTo(expectedGeneratedBeanName);
	}

	@Test
	public void generateBeanNameFromComposedControllerAnnotationWithBlankName() {
		// SPR-11360
		BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(ComposedControllerAnnotationWithBlankName.class);
		String beanName = this.beanNameGenerator.generateBeanName(bd, registry);
		String expectedGeneratedBeanName = this.beanNameGenerator.buildDefaultBeanName(bd);
		assertThat(beanName).isEqualTo(expectedGeneratedBeanName);
	}

	@Test
	public void generateBeanNameFromComposedControllerAnnotationWithStringValue() {
		// SPR-11360
		BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		AnnotatedBeanDefinition bd = new AnnotatedGenericBeanDefinition(
			ComposedControllerAnnotationWithStringValue.class);
		String beanName = this.beanNameGenerator.generateBeanName(bd, registry);
		assertThat(beanName).isEqualTo("restController");
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

	@Service("henry")
	private static class ComponentFromStringMeta {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Component
	public @interface NonStringMetaComponent {

		long value();
	}

	@NonStringMetaComponent(123)
	private static class ComponentFromNonStringMeta {
	}

	/**
	 * @see org.springframework.web.bind.annotation.RestController
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Controller
	public static @interface TestRestController {

		String value() default "";
	}

	@TestRestController
	public static class ComposedControllerAnnotationWithoutName {
	}

	@TestRestController(" ")
	public static class ComposedControllerAnnotationWithBlankName {
	}

	@TestRestController("restController")
	public static class ComposedControllerAnnotationWithStringValue {
	}

}
