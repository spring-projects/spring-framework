/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.index.processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Store {@link CandidateComponentsMetadata} on the filesystem.
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
class MetadataStore {

	static final String METADATA_PATH = "META-INF/spring.components";

	private final ProcessingEnvironment environment;


	public MetadataStore(ProcessingEnvironment environment) {
		this.environment = environment;
	}


	public CandidateComponentsMetadata readMetadata() {
		try {
			return readMetadata(getMetadataResource().openInputStream());
		}
		catch (IOException ex) {
			// Failed to read metadata -> ignore.
			return null;
		}
	}

	public void writeMetadata(CandidateComponentsMetadata metadata) throws IOException {
		if (!metadata.getItems().isEmpty()) {
			try (OutputStream outputStream = createMetadataResource().openOutputStream()) {
				PropertiesMarshaller.write(metadata, outputStream);
			}
		}
	}


	private CandidateComponentsMetadata readMetadata(InputStream in) throws IOException {
		try (in) {
			return PropertiesMarshaller.read(in);
		}
	}

	private FileObject getMetadataResource() throws IOException {
		return this.environment.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", METADATA_PATH);
	}

	private FileObject createMetadataResource() throws IOException {
		return this.environment.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", METADATA_PATH);
	}

}
