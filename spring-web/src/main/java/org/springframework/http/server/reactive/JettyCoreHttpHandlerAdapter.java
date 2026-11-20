/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import org.springframework.core.io.buffer.JettyDataBufferFactory;
import org.springframework.util.Assert;

/**
 * Adapt {@link HttpHandler} to the Jetty {@link org.eclipse.jetty.server.Handler} abstraction.
 *
 * @author Greg Wilkins
 * @author Lachlan Roberts
 * @author Arjen Poutsma
 * @since 6.2
 */
public class JettyCoreHttpHandlerAdapter extends Handler.Abstract.NonBlocking {

	private final HttpHandler httpHandler;

	private JettyDataBufferFactory dataBufferFactory = new JettyDataBufferFactory();


	public JettyCoreHttpHandlerAdapter(HttpHandler httpHandler) {
		this.httpHandler = httpHandler;
	}


	public void setDataBufferFactory(JettyDataBufferFactory dataBufferFactory) {
		Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
		this.dataBufferFactory = dataBufferFactory;
	}


	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		this.httpHandler.handle(new JettyCoreServerHttpRequest(request, this.dataBufferFactory),
						new JettyCoreServerHttpResponse(response, this.dataBufferFactory))
				.subscribe(unused -> {}, callback::failed, callback::succeeded);
		return true;
	}

}
