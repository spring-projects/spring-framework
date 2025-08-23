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

import java.io.InputStream;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.io.JsonStringEncoder;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.util.Assert;

/**
 * A Jackson 3.x codec for encoding and decoding SockJS messages.
 *
 * <p>The default constructor loads {@link tools.jackson.databind.JacksonModule}s
 * found by {@link MapperBuilder#findModules(ClassLoader)}.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
public class JacksonJsonSockJsMessageCodec extends AbstractSockJsMessageCodec {

	private final JsonMapper jsonMapper;


	/**
	 * Construct a new instance with a {@link JsonMapper} customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 */
	public JacksonJsonSockJsMessageCodec() {
		this.jsonMapper = JsonMapper.builder().findAndAddModules(JacksonJsonSockJsMessageCodec.class.getClassLoader()).build();
	}

	/**
	 * Construct a new instance with the provided {@link JsonMapper}.
	 * @see JsonMapper#builder()
	 * @see MapperBuilder#findAndAddModules(ClassLoader)
	 */
	public JacksonJsonSockJsMessageCodec(JsonMapper jsonMapper) {
		Assert.notNull(jsonMapper, "JsonMapper must not be null");
		this.jsonMapper = jsonMapper;
	}


	@Override
	public String @Nullable [] decode(String content) {
		return this.jsonMapper.readValue(content, String[].class);
	}

	@Override
	public String @Nullable [] decodeInputStream(InputStream content) {
		return this.jsonMapper.readValue(content, String[].class);
	}

	@Override
	protected char[] applyJsonQuoting(String content) {
		return JsonStringEncoder.getInstance().quoteAsCharArray(content);
	}

}
