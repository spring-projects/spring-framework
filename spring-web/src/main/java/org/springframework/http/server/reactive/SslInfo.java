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

package org.springframework.http.server.reactive;

import java.security.cert.X509Certificate;

import org.jspecify.annotations.Nullable;

/**
 * A holder for SSL session information.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0.2
 * @see javax.net.ssl.SSLSession
 */
public interface SslInfo {

	/**
	 * Return the SSL session ID, if any.
	 */
	@Nullable String getSessionId();

	/**
	 * Return SSL certificates associated with the request, if any.
	 */
	X509Certificate @Nullable [] getPeerCertificates();


	/**
	 * Create {@link SslInfo} configured with the supplied session ID.
	 * @since 7.0
	 */
	static SslInfo from(String sessionId) {
		return new DefaultSslInfo(sessionId, new X509Certificate[0]);
	}

	/**
	 * Create {@link SslInfo} configured with the supplied session ID and certificates.
	 * @since 7.0
	 */
	static SslInfo from(String sessionId, X509Certificate... peerCertificates) {
		return new DefaultSslInfo(sessionId, peerCertificates);
	}

}
