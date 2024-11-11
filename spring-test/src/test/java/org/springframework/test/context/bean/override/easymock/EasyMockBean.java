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

package org.springframework.test.context.bean.override.easymock;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.easymock.MockType;

import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.bean.override.BeanOverride;

/**
 * {@code @EasyMockBean} is a field-level annotation that can be used in a test
 * class to signal that a bean should be replaced with an {@link org.easymock.EasyMock
 * EasyMock} mock.
 *
 * @author Sam Brannen
 * @since 6.2
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BeanOverride(EasyMockBeanOverrideProcessor.class)
public @interface EasyMockBean {

	/**
	 * Alias for {@link #name}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * The name of the bean to mock.
	 * <p>Defaults to an empty string to denote that the name of the annotated
	 * field should be used as the bean name.
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * The {@link MockType} to use when creating the mock.
	 * <p>Defaults to {@link MockType#STRICT}.
	 */
	MockType mockType() default MockType.STRICT;

}
