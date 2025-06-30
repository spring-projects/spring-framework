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

package org.springframework.scheduling.annotation;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.CollectionUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * Abstract base {@code Configuration} class providing common structure for enabling
 * Spring's asynchronous method execution capability.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 * @see EnableAsync
 */
@Configuration(proxyBeanMethods = false)
public abstract class AbstractAsyncConfiguration implements ImportAware {

	protected @Nullable AnnotationAttributes enableAsync;

	protected @Nullable Supplier<? extends @Nullable Executor> executor;

	protected @Nullable Supplier<? extends @Nullable AsyncUncaughtExceptionHandler> exceptionHandler;


	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.enableAsync = AnnotationAttributes.fromMap(
				importMetadata.getAnnotationAttributes(EnableAsync.class.getName()));
		if (this.enableAsync == null) {
			throw new IllegalArgumentException(
					"@EnableAsync is not present on importing class " + importMetadata.getClassName());
		}
	}

	/**
	 * Collect any {@link AsyncConfigurer} beans through autowiring.
	 */
	@Autowired
	@SuppressWarnings("NullAway") // https://github.com/uber/NullAway/issues/1128
	void setConfigurers(ObjectProvider<AsyncConfigurer> configurers) {
		SingletonSupplier<AsyncConfigurer> configurer = SingletonSupplier.ofNullable(() -> {
			List<AsyncConfigurer> candidates = configurers.stream().toList();
			if (CollectionUtils.isEmpty(candidates)) {
				return null;
			}
			if (candidates.size() > 1) {
				throw new IllegalStateException("Only one AsyncConfigurer may exist");
			}
			return candidates.get(0);
		});
		this.executor = adapt(configurer, AsyncConfigurer::getAsyncExecutor);
		this.exceptionHandler = adapt(configurer, AsyncConfigurer::getAsyncUncaughtExceptionHandler);
	}

	private <T> Supplier<@Nullable T> adapt(SingletonSupplier<AsyncConfigurer> supplier, Function<AsyncConfigurer, @Nullable T> provider) {
		return () -> {
			AsyncConfigurer configurer = supplier.get();
			return (configurer != null ? provider.apply(configurer) : null);
		};
	}

}
