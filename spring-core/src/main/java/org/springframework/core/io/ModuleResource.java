/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link Resource} implementation for {@link java.lang.Module} resolution,
 * performing {@link #getInputStream()} access via {@link Module#getResourceAsStream}.
 *
 * <p>Alternatively, consider accessing resources in a module path layout via
 * {@link ClassPathResource} for exported resources, or specifically relative to
 * a {@code Class} via {@link ClassPathResource#ClassPathResource(String, Class)}
 * for local resolution within the containing module of that specific class.
 * In common scenarios, module resources will simply be transparently visible as
 * classpath resources and therefore do not need any special treatment at all.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 6.1
 * @see Module#getResourceAsStream
 * @see ClassPathResource
 */
public class ModuleResource extends AbstractResource {

	private final Module module;

	private final String path;


	/**
	 * Create a new {@code ModuleResource} for the given {@link Module}
	 * and the given resource path.
	 * @param module the runtime module to search within
	 * @param path the resource path within the module
	 */
	public ModuleResource(Module module, String path) {
		Assert.notNull(module, "Module must not be null");
		Assert.notNull(path, "Path must not be null");
		this.module = module;
		this.path = path;
	}


	/**
	 * Return the {@link Module} for this resource.
	 */
	public final Module getModule() {
		return this.module;
	}

	/**
	 * Return the path for this resource.
	 */
	public final String getPath() {
		return this.path;
	}


	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is = this.module.getResourceAsStream(this.path);
		if (is == null) {
			throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
		}
		return is;
	}

	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new ModuleResource(this.module, pathToUse);
	}

	@Override
	@Nullable
	public String getFilename() {
		return StringUtils.getFilename(this.path);
	}

	@Override
	public String getDescription() {
		return "module resource [" + this.path + "]" +
				(this.module.isNamed() ? " from module [" + this.module.getName() + "]" : "");
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		return (this == obj || (obj instanceof ModuleResource that &&
				this.module.equals(that.module) && this.path.equals(that.path)));
	}

	@Override
	public int hashCode() {
		return this.module.hashCode() * 31 + this.path.hashCode();
	}

}
