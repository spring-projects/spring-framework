/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.jms.connection;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link UserCredentialsConnectionFactoryAdapter}.
 *
 * @author Stephane Nicoll
 */
class UserCredentialsConnectionFactoryAdapterTests {

	private static final JMSContext MOCK_CONTEXT = mock(JMSContext.class);

	private final ConnectionFactory target;

	private final UserCredentialsConnectionFactoryAdapter adapter;

	UserCredentialsConnectionFactoryAdapterTests() {
		this.target = mock(ConnectionFactory.class);
		this.adapter = new UserCredentialsConnectionFactoryAdapter();
		this.adapter.setTargetConnectionFactory(this.target);
	}

	@Test
	void createContextWhenNoAuthentication() {
		given(this.target.createContext()).willReturn(MOCK_CONTEXT);
		assertThat(this.adapter.createContext()).isSameAs(MOCK_CONTEXT);
		verify(this.target).createContext();
		verifyNoMoreInteractions(this.target);
	}

	@Test
	void createContextWhenAuthentication() {
		this.adapter.setUsername("user");
		this.adapter.setPassword("password");
		given(this.target.createContext("user", "password")).willReturn(MOCK_CONTEXT);
		assertThat(this.adapter.createContext()).isSameAs(MOCK_CONTEXT);
		verify(this.target).createContext("user", "password");
		verifyNoMoreInteractions(this.target);
	}

	@Test
	void createContextWhenThreadLevelAuthentication() {
		this.adapter.setCredentialsForCurrentThread("user", "password");
		given(this.target.createContext("user", "password")).willReturn(MOCK_CONTEXT);
		assertThat(this.adapter.createContext()).isSameAs(MOCK_CONTEXT);
		verify(this.target).createContext("user", "password");
		verifyNoMoreInteractions(this.target);
	}

	@Test
	void createContextWhenAuthenticationAndThreadLevelAuthentication() {
		this.adapter.setCredentialsForCurrentThread("specific", "secret");
		this.adapter.setUsername("user");
		this.adapter.setPassword("password");
		given(this.target.createContext("specific", "secret")).willReturn(MOCK_CONTEXT);
		assertThat(this.adapter.createContext()).isSameAs(MOCK_CONTEXT);
		verify(this.target).createContext("specific", "secret");
		verifyNoMoreInteractions(this.target);
	}

	@Test
	void createContextWithSessionModeWhenNoAuthentication() {
		given(this.target.createContext(1)).willReturn(MOCK_CONTEXT);
		assertThat(this.adapter.createContext(1)).isSameAs(MOCK_CONTEXT);
		verify(this.target).createContext(1);
		verifyNoMoreInteractions(this.target);
	}

	@Test
	void createContextWithSessionModeWhenAuthentication() {
		this.adapter.setUsername("user");
		this.adapter.setPassword("password");
		given(this.target.createContext("user", "password", 1)).willReturn(MOCK_CONTEXT);
		assertThat(this.adapter.createContext(1)).isSameAs(MOCK_CONTEXT);
		verify(this.target).createContext("user", "password", 1);
		verifyNoMoreInteractions(this.target);
	}

	@Test
	void createContextWithSessionModeWhenThreadLevelAuthentication() {
		this.adapter.setCredentialsForCurrentThread("user", "password");
		given(this.target.createContext("user", "password", 1)).willReturn(MOCK_CONTEXT);
		assertThat(this.adapter.createContext(1)).isSameAs(MOCK_CONTEXT);
		verify(this.target).createContext("user", "password", 1);
		verifyNoMoreInteractions(this.target);
	}

	@Test
	void createContextWithSessionModeWhenAuthenticationAndThreadLevelAuthentication() {
		this.adapter.setCredentialsForCurrentThread("specific", "secret");
		this.adapter.setUsername("user");
		this.adapter.setPassword("password");
		given(this.target.createContext("specific", "secret", 1)).willReturn(MOCK_CONTEXT);
		assertThat(this.adapter.createContext(1)).isSameAs(MOCK_CONTEXT);
		verify(this.target).createContext("specific", "secret", 1);
		verifyNoMoreInteractions(this.target);
	}

	@Test
	void createContextWithUsernamePasswordIgnoresAuthentication() {
		this.adapter.setUsername("user");
		this.adapter.setPassword("password");
		given(this.target.createContext("specific", "secret")).willReturn(MOCK_CONTEXT);
		assertThat(this.adapter.createContext("specific", "secret")).isSameAs(MOCK_CONTEXT);
		verify(this.target).createContext("specific", "secret");
		verifyNoMoreInteractions(this.target);
	}

	@Test
	void createContextWithSessionModeAndUsernamePasswordIgnoresAuthentication() {
		this.adapter.setUsername("user");
		this.adapter.setPassword("password");
		given(this.target.createContext("specific", "secret", 1)).willReturn(MOCK_CONTEXT);
		assertThat(this.adapter.createContext("specific", "secret", 1)).isSameAs(MOCK_CONTEXT);
		verify(this.target).createContext("specific", "secret", 1);
		verifyNoMoreInteractions(this.target);
	}

}
