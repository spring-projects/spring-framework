/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.codec.support;

import java.util.Collections;
import java.util.List;

import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.FormHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.ServerSentEventHttpMessageWriter;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.http.codec.multipart.SynchronossPartHttpMessageReader;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Default implementation of {@link ServerCodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultServerCodecConfigurer extends AbstractCodecConfigurer implements ServerCodecConfigurer {

	static final boolean synchronossMultipartPresent =
			ClassUtils.isPresent("org.synchronoss.cloud.nio.multipart.NioMultipartParser",
					DefaultServerCodecConfigurer.class.getClassLoader());


	public DefaultServerCodecConfigurer() {
		super(new ServerDefaultCodecsImpl());
	}

	@Override
	public ServerDefaultCodecs defaultCodecs() {
		return (ServerDefaultCodecs) super.defaultCodecs();
	}


	/**
	 * Default implementation of {@link ServerDefaultCodecs}.
	 */
	private static class ServerDefaultCodecsImpl extends AbstractDefaultCodecs implements ServerDefaultCodecs {

		@Nullable
		private Encoder<?> sseEncoder;

		@Override
		public void serverSentEventEncoder(Encoder<?> encoder) {
			this.sseEncoder = encoder;
		}

		@Override
		List<HttpMessageReader<?>> getTypedReaders() {
			if (!shouldRegisterDefaults()) {
				return Collections.emptyList();
			}
			List<HttpMessageReader<?>> result = super.getTypedReaders();
			result.add(new FormHttpMessageReader());
			if (synchronossMultipartPresent) {
				SynchronossPartHttpMessageReader partReader = new SynchronossPartHttpMessageReader();
				result.add(partReader);
				result.add(new MultipartHttpMessageReader(partReader));
			}
			return result;
		}

		@Override
		List<HttpMessageWriter<?>> getObjectWriters() {
			if (!shouldRegisterDefaults()) {
				return Collections.emptyList();
			}
			List<HttpMessageWriter<?>> result = super.getObjectWriters();
			result.add(new ServerSentEventHttpMessageWriter(getSseEncoder()));
			return result;
		}

		@Nullable
		private Encoder<?> getSseEncoder() {
			if (this.sseEncoder != null) {
				return this.sseEncoder;
			}
			return jackson2Present ? getJackson2JsonEncoder() : null;
		}
	}

}
