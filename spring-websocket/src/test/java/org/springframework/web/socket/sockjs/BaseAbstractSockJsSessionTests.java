/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.sockjs;

import org.junit.Before;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketHandler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


/**
 * Base class for {@link AbstractSockJsSession} classes.
 *
 * @author Rossen Stoyanchev
 */
public abstract class BaseAbstractSockJsSessionTests<S extends AbstractSockJsSession> {

	protected WebSocketHandler webSocketHandler;

	protected StubSockJsConfig sockJsConfig;

	protected TaskScheduler taskScheduler;

	protected S session;


	@Before
	public void setUp() {
		this.webSocketHandler = mock(WebSocketHandler.class);
		this.taskScheduler = mock(TaskScheduler.class);

		this.sockJsConfig = new StubSockJsConfig();
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
		assertEquals(isNew, this.session.isNew());
		assertEquals(isOpen, this.session.isOpen());
		assertEquals(isClosed, this.session.isClosed());
	}

}
