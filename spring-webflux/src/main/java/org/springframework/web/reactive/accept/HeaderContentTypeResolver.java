/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.accept;

import java.util.List;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolver that looks at the 'Accept' header of the request.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HeaderContentTypeResolver implements RequestedContentTypeResolver {

	@Override
	public List<MediaType> resolveMediaTypes(ServerWebExchange exchange) throws NotAcceptableStatusException {
		try {
			List<MediaType> mediaTypes = exchange.getRequest().getHeaders().getAccept();
			MimeTypeUtils.sortBySpecificity(mediaTypes);
			return (!CollectionUtils.isEmpty(mediaTypes) ? mediaTypes : MEDIA_TYPE_ALL_LIST);
		}
		catch (InvalidMediaTypeException ex) {
			String value = exchange.getRequest().getHeaders().getFirst("Accept");
			throw new NotAcceptableStatusException(
					"Could not parse 'Accept' header [" + value + "]: " + ex.getMessage());
		}
	}

}
