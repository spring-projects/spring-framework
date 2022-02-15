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

import io.micrometer.api.instrument.Tags;

/**
 * Default {@link R2dbcTagsProvider}.
 *
 * @author Marcin Grzejszczak
 * @since 6.0.0
 */
public class DefaultR2dbcTagsProvider implements R2dbcTagsProvider {
	@Override
	public Tags getLowCardinalityTags(R2dbcContext context) {
		return Tags.of(R2dbcObservation.LowCardinalityTags.BEAN_NAME.of(context.getConnectionFactoryName()),
				R2dbcObservation.LowCardinalityTags.CONNECTION.of(context.getConnectionName()),
				R2dbcObservation.LowCardinalityTags.THREAD.of(context.getThreadName()));
	}
}
