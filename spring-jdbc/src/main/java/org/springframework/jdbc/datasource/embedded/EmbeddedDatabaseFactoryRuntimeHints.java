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

package org.springframework.jdbc.datasource.embedded;

import java.util.Collections;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.Nullable;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers reflection hints
 * for {@code EmbeddedDataSourceProxy#shutdown} in order to allow it to be used
 * as a bean destroy method.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 */
class EmbeddedDatabaseFactoryRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.reflection().registerTypeIfPresent(classLoader,
				"org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory$EmbeddedDataSourceProxy",
				builder -> builder
						.onReachableType(EmbeddedDatabaseFactory.class)
						.withMethod("shutdown", Collections.emptyList(), ExecutableMode.INVOKE));
	}

}
