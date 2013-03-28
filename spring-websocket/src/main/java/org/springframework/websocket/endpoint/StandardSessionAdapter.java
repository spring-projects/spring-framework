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

package org.springframework.websocket.endpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.websocket.Session;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardSessionAdapter implements Session {

	private static Log logger = LogFactory.getLog(StandardSessionAdapter.class);

	private javax.websocket.Session sourceSession;


	public StandardSessionAdapter(javax.websocket.Session sourceSession) {
		this.sourceSession = sourceSession;
	}

	@Override
	public void sendText(String text) throws Exception {
		logger.trace("Sending text message: " + text);
		this.sourceSession.getBasicRemote().sendText(text);
	}

	@Override
	public void close(int code, String reason) throws Exception {
		this.sourceSession = null;
	}

}
