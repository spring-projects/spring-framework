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

package org.springframework.aop.aspectj.annotation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Decorator to cause a {@link MetadataAwareAspectInstanceFactory} to instantiate only once.
 * <p>惰性单例方面实例工厂装饰器(LazySingletonAspectInstanceFactoryDecorator)
 * <p>装饰器使{@link MetadataAwareAspectInstanceFactory} 只实例化一次。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class LazySingletonAspectInstanceFactoryDecorator implements MetadataAwareAspectInstanceFactory, Serializable {

	private final MetadataAwareAspectInstanceFactory maaif;

	@Nullable
	private volatile Object materialized;


	/**
	 * Create a new lazily initializing decorator for the given AspectInstanceFactory.
	 * <p>为给定的AspectInstanceFactory创建一个新的惰性初始化装饰器
	 *
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
			} else {
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
	@Nullable
	public ClassLoader getAspectClassLoader() {
		return this.maaif.getAspectClassLoader();
	}

	@Override
	public AspectMetadata getAspectMetadata() {
		return this.maaif.getAspectMetadata();
	}

	@Override
	@Nullable
	public Object getAspectCreationMutex() {
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
