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

package org.springframework.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated class is a <em>component</em>.
 *
 * <p>Such classes are considered as candidates for auto-detection
 * when using annotation-based configuration and classpath scanning.
 *
 * <p>A component may optionally specify a logical component name via the
 * {@link #value value} attribute of this annotation.
 *
 * <p>Other class-level annotations may be considered as identifying
 * a component as well, typically a special kind of component &mdash;
 * for example, the {@link Repository @Repository} annotation or AspectJ's
 * {@link org.aspectj.lang.annotation.Aspect @Aspect} annotation. Note, however,
 * that the {@code @Aspect} annotation does not automatically make a class
 * eligible for classpath scanning.
 *
 * <p>Any annotation meta-annotated with {@code @Component} is considered a
 * <em>stereotype</em> annotation which makes the annotated class eligible for
 * classpath scanning. For example, {@link Service @Service},
 * {@link Controller @Controller}, and {@link Repository @Repository} are
 * stereotype annotations. Stereotype annotations may also support configuration
 * of a logical component name by overriding the {@link #value} attribute of this
 * annotation via {@link org.springframework.core.annotation.AliasFor @AliasFor}.
 *
 * <p>As of Spring Framework 6.1, support for configuring the name of a stereotype
 * component by convention (i.e., via a {@code String value()} attribute without
 * {@code @AliasFor}) is deprecated and will be removed in a future version of the
 * framework. Consequently, custom stereotype annotations must use {@code @AliasFor}
 * to declare an explicit alias for this annotation's {@link #value} attribute.
 * See the source code declaration of {@link Repository#value()} and
 * {@link org.springframework.web.bind.annotation.ControllerAdvice#name()
 * ControllerAdvice.name()} for concrete examples.
 *
 * @author Mark Fisher
 * @author Sam Brannen
 * @since 2.5
 * @see Repository
 * @see Service
 * @see Controller
 * @see org.springframework.context.annotation.ClassPathBeanDefinitionScanner
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface Component {

	/**
	 * The value may indicate a suggestion for a logical component name,
	 * to be turned into a Spring bean name in case of an autodetected component.
	 * @return the suggested component name, if any (or empty String otherwise)
	 */
	String value() default "";

}
