/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * @author Sebastien Deleuze
 * @since 7.0
 */
public class JacksonJsonSockJsMessageCodec extends AbstractSockJsMessageCodec {

	private final JsonMapper mapper;


	/**
	 * Construct a new instance with a {@link JsonMapper} customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 * @see JsonMapper#builder()
	 */
	public JacksonJsonSockJsMessageCodec() {
		this(JsonMapper.builder());
	}

	/**
	 * Construct a new instance with the provided {@link JsonMapper.Builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 * @see JsonMapper#builder()
	 */
	public JacksonJsonSockJsMessageCodec(JsonMapper.Builder builder) {
		Assert.notNull(builder, "JsonMapper.Builder must not be null");
		this.mapper = builder.findAndAddModules(JacksonJsonSockJsMessageCodec.class.getClassLoader()).build();
	}

	/**
	 * Construct a new instance with the provided {@link JsonMapper}.
	 * @see JsonMapper#builder()
	 */
	public JacksonJsonSockJsMessageCodec(JsonMapper mapper) {
		Assert.notNull(mapper, "JsonMapper must not be null");
		this.mapper = mapper;
	}


	@Override
	public String @Nullable [] decode(String content) {
		return this.mapper.readValue(content, String[].class);
	}

	@Override
	public String @Nullable [] decodeInputStream(InputStream content) {
		return this.mapper.readValue(content, String[].class);
	}

	@Override
	protected char[] applyJsonQuoting(String content) {
		return JsonStringEncoder.getInstance().quoteAsCharArray(content);
	}

}
