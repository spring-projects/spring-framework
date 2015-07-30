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

package org.springframework.web.socket.sockjs.client;

import java.net.URI;

/**
 * A simple contract for executing the SockJS "Info" request before the SockJS
 * session starts. The request is used to check server capabilities such as
 * whether it permits use of the WebSocket transport.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface InfoReceiver {

	/**
	 * Perform an HTTP request to the SockJS "Info" URL.
	 * and return the resulting JSON response content, or raise an exception.
	 * @param infoUrl the URL to obtain SockJS server information from
	 * @return the body of the response
	 */
	String executeInfoRequest(URI infoUrl);

}
