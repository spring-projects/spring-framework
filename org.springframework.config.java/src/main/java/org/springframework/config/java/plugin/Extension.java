/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.config.java.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.Ordered;


/**
 * TODO: JAVADOC
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Inherited
public @interface Extension {
	/**
	 * The class that handles this plugin.
	 */
	// TODO: SJC-242 rename to handlerType / handlerClass
	Class<? extends ExtensionAnnotationBeanDefinitionRegistrar<?>> handler();

	/**
	 * The order in which this plugin will be processed relative to others. Per the
	 * semantics of {@link Ordered}, lower integer values will be treated as higher
	 * priority.
	 * 
	 * @see Ordered
	 */
	int order() default 0;
}
