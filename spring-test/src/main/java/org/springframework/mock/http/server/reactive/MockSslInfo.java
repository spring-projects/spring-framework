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

package org.springframework.mock.http.server.reactive;

import java.security.cert.X509Certificate;

import org.jspecify.annotations.Nullable;

import org.springframework.core.style.DefaultToStringStyler;
import org.springframework.core.style.SimpleValueStyler;
import org.springframework.core.style.ToStringCreator;
import org.springframework.http.server.reactive.SslInfo;

/**
 * Mock implementation of {@link SslInfo} for use in tests without an actual server.
 *
 * @author Sam Brannen
 * @since 7.0
 */
public class MockSslInfo implements SslInfo {

	private @Nullable String sessionId;

	private X509Certificate @Nullable [] peerCertificates;


	/**
	 * Construct {@code MockSslInfo} without a session ID or certificates.
	 */
	public MockSslInfo() {
	}

	/**
	 * Construct {@code MockSslInfo} with a session ID.
	 */
	public MockSslInfo(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * Construct {@code MockSslInfo} with a session ID and certificates.
	 */
	public MockSslInfo(String sessionId, X509Certificate[] peerCertificates) {
		this.sessionId = sessionId;
		this.peerCertificates = peerCertificates;
	}


	/**
	 * Set the SSL session ID.
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public @Nullable String getSessionId() {
		return this.sessionId;
	}

	/**
	 * Set the SSL certificates associated with the request.
	 */
	public void setPeerCertificates(X509Certificate[] peerCertificates) {
		this.peerCertificates = peerCertificates;
	}

	@Override
	public X509Certificate @Nullable [] getPeerCertificates() {
		return this.peerCertificates;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this, new DefaultToStringStyler(new SimpleValueStyler()))
				.append("sessionId", this.sessionId)
				.append("peerCertificates", this.peerCertificates)
				.toString();
	}

}
