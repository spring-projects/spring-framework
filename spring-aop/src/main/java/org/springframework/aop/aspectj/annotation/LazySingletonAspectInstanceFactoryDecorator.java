/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.annotation;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Decorator to cause a {@link MetadataAwareAspectInstanceFactory} to instantiate only once.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class LazySingletonAspectInstanceFactoryDecorator implements MetadataAwareAspectInstanceFactory, Serializable {

	private final MetadataAwareAspectInstanceFactory maaif;

	private volatile @Nullable Object materialized;


	/**
	 * Create a new lazily initializing decorator for the given AspectInstanceFactory.
	 * @param maaif the MetadataAwareAspectInstanceFactory to decorate
	 */
	public LazySingletonAspectInstanceFactoryDecorator(MetadataAwareAspectInstanceFactory maaif) {
		Assert.notNull(maaif, "AspectInstanceFactory must not be null");
		this.maaif = maaif;
	}


	@Override
	public Object getAspectInstance() {
		Object aspectInstance = this.materialized;
		if (aspectInstance == null) {
			Object mutex = this.maaif.getAspectCreationMutex();
			if (mutex == null) {
				aspectInstance = this.maaif.getAspectInstance();
				this.materialized = aspectInstance;
			}
			else {
				synchronized (mutex) {
					aspectInstance = this.materialized;
					if (aspectInstance == null) {
						aspectInstance = this.maaif.getAspectInstance();
						this.materialized = aspectInstance;
					}
				}
			}
		}
		return aspectInstance;
	}

	public boolean isMaterialized() {
		return (this.materialized != null);
	}

	@Override
	public @Nullable ClassLoader getAspectClassLoader() {
		return this.maaif.getAspectClassLoader();
	}

	@Override
	public AspectMetadata getAspectMetadata() {
		return this.maaif.getAspectMetadata();
	}

	@Override
	public @Nullable Object getAspectCreationMutex() {
		return this.maaif.getAspectCreationMutex();
	}

	@Override
	public int getOrder() {
		return this.maaif.getOrder();
	}


	@Override
	public String toString() {
		return "LazySingletonAspectInstanceFactoryDecorator: decorating " + this.maaif;
	}

}
