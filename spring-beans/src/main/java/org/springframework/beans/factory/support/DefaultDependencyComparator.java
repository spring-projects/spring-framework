/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.util.Comparator;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.DefaultOrderProviderComparator;

/**
 * The default {@link Comparator} to use to order dependencies. Extend
 * from {@link DefaultOrderProviderComparator} so that the bean factory
 * has the ability to provide an {@link org.springframework.core.annotation.OrderProvider}
 * that is aware of more bean metadata, if any.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see org.springframework.core.annotation.OrderProviderComparator
 * @see org.springframework.core.annotation.OrderProvider
 * @see DefaultListableBeanFactory#setDependencyComparator(java.util.Comparator)
 */
public class DefaultDependencyComparator extends DefaultOrderProviderComparator implements Comparator<Object> {

	/**
	 * Shared default instance of DefaultDependencyComparator.
	 */
	public static final DefaultDependencyComparator INSTANCE = new DefaultDependencyComparator();

	private final Comparator<Object> comparator;

	public DefaultDependencyComparator(Comparator<Object> comparator) {
		this.comparator = comparator;
	}

	public DefaultDependencyComparator() {
		this(AnnotationAwareOrderComparator.INSTANCE);
	}

	@Override
	public int compare(Object o1, Object o2) {
		return comparator.compare(o1, o2);
	}

}
