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
		try {
			return PropertiesMarshaller.read(in);
		}
		finally {
			in.close();
		}
	}

	private FileObject getMetadataResource() throws IOException {
		return this.environment.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", METADATA_PATH);
	}

	private FileObject createMetadataResource() throws IOException {
		return this.environment.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", METADATA_PATH);
	}

}
