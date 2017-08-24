/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.cache.caffeine;

import java.util.function.Function;

import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;

/**
 * Supplier for {@link Caffeine} instances, constructed from {@link com.github.benmanes.caffeine.cache.CaffeineSpec} values.
 * Allows configuring different expiration, capacity and other caching parameters for different cache names.
 *
 * <p>Requires Caffeine 2.1 or higher.
 *
 * @author Igor Stepanov
 * @since 5.0
 * @see Caffeine
 */
public class CaffeineSupplier implements Function<String, Caffeine<Object, Object>>, EnvironmentAware {

	public static final String CACHE_CAFFEINE_SPEC = "spring.cache.caffeine.spec.%s";

	@Nullable
	private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public Caffeine<Object, Object> apply(String name) {
		String value = this.environment.getProperty(composeKey(name));
		if (value != null) {
			return Caffeine.from(value);
		}
		return null;
	}

	protected String composeKey(String name) {
		return String.format(CACHE_CAFFEINE_SPEC, name);
	}

}
