/*
 * Copyright 2002-2009 the original author or authors.
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
import org.springframework.stereotype.Component;

/**
 * When used as a type-level annotation in conjunction with the {@link Component}
 * annotation, indicates the name of a scope to use for instances of the annotated
 * type.
 * 
 * <p>When used as a method-level annotation in conjunction with the
 * {@link Bean} annotation, indicates the name of a scope to use for
 * the instance returned from the method.
 *
 * <p>In this context, scope means the lifecycle of an instance, such as
 * {@literal singleton}, {@literal prototype}, and so forth.
 * 
 * @author Mark Fisher
 * @author Chris Beams
 * @since 2.5
 * @see Component
 * @see Bean
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {

	/**
	 * Specifies the scope to use for the annotated component/bean.
	 * @return the specified scope
	 */
	String value() default BeanDefinition.SCOPE_SINGLETON;

	/**
	 * Specifies whether a component should be configured as a scoped proxy
	 * and if so, whether the proxy should be interface-based or subclass-based.
	 * <p>Defaults to {@link ScopedProxyMode#NO}, indicating that no scoped
	 * proxy should be created.
	 * <p>Analogous to {@literal <aop:scoped-proxy/>} support in Spring XML.
	 */
	ScopedProxyMode proxyMode() default ScopedProxyMode.DEFAULT;

}
