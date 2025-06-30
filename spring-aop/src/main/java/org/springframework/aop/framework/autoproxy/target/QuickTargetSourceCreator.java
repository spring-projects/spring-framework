/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.aop.framework.autoproxy.target;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.target.AbstractBeanFactoryBasedTargetSource;
import org.springframework.aop.target.CommonsPool2TargetSource;
import org.springframework.aop.target.PrototypeTargetSource;
import org.springframework.aop.target.ThreadLocalTargetSource;

/**
 * Convenient TargetSourceCreator using bean name prefixes to create one of three
 * well-known TargetSource types:
 * <ul>
 * <li>: CommonsPool2TargetSource</li>
 * <li>% ThreadLocalTargetSource</li>
 * <li>! PrototypeTargetSource</li>
 * </ul>
 *
 * @author Rod Johnson
 * @author Stephane Nicoll
 * @see org.springframework.aop.target.CommonsPool2TargetSource
 * @see org.springframework.aop.target.ThreadLocalTargetSource
 * @see org.springframework.aop.target.PrototypeTargetSource
 */
public class QuickTargetSourceCreator extends AbstractBeanFactoryBasedTargetSourceCreator {

	/**
	 * The CommonsPool2TargetSource prefix.
	 */
	public static final String PREFIX_COMMONS_POOL = ":";

	/**
	 * The ThreadLocalTargetSource prefix.
	 */
	public static final String PREFIX_THREAD_LOCAL = "%";

	/**
	 * The PrototypeTargetSource prefix.
	 */
	public static final String PREFIX_PROTOTYPE = "!";

	@Override
	protected final @Nullable AbstractBeanFactoryBasedTargetSource createBeanFactoryBasedTargetSource(
			Class<?> beanClass, String beanName) {

		if (beanName.startsWith(PREFIX_COMMONS_POOL)) {
			CommonsPool2TargetSource cpts = new CommonsPool2TargetSource();
			cpts.setMaxSize(25);
			return cpts;
		}
		else if (beanName.startsWith(PREFIX_THREAD_LOCAL)) {
			return new ThreadLocalTargetSource();
		}
		else if (beanName.startsWith(PREFIX_PROTOTYPE)) {
			return new PrototypeTargetSource();
		}
		else {
			// No match. Don't create a custom target source.
			return null;
		}
	}

}
