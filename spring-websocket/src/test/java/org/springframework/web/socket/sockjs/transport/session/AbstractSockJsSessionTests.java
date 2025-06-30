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

package org.springframework.web.socket.sockjs.transport.session;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Base class for SockJS Session tests classes.
 *
 * @author Rossen Stoyanchev
 */
abstract class AbstractSockJsSessionTests<S extends AbstractSockJsSession> {

	protected WebSocketHandler webSocketHandler = mock();

	protected TaskScheduler taskScheduler = mock();

	protected StubSockJsServiceConfig sockJsConfig = new StubSockJsServiceConfig();

	protected S session;


	@BeforeEach
	protected void setUp() {
		this.sockJsConfig.setTaskScheduler(this.taskScheduler);
		this.session = initSockJsSession();
	}

	protected abstract S initSockJsSession();

	protected void assertNew() {
		assertState(true, false, false);
	}

	protected void assertOpen() {
		assertState(false, true, false);
	}

	protected void assertClosed() {
		assertState(false, false, true);
	}

	private void assertState(boolean isNew, boolean isOpen, boolean isClosed) {
		assertThat(this.session.isNew()).isEqualTo(isNew);
		assertThat(this.session.isOpen()).isEqualTo(isOpen);
		assertThat(this.session.isClosed()).isEqualTo(isClosed);
	}

}
