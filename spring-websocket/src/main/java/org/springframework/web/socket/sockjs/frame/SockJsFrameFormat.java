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

package org.springframework.web.socket.sockjs.frame;

/**
 * Applies a transport-specific format to the content of a SockJS frame resulting
 * in a content that can be written out. Primarily for use in HTTP server-side
 * transports that push data.
 *
 * <p>Formatting may vary from simply appending a new line character for XHR
 * polling and streaming transports, to a jsonp-style callback function,
 * surrounding script tags, and more.
 *
 * <p>For the various SockJS frame formats in use, see implementations of
 * {@link  org.springframework.web.socket.sockjs.transport.handler.AbstractHttpSendingTransportHandler#getFrameFormat(org.springframework.http.server.ServerHttpRequest) AbstractHttpSendingTransportHandler.getFrameFormat}
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SockJsFrameFormat {

	String format(SockJsFrame frame);

}
