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

package org.springframework.http.server.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * Documented {@link KeyValue KeyValues} for the HTTP server
 * observations for Servlet-based web applications, following the stable OpenTelemetry semantic conventions.
 *
 * <p>This class is used by automated tools to document KeyValues attached to the
 * HTTP server observations.
 *
 * @author Brian Clozel
 * @author Tommy Ludwig
 * @since 7.0
 * @see <a href="https://github.com/open-telemetry/semantic-conventions/blob/v1.36.0/docs/http/http-metrics.md">OpenTelemetry Semantic Conventions for HTTP Metrics (v1.36.0)</a>
 * @see <a href="https://github.com/open-telemetry/semantic-conventions/blob/v1.36.0/docs/http/http-spans.md">OpenTelemetry Semantic Conventions for HTTP Spans (v1.36.0)</a>
 */
public enum OpenTelemetryServerHttpObservationDocumentation implements ObservationDocumentation {

	/**
	 * HTTP request observations for Servlet-based servers.
	 */
	HTTP_SERVLET_SERVER_REQUESTS {
		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return OpenTelemetryServerRequestObservationConvention.class;
		}

		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityKeyNames.values();
		}

		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return HighCardinalityKeyNames.values();
		}

	};

	public enum LowCardinalityKeyNames implements KeyName {

		/**
		 * Name of the HTTP request method or {@value KeyValue#NONE_VALUE} if the
		 * request was not received properly. Normalized to known methods defined in internet standards.
		 */
		METHOD {
			@Override
			public String asString() {
				return "http.request.method";
			}

		},

		/**
		 * HTTP response raw status code, or {@code "UNKNOWN"} if no response was
		 * created.
		 */
		STATUS {
			@Override
			public String asString() {
				return "http.response.status_code";
			}
		},

		/**
		 * URI pattern for the matching handler if available, falling back to
		 * {@code REDIRECTION} for 3xx responses, {@code NOT_FOUND} for 404
		 * responses, {@code root} for requests with no path info, and
		 * {@code UNKNOWN} for all other requests.
		 */
		ROUTE {
			@Override
			public String asString() {
				return "http.route";
			}
		},

		/**
		 * Fully qualified name of the exception thrown during the exchange, or
		 * {@value KeyValue#NONE_VALUE} if no exception was thrown.
		 */
		EXCEPTION {
			@Override
			public String asString() {
				return "error.type";
			}
		},

		/**
		 * The scheme of the original client request, if known (e.g. from Forwarded#proto, X-Forwarded-Proto, or a similar header). Otherwise, the scheme of the immediate peer request.
		 */
		SCHEME {
			@Override
			public String asString() {
				return "url.scheme";
			}
		},

		/**
		 * Outcome of the HTTP server exchange.
		 * @see org.springframework.http.HttpStatus.Series
		 */
		OUTCOME {
			@Override
			public String asString() {
				return "outcome";
			}
		}
	}

	public enum HighCardinalityKeyNames implements KeyName {

		/**
		 * HTTP request URL.
		 */
		URL_PATH {
			@Override
			public String asString() {
				return "url.path";
			}
		},

		/**
		 * Original HTTP method sent by the client in the request line.
		 */
		METHOD_ORIGINAL {
			@Override
			public String asString() {
				return "http.request.method_original";
			}
		}

	}

}
