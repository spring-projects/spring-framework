/*
 * Copyright 2002-2022 the original author or authors.
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
 * Indicates that the classes specified in the annotation attributes require some
 * reflection hints for binding or reflection-based serialization purposes. For each
 * class specified, hints on constructors, fields, properties, record components,
 * including types transitively used on properties and record components are registered.
 * At least one class must be specified in the {@code value} or {@code classes} annotation
 * attributes.
 *
 * <p>The annotated element can be a configuration class &mdash; for example:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;RegisterReflectionForBinding({ Foo.class, Bar.class })
 * public class MyConfig {
 *     // ...
 * }</pre>
 *
 * <p>The annotated element can be any Spring bean class, constructor, field,
 * or method &mdash; for example:
 *
 * <pre class="code">
 * &#064;Service
 * public class MyService {
 *
 *     &#064;RegisterReflectionForBinding(Baz.class)
 *     public void process() {
 *         // ...
 *     }
 *
 * }</pre>
 *
 * <p>The annotated element can also be any test class that uses the <em>Spring
 * TestContext Framework</em> to load an {@code ApplicationContext}.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 * @see org.springframework.aot.hint.BindingReflectionHintsRegistrar
 * @see Reflective @Reflective
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Reflective(RegisterReflectionForBindingProcessor.class)
public @interface RegisterReflectionForBinding {

	/**
	 * Alias for {@link #classes()}.
	 */
	@AliasFor("classes")
	Class<?>[] value() default {};

	/**
	 * Classes for which reflection hints should be registered.
	 * <p>At least one class must be specified either via {@link #value} or
	 * {@link #classes}.
	 * @see #value()
	 */
	@AliasFor("value")
	Class<?>[] classes() default {};

}
