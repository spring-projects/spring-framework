/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;

/**
 * Internal helper used to load JSON from various sources.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 6.2
 */
class JsonLoader {

	@Nullable
	private final Class<?> resourceLoadClass;

	private final Charset charset;


	JsonLoader(@Nullable Class<?> resourceLoadClass, @Nullable Charset charset) {
		this.resourceLoadClass = resourceLoadClass;
		this.charset = (charset != null ? charset : StandardCharsets.UTF_8);
	}


	@Nullable
	String getJson(@Nullable CharSequence source) {
		if (source == null) {
			return null;
		}
		String jsonSource = source.toString();
		if (jsonSource.endsWith(".json")) {
			return getJson(new ClassPathResource(jsonSource, this.resourceLoadClass));
		}
		return jsonSource;
	}

	String getJson(Resource source) {
		try {
			return getJson(source.getInputStream());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to load JSON from " + source, ex);
		}
	}

	private String getJson(InputStream source) throws IOException {
		return FileCopyUtils.copyToString(new InputStreamReader(source, this.charset));
	}

}
