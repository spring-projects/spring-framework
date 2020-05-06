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

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.HttpMessageWriter;

/**
 * Default implementation of {@link ClientCodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultClientCodecConfigurer extends BaseCodecConfigurer implements ClientCodecConfigurer {


	public DefaultClientCodecConfigurer() {
		super(new ClientDefaultCodecsImpl());
		((ClientDefaultCodecsImpl) defaultCodecs()).setPartWritersSupplier(this::getPartWriters);
	}

	private DefaultClientCodecConfigurer(DefaultClientCodecConfigurer other) {
		super(other);
		((ClientDefaultCodecsImpl) defaultCodecs()).setPartWritersSupplier(this::getPartWriters);
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

	private List<HttpMessageWriter<?>> getPartWriters() {
		List<HttpMessageWriter<?>> result = new ArrayList<>();
		result.addAll(this.customCodecs.getTypedWriters().keySet());
		result.addAll(this.defaultCodecs.getBaseTypedWriters());
		result.addAll(this.customCodecs.getObjectWriters().keySet());
		result.addAll(this.defaultCodecs.getBaseObjectWriters());
		result.addAll(this.defaultCodecs.getCatchAllWriters());
		return result;
	}

}
