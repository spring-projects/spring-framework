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

package org.springframework.aop.aspectj.annotation;

import org.springframework.aop.aspectj.SimpleAspectInstanceFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;

/**
 * Implementation of {@link MetadataAwareAspectInstanceFactory} that
 * creates a new instance of the specified aspect class for every
 * {@link #getAspectInstance()} call.
 *
 * @author Juergen Hoeller
 * @since 2.0.4
 */
public class SimpleMetadataAwareAspectInstanceFactory extends SimpleAspectInstanceFactory
		implements MetadataAwareAspectInstanceFactory {

	private final AspectMetadata metadata;


	/**
	 * Create a new SimpleMetadataAwareAspectInstanceFactory for the given aspect class.
	 * @param aspectClass the aspect class
	 * @param aspectName the aspect name
	 */
	public SimpleMetadataAwareAspectInstanceFactory(Class<?> aspectClass, String aspectName) {
		super(aspectClass);
		this.metadata = new AspectMetadata(aspectClass, aspectName);
	}


	@Override
	public final AspectMetadata getAspectMetadata() {
		return this.metadata;
	}

	@Override
	protected int getOrderForAspectClass(Class<?> aspectClass) {
		return OrderUtils.getOrder(aspectClass, Ordered.LOWEST_PRECEDENCE);
	}

}
