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

package org.springframework.cache.annotation;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.CollectionUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * Abstract base {@code @Configuration} class providing common structure
 * for enabling Spring's annotation-driven cache management capability.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 3.1
 * @see EnableCaching
 */
@Configuration(proxyBeanMethods = false)
public abstract class AbstractCachingConfiguration implements ImportAware {

	protected @Nullable AnnotationAttributes enableCaching;

	protected @Nullable Supplier<@Nullable CacheManager> cacheManager;

	protected @Nullable Supplier<@Nullable CacheResolver> cacheResolver;

	protected @Nullable Supplier<@Nullable KeyGenerator> keyGenerator;

	protected @Nullable Supplier<@Nullable CacheErrorHandler> errorHandler;


	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.enableCaching = AnnotationAttributes.fromMap(
				importMetadata.getAnnotationAttributes(EnableCaching.class.getName()));
		if (this.enableCaching == null) {
			throw new IllegalArgumentException(
					"@EnableCaching is not present on importing class " + importMetadata.getClassName());
		}
	}

	@Autowired
	void setConfigurers(ObjectProvider<CachingConfigurer> configurers) {
		Supplier<@Nullable CachingConfigurer> configurer = () -> {
			List<CachingConfigurer> candidates = configurers.stream().toList();
			if (CollectionUtils.isEmpty(candidates)) {
				return null;
			}
			if (candidates.size() > 1) {
				throw new IllegalStateException(candidates.size() + " implementations of " +
						"CachingConfigurer were found when only 1 was expected. " +
						"Refactor the configuration such that CachingConfigurer is " +
						"implemented only once or not at all.");
			}
			return candidates.get(0);
		};
		useCachingConfigurer(new CachingConfigurerSupplier(configurer));
	}

	/**
	 * Extract the configuration from the nominated {@link CachingConfigurer}.
	 */
	@SuppressWarnings("NullAway") // https://github.com/uber/NullAway/issues/1128
	protected void useCachingConfigurer(CachingConfigurerSupplier cachingConfigurerSupplier) {
		this.cacheManager = cachingConfigurerSupplier.adapt(CachingConfigurer::cacheManager);
		this.cacheResolver = cachingConfigurerSupplier.adapt(CachingConfigurer::cacheResolver);
		this.keyGenerator = cachingConfigurerSupplier.adapt(CachingConfigurer::keyGenerator);
		this.errorHandler = cachingConfigurerSupplier.adapt(CachingConfigurer::errorHandler);
	}


	protected static class CachingConfigurerSupplier {

		private final SingletonSupplier<CachingConfigurer> supplier;

		public CachingConfigurerSupplier(Supplier<@Nullable CachingConfigurer> supplier) {
			this.supplier = SingletonSupplier.ofNullable(supplier);
		}

		/**
		 * Adapt the {@link CachingConfigurer} supplier to another supplier
		 * provided by the specified mapping function. If the underlying
		 * {@link CachingConfigurer} is {@code null}, {@code null} is returned
		 * and the mapping function is not invoked.
		 * @param provider the provider to use to adapt the supplier
		 * @param <T> the type of the supplier
		 * @return another supplier mapped by the specified function
		 */
		public <T> Supplier<@Nullable T> adapt(Function<CachingConfigurer, @Nullable T> provider) {
			return () -> {
				CachingConfigurer cachingConfigurer = this.supplier.get();
				return (cachingConfigurer != null ? provider.apply(cachingConfigurer) : null);
			};
		}

	}

}
