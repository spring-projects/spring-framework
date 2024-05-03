/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.aot.hint.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Register reflection hints for data binding or reflection-based serialization
 * against an arbitrary number of target classes.
 *
 * <p>For each class hints are registered for constructors, fields, properties,
 * and record components. Hints are also registered for types transitively used
 * on properties and record components.
 *
 * <p>You can use this annotation on any bean that is contributed to the context:
 * <pre><code class="java">
 * &#064;Configuration
 * &#064;RegisterReflectionForBinding({Foo.class, Bar.class})
 * class MyConfig {
 *     // ...
 * }</code></pre>
 *
 * <p>If scanning of {@link Reflective} is enabled, any type in the configured
 * packages can use this annotation as well.
 *
 * <p>When the annotated element is a type, the type itself is registered if no
 * candidates are provided:<pre><code class="java">
 * &#064;Component
 * &#064;RegisterReflectionForBinding
 * class MyBean {
 *     // ...
 * }</code></pre>
 *
 * The annotation can also be specified on a method. In that case, at least one
 * target class must be specified:<pre><code class="java">
 * &#064;Component
 * class MyService {
 *
 *     &#064;RegisterReflectionForBinding(Baz.class)
 *     public Baz process() {
 *         // ...
 *     }
 *
 * }</code></pre>
 *
 * <p>The annotated element can also be any test class that uses the <em>Spring
 * TestContext Framework</em> to load an {@code ApplicationContext}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @since 6.0
 * @see org.springframework.aot.hint.BindingReflectionHintsRegistrar
 * @see RegisterReflection @RegisterReflection
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RegisterReflection
@Reflective(RegisterReflectionForBindingProcessor.class)
public @interface RegisterReflectionForBinding {

	/**
	 * Alias for {@link #classes()}.
	 */
	@AliasFor(annotation = RegisterReflection.class, attribute = "classes")
	Class<?>[] value() default {};

	/**
	 * Classes for which reflection hints should be registered.
	 * <p>At least one class must be specified either via {@link #value} or {@code classes}.
	 * @see #value()
	 */
	@AliasFor(annotation = RegisterReflection.class, attribute = "classes")
	Class<?>[] classes() default {};

	/**
	 * Alternative to {@link #classes()} to specify the classes as class names.
	 * @see #classes()
	 */
	@AliasFor(annotation = RegisterReflection.class, attribute = "classNames")
	String[] classNames() default {};

}
