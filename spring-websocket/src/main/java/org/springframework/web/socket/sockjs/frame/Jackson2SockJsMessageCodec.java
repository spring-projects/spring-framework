/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.frame;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;

/**
 * A Jackson 2.x codec for encoding and decoding SockJS messages.
 *
 * <p>It customizes Jackson's default properties with the following ones:
 * <ul>
 * <li>{@link MapperFeature#DEFAULT_VIEW_INCLUSION} is disabled</li>
 * <li>{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} is disabled</li>
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @deprecated since 7.0 in favor of {@link JacksonJsonSockJsMessageCodec}
 */
@Deprecated(since = "7.0", forRemoval = true)
@SuppressWarnings("removal")
public class Jackson2SockJsMessageCodec extends AbstractSockJsMessageCodec {

	private final ObjectMapper objectMapper;


	public Jackson2SockJsMessageCodec() {
		this.objectMapper = Jackson2ObjectMapperBuilder.json().build();
	}

	public Jackson2SockJsMessageCodec(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.objectMapper = objectMapper;
	}


	@Override
	public String @Nullable [] decode(String content) throws IOException {
		return this.objectMapper.readValue(content, String[].class);
	}

	@Override
	public String @Nullable [] decodeInputStream(InputStream content) throws IOException {
		return this.objectMapper.readValue(content, String[].class);
	}

	@Override
	protected char[] applyJsonQuoting(String content) {
		return JsonStringEncoder.getInstance().quoteAsString(content);
	}

}
