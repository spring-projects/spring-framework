/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.env;

public class DummyEnvironment implements Environment {

	@Override
	public boolean containsProperty(String key) {
		return false;
	}

	@Override
	public String getProperty(String key) {
		return null;
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		return null;
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType) {
		return null;
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		return null;
	}

	@Override
	@Deprecated
	public <T> Class<T> getPropertyAsClass(String key, Class<T> targetType) {
		return null;
	}

	@Override
	public String getRequiredProperty(String key) throws IllegalStateException {
		return null;
	}

	@Override
	public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
		return null;
	}

	@Override
	public String resolvePlaceholders(String text) {
		return null;
	}

	@Override
	public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
		return null;
	}

	@Override
	public String[] getActiveProfiles() {
		return null;
	}

	@Override
	public String[] getDefaultProfiles() {
		return null;
	}

	@Override
	public boolean acceptsProfiles(String... profiles) {
		return false;
	}

}
