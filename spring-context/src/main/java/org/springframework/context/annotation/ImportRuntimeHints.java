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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Indicates that one or more {@link RuntimeHintsRegistrar} implementations
 * should be processed.
 *
 * <p>Unlike declaring {@link RuntimeHintsRegistrar} using
 * {@code META-INF/spring/aot.factories}, this annotation allows for more flexible
 * registration where it is only processed if the annotated component or bean
 * method is actually registered in the bean factory. To illustrate this
 * behavior, consider the following example:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class MyConfiguration {
 *
 *     &#064;Bean
 *     &#064;ImportRuntimeHints(MyHints.class)
 *     &#064;Conditional(MyCondition.class)
 *     public MyService myService() {
 *         return new MyService();
 *     }
 *
 * }</pre>
 *
 * <p>If the configuration class above is processed, {@code MyHints} will be
 * contributed only if {@code MyCondition} matches. If the condition does not
 * match, {@code MyService} will not be defined as a bean and the hints will
 * not be processed either.
 *
 * <p>{@code @ImportRuntimeHints} can also be applied to any test class that uses
 * the <em>Spring TestContext Framework</em> to load an {@code ApplicationContext}.
 *
 * <p>If several components or test classes refer to the same {@link RuntimeHintsRegistrar}
 * implementation, the registrar will only be invoked once for the given bean factory
 * processing or test suite.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 6.0
 * @see org.springframework.aot.hint.RuntimeHints
 * @see org.springframework.aot.hint.annotation.Reflective
 * @see org.springframework.aot.hint.annotation.RegisterReflectionForBinding
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ImportRuntimeHints {

	/**
	 * {@link RuntimeHintsRegistrar} implementations to process.
	 */
	Class<? extends RuntimeHintsRegistrar>[] value();

}
