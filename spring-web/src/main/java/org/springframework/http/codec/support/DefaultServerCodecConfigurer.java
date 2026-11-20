/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.support;

import org.springframework.http.codec.ServerCodecConfigurer;

/**
 * Default implementation of {@link ServerCodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultServerCodecConfigurer extends BaseCodecConfigurer implements ServerCodecConfigurer {


	public DefaultServerCodecConfigurer() {
		super(new ServerDefaultCodecsImpl());
	}

	private DefaultServerCodecConfigurer(BaseCodecConfigurer other) {
		super(other);
	}


	@Override
	public ServerDefaultCodecs defaultCodecs() {
		return (ServerDefaultCodecs) super.defaultCodecs();
	}

	@Override
	public DefaultServerCodecConfigurer clone() {
		return new DefaultServerCodecConfigurer(this);
	}

	@Override
	protected BaseDefaultCodecs cloneDefaultCodecs() {
		return new ServerDefaultCodecsImpl((ServerDefaultCodecsImpl) defaultCodecs());
	}
}
