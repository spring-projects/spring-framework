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

import java.net.URI;

import io.micrometer.api.instrument.Tag;
import io.micrometer.api.instrument.observation.Observation;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingObservationHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link TracingObservationHandler} for R2DBC.
 *
 * @author Marcin Grzejszczak
 * @since 6.0.0
 */
public class R2dbcObservationTracingHandler implements TracingObservationHandler<Observation.Context> {

	private static final Log log = LogFactory.getLog(R2dbcObservationTracingHandler.class);

	private final Tracer tracer;

	/**
	 * Creates an instance of {@link R2dbcObservationTracingHandler}.
	 *
	 * @param tracer tracer
	 */
	public R2dbcObservationTracingHandler(Tracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public void onStart(Observation.Context context) {
		Span.Builder builder = this.tracer.spanBuilder()
				.name(context.getContextualName())
				.kind(Span.Kind.CLIENT);
		getTracingContext(context).setSpan(builder.start());
	}

	@Override
	public void onStop(Observation.Context context) {
		Span span = getRequiredSpan(context);
		tagSpan(context, span);
		String connectionName = "r2dbc";
		String url = null;
		for (Tag tag : context.getLowCardinalityTags()) {
			if (tag.getKey().equals(R2dbcObservation.LowCardinalityTags.CONNECTION.getKey())) {
				connectionName = tag.getValue();
			}
			else if (tag.getKey().equals(R2dbcObservation.LowCardinalityTags.URL.getKey())) {
				url = tag.getValue();
			}
		}
		span.remoteServiceName(connectionName);
		if (url != null) {
			try {
				URI uri = URI.create(url);
				span.remoteIpAndPort(uri.getHost(), uri.getPort());
			}
			catch (Exception ex) {
				if (log.isDebugEnabled()) {
					log.debug("Failed to parse the url [" + url + "]. Won't set this value on the span");
				}
			}
		}
		span.end();
	}

	@Override
	public boolean supportsContext(Observation.Context context) {
		return R2dbcObservation.R2DBC_QUERY_OBSERVATION.getName().equals(context.getName());
	}

	@Override
	public Tracer getTracer() {
		return this.tracer;
	}
}
