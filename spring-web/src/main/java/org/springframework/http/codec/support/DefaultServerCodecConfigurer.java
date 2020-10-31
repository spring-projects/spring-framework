/*
 * Copyright 2002-2019 the original author or authors.
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
