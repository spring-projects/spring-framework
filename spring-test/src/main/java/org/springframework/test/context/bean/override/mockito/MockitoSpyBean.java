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

package org.springframework.test.context.bean.override.mockito;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mockito.Mockito;

import org.springframework.test.context.bean.override.BeanOverride;

/**
 * Mark a field to trigger a bean override using a Mockito spy, which will wrap
 * the original instance. If no explicit {@link #name()} is specified, the
 * annotated field's name is interpreted to be the target of the override.
 * In either case, it is required that the target bean is previously registered
 * in the context.
 *
 * <p>Dependencies that are known to the application context but are not beans
 * (such as those
 * {@link org.springframework.beans.factory.config.ConfigurableListableBeanFactory#registerResolvableDependency(Class, Object)
 * registered directly}) will not be found.
 *
 * @author Simon Baslé
 * @since 6.2
 * @see MockitoBean
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BeanOverride(MockitoBeanOverrideProcessor.class)
public @interface MockitoSpyBean {

	/**
	 * The name of the bean to spy.
	 * <p>If not specified, the name of the annotated field will be used.
	 * @return the name of the spied bean
	 */
	String name() default "";

	/**
	 * The reset mode to apply to the spied bean.
	 * <p>The default is {@link MockReset#AFTER} meaning that spies are automatically
	 * reset after each test method is invoked.
	 * @return the reset mode
	 */
	MockReset reset() default MockReset.AFTER;

	/**
	 * Indicates that Mockito methods such as {@link Mockito#verify(Object)
	 * verify(mock)} should use the {@code target} of AOP advised beans,
	 * rather than the proxy itself.
	 * <p>Defaults to {@code true}.
	 * <p>If set to {@code false} you may need to use the result of
	 * {@link org.springframework.test.util.AopTestUtils#getUltimateTargetObject(Object)
	 * AopTestUtils.getUltimateTargetObject(...)} when calling Mockito methods.
	 * @return {@code true} if the target of AOP advised beans is used, or
	 * {@code false} if the proxy is used directly
	 */
	boolean proxyTargetAware() default true;

}
