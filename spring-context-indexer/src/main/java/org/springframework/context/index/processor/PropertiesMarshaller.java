/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.index.processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Marshaller to write {@link CandidateComponentsMetadata} as properties.
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
abstract class PropertiesMarshaller {

	public static void write(CandidateComponentsMetadata metadata, OutputStream out) throws IOException {
		Properties props = new Properties();
		metadata.getItems().forEach(m -> props.put(m.getType(), String.join(",", m.getStereotypes())));
		props.store(out, "");
	}

	public static CandidateComponentsMetadata read(InputStream in) throws IOException {
		CandidateComponentsMetadata result = new CandidateComponentsMetadata();
		Properties props = new Properties();
		props.load(in);
		props.forEach((type, value) -> {
			Set<String> candidates = new HashSet<>(Arrays.asList(((String) value).split(",")));
			result.add(new ItemMetadata((String) type, candidates));
		});
		return result;
	}

}
