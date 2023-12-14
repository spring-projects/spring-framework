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

package org.springframework.test.bean.override.convention;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.bean.override.BeanOverride;

/**
 * Mark a field to represent a "method" bean override of the bean of the same
 * name and inject the field with the overriding instance.
 *
 * <p>The instance is created from a static method in the declaring class which
 * return type is compatible with the annotated field and which name follows the
 * convention:
 * <ul>
 *     <li>if the annotation's {@link #methodName()} is specified,
 *     look for that one.</li>
 *     <li>if not, look for exactly one method named with the
 *     {@link #CONVENTION_SUFFIX} suffix and either:</li>
 *     <ul>
 *         <li>starting with the annotated field name</li>
 *         <li>starting with the bean name</li>
 *     </ul>
 * </ul>
 *
 * <p>The annotated field's name is interpreted to be the name of the original
 * bean to override, unless the annotation's {@link #name()} is specified.
 *
 * @see TestBeanOverrideProcessor
 * @author Simon Baslé
 * @since 6.2
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BeanOverride(TestBeanOverrideProcessor.class)
public @interface TestBean {

	/**
	 * The method suffix expected as a convention in static methods which
	 * provides an override instance.
	 */
	String CONVENTION_SUFFIX = "TestOverride";

	/**
	 * The name of a static method to look for in the Configuration, which will
	 * be used to instantiate the override bean and inject the annotated field.
	 * <p> Default is {@code ""} (the empty String), which is translated into
	 * the annotated field's name concatenated with the
	 * {@link #CONVENTION_SUFFIX}.
	 */
	String methodName() default "";

	/**
	 * The name of the original bean to override, or {@code ""} (the empty
	 * String) to deduce the name from the annotated field.
	 */
	String name() default "";
}
