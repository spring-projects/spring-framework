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

package org.springframework.beans.factory;

import org.springframework.core.env.Environment;

/**
 * Contract for registering beans programmatically. Implementations use the
 * {@link BeanRegistry} and {@link Environment} to register beans:
 *
 * <pre class="code">
 * class MyBeanRegistrar implements BeanRegistrar {
 *
 *     &#064;Override
 *     public void register(BeanRegistry registry, Environment env) {
 *         registry.registerBean("foo", Foo.class);
 *         registry.registerBean("bar", Bar.class, spec -> spec
 *                 .prototype()
 *                 .lazyInit()
 *                 .description("Custom description")
 *                 .supplier(context -> new Bar(context.bean(Foo.class))));
 *         if (env.matchesProfiles("baz")) {
 *             registry.registerBean(Baz.class, spec -> spec
 *                     .supplier(context -> new Baz("Hello World!")));
 *         }
 *     }
 * }</pre>
 *
 * <p>{@code BeanRegistrar} implementations are not Spring components: they must have
 * a no-arg constructor and cannot rely on dependency injection or any other
 * component-model feature. They can be used in two distinct ways depending on the
 * application context setup.
 *
 * <h3>With the {@code @Configuration} model</h3>
 *
 * <p>A {@code BeanRegistrar} must be imported via
 * {@link org.springframework.context.annotation.Import @Import} on a
 * {@link org.springframework.context.annotation.Configuration @Configuration} class:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;Import(MyBeanRegistrar.class)
 * class MyConfiguration {
 * }</pre>
 *
 * <p>This is the only mechanism that triggers bean registration in the annotation-based
 * configuration model. Annotating an implementation with {@code @Configuration} or
 * {@code @Component}, or returning an instance from a {@code @Bean} method, registers
 * it as a bean but does <strong>not</strong> invoke its
 * {@link #register(BeanRegistry, Environment) register} method.
 *
 * <p>When imported, the registrar is invoked in the order it is encountered during
 * configuration class processing. It can therefore check for and build on beans that
 * have already been defined, but has no visibility into beans that will be registered
 * by classes processed later.
 *
 * <h3>Programmatic usage</h3>
 *
 * <p>A {@code BeanRegistrar} can also be applied directly to a
 * {@link org.springframework.context.support.GenericApplicationContext}:
 *
 * <pre class="code">
 * GenericApplicationContext context = new GenericApplicationContext();
 * context.register(new MyBeanRegistrar());
 * context.registerBean("myBean", MyBean.class);
 * context.refresh();</pre>
 *
 * <p>This mode is primarily intended for fully programmatic application context setups.
 * Registrars applied this way are invoked before any {@code @Configuration} class is
 * processed. They can therefore observe beans registered programmatically (e.g., via
 * one of the {@code GenericApplicationContext#registerBean} methods), but will
 * <strong>not</strong> see any beans defined in {@code @Configuration} classes also
 * registered with the context.
 *
 * <p>A {@code BeanRegistrar} implementing {@link org.springframework.context.annotation.ImportAware}
 * can optionally introspect import metadata when used in an import scenario; otherwise
 * the {@code setImportMetadata} method is not called.
 *
 * <p>In Kotlin, it is recommended to use {@code BeanRegistrarDsl} instead of
 * implementing {@code BeanRegistrar}.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
@FunctionalInterface
public interface BeanRegistrar {

	/**
	 * Register beans on the given {@link BeanRegistry} in a programmatic way.
	 * @param registry the bean registry to operate on
	 * @param env the environment that can be used to get the active profile or some properties
	 */
	void register(BeanRegistry registry, Environment env);

}
