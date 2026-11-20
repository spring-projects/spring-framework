/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.index.processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Set;

/**
 * Marshaller to write {@link CandidateComponentsMetadata} as properties.
 *
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 5.0
 */
abstract class PropertiesMarshaller {

	public static void write(CandidateComponentsMetadata metadata, OutputStream out) throws IOException {
		Properties props = new SortedProperties(true);
		metadata.getItems().forEach(m -> props.put(m.getType(), String.join(",", m.getStereotypes())));
		props.store(out, null);
	}

	public static CandidateComponentsMetadata read(InputStream in) throws IOException {
		CandidateComponentsMetadata result = new CandidateComponentsMetadata();
		Properties props = new Properties();
		props.load(in);
		props.forEach((type, value) -> {
			Set<String> candidates = Set.of(((String) value).split(","));
			result.add(new ItemMetadata((String) type, candidates));
		});
		return result;
	}

}
