/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.rx.web.servlet;

import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.reactivestreams.Publisher;

/**
 * @author Arjen Poutsma
 */
@WebServlet(asyncSupported = true )
public class HttpHandlerServlet extends HttpServlet {

	private static final int BUFFER_SIZE = 4096;

	private HttpHandler handler;

	public void setHandler(HttpHandler handler) {
		this.handler = handler;
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		AsyncContext context = request.startAsync();
		final AsyncContextSynchronizer contextSynchronizer =
				new AsyncContextSynchronizer(context);

		RequestBodyPublisher requestPublisher = new RequestBodyPublisher(contextSynchronizer, BUFFER_SIZE);
		request.getInputStream().setReadListener(requestPublisher);

		ResponseBodySubscriber responseSubscriber = new ResponseBodySubscriber(contextSynchronizer);
		response.getOutputStream().setWriteListener(responseSubscriber);

		Publisher<byte[]> responsePublisher = this.handler.handle(requestPublisher);

		responsePublisher.subscribe(responseSubscriber);
	}

}
