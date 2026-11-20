/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * Documented {@link io.micrometer.common.KeyValue KeyValues} for the HTTP server
 * observations for reactive web applications.
 *
 * <p>This class is used by automated tools to document KeyValues attached to the
 * HTTP server observations.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public enum ServerHttpObservationDocumentation implements ObservationDocumentation {

	/**
	 * HTTP request observations for reactive servers.
	 */
	HTTP_REACTIVE_SERVER_REQUESTS {
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
		 * Name of the HTTP request method or {@value KeyValue#NONE_VALUE} if the
		 * request was not received properly.
		 */
		METHOD {
			@Override
			public String asString() {
				return "method";
			}

		},

		/**
		 * HTTP response raw status code, or {@code "UNKNOWN"} if no response was
		 * created.
		 */
		STATUS {
			@Override
			public String asString() {
				return "status";
			}
		},

		/**
		 * URI pattern for the matching handler if available, falling back to
		 * {@code REDIRECTION} for 3xx responses, {@code NOT_FOUND} for 404
		 * responses, {@code root} for requests with no path info, and
		 * {@code UNKNOWN} for all other requests.
		 */
		URI {
			@Override
			public String asString() {
				return "uri";
			}
		},

		/**
		 * Name of the exception thrown during the exchange, or
		 * {@value KeyValue#NONE_VALUE} if no exception was thrown.
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
		 * HTTP request URL.
		 */
		HTTP_URL {
			@Override
			public String asString() {
				return "http.url";
			}
		}

	}

}
