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

package org.springframework.web.messaging.stomp.support;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessageHeaders;
import org.springframework.web.messaging.stomp.StompHeaders;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompHeaderMapper {

	private static Log logger = LogFactory.getLog(StompHeaderMapper.class);

	private static final String[][] stompHeaderNames;

	static {
		stompHeaderNames = new String[2][StompHeaders.STANDARD_HEADER_NAMES.size()];
		for (int i=0 ; i < StompHeaders.STANDARD_HEADER_NAMES.size(); i++) {
			stompHeaderNames[0][i] = StompHeaders.STANDARD_HEADER_NAMES.get(i);
			stompHeaderNames[1][i] = "stomp." + StompHeaders.STANDARD_HEADER_NAMES.get(i);
		}
	}


	public Map<String, Object> toMessageHeaders(StompHeaders stompHeaders) {

		Map<String, Object> headers = new HashMap<String, Object>();

		// prefixed STOMP headers
		for (int i=0; i < stompHeaderNames[0].length; i++) {
			String header = stompHeaderNames[0][i];
			if (stompHeaders.containsKey(header)) {
				String prefixedHeader = stompHeaderNames[1][i];
				headers.put(prefixedHeader, stompHeaders.getFirst(header));
			}
		}

		// for generic use (not-prefixed)
		if (stompHeaders.getDestination() != null) {
			headers.put("destination", stompHeaders.getDestination());
		}
		if (stompHeaders.getContentType() != null) {
			headers.put("content-type", stompHeaders.getContentType());
		}

		return headers;
	}

	public void fromMessageHeaders(MessageHeaders messageHeaders, StompHeaders stompHeaders) {

		// prefixed STOMP headers
		for (int i=0; i < stompHeaderNames[0].length; i++) {
			String prefixedHeader = stompHeaderNames[1][i];
			if (messageHeaders.containsKey(prefixedHeader)) {
				String header = stompHeaderNames[0][i];
				stompHeaders.add(header, (String) messageHeaders.get(prefixedHeader));
			}
		}

		// generic (not prefixed)
		String destination = (String) messageHeaders.get("destination");
		if (destination != null) {
			stompHeaders.setDestination(destination);
		}
		Object contentType = messageHeaders.get("content-type");
		if (contentType != null) {
			if (contentType instanceof String) {
				stompHeaders.setContentType(MediaType.valueOf((String) contentType));
			}
			else if (contentType instanceof MediaType) {
				stompHeaders.setContentType((MediaType) contentType);
			}
			else {
				logger.warn("Invalid contentType class: " + contentType.getClass());
			}
		}
	}

}
