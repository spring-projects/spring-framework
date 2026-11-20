/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.support;

import org.springframework.http.codec.ClientCodecConfigurer;

/**
 * Default implementation of {@link ClientCodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultClientCodecConfigurer extends BaseCodecConfigurer implements ClientCodecConfigurer {


	public DefaultClientCodecConfigurer() {
		super(new ClientDefaultCodecsImpl());
	}

	private DefaultClientCodecConfigurer(DefaultClientCodecConfigurer other) {
		super(other);
	}


	@Override
	public ClientDefaultCodecs defaultCodecs() {
		return (ClientDefaultCodecs) super.defaultCodecs();
	}

	@Override
	public DefaultClientCodecConfigurer clone() {
		return new DefaultClientCodecConfigurer(this);
	}

	@Override
	protected BaseDefaultCodecs cloneDefaultCodecs() {
		return new ClientDefaultCodecsImpl((ClientDefaultCodecsImpl) defaultCodecs());
	}


}
