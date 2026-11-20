/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.DigestUtils;
import org.springframework.util.StreamUtils;

/**
 * A {@code VersionStrategy} that calculates a Hex MD5 hash from the content
 * of the resource and appends it to the file name, for example,
 * {@code "styles/main-e36d2e05253c6c7085a91522ce43a0b4.css"}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 * @see VersionResourceResolver
 */
public class ContentVersionStrategy extends AbstractFileNameVersionStrategy {


	@Override
	public Mono<String> getResourceVersion(Resource resource) {
		Flux<DataBuffer> flux = DataBufferUtils.read(
				resource, DefaultDataBufferFactory.sharedInstance, StreamUtils.BUFFER_SIZE);

		return DataBufferUtils.join(flux)
				.map(buffer -> {
					byte[] result = new byte[buffer.readableByteCount()];
					buffer.read(result);
					DataBufferUtils.release(buffer);
					return DigestUtils.md5DigestAsHex(result);
				});
	}

}
