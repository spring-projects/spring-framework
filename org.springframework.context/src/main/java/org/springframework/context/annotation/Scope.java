/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Indicates the name of a scope to use for instances of the annotated class.
 *
 * <p>In this context, scope means the lifecycle of an instance, such as
 * '<code>singleton</code>', '<code>prototype</code>', and so forth.
 * 
 * @author Mark Fisher
 * @since 2.5
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {

	/**
	 * Specifies the scope to use for instances of the annotated class.
	 * @return the desired scope
	 */
	String value() default BeanDefinition.SCOPE_SINGLETON;
	
}
