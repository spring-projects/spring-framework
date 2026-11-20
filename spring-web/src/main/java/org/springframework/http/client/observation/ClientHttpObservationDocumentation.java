/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

import org.springframework.http.client.ClientHttpRequestFactory;


/**
 * Documented {@link io.micrometer.common.KeyValue KeyValues} for
 * {@link ClientHttpRequestFactory HTTP client} observations.
 *
 * <p>This class is used by automated tools to document KeyValues attached to the
 * HTTP client observations.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public enum ClientHttpObservationDocumentation implements ObservationDocumentation {

	/**
	 * HTTP exchanges observations for clients.
	 */
	HTTP_CLIENT_EXCHANGES {
		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return DefaultClientRequestObservationConvention.class;
		}

		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityKeyNames.values();
		}

		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return new KeyName[] {HighCardinalityKeyNames.HTTP_URL};
		}
	};


	public enum LowCardinalityKeyNames implements KeyName {

		/**
		 * Name of HTTP request method or {@value KeyValue#NONE_VALUE} if the
		 * request could not be created.
		 */
		METHOD {
			@Override
			public String asString() {
				return "method";
			}
		},

		/**
		 * URI template used for HTTP request, or {@value KeyValue#NONE_VALUE} if
		 * none was provided.
		 * <p>The protocol, host and port part of the URI are not considered.
		 */
		URI {
			@Override
			public String asString() {
				return "uri";
			}
		},

		/**
		 * HTTP response raw status code, or {@code "IO_ERROR"} in case of {@code IOException},
		 * or {@code "CLIENT_ERROR"} if no response was received.
		 */
		STATUS {
			@Override
			public String asString() {
				return "status";
			}
		},

		/**
		 * Client name derived from the request URI host.
		 * @since 6.0.5
		 */
		CLIENT_NAME {
			@Override
			public String asString() {
				return "client.name";
			}
		},

		/**
		 * Name of the exception thrown during the exchange, or
		 * {@value KeyValue#NONE_VALUE} if no exception happened.
		 */
		EXCEPTION {
			@Override
			public String asString() {
				return "exception";
			}
		},

		/**
		 * Outcome of the HTTP client exchange.
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
