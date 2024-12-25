/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.aop.aspectj;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

/**
 * Interface implemented to provide an instance of an AspectJ aspect.
 * Decouples from Spring's bean factory.
 *
 * <p>Extends the {@link org.springframework.core.Ordered} interface
 * to express an order value for the underlying aspect in a chain.
 *
 * <p>切面实例工厂(AspectInstanceFactory)
 * <p>接口实现，以提供AspectJ方面的实例。从Spring的bean工厂解耦
 * <p>扩展{@link org.springframework.core.Ordered} 接口来表示链中底层方面的顺序值。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.BeanFactory#getBean
 * @since 2.0
 */
public interface AspectInstanceFactory extends Ordered {

	/**
	 * Create an instance of this factory's aspect.
	 * <p>创建该工厂切面的一个实例
	 *
	 * @return the aspect instance (never {@code null})
	 */
	Object getAspectInstance();

	/**
	 * Expose the aspect class loader that this factory uses.
	 * <p>公开此工厂使用的切面类装入器
	 *
	 * @return the aspect class loader (or {@code null} for the bootstrap loader)
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 */
	@Nullable
	ClassLoader getAspectClassLoader();

}
