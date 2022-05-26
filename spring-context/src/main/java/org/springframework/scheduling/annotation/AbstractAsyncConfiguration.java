/*
 * Copyright 2002-2021 the original author or authors.
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
import java.util.stream.Collectors;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
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

	@Nullable
	protected AnnotationAttributes enableAsync;

	@Nullable
	protected Supplier<Executor> executor;

	@Nullable
	protected Supplier<AsyncUncaughtExceptionHandler> exceptionHandler;


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
	void setConfigurers(ObjectProvider<AsyncConfigurer> configurers) {
		Supplier<AsyncConfigurer> configurer = SingletonSupplier.of(() -> {
			List<AsyncConfigurer> candidates = configurers.stream().collect(Collectors.toList());
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

	private <T> Supplier<T> adapt(Supplier<AsyncConfigurer> supplier, Function<AsyncConfigurer, T> provider) {
		return () -> {
			AsyncConfigurer configurer = supplier.get();
			return (configurer != null ? provider.apply(configurer) : null);
		};
	}

}
