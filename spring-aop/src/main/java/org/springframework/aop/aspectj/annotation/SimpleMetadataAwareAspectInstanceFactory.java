/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	public Object getAspectCreationMutex() {
		return this;
	}

	@Override
	protected int getOrderForAspectClass(Class<?> aspectClass) {
		return OrderUtils.getOrder(aspectClass, Ordered.LOWEST_PRECEDENCE);
	}

}
