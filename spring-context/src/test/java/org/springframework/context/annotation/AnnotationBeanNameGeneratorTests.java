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

package org.springframework.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import example.scannable.DefaultNamedComponent;
import example.scannable.JakartaManagedBeanComponent;
import example.scannable.JakartaNamedComponent;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link AnnotationBeanNameGenerator}.
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Chris Beams
 * @author Sam Brannen
 */
class AnnotationBeanNameGeneratorTests {

	private final BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

	private final AnnotationBeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();


	@Test
	void buildDefaultBeanName() {
		BeanDefinition bd = annotatedBeanDef(ComponentFromNonStringMeta.class);
		assertThat(this.beanNameGenerator.buildDefaultBeanName(bd, this.registry))
				.isEqualTo("annotationBeanNameGeneratorTests.ComponentFromNonStringMeta");
	}

	@Test
	void generateBeanNameWithNamedComponent() {
		assertGeneratedName(ComponentWithName.class, "walden");
	}

	@Test
	void generateBeanNameWithNamedComponentWhereTheNameIsBlank() {
		assertGeneratedNameIsDefault(ComponentWithBlankName.class);
	}

	@Test
	void generateBeanNameForComponentWithDuplicateIdenticalNames() {
		assertGeneratedName(ComponentWithDuplicateIdenticalNames.class, "myComponent");
	}

	@Test
	void generateBeanNameForComponentWithConflictingNames() {
		BeanDefinition bd = annotatedBeanDef(ComponentWithMultipleConflictingNames.class);
		assertThatIllegalStateException()
				.isThrownBy(() -> generateBeanName(bd))
				.withMessage("Stereotype annotations suggest inconsistent component names: '%s' versus '%s'",
						"myComponent", "myService");
	}

	@Test
	void generateBeanNameWithJakartaNamedComponent() {
		assertGeneratedName(JakartaNamedComponent.class, "myJakartaNamedComponent");
	}

	@Test
	void generateBeanNameWithJakartaManagedBeanComponent() {
		assertGeneratedName(JakartaManagedBeanComponent.class, "myJakartaManagedBeanComponent");
	}

	@Test
	void generateBeanNameWithCustomStereotypeComponent() {
		assertGeneratedName(DefaultNamedComponent.class, "thoreau");
	}

	@Test
	void generateBeanNameWithAnonymousComponentYieldsGeneratedBeanName() {
		assertGeneratedNameIsDefault(AnonymousComponent.class);
	}

	@Test
	void generateBeanNameFromMetaComponentWithStringValue() {
		assertGeneratedName(ComponentFromStringMeta.class, "henry");
	}

	@Test
	void generateBeanNameFromMetaComponentWithNonStringValue() {
		assertGeneratedNameIsDefault(ComponentFromNonStringMeta.class);
	}

	@Test  // SPR-11360
	void generateBeanNameFromComposedControllerAnnotationWithoutName() {
		assertGeneratedNameIsDefault(ComposedControllerAnnotationWithoutName.class);
	}

	@Test  // SPR-11360
	void generateBeanNameFromComposedControllerAnnotationWithBlankName() {
		assertGeneratedNameIsDefault(ComposedControllerAnnotationWithBlankName.class);
	}

	@Test  // SPR-11360
	void generateBeanNameFromComposedControllerAnnotationWithStringValue() {
		assertGeneratedName(ComposedControllerAnnotationWithStringValue.class, "restController");
	}


	private void assertGeneratedName(Class<?> clazz, String expectedName) {
		BeanDefinition bd = annotatedBeanDef(clazz);
		assertThat(generateBeanName(bd)).isNotBlank().isEqualTo(expectedName);
	}

	private void assertGeneratedNameIsDefault(Class<?> clazz) {
		BeanDefinition bd = annotatedBeanDef(clazz);
		String expectedName = this.beanNameGenerator.buildDefaultBeanName(bd);
		assertThat(generateBeanName(bd)).isNotBlank().isEqualTo(expectedName);
	}

	private AnnotatedBeanDefinition annotatedBeanDef(Class<?> clazz) {
		return new AnnotatedGenericBeanDefinition(clazz);
	}

	private String generateBeanName(BeanDefinition bd) {
		return this.beanNameGenerator.generateBeanName(bd, registry);
	}


	@Component("walden")
	private static class ComponentWithName {
	}

	@Component(" ")
	private static class ComponentWithBlankName {
	}

	@Component("myComponent")
	@Service("myComponent")
	static class ComponentWithDuplicateIdenticalNames {
	}

	@Component("myComponent")
	@Service("myService")
	static class ComponentWithMultipleConflictingNames {
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
	@interface NonStringMetaComponent {

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
	@interface TestRestController {

		String value() default "";
	}

	@TestRestController
	static class ComposedControllerAnnotationWithoutName {
	}

	@TestRestController(" ")
	static class ComposedControllerAnnotationWithBlankName {
	}

	@TestRestController("restController")
	static class ComposedControllerAnnotationWithStringValue {
	}

}
