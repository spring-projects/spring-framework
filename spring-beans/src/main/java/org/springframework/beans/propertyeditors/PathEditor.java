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
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

/**
 * Editor for {@code java.nio.file.Path}, to directly populate a Path
 * property instead of using a String property as bridge.
 *
 * <p>Based on {@link Paths#get(URI)}'s resolution algorithm, checking
 * registered NIO file system providers, including the default file system
 * for "file:..." paths. Also supports Spring-style URL notation: any fully
 * qualified standard URL and Spring's special "classpath:" pseudo-URL, as
 * well as Spring's context-specific relative file paths. As a fallback, a
 * path will be resolved in the file system via {@code Paths#get(String)}
 * if no existing context-relative resource could be found.
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
		boolean nioPathCandidate = !text.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX);
		if (nioPathCandidate && !text.startsWith("/")) {
			try {
				URI uri = new URI(text);
				String scheme = uri.getScheme();
				if (scheme != null) {
					// No NIO candidate except for "C:" style drive letters
					nioPathCandidate = (scheme.length() == 1);
					// Let's try NIO file system providers via Paths.get(URI)
					setValue(Paths.get(uri).normalize());
					return;
				}
			}
			catch (URISyntaxException ex) {
				// Not a valid URI; potentially a Windows-style path after
				// a file prefix (let's try as Spring resource location)
				nioPathCandidate = !text.startsWith(ResourceUtils.FILE_URL_PREFIX);
			}
			catch (FileSystemNotFoundException | IllegalArgumentException ex) {
				// URI scheme not registered for NIO or not meeting Paths requirements:
				// let's try URL protocol handlers via Spring's resource mechanism.
			}
		}

		this.resourceEditor.setAsText(text);
		Resource resource = (Resource) this.resourceEditor.getValue();
		if (resource == null) {
			setValue(null);
		}
		else if (nioPathCandidate && !resource.exists()) {
			setValue(Paths.get(text).normalize());
		}
		else {
			try {
				setValue(resource.getFile().toPath());
			}
			catch (IOException ex) {
				String msg = "Could not resolve \"" + text + "\" to 'java.nio.file.Path' for " + resource + ": " +
						ex.getMessage();
				if (nioPathCandidate) {
					msg += " - In case of ambiguity, consider adding the 'file:' prefix for an explicit reference " +
							"to a file system resource of the same name: \"file:" + text + "\"";
				}
				throw new IllegalArgumentException(msg);
			}
		}
	}

	@Override
	public String getAsText() {
		Path value = (Path) getValue();
		return (value != null ? value.toString() : "");
	}

}
