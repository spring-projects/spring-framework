/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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

	private final @Nullable Class<?> resourceLoadClass;

	private final Charset charset;


	JsonLoader(@Nullable Class<?> resourceLoadClass, @Nullable Charset charset) {
		this.resourceLoadClass = resourceLoadClass;
		this.charset = (charset != null ? charset : StandardCharsets.UTF_8);
	}


	@Nullable String getJson(@Nullable CharSequence source) {
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
