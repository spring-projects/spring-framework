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

package org.springframework.aop.aspectj.annotation;

import org.springframework.util.Assert;

/**
 * Decorator to cause a {@link MetadataAwareAspectInstanceFactory} to instantiate only once.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
public class LazySingletonAspectInstanceFactoryDecorator implements MetadataAwareAspectInstanceFactory {

	private final MetadataAwareAspectInstanceFactory maaif;

	private volatile Object materialized;


	/**
	 * Create a new lazily initializing decorator for the given AspectInstanceFactory.
	 * @param maaif the MetadataAwareAspectInstanceFactory to decorate
	 */
	public LazySingletonAspectInstanceFactoryDecorator(MetadataAwareAspectInstanceFactory maaif) {
		Assert.notNull(maaif, "AspectInstanceFactory must not be null");
		this.maaif = maaif;
	}


	public synchronized Object getAspectInstance() {
		if (this.materialized == null) {
			synchronized (this) {
				if (this.materialized == null) {
					this.materialized = this.maaif.getAspectInstance();
				}
			}
		}
		return this.materialized;
	}

	public boolean isMaterialized() {
		return (this.materialized != null);
	}

	public ClassLoader getAspectClassLoader() {
		return this.maaif.getAspectClassLoader();
	}

	public AspectMetadata getAspectMetadata() {
		return this.maaif.getAspectMetadata();
	}

	public int getOrder() {
		return this.maaif.getOrder();
	}


	@Override
	public String toString() {
		return "LazySingletonAspectInstanceFactoryDecorator: decorating " + this.maaif;
	}

}
