/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
