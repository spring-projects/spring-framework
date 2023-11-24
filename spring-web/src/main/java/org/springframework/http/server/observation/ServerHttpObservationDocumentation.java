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

package org.springframework.http.server.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * Documented {@link io.micrometer.common.KeyValue KeyValues} for the HTTP server observations
 * for Servlet-based web applications.
 * <p>This class is used by automated tools to document KeyValues attached to the HTTP server observations.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public enum ServerHttpObservationDocumentation implements ObservationDocumentation {

	/**
	 * HTTP request observations for Servlet-based servers.
	 */
	HTTP_SERVLET_SERVER_REQUESTS {
		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return DefaultServerRequestObservationConvention.class;
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
		 * Name of HTTP request method or {@value KeyValue#NONE_VALUE} if the request was not received properly.
		 */
		METHOD {
			@Override
			public String asString() {
				return "method";
			}

		},

		/**
		 * HTTP response raw status code, or {@code "UNKNOWN"} if no response was created.
		 */
		STATUS {
			@Override
			public String asString() {
				return "status";
			}
		},

		/**
		 * URI pattern for the matching handler if available, falling back to {@code REDIRECTION} for 3xx responses,
		 * {@code NOT_FOUND} for 404 responses, {@code root} for requests with no path info,
		 * and {@code UNKNOWN} for all other requests.
		 */
		URI {
			@Override
			public String asString() {
				return "uri";
			}
		},

		/**
		 * Name of the exception thrown during the exchange, or {@value KeyValue#NONE_VALUE}} if no exception happened.
		 */
		EXCEPTION {
			@Override
			public String asString() {
				return "exception";
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
		 * HTTP request URI.
		 */
		HTTP_URL {
			@Override
			public String asString() {
				return "http.url";
			}
		}

	}
}
