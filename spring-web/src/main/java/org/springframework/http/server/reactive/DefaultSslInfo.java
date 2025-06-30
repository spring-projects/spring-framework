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
