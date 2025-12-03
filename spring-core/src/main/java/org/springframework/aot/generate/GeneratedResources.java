/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.aot.generate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import org.springframework.javapoet.ClassName;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A managed collection of generated resources.
 *
 * <p>This class is stateful, so the same instance should be used for all resource
 * generation.
 *
 * @author Stephane Nicoll
 * @since 7.1
 * @see GeneratedResource
 */
public class GeneratedResources {

	private final NameGenerator nameGenerator;

	private final List<GeneratedResource> resources;

	private final Map<String, GeneratedResource> resourcesByPath;

	private final Map<Owner, GeneratedResource> resourcesByOwner;


	GeneratedResources(NameGenerator nameGenerator) {
		this(nameGenerator, new ArrayList<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
	}

	private GeneratedResources(NameGenerator nameGenerator, List<GeneratedResource> resources,
			Map<String, GeneratedResource> resourcesByPath, Map<Owner, GeneratedResource> resourcesByOwner) {

		this.nameGenerator = nameGenerator;
		this.resources = resources;
		this.resourcesByPath = resourcesByPath;
		this.resourcesByOwner = resourcesByOwner;
	}


	/**
	 * Return the {@link GeneratedResource} at the given {@code path}.
	 * <p>A valid {@code path} is an absolute location on the classpath with
	 * directories separated with {@code /}, and without a leading slash.
	 * For instance {@code com/example/my-resource.txt}.
	 * @param path the absolute path of the resource on the classpath
	 * @return a generated resource
	 */
	public GeneratedResource getOrAdd(String path) {
		Assert.hasLength(path, "'path' must not be empty");
		if (!path.equals(cleanedPath(path))) {
			throw new IllegalArgumentException("Invalid classpath location '%s'".formatted(path));
		}
		return this.resourcesByPath.computeIfAbsent(path, key -> {
			GeneratedResource resource = new GeneratedResource(path);
			this.resources.add(resource);
			return resource;
		});
	}

	private static String cleanedPath(String path) {
		String cleanedPath = StringUtils.cleanPath(path);
		if (cleanedPath.startsWith("/")) {
			cleanedPath = cleanedPath.substring(1);
		}
		return cleanedPath;
	}

	public GeneratedResource getOrAddForFeature(String extension, String featureName) {
		Assert.hasLength(extension, "'extension' must not be empty");
		Assert.hasLength(featureName, "'featureName' must not be empty");
		Owner owner = new Owner(extension, this.nameGenerator.getFeatureNamePrefix(), featureName, null);
		return this.resourcesByOwner.computeIfAbsent(owner,
				key -> createAndAddGeneratedResource(extension, featureName, null));
	}

	public GeneratedResource getOrAddForFeatureComponent(String extension, String featureName,
			ClassName targetComponent) {
		Assert.hasLength(extension, "'extension' must not be empty");
		Assert.hasLength(featureName, "'featureName' must not be empty");
		Assert.notNull(targetComponent, "'targetComponent' must not be null");
		Owner owner = new Owner(extension, this.nameGenerator.getFeatureNamePrefix(), featureName, targetComponent);
		return this.resourcesByOwner.computeIfAbsent(owner,
				key -> createAndAddGeneratedResource(extension, featureName, targetComponent));
	}

	public GeneratedResource addForFeature(String extension, String featureName) {
		Assert.hasLength(extension, "'extension' must not be empty");
		Assert.hasLength(featureName, "'featureName' must not be empty");
		return createAndAddGeneratedResource(extension, featureName, null);
	}

	public GeneratedResource addForFeatureComponent(String extension, String featureName,
			ClassName targetComponent) {
		Assert.hasLength(extension, "'extension' must not be empty");
		Assert.hasLength(featureName, "'featureName' must not be empty");
		Assert.notNull(targetComponent, "'targetComponent' must not be null");
		return createAndAddGeneratedResource(extension, featureName, targetComponent);
	}

	private GeneratedResource createAndAddGeneratedResource(String extension, String featureName,
			@Nullable ClassName targetComponent) {

		String path = this.nameGenerator.generateResourcePath(extension, featureName, targetComponent);
		GeneratedResource generatedResource = new GeneratedResource(path);
		this.resources.add(generatedResource);
		return generatedResource;
	}

	/**
	 * Write the {@link GeneratedResource generated resources} using the given
	 * {@link GeneratedFiles} instance.
	 * @param generatedFiles where to write the generated resources
	 */
	void writeTo(GeneratedFiles generatedFiles) {
		Assert.notNull(generatedFiles, "'generatedFiles' must not be null");
		List<GeneratedResource> generatedResources = new ArrayList<>(this.resources);
		generatedResources.sort(Comparator.comparing(GeneratedResource::getPath));
		for (GeneratedResource generatedResource : generatedResources) {
			generatedResource.writeTo(generatedFiles);
		}
	}

	/**
	 * Create a new instance using the specified feature name prefix to qualify
	 * generated paths for a dedicated round of AOT processing.
	 * @param featureNamePrefix the feature name prefix to use
	 * @return a new instance for the specified feature name prefix
	 */
	GeneratedResources withFeatureNamePrefix(String featureNamePrefix) {
		return new GeneratedResources(this.nameGenerator.withFeatureNamePrefix(featureNamePrefix),
				this.resources, this.resourcesByPath, this.resourcesByOwner);
	}

	private record Owner(String extension, String featureNamePrefix, String featureName, @Nullable ClassName target) {
	}

}
