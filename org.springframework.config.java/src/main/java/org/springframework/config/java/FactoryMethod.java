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
package org.springframework.config.java;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.sf.cglib.proxy.MethodInterceptor;

import org.springframework.config.java.ext.AbstractMethodInterceptor;
import org.springframework.config.java.ext.Bean;


/**
 * Meta-annotation used to identify method annotations as producers of beans and/or values.
 * Provides a model that's open for extension. i.e.: The {@link Bean} annotation is
 * annotated as a {@link FactoryMethod}. In this same fashion, any custom annotation can be
 * devised with its own semantics. It need only provide a custom registrar, interceptor and
 * optionally, validators.
 * 
 * @see Bean
 * @see BeanDefinitionRegistrar
 * @see AbstractMethodInterceptor
 * @see Validator
 * 
 * @author Chris Beams
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Documented
public @interface FactoryMethod {

	/**
	 * Specifies which registrar should be used to register bean definitions when processing
	 * this {@link FactoryMethod}.
	 */
	Class<? extends BeanDefinitionRegistrar> registrar();

	/**
	 * Specifies what interceptor should be used when processing this {@link FactoryMethod}.
	 * Defaults to {@link NoOpInterceptor} which does nothing.
	 */
	Class<? extends MethodInterceptor> interceptor() default NoOpInterceptor.class;

	/**
	 * Optionally specifies any {@link Validator} types capable of validating the syntax of
	 * this {@link FactoryMethod}. Usually used when a factory method may have multiple
	 * annotations such as {@link Bean} and {@link ScopedProxy}.
	 */
	Class<? extends Validator>[] validators() default {};
}
