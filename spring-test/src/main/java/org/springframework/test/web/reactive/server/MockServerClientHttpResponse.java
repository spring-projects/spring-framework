/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server;

import org.springframework.http.client.reactive.ClientHttpResponse;

/**
 * Simple {@link ClientHttpResponse} extension that also exposes a result object
 * from the underlying mock server exchange for further assertions on the state
 * of the server response after the request is performed.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public interface MockServerClientHttpResponse extends ClientHttpResponse {

	/**
	 * Return the result object with the server request and response.
	 */
	Object getServerResult();

}
