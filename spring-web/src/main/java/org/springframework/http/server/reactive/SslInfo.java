/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
