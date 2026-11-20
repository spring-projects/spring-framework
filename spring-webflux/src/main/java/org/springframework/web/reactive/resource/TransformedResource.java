/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * An extension of {@link ByteArrayResource} that a {@link ResourceTransformer}
 * can use to represent an original resource preserving all other information
 * except the content.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class TransformedResource extends ByteArrayResource {

	private final @Nullable String filename;

	private final long lastModified;


	public TransformedResource(Resource original, byte[] transformedContent) {
		super(transformedContent);
		this.filename = original.getFilename();
		try {
			this.lastModified = original.lastModified();
		}
		catch (IOException ex) {
			// should never happen
			throw new IllegalArgumentException(ex);
		}
	}


	@Override
	public @Nullable String getFilename() {
		return this.filename;
	}

	@Override
	public long lastModified() {
		return this.lastModified;
	}

}
