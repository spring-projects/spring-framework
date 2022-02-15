/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.r2dbc.connection.observability;

import io.micrometer.api.instrument.observation.Observation;

/**
 * {@link Observation.TagsProvider} for {@link R2dbcContext}.
 *
 * @author Marcin Grzejszczak
 * @since 6.0.0
 */
public interface R2dbcTagsProvider extends Observation.TagsProvider<R2dbcContext> {
	@Override
	default boolean supportsContext(Observation.Context context) {
		return context instanceof R2dbcContext;
	}
}
