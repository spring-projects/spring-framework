/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.support;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.core.env.MapPropertySource;
import org.springframework.lang.Nullable;
import org.springframework.util.function.SupplierUtils;

/**
 * {@link MapPropertySource} backed by a map with dynamically supplied values.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 5.2.5
 */
class DynamicValuesPropertySource extends MapPropertySource {

	static final String PROPERTY_SOURCE_NAME = "Dynamic Test Properties";


	DynamicValuesPropertySource(Map<String, Supplier<Object>> valueSuppliers) {
		super(PROPERTY_SOURCE_NAME, Collections.unmodifiableMap(valueSuppliers));
	}


	@Override
	@Nullable
	public Object getProperty(String name) {
		return SupplierUtils.resolve(super.getProperty(name));
	}

}
