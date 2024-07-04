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

package org.springframework.ui.freemarker;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * Factory that configures a FreeMarker {@link Configuration}.
 *
 * <p>Can be used standalone, but typically you will either use
 * {@link FreeMarkerConfigurationFactoryBean} for preparing a {@code Configuration}
 * as a bean reference, or {@code FreeMarkerConfigurer} for web views.
 *
 * <p>The optional "configLocation" property sets the location of a FreeMarker
 * properties file, within the current application. FreeMarker properties can be
 * overridden via "freemarkerSettings". All of these properties will be set by
 * calling FreeMarker's {@code Configuration.setSettings()} method and are
 * subject to constraints set by FreeMarker.
 *
 * <p>The "freemarkerVariables" property can be used to specify a Map of
 * shared variables that will be applied to the {@code Configuration} via the
 * {@code setAllSharedVariables()} method. Like {@code setSettings()},
 * these entries are subject to FreeMarker constraints.
 *
 * <p>The simplest way to use this class is to specify a "templateLoaderPath";
 * FreeMarker does not need any further configuration then.
 *
 * <p>Note: Spring's FreeMarker support requires FreeMarker 2.3.26 or higher.
 *
 * @author Darren Davison
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 03.03.2004
 * @see #setConfigLocation
 * @see #setFreemarkerSettings
 * @see #setFreemarkerVariables
 * @see #setTemplateLoaderPath
 * @see #createConfiguration
 * @see FreeMarkerConfigurationFactoryBean
 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer
 * @see freemarker.template.Configuration
 */
public class FreeMarkerConfigurationFactory {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private Resource configLocation;

	@Nullable
	private Properties freemarkerSettings;

	@Nullable
	private Map<String, Object> freemarkerVariables;

	@Nullable
	private String defaultEncoding;

	private final List<TemplateLoader> templateLoaders = new ArrayList<>();

	@Nullable
	private List<TemplateLoader> preTemplateLoaders;

	@Nullable
	private List<TemplateLoader> postTemplateLoaders;

	@Nullable
	private String[] templateLoaderPaths;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	private boolean preferFileSystemAccess = true;


	/**
	 * Set the location of the FreeMarker config file.
	 * <p>Alternatively, you can specify all settings locally.
	 * @see #setFreemarkerSettings
	 * @see #setTemplateLoaderPath
	 */
	public void setConfigLocation(Resource resource) {
		this.configLocation = resource;
	}

	/**
	 * Set properties that contain well-known FreeMarker keys which will be
	 * passed to FreeMarker's {@code Configuration.setSettings} method.
	 * @see freemarker.template.Configuration#setSettings
	 */
	public void setFreemarkerSettings(Properties settings) {
		this.freemarkerSettings = settings;
	}

	/**
	 * Set a Map that contains well-known FreeMarker objects which will be passed
	 * to FreeMarker's {@code Configuration.setAllSharedVariables()} method.
	 * @see freemarker.template.Configuration#setAllSharedVariables
	 */
	public void setFreemarkerVariables(Map<String, Object> variables) {
		this.freemarkerVariables = variables;
	}

	/**
	 * Set the default encoding for the FreeMarker {@link Configuration}, which
	 * is used to decode byte sequences to character sequences when reading template
	 * files.
	 * <p>If not specified, FreeMarker will read template files using the platform
	 * file encoding (defined by the JVM system property {@code file.encoding})
	 * or UTF-8 if the platform file encoding is undefined.
	 * <p>Note that the supplied encoding may or may not be used for template
	 * rendering. See the documentation for Spring's {@code FreeMarkerView} and
	 * {@code FreeMarkerViewResolver} implementations for further details.
	 * @see #setDefaultCharset(Charset)
	 * @see freemarker.template.Configuration#setDefaultEncoding
	 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerView#setEncoding
	 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerView#setContentType
	 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver#setContentType
	 * @see org.springframework.web.reactive.result.view.freemarker.FreeMarkerView#setEncoding
	 * @see org.springframework.web.reactive.result.view.freemarker.FreeMarkerView#setSupportedMediaTypes
	 * @see org.springframework.web.reactive.result.view.freemarker.FreeMarkerViewResolver#setSupportedMediaTypes
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * Set the {@link Charset} for the default encoding for the FreeMarker
	 * {@link Configuration}, which is used to decode byte sequences to character
	 * sequences when reading template files.
	 * <p>See {@link #setDefaultEncoding(String)} for details.
	 * @since 6.2
	 * @see java.nio.charset.StandardCharsets
	 */
	public void setDefaultCharset(Charset defaultCharset) {
		this.defaultEncoding = defaultCharset.name();
	}

	/**
	 * Set a list of {@link TemplateLoader TemplateLoaders} that will be used to
	 * search for templates.
	 * <p>For example, one or more custom loaders such as database loaders could
	 * be configured and injected here.
	 * <p>The {@code TemplateLoaders} specified here will be registered <i>before</i>
	 * the default template loaders that this factory registers (such as loaders
	 * for specified "templateLoaderPaths" or any loaders registered in
	 * {@link #postProcessTemplateLoaders}).
	 * @see #setTemplateLoaderPaths
	 * @see #postProcessTemplateLoaders
	 */
	public void setPreTemplateLoaders(TemplateLoader... preTemplateLoaders) {
		this.preTemplateLoaders = Arrays.asList(preTemplateLoaders);
	}

	/**
	 * Set a list of {@link TemplateLoader TemplateLoaders} that will be used to
	 * search for templates.
	 * <p>For example, one or more custom loaders such as database loaders could
	 * be configured and injected here.
	 * <p>The {@code TemplateLoaders} specified here will be registered <i>after</i>
	 * the default template loaders that this factory registers (such as loaders
	 * for specified "templateLoaderPaths" or any loaders registered in
	 * {@link #postProcessTemplateLoaders}).
	 * @see #setTemplateLoaderPaths
	 * @see #postProcessTemplateLoaders
	 */
	public void setPostTemplateLoaders(TemplateLoader... postTemplateLoaders) {
		this.postTemplateLoaders = Arrays.asList(postTemplateLoaders);
	}

	/**
	 * Set the Freemarker template loader path via a Spring resource location.
	 * <p>See the "templateLoaderPaths" property for details on path handling.
	 * @see #setTemplateLoaderPaths
	 */
	public void setTemplateLoaderPath(String templateLoaderPath) {
		this.templateLoaderPaths = new String[] {templateLoaderPath};
	}

	/**
	 * Set multiple Freemarker template loader paths via Spring resource locations.
	 * <p>When populated via a String, standard URLs like "file:" and "classpath:"
	 * pseudo URLs are supported, as understood by ResourceEditor. Allows for
	 * relative paths when running in an {@code ApplicationContext}.
	 * <p>Will define a path for the default FreeMarker template loader. If a
	 * specified resource cannot be resolved to a {@code java.io.File}, a generic
	 * {@link SpringTemplateLoader} will be used, without modification detection.
	 * <p>To enforce the use of {@code SpringTemplateLoader}, i.e. to not resolve
	 * a path as file system resource in any case, turn off the "preferFileSystemAccess"
	 * flag. See the latter's javadoc for details.
	 * <p>If you wish to specify your own list of TemplateLoaders, do not set this
	 * property and instead use {@link #setPostTemplateLoaders(TemplateLoader...)}.
	 * @see org.springframework.core.io.ResourceEditor
	 * @see org.springframework.context.ApplicationContext#getResource
	 * @see freemarker.template.Configuration#setDirectoryForTemplateLoading
	 * @see SpringTemplateLoader
	 * @see #setPreferFileSystemAccess(boolean)
	 */
	public void setTemplateLoaderPaths(String... templateLoaderPaths) {
		this.templateLoaderPaths = templateLoaderPaths;
	}

	/**
	 * Set the {@link ResourceLoader} to use for loading FreeMarker template files.
	 * <p>The default is {@link DefaultResourceLoader}. Will get overridden by the
	 * {@code ApplicationContext} if running in a context.
	 * @see org.springframework.core.io.DefaultResourceLoader
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Return the {@link ResourceLoader} to use for loading FreeMarker template files.
	 */
	protected ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	/**
	 * Set whether to prefer file system access for template loading.
	 * <p>File system access enables hot detection of template changes.
	 * <p>If this is enabled, FreeMarkerConfigurationFactory will try to resolve
	 * the specified "templateLoaderPath" as file system resource (which will work
	 * for expanded class path resources and ServletContext resources too).
	 * <p>Default is "true". Turn this off to always load via {@link SpringTemplateLoader}
	 * (i.e. as stream, without hot detection of template changes), which might
	 * be necessary if some of your templates reside in an expanded classes
	 * directory while others reside in jar files.
	 * @see #setTemplateLoaderPath
	 */
	public void setPreferFileSystemAccess(boolean preferFileSystemAccess) {
		this.preferFileSystemAccess = preferFileSystemAccess;
	}

	/**
	 * Return whether to prefer file system access for template loading.
	 */
	protected boolean isPreferFileSystemAccess() {
		return this.preferFileSystemAccess;
	}


	/**
	 * Prepare the FreeMarker {@link Configuration} and return it.
	 * @return the FreeMarker {@code Configuration} object
	 * @throws IOException if the config file wasn't found
	 * @throws TemplateException on FreeMarker initialization failure
	 */
	public Configuration createConfiguration() throws IOException, TemplateException {
		Configuration config = newConfiguration();
		Properties props = new Properties();

		// Load config file if specified.
		if (this.configLocation != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Loading FreeMarker configuration from " + this.configLocation);
			}
			PropertiesLoaderUtils.fillProperties(props, this.configLocation);
		}

		// Merge local properties if specified.
		if (this.freemarkerSettings != null) {
			props.putAll(this.freemarkerSettings);
		}

		// FreeMarker will only accept known keys in its setSettings and
		// setAllSharedVariables methods.
		if (!props.isEmpty()) {
			config.setSettings(props);
		}

		if (!CollectionUtils.isEmpty(this.freemarkerVariables)) {
			config.setAllSharedVariables(new SimpleHash(this.freemarkerVariables, config.getObjectWrapper()));
		}

		if (this.defaultEncoding != null) {
			config.setDefaultEncoding(this.defaultEncoding);
		}

		List<TemplateLoader> templateLoaders = new ArrayList<>(this.templateLoaders);

		// Register template loaders that are supposed to kick in early.
		if (this.preTemplateLoaders != null) {
			templateLoaders.addAll(this.preTemplateLoaders);
		}

		// Register default template loaders.
		if (this.templateLoaderPaths != null) {
			for (String path : this.templateLoaderPaths) {
				templateLoaders.add(getTemplateLoaderForPath(path));
			}
		}
		postProcessTemplateLoaders(templateLoaders);

		// Register template loaders that are supposed to kick in late.
		if (this.postTemplateLoaders != null) {
			templateLoaders.addAll(this.postTemplateLoaders);
		}

		TemplateLoader loader = getAggregateTemplateLoader(templateLoaders);
		if (loader != null) {
			config.setTemplateLoader(loader);
		}

		postProcessConfiguration(config);
		return config;
	}

	/**
	 * Return a new {@link Configuration} object.
	 * <p>Subclasses can override this for custom initialization &mdash; for example,
	 * to specify a FreeMarker compatibility level (which is a new feature in
	 * FreeMarker 2.3.21), or to use a mock object for testing.
	 * <p>Called by {@link #createConfiguration()}.
	 * @return the {@code Configuration} object
	 * @throws IOException if a config file wasn't found
	 * @throws TemplateException on FreeMarker initialization failure
	 * @see #createConfiguration()
	 */
	protected Configuration newConfiguration() throws IOException, TemplateException {
		return new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
	}

	/**
	 * Determine a FreeMarker {@link TemplateLoader} for the given path.
	 * <p>Default implementation creates either a {@link FileTemplateLoader} or
	 * a {@link SpringTemplateLoader}.
	 * @param templateLoaderPath the path to load templates from
	 * @return an appropriate {@code TemplateLoader}
	 * @see freemarker.cache.FileTemplateLoader
	 * @see SpringTemplateLoader
	 */
	protected TemplateLoader getTemplateLoaderForPath(String templateLoaderPath) {
		if (isPreferFileSystemAccess()) {
			// Try to load via the file system, fall back to SpringTemplateLoader
			// (for hot detection of template changes, if possible).
			try {
				Resource path = getResourceLoader().getResource(templateLoaderPath);
				File file = path.getFile();  // will fail if not resolvable in the file system
				if (logger.isDebugEnabled()) {
					logger.debug(
							"Template loader path [" + path + "] resolved to file path [" + file.getAbsolutePath() + "]");
				}
				return new FileTemplateLoader(file);
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot resolve template loader path [" + templateLoaderPath +
							"] to [java.io.File]: using SpringTemplateLoader as fallback", ex);
				}
				return new SpringTemplateLoader(getResourceLoader(), templateLoaderPath);
			}
		}
		else {
			// Always load via SpringTemplateLoader (without hot detection of template changes).
			logger.debug("File system access not preferred: using SpringTemplateLoader");
			return new SpringTemplateLoader(getResourceLoader(), templateLoaderPath);
		}
	}

	/**
	 * To be overridden by subclasses that want to register custom
	 * {@link TemplateLoader} instances after this factory created its default
	 * template loaders.
	 * <p>Called by {@link #createConfiguration()}. Note that specified
	 * "postTemplateLoaders" will be registered <i>after</i> any loaders
	 * registered by this callback; as a consequence, they are <i>not</i>
	 * included in the given List.
	 * @param templateLoaders the current List of TemplateLoader instances,
	 * to be modified by a subclass
	 * @see #createConfiguration()
	 * @see #setPostTemplateLoaders
	 */
	protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
	}

	/**
	 * Return a {@link TemplateLoader} based on the given {@code TemplateLoader} list.
	 * <p>If more than one TemplateLoader has been registered, a FreeMarker
	 * {@link MultiTemplateLoader} will be created.
	 * @param templateLoaders the final List of {@code TemplateLoader} instances
	 * @return the aggregate TemplateLoader
	 */
	@Nullable
	protected TemplateLoader getAggregateTemplateLoader(List<TemplateLoader> templateLoaders) {
		return switch (templateLoaders.size()) {
			case 0 -> {
				logger.debug("No FreeMarker TemplateLoaders specified");
				yield null;
			}
			case 1 -> templateLoaders.get(0);
			default -> {
				TemplateLoader[] loaders = templateLoaders.toArray(new TemplateLoader[0]);
				yield new MultiTemplateLoader(loaders);
			}
		};
	}

	/**
	 * To be overridden by subclasses that want to perform custom
	 * post-processing of the {@link Configuration} object after this factory
	 * performed its default initialization.
	 * <p>Called by {@link #createConfiguration()}.
	 * @param config the current {@code Configuration} object
	 * @throws IOException if a config file wasn't found
	 * @throws TemplateException on FreeMarker initialization failure
	 * @see #createConfiguration()
	 */
	protected void postProcessConfiguration(Configuration config) throws IOException, TemplateException {
	}

}
