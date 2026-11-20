/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLSession;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Default implementation of {@link SslInfo}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0.2
 */
final class DefaultSslInfo implements SslInfo {

	private final @Nullable String sessionId;

	private final X509Certificate @Nullable [] peerCertificates;


	DefaultSslInfo(@Nullable String sessionId, X509Certificate[] peerCertificates) {
		Assert.notNull(peerCertificates, "No SSL certificates");
		this.sessionId = sessionId;
		this.peerCertificates = peerCertificates;
	}

	DefaultSslInfo(SSLSession session) {
		Assert.notNull(session, "SSLSession is required");
		this.sessionId = initSessionId(session);
		this.peerCertificates = initCertificates(session);
	}


	@Override
	public @Nullable String getSessionId() {
		return this.sessionId;
	}

	@Override
	public X509Certificate @Nullable [] getPeerCertificates() {
		return this.peerCertificates;
	}


	private static @Nullable String initSessionId(SSLSession session) {
		byte [] bytes = session.getId();
		if (bytes == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			String digit = Integer.toHexString(b);
			if (digit.length() < 2) {
				sb.append('0');
			}
			if (digit.length() > 2) {
				digit = digit.substring(digit.length() - 2);
			}
			sb.append(digit);
		}
		return sb.toString();
	}

	private static X509Certificate @Nullable [] initCertificates(SSLSession session) {
		Certificate[] certificates;
		try {
			certificates = session.getPeerCertificates();
		}
		catch (Throwable ex) {
			return null;
		}

		List<X509Certificate> result = new ArrayList<>(certificates.length);
		for (Certificate certificate : certificates) {
			if (certificate instanceof X509Certificate x509Certificate) {
				result.add(x509Certificate);
			}
		}
		return (!result.isEmpty() ? result.toArray(new X509Certificate[0]) : null);
	}

}
