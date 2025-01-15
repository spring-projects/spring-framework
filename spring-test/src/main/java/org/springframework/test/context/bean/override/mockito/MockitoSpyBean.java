/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.bean.override.mockito;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.bean.override.BeanOverride;

/**
 * {@code @MockitoSpyBean} is an annotation that can be applied to a non-static
 * field in a test class to override a bean in the test's
 * {@link org.springframework.context.ApplicationContext ApplicationContext}
 * with a Mockito spy that wraps the original bean instance.
 *
 * <p>By default, the bean to spy is inferred from the type of the annotated
 * field. If multiple candidates exist, a {@code @Qualifier} annotation can be
 * used to help disambiguate. In the absence of a {@code @Qualifier} annotation,
 * the name of the annotated field will be used as a fallback qualifier.
 * Alternatively, you can explicitly specify a bean name to spy by setting the
 * {@link #value() value} or {@link #name() name} attribute. If a bean name is
 * specified, it is required that a target bean with that name has been previously
 * registered in the application context.
 *
 * <p>A spy cannot be created for components which are known to the application
 * context but are not beans &mdash; for example, components
 * {@link org.springframework.beans.factory.config.ConfigurableListableBeanFactory#registerResolvableDependency(Class, Object)
 * registered directly} as resolvable dependencies.
 *
 * <p><strong>NOTE</strong>: Only <em>singleton</em> beans can be spied. Any attempt
 * to create a spy for a non-singleton bean will result in an exception. When
 * creating a spy for a {@link org.springframework.beans.factory.FactoryBean FactoryBean},
 * a spy will be created for the object created by the {@code FactoryBean}, not
 * for the {@code FactoryBean} itself.
 *
 * <p>There are no restrictions on the visibility of a {@code @MockitoSpyBean} field.
 * Such fields can therefore be {@code public}, {@code protected}, package-private
 * (default visibility), or {@code private} depending on the needs or coding
 * practices of the project.
 *
 * <p>{@code @MockitoSpyBean} fields will be inherited from an enclosing test class by default.
 * See {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}
 * for details.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 * @see org.springframework.test.context.bean.override.mockito.MockitoBean @MockitoBean
 * @see org.springframework.test.context.bean.override.convention.TestBean @TestBean
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BeanOverride(MockitoBeanOverrideProcessor.class)
public @interface MockitoSpyBean {

	/**
	 * Alias for {@link #name()}.
	 * <p>Intended to be used when no other attributes are needed &mdash; for
	 * example, {@code @MockitoSpyBean("customBeanName")}.
	 * @see #name()
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * Name of the bean to spy.
	 * <p>If left unspecified, the bean to spy is selected according to the
	 * annotated field's type, taking qualifiers into account if necessary. See
	 * the {@linkplain MockitoSpyBean class-level documentation} for details.
	 * @see #value()
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * The reset mode to apply to the spied bean.
	 * <p>The default is {@link MockReset#AFTER} meaning that spies are automatically
	 * reset after each test method is invoked.
	 * @return the reset mode
	 */
	MockReset reset() default MockReset.AFTER;

}
