/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec;

import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.codec.ResourceDecoder;
import org.springframework.core.io.Resource;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;

/**
 * {@code HttpMessageReader} that wraps and delegates to a {@link ResourceDecoder}
 * that extracts the filename from the {@code "Content-Disposition"} header, if
 * available, and passes it as the {@link ResourceDecoder#FILENAME_HINT}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class ResourceHttpMessageReader extends DecoderHttpMessageReader<Resource> {

	public ResourceHttpMessageReader() {
		super(new ResourceDecoder());
	}

	public ResourceHttpMessageReader(ResourceDecoder resourceDecoder) {
		super(resourceDecoder);
	}


	@Override
	protected Map<String, Object> getReadHints(ResolvableType elementType, ReactiveHttpInputMessage message) {
		String filename = message.getHeaders().getContentDisposition().getFilename();
		return (StringUtils.hasText(filename) ?
				Hints.from(ResourceDecoder.FILENAME_HINT, filename) : Hints.none());
	}

	@Override
	protected Map<String, Object> getReadHints(ResolvableType actualType, ResolvableType elementType,
			ServerHttpRequest request, ServerHttpResponse response) {

		return getReadHints(elementType, request);
	}

}
