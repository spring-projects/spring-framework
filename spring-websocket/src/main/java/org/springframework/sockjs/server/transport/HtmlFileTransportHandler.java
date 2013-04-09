/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.sockjs.server.transport;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.sockjs.server.TransportType;
import org.springframework.sockjs.server.SockJsFrame.DefaultFrameFormat;
import org.springframework.sockjs.server.SockJsFrame.FrameFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.util.JavaScriptUtils;


/**
 * TODO
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HtmlFileTransportHandler extends AbstractStreamingTransportHandler {

	private static final String PARTIAL_HTML_CONTENT;

	static {
		StringBuilder sb = new StringBuilder(
				"<!doctype html>\n" +
				"<html><head>\n" +
				"  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\" />\n" +
				"  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
				"</head><body><h2>Don't panic!</h2>\n" +
				"  <script>\n" +
				"    document.domain = document.domain;\n" +
				"    var c = parent.%s;\n" +
				"    c.start();\n" +
				"    function p(d) {c.message(d);};\n" +
				"    window.onload = function() {c.stop();};\n" +
				"  </script>"
				);

        // Safari needs at least 1024 bytes to parse the website.
        // http://code.google.com/p/browsersec/wiki/Part2#Survey_of_content_sniffing_behaviors
		int spaces = 1024 - sb.length();
		for (int i=0; i < spaces; i++) {
			sb.append(' ');
		}

		PARTIAL_HTML_CONTENT = sb.toString();
	}


	public HtmlFileTransportHandler(SockJsConfiguration sockJsConfig) {
		super(sockJsConfig);
	}

	@Override
	public TransportType getTransportType() {
		return TransportType.HTML_FILE;
	}

	@Override
	protected MediaType getContentType() {
		return new MediaType("text", "html", Charset.forName("UTF-8"));
	}

	@Override
	public void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			AbstractHttpServerSession session) throws Exception {

		String callback = request.getQueryParams().getFirst("c");
		if (! StringUtils.hasText(callback)) {
			response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			response.getBody().write("\"callback\" parameter required".getBytes("UTF-8"));
			return;
		}
		super.handleRequestInternal(request, response, session);
	}

	@Override
	protected void writePrelude(ServerHttpRequest request, ServerHttpResponse response) throws IOException {

		// we already validated the parameter..
		String callback = request.getQueryParams().getFirst("c");

		String html = String.format(PARTIAL_HTML_CONTENT, callback);
		response.getBody().write(html.getBytes("UTF-8"));
		response.getBody().flush();
	}

	@Override
	protected FrameFormat getFrameFormat(ServerHttpRequest request) {
		return new DefaultFrameFormat("<script>\np(\"%s\");\n</script>\r\n") {
			@Override
			protected String preProcessContent(String content) {
				return JavaScriptUtils.javaScriptEscape(content);
			}
		};
	}

}
