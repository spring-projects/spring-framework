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

package org.springframework.web.socket.sockjs.support;

import java.io.IOException;
import java.io.InputStream;

import org.codehaus.jackson.io.JsonStringEncoder;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.util.Assert;


/**
 * A Jackson 1.x codec for encoding and decoding SockJS messages.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class JacksonSockJsMessageCodec extends AbstractSockJsMessageCodec {

	private final ObjectMapper objectMapper;


	public JacksonSockJsMessageCodec() {
		this.objectMapper = new ObjectMapper();
	}

	public JacksonSockJsMessageCodec(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "objectMapper is required");
		this.objectMapper = objectMapper;
	}

	@Override
	public String[] decode(String content) throws IOException {
		return this.objectMapper.readValue(content, String[].class);
	}

	@Override
	public String[] decodeInputStream(InputStream content) throws IOException {
		return this.objectMapper.readValue(content, String[].class);
	}

	@Override
	protected char[] applyJsonQuoting(String content) {
		return JsonStringEncoder.getInstance().quoteAsString(content);
	}

}
