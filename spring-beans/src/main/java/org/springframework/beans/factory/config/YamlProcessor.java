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

package org.springframework.beans.factory.config;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.composer.ComposerException;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TagInspector;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;

import org.springframework.core.CollectionFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for YAML factories.
 *
 * <p>Requires SnakeYAML 2.0 or higher, as of Spring Framework 6.1.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 4.1
 */
public abstract class YamlProcessor {

	private final Log logger = LogFactory.getLog(getClass());

	private ResolutionMethod resolutionMethod = ResolutionMethod.OVERRIDE;

	private Resource[] resources = new Resource[0];

	private List<DocumentMatcher> documentMatchers = Collections.emptyList();

	private boolean matchDefault = true;

	private Set<String> supportedTypes = Collections.emptySet();


	/**
	 * A map of document matchers allowing callers to selectively use only
	 * some of the documents in a YAML resource. In YAML documents are
	 * separated by {@code ---} lines, and each document is converted
	 * to properties before the match is made. For example,
	 * <pre class="code">
	 * environment: dev
	 * url: https://dev.bar.com
	 * name: Developer Setup
	 * ---
	 * environment: prod
	 * url:https://foo.bar.com
	 * name: My Cool App
	 * </pre>
	 * when mapped with
	 * <pre class="code">
	 * setDocumentMatchers(properties -&gt;
	 *     ("prod".equals(properties.getProperty("environment")) ? MatchStatus.FOUND : MatchStatus.NOT_FOUND));
	 * </pre>
	 * would end up as
	 * <pre class="code">
	 * environment=prod
	 * url=https://foo.bar.com
	 * name=My Cool App
	 * </pre>
	 */
	public void setDocumentMatchers(DocumentMatcher... matchers) {
		this.documentMatchers = List.of(matchers);
	}

	/**
	 * Flag indicating that a document for which all the
	 * {@link #setDocumentMatchers(DocumentMatcher...) document matchers} abstain will
	 * nevertheless match. Default is {@code true}.
	 */
	public void setMatchDefault(boolean matchDefault) {
		this.matchDefault = matchDefault;
	}

	/**
	 * Method to use for resolving resources. Each resource will be converted to a Map,
	 * so this property is used to decide which map entries to keep in the final output
	 * from this factory. Default is {@link ResolutionMethod#OVERRIDE}.
	 */
	public void setResolutionMethod(ResolutionMethod resolutionMethod) {
		Assert.notNull(resolutionMethod, "ResolutionMethod must not be null");
		this.resolutionMethod = resolutionMethod;
	}

	/**
	 * Set locations of YAML {@link Resource resources} to be loaded.
	 * @see ResolutionMethod
	 */
	public void setResources(Resource... resources) {
		this.resources = resources;
	}

	/**
	 * Set the supported types that can be loaded from YAML documents.
	 * <p>If no supported types are configured, only Java standard classes
	 * (as defined in {@link org.yaml.snakeyaml.constructor.SafeConstructor})
	 * encountered in YAML documents will be supported.
	 * If an unsupported type is encountered, a {@link ComposerException}
	 * will be thrown when the corresponding YAML node is processed.
	 * @param supportedTypes the supported types, or an empty array to clear the
	 * supported types
	 * @since 5.1.16
	 * @see #createYaml()
	 */
	public void setSupportedTypes(Class<?>... supportedTypes) {
		if (ObjectUtils.isEmpty(supportedTypes)) {
			this.supportedTypes = Collections.emptySet();
		}
		else {
			Assert.noNullElements(supportedTypes, "'supportedTypes' must not contain null elements");
			this.supportedTypes = Arrays.stream(supportedTypes).map(Class::getName)
					.collect(Collectors.toUnmodifiableSet());
		}
	}

	/**
	 * Provide an opportunity for subclasses to process the Yaml parsed from the supplied
	 * resources. Each resource is parsed in turn and the documents inside checked against
	 * the {@link #setDocumentMatchers(DocumentMatcher...) matchers}. If a document
	 * matches it is passed into the callback, along with its representation as Properties.
	 * Depending on the {@link #setResolutionMethod(ResolutionMethod)} not all the
	 * documents will be parsed.
	 * @param callback a callback to delegate to once matching documents are found
	 * @see #createYaml()
	 */
	protected void process(MatchCallback callback) {
		Yaml yaml = createYaml();
		for (Resource resource : this.resources) {
			boolean found = process(callback, yaml, resource);
			if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND && found) {
				return;
			}
		}
	}

	/**
	 * Create the {@link Yaml} instance to use.
	 * <p>The default implementation sets the "allowDuplicateKeys" flag to {@code false},
	 * enabling built-in duplicate key handling.
	 * <p>If custom {@linkplain #setSupportedTypes supported types} have been configured,
	 * the default implementation creates a {@code Yaml} instance that filters out
	 * unsupported types encountered in YAML documents.
	 * If an unsupported type is encountered, a {@link ComposerException} will be
	 * thrown when the node is processed.
	 * @see LoaderOptions#setAllowDuplicateKeys(boolean)
	 */
	protected Yaml createYaml() {
		LoaderOptions loaderOptions = new LoaderOptions();
		loaderOptions.setAllowDuplicateKeys(false);
		loaderOptions.setTagInspector(new SupportedTagInspector());
		DumperOptions dumperOptions = new DumperOptions();
		return new Yaml(new Constructor(loaderOptions), new Representer(dumperOptions),
				dumperOptions, loaderOptions);
	}

	private boolean process(MatchCallback callback, Yaml yaml, Resource resource) {
		int count = 0;
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Loading from YAML: " + resource);
			}
			try (Reader reader = new UnicodeReader(resource.getInputStream())) {
				for (Object object : yaml.loadAll(reader)) {
					if (object != null && process(asMap(object), callback)) {
						count++;
						if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND) {
							break;
						}
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Loaded " + count + " document" + (count > 1 ? "s" : "") +
							" from YAML resource: " + resource);
				}
			}
		}
		catch (IOException ex) {
			handleProcessError(resource, ex);
		}
		return (count > 0);
	}

	private void handleProcessError(Resource resource, IOException ex) {
		if (this.resolutionMethod != ResolutionMethod.FIRST_FOUND &&
				this.resolutionMethod != ResolutionMethod.OVERRIDE_AND_IGNORE) {
			throw new IllegalStateException(ex);
		}
		if (logger.isWarnEnabled()) {
			logger.warn("Could not load map from " + resource + ": " + ex.getMessage());
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private Map<String, Object> asMap(Object object) {
		// YAML can have numbers as keys
		Map<String, Object> result = new LinkedHashMap<>();
		if (!(object instanceof Map map)) {
			// A document can be a text literal
			result.put("document", object);
			return result;
		}

		map.forEach((key, value) -> {
			if (value instanceof Map) {
				value = asMap(value);
			}
			if (key instanceof CharSequence) {
				result.put(key.toString(), value);
			}
			else {
				// It has to be a map key in this case
				result.put("[" + key.toString() + "]", value);
			}
		});
		return result;
	}

	private boolean process(Map<String, Object> map, MatchCallback callback) {
		Properties properties = CollectionFactory.createStringAdaptingProperties();
		properties.putAll(getFlattenedMap(map));

		if (this.documentMatchers.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Merging document (no matchers set): " + map);
			}
			callback.process(properties, map);
			return true;
		}

		MatchStatus result = MatchStatus.ABSTAIN;
		for (DocumentMatcher matcher : this.documentMatchers) {
			MatchStatus match = matcher.matches(properties);
			result = MatchStatus.getMostSpecific(match, result);
			if (match == MatchStatus.FOUND) {
				if (logger.isDebugEnabled()) {
					logger.debug("Matched document with document matcher: " + properties);
				}
				callback.process(properties, map);
				return true;
			}
		}

		if (result == MatchStatus.ABSTAIN && this.matchDefault) {
			if (logger.isDebugEnabled()) {
				logger.debug("Matched document with default matcher: " + map);
			}
			callback.process(properties, map);
			return true;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Unmatched document: " + map);
		}
		return false;
	}

	/**
	 * Return a flattened version of the given map, recursively following any nested Map
	 * or Collection values. Entries from the resulting map retain the same order as the
	 * source. When called with the Map from a {@link MatchCallback} the result will
	 * contain the same values as the {@link MatchCallback} Properties.
	 * @param source the source map
	 * @return a flattened map
	 * @since 4.1.3
	 */
	protected final Map<String, Object> getFlattenedMap(Map<String, Object> source) {
		Map<String, Object> result = new LinkedHashMap<>();
		buildFlattenedMap(result, source, null);
		return result;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, @Nullable String path) {
		source.forEach((key, value) -> {
			if (StringUtils.hasText(path)) {
				if (key.startsWith("[")) {
					key = path + key;
				}
				else {
					key = path + '.' + key;
				}
			}
			if (value instanceof String) {
				result.put(key, value);
			}
			else if (value instanceof Map map) {
				// Need a compound key
				buildFlattenedMap(result, map, key);
			}
			else if (value instanceof Collection collection) {
				// Need a compound key
				if (collection.isEmpty()) {
					result.put(key, "");
				}
				else {
					int count = 0;
					for (Object object : collection) {
						buildFlattenedMap(result, Collections.singletonMap(
								"[" + (count++) + "]", object), key);
					}
				}
			}
			else {
				result.put(key, (value != null ? value : ""));
			}
		});
	}


	/**
	 * Callback interface used to process the YAML parsing results.
	 */
	@FunctionalInterface
	public interface MatchCallback {

		/**
		 * Process the given representation of the parsing results.
		 * @param properties the properties to process (as a flattened
		 * representation with indexed keys in case of a collection or map)
		 * @param map the result map (preserving the original value structure
		 * in the YAML document)
		 */
		void process(Properties properties, Map<String, Object> map);
	}


	/**
	 * Strategy interface used to test if properties match.
	 */
	@FunctionalInterface
	public interface DocumentMatcher {

		/**
		 * Test if the given properties match.
		 * @param properties the properties to test
		 * @return the status of the match
		 */
		MatchStatus matches(Properties properties);
	}


	/**
	 * Status returned from {@link DocumentMatcher#matches(java.util.Properties)}.
	 */
	public enum MatchStatus {

		/**
		 * A match was found.
		 */
		FOUND,

		/**
		 * No match was found.
		 */
		NOT_FOUND,

		/**
		 * The matcher should not be considered.
		 */
		ABSTAIN;

		/**
		 * Compare two {@link MatchStatus} items, returning the most specific status.
		 */
		public static MatchStatus getMostSpecific(MatchStatus a, MatchStatus b) {
			return (a.ordinal() < b.ordinal() ? a : b);
		}
	}


	/**
	 * Method to use for resolving resources.
	 */
	public enum ResolutionMethod {

		/**
		 * Replace values from earlier in the list.
		 */
		OVERRIDE,

		/**
		 * Replace values from earlier in the list, ignoring any failures.
		 */
		OVERRIDE_AND_IGNORE,

		/**
		 * Take the first resource in the list that exists and use just that.
		 */
		FIRST_FOUND
	}

	private class SupportedTagInspector implements TagInspector {

		@Override
		public boolean isGlobalTagAllowed(Tag tag) {
			return supportedTypes.contains(tag.getClassName());
		}
	}

}
