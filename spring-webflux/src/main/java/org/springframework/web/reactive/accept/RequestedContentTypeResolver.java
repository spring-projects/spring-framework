/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.accept;

import java.util.Collections;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Strategy to resolve the requested media types for a {@code ServerWebExchange}.
 *
 * <p>See {@link RequestedContentTypeResolverBuilder} to create a sequence of
 * strategies.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface RequestedContentTypeResolver {

	/**
	 * A singleton list with {@link MediaType#ALL} that is returned from
	 * {@link #resolveMediaTypes} when no specific media types are requested.
	 * @since 5.0.5
	 */
	List<MediaType> MEDIA_TYPE_ALL_LIST = Collections.singletonList(MediaType.ALL);


	/**
	 * Resolve the given request to a list of requested media types. The returned
	 * list is ordered by specificity first and by quality parameter second.
	 * @param exchange the current exchange
	 * @return the requested media types, or {@link #MEDIA_TYPE_ALL_LIST} if none
	 * were requested.
	 * @throws NotAcceptableStatusException if the requested media type is invalid
	 */
	List<MediaType> resolveMediaTypes(ServerWebExchange exchange);

}
