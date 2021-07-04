/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.socket.sockjs.client;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;

/**
 * A component that can execute the SockJS "Info" request that needs to be
 * performed before the SockJS session starts in order to check server endpoint
 * capabilities such as whether the endpoint permits use of WebSocket.
 *
 * <p>Typically {@link XhrTransport} implementations are also implementations
 * of this contract.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see AbstractXhrTransport
 */
public interface InfoReceiver {

	/**
	 * Perform an HTTP request to the SockJS "Info" URL.
	 * and return the resulting JSON response content, or raise an exception.
	 * <p>Note that as of 4.2 this method accepts a {@code headers} parameter.
	 * @param infoUrl the URL to obtain SockJS server information from
	 * @param headers the headers to use for the request
	 * @return the body of the response
	 */
	String executeInfoRequest(URI infoUrl, @Nullable HttpHeaders headers);

}
