/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.sockjs.frame;

import org.springframework.util.Assert;

/**
 * A default implementation of
 * {@link org.springframework.web.socket.sockjs.frame.SockJsFrameFormat} that relies
 * on {@link java.lang.String#format(String, Object...)}..
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultSockJsFrameFormat implements SockJsFrameFormat {

	private final String format;


	public DefaultSockJsFrameFormat(String format) {
		Assert.notNull(format, "format must not be null");
		this.format = format;
	}


	@Override
	public String format(SockJsFrame frame) {
		return String.format(this.format, preProcessContent(frame.getContent()));
	}

	protected String preProcessContent(String content) {
		return content;
	}

}
