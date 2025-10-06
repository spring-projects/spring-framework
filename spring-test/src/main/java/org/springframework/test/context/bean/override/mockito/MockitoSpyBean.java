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

package org.springframework.test.context.bean.override.mockito;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.bean.override.BeanOverride;

/**
 * {@code @MockitoSpyBean} is an annotation that can be used in test classes to
 * override a bean in the test's
 * {@link org.springframework.context.ApplicationContext ApplicationContext}
 * with a Mockito spy that wraps the original bean instance.
 *
 * <p>{@code @MockitoSpyBean} can be applied in the following ways.
 * <ul>
 * <li>On a non-static field in a test class or any of its superclasses.</li>
 * <li>On a non-static field in an enclosing class for a {@code @Nested} test class
 * or in any class in the type hierarchy or enclosing class hierarchy above the
 * {@code @Nested} test class.</li>
 * <li>At the type level on a test class or any superclass or implemented interface
 * in the type hierarchy above the test class.</li>
 * <li>At the type level on an enclosing class for a {@code @Nested} test class
 * or on any class or interface in the type hierarchy or enclosing class hierarchy
 * above the {@code @Nested} test class.</li>
 * </ul>
 *
 * <p>When {@code @MockitoSpyBean} is declared on a field, the bean to spy is
 * inferred from the type of the annotated field. If multiple candidates exist in
 * the {@code ApplicationContext}, a {@code @Qualifier} annotation can be declared
 * on the field to help disambiguate. In the absence of a {@code @Qualifier}
 * annotation, the name of the annotated field will be used as a <em>fallback
 * qualifier</em>. Alternatively, you can explicitly specify a bean name to spy
 * by setting the {@link #value() value} or {@link #name() name} attribute. If a
 * bean name is specified, it is required that a target bean with that name has
 * been previously registered in the application context.
 *
 * <p>When {@code @MockitoSpyBean} is declared at the type level, the type of bean
 * (or beans) to spy must be supplied via the {@link #types() types} attribute.
 * If multiple candidates exist in the {@code ApplicationContext}, you can
 * explicitly specify a bean name to spy by setting the {@link #name() name}
 * attribute. Note, however, that the {@code types} attribute must contain a
 * single type if an explicit bean {@code name} is configured.
 *
 * <p>A spy cannot be created for components which are known to the application
 * context but are not beans &mdash; for example, components
 * {@link org.springframework.beans.factory.config.ConfigurableListableBeanFactory#registerResolvableDependency(Class, Object)
 * registered directly} as resolvable dependencies.
 *
 * <p><strong>NOTE</strong>: As stated in the documentation for Mockito, there are
 * times when using {@code Mockito.when()} is inappropriate for stubbing a spy
 * &mdash; for example, if calling a real method on a spy results in undesired
 * side effects. To avoid such undesired side effects, consider using
 * {@link org.mockito.Mockito#doReturn(Object) Mockito.doReturn(...).when(spy)...},
 * {@link org.mockito.Mockito#doThrow(Class) Mockito.doThrow(...).when(spy)...},
 * {@link org.mockito.Mockito#doNothing() Mockito.doNothing().when(spy)...}, and
 * similar methods.
 *
 * <p><strong>WARNING</strong>: Using {@code @MockitoSpyBean} in conjunction with
 * {@code @ContextHierarchy} can lead to undesirable results since each
 * {@code @MockitoSpyBean} will be applied to all context hierarchy levels by default.
 * To ensure that a particular {@code @MockitoSpyBean} is applied to a single context
 * hierarchy level, set the {@link #contextName() contextName} to match a
 * configured {@code @ContextConfiguration}
 * {@link org.springframework.test.context.ContextConfiguration#name() name}.
 * See the Javadoc for {@link org.springframework.test.context.ContextHierarchy @ContextHierarchy}
 * for further details and examples.
 *
 * <p><strong>NOTE</strong>: When creating a spy for a non-singleton bean, the
 * corresponding bean definition will be converted to a singleton. Consequently,
 * if you create a spy for a prototype or scoped bean, the spy will be treated as
 * a singleton. Similarly, when creating a spy for a
 * {@link org.springframework.beans.factory.FactoryBean FactoryBean}, a spy will
 * be created for the object created by the {@code FactoryBean}, not for the
 * {@code FactoryBean} itself.
 *
 * <p>There are no restrictions on the visibility of a {@code @MockitoSpyBean} field.
 * Such fields can therefore be {@code public}, {@code protected}, package-private
 * (default visibility), or {@code private} depending on the needs or coding
 * practices of the project.
 *
 * <p>{@code @MockitoSpyBean} fields and type-level {@code @MockitoSpyBean} declarations
 * will be inherited from an enclosing test class by default. See
 * {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}
 * for details.
 *
 * <p>{@code @MockitoSpyBean} may be used as a <em>meta-annotation</em> to create
 * custom <em>composed annotations</em> &mdash; for example, to define common spy
 * configuration in a single annotation that can be reused across a test suite.
 * {@code @MockitoSpyBean} can also be used as a <em>{@linkplain Repeatable repeatable}</em>
 * annotation at the type level &mdash; for example, to spy on several beans by
 * {@link #name() name}.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 * @see org.springframework.test.context.bean.override.mockito.MockitoBean @MockitoBean
 * @see org.springframework.test.context.bean.override.convention.TestBean @TestBean
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(MockitoSpyBeans.class)
@BeanOverride(MockitoBeanOverrideProcessor.class)
public @interface MockitoSpyBean {

	/**
	 * Alias for {@link #name() name}.
	 * <p>Intended to be used when no other attributes are needed &mdash; for
	 * example, {@code @MockitoSpyBean("customBeanName")}.
	 * @see #name()
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * Name of the bean to spy.
	 * <p>If left unspecified, the bean to spy is selected according to the
	 * configured {@link #types() types} or the annotated field's type, taking
	 * qualifiers into account if necessary. See the {@linkplain MockitoSpyBean
	 * class-level documentation} for details.
	 * @see #value()
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * One or more types to spy.
	 * <p>Defaults to none.
	 * <p>Each type specified will result in a spy being created and registered
	 * with the {@code ApplicationContext}.
	 * <p>Types must be omitted when the annotation is used on a field.
	 * <p>When {@code @MockitoSpyBean} also defines a {@link #name name}, this
	 * attribute can only contain a single value.
	 * @return the types to spy
	 * @since 6.2.3
	 */
	Class<?>[] types() default {};

	/**
	 * The name of the context hierarchy level in which this {@code @MockitoSpyBean}
	 * should be applied.
	 * <p>Defaults to an empty string which indicates that this {@code @MockitoSpyBean}
	 * should be applied to all application contexts.
	 * <p>If a context name is configured, it must match a name configured via
	 * {@code @ContextConfiguration(name=...)}.
	 * @since 6.2.6
	 * @see org.springframework.test.context.ContextHierarchy @ContextHierarchy
	 * @see org.springframework.test.context.ContextConfiguration#name() @ContextConfiguration(name=...)
	 */
	String contextName() default "";

	/**
	 * The reset mode to apply to the spied bean.
	 * <p>The default is {@link MockReset#AFTER} meaning that spies are automatically
	 * reset after each test method is invoked.
	 * @return the reset mode
	 */
	MockReset reset() default MockReset.AFTER;

}
