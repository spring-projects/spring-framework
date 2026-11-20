/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.Assert;

/**
 * One-way PropertyEditor which can convert from a text String to a
 * {@code java.io.Reader}, interpreting the given String as a Spring
 * resource location (for example, a URL String).
 *
 * <p>Supports Spring-style URL notation: any fully qualified standard URL
 * ("file:", "http:", etc.) and Spring's special "classpath:" pseudo-URL.
 *
 * <p>Note that such readers usually do not get closed by Spring itself!
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see java.io.Reader
 * @see org.springframework.core.io.ResourceEditor
 * @see org.springframework.core.io.ResourceLoader
 * @see InputStreamEditor
 */
public class ReaderEditor extends PropertyEditorSupport {

	private final ResourceEditor resourceEditor;


	/**
	 * Create a new ReaderEditor, using the default ResourceEditor underneath.
	 */
	public ReaderEditor() {
		this.resourceEditor = new ResourceEditor();
	}

	/**
	 * Create a new ReaderEditor, using the given ResourceEditor underneath.
	 * @param resourceEditor the ResourceEditor to use
	 */
	public ReaderEditor(ResourceEditor resourceEditor) {
		Assert.notNull(resourceEditor, "ResourceEditor must not be null");
		this.resourceEditor = resourceEditor;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		this.resourceEditor.setAsText(text);
		Resource resource = (Resource) this.resourceEditor.getValue();
		try {
			setValue(resource != null ? new EncodedResource(resource).getReader() : null);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to retrieve Reader for " + resource, ex);
		}
	}

	/**
	 * This implementation returns {@code null} to indicate that
	 * there is no appropriate text representation.
	 */
	@Override
	public @Nullable String getAsText() {
		return null;
	}

}
