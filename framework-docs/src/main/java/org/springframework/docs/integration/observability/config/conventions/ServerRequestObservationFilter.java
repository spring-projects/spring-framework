/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.docs.integration.observability.config.conventions;


import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;

import org.springframework.http.server.observation.ServerRequestObservationContext;

public class ServerRequestObservationFilter implements ObservationFilter {

	@Override
	public Observation.Context map(Observation.Context context) {
		if (context instanceof ServerRequestObservationContext serverContext) {
			context.setName("custom.observation.name");
			context.addLowCardinalityKeyValue(KeyValue.of("project", "spring"));
			String customAttribute = (String) serverContext.getCarrier().getAttribute("customAttribute");
			context.addLowCardinalityKeyValue(KeyValue.of("custom.attribute", customAttribute));
		}
		return context;
	}
}
