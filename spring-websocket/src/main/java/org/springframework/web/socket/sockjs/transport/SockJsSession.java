/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.sockjs.transport;

import org.springframework.web.socket.WebSocketSession;

/**
 * SockJS extension of Spring's standard {@link WebSocketSession}.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SockJsSession extends WebSocketSession {

	/**
	 * Return the time (in ms) since the session was last active, or otherwise
	 * if the session is new, then the time since the session was created.
	 */
	long getTimeSinceLastActive();

	/**
	 * Disable the SockJS heartbeat, presumably because a higher-level protocol
	 * has heartbeats enabled for the session already. It is not recommended to
	 * disable this otherwise, as it helps proxies to know the connection is
	 * not hanging.
	 */
	void disableHeartbeat();

}
