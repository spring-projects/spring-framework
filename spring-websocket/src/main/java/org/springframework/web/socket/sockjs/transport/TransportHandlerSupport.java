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

package org.springframework.web.socket.sockjs.transport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.socket.sockjs.SockJsConfiguration;


/**
 * @author Rossen Stoyanchev
 * @sicne 4.0
 */
public abstract class TransportHandlerSupport {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private SockJsConfiguration sockJsConfig;


	public void setSockJsConfiguration(SockJsConfiguration sockJsConfig) {
		this.sockJsConfig = sockJsConfig;
	}

	public SockJsConfiguration getSockJsConfig() {
		return this.sockJsConfig;
	}


}
