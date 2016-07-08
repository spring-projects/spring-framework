/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * Editor for {@code java.nio.file.Path}, to directly populate a Path
 * property instead of using a String property as bridge.
 *
 * <p>Based on {@link Paths#get(URI)}'s resolution algorithm, checking
 * registered NIO file system providers, including the default file system
 * for "file:..." paths. Also supports Spring-style URL notation: any fully
 * qualified standard URL and Spring's special "classpath:" pseudo-URL,
 * as well as Spring's context-specific relative file paths.
 *
 * <p>Note that, in contrast to {@link FileEditor}, relative paths are only
 * supported by Spring's resource abstraction here. Direct {@code Paths.get}
 * resolution in a file system always has to go through the corresponding
 * file system provider's scheme, i.e. "file" for the default file system.
 *
 * @author Juergen Hoeller
 * @since 4.3.2
 * @see java.nio.file.Path
 * @see Paths#get(URI)
 * @see ResourceEditor
 * @see org.springframework.core.io.ResourceLoader
 * @see FileEditor
 * @see URLEditor
 */
public class PathEditor extends PropertyEditorSupport {

	private final ResourceEditor resourceEditor;


	/**
	 * Create a new PathEditor, using the default ResourceEditor underneath.
	 */
	public PathEditor() {
		this.resourceEditor = new ResourceEditor();
	}

	/**
	 * Create a new PathEditor, using the given ResourceEditor underneath.
	 * @param resourceEditor the ResourceEditor to use
	 */
	public PathEditor(ResourceEditor resourceEditor) {
		Assert.notNull(resourceEditor, "ResourceEditor must not be null");
		this.resourceEditor = resourceEditor;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (!text.startsWith("/") && !text.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX)) {
			try {
				URI uri = new URI(text);
				if (uri.getScheme() != null) {
					// Let's try NIO file system providers via Paths.get(URI)
					setValue(Paths.get(uri).normalize());
					return;
				}
			}
			catch (URISyntaxException ex) {
				// Not a valid URI: Let's try as Spring resource location.
			}
			catch (FileSystemNotFoundException ex) {
				// URI scheme not registered for NIO:
				// Let's try URL protocol handlers via Spring's resource mechanism.
			}
		}

		this.resourceEditor.setAsText(text);
		Resource resource = (Resource) this.resourceEditor.getValue();
		try {
			setValue(resource != null ? resource.getFile().toPath() : null);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to retrieve file for " + resource, ex);
		}
	}

	@Override
	public String getAsText() {
		Path value = (Path) getValue();
		return (value != null ? value.toString() : "");
	}

}
