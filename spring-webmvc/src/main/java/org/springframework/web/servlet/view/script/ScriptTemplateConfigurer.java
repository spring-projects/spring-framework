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

package org.springframework.web.servlet.view.script;

import java.nio.charset.Charset;
import java.util.function.Supplier;

import javax.script.Bindings;
import javax.script.ScriptEngine;

import org.jspecify.annotations.Nullable;

/**
 * An implementation of Spring MVC's {@link ScriptTemplateConfig} for creating
 * a {@code ScriptEngine} for use in a web application.
 *
 * <pre class="code">
 * // Add the following to an &#64;Configuration class
 * &#64;Bean
 * public ScriptTemplateConfigurer mustacheConfigurer() {
 *    ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
 *    configurer.setEngineName("nashorn");
 *    configurer.setScripts("mustache.js");
 *    configurer.setRenderObject("Mustache");
 *    configurer.setRenderFunction("render");
 *    return configurer;
 * }
 * </pre>
 *
 * <p><b>NOTE:</b> It is possible to use non thread-safe script engines with
 * templating libraries not designed for concurrency, like Handlebars or React running on
 * Nashorn, by setting the {@link #setSharedEngine sharedEngine} property to {@code false}.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 * @see ScriptTemplateView
 */
public class ScriptTemplateConfigurer implements ScriptTemplateConfig {

	private @Nullable ScriptEngine engine;

	private @Nullable Supplier<ScriptEngine> engineSupplier;

	private @Nullable String engineName;

	private @Nullable Boolean sharedEngine;

	private String @Nullable [] scripts;

	private @Nullable String renderObject;

	private @Nullable String renderFunction;

	private @Nullable String contentType;

	private @Nullable Charset charset;

	private @Nullable String resourceLoaderPath;


	/**
	 * Default constructor.
	 */
	public ScriptTemplateConfigurer() {
	}

	/**
	 * Create a new ScriptTemplateConfigurer using the given engine name.
	 */
	public ScriptTemplateConfigurer(String engineName) {
		this.engineName = engineName;
	}


	/**
	 * Set the {@link ScriptEngine} to use by the view.
	 * If {@code renderFunction} is specified, the script engine must implement {@code Invocable}.
	 * You must define {@code engine} or {@code engineName}, not both.
	 * <p>When the {@code sharedEngine} flag is set to {@code false}, you should not specify
	 * the script engine with this setter, but with {@link #setEngineName(String)}
	 * or {@link #setEngineSupplier(Supplier)} since it implies multiple lazy
	 * instantiations of the script engine.
	 * @see #setEngineName(String)
	 * @see #setEngineSupplier(Supplier)
	 */
	public void setEngine(@Nullable ScriptEngine engine) {
		this.engine = engine;
	}

	@Override
	public @Nullable ScriptEngine getEngine() {
		return this.engine;
	}

	/**
	 * Set the {@link ScriptEngine} supplier to use by the view, usually used with
	 * {@link #setSharedEngine(Boolean)} set to {@code false}.
	 * If {@code renderFunction} is specified, the script engine must implement {@code Invocable}.
	 * You must either define {@code engineSupplier}, {@code engine} or {@code engineName}.
	 * @since 5.2
	 * @see #setEngine(ScriptEngine)
	 * @see #setEngineName(String)
	 */
	public void setEngineSupplier(@Nullable Supplier<ScriptEngine> engineSupplier) {
		this.engineSupplier = engineSupplier;
	}

	@Override
	public @Nullable Supplier<ScriptEngine> getEngineSupplier() {
		return this.engineSupplier;
	}

	/**
	 * Set the engine name that will be used to instantiate the {@link ScriptEngine}.
	 * If {@code renderFunction} is specified, the script engine must implement {@code Invocable}.
	 * You must define {@code engine} or {@code engineName}, not both.
	 * @see #setEngine(ScriptEngine)
	 * @see #setEngineSupplier(Supplier)
	 */
	public void setEngineName(@Nullable String engineName) {
		this.engineName = engineName;
	}

	@Override
	public @Nullable String getEngineName() {
		return this.engineName;
	}

	/**
	 * When set to {@code false}, use thread-local {@link ScriptEngine} instances instead
	 * of one single shared instance. This flag should be set to {@code false} for those
	 * using non thread-safe script engines with templating libraries not designed for
	 * concurrency, like Handlebars or React running on Nashorn for example.
	 * <p>When this flag is set to {@code false}, the script engine must be specified using
	 * {@link #setEngineName(String)} or {@link #setEngineSupplier(Supplier)}.
	 * Using {@link #setEngine(ScriptEngine)} is not possible because multiple instances
	 * of the script engine need to be created lazily (one per thread).
	 * @see <a href="https://docs.oracle.com/javase/8/docs/api/javax/script/ScriptEngineFactory.html#getParameter-java.lang.String-">THREADING ScriptEngine parameter</a>
	 */
	public void setSharedEngine(@Nullable Boolean sharedEngine) {
		this.sharedEngine = sharedEngine;
	}

	@Override
	public @Nullable Boolean isSharedEngine() {
		return this.sharedEngine;
	}

	/**
	 * Set the scripts to be loaded by the script engine (library or user provided).
	 * Since {@code resourceLoaderPath} default value is "classpath:", you can load easily
	 * any script available on the classpath.
	 * <p>For example, in order to use a JavaScript library available as a WebJars dependency
	 * and a custom "render.js" file, you should call
	 * {@code configurer.setScripts("/META-INF/resources/webjars/library/version/library.js",
	 * "com/myproject/script/render.js");}.
	 * @see #setResourceLoaderPath
	 * @see <a href="https://www.webjars.org">WebJars</a>
	 */
	public void setScripts(String @Nullable ... scriptNames) {
		this.scripts = scriptNames;
	}

	@Override
	public String @Nullable [] getScripts() {
		return this.scripts;
	}

	/**
	 * Set the object where the render function belongs (optional).
	 * For example, in order to call {@code Mustache.render()}, {@code renderObject}
	 * should be set to {@code "Mustache"} and {@code renderFunction} to {@code "render"}.
	 */
	public void setRenderObject(@Nullable String renderObject) {
		this.renderObject = renderObject;
	}

	@Override
	public @Nullable String getRenderObject() {
		return this.renderObject;
	}

	/**
	 * Set the render function name (optional). If not specified, the script templates
	 * will be evaluated with {@link ScriptEngine#eval(String, Bindings)}.
	 * <p>This function will be called with the following parameters:
	 * <ol>
	 * <li>{@code String template}: the template content</li>
	 * <li>{@code Map model}: the view model</li>
	 * <li>{@code RenderingContext context}: the rendering context (since 5.0)</li>
	 * </ol>
	 * @see RenderingContext
	 */
	public void setRenderFunction(@Nullable String renderFunction) {
		this.renderFunction = renderFunction;
	}

	@Override
	public @Nullable String getRenderFunction() {
		return this.renderFunction;
	}

	/**
	 * Set the content type to use for the response.
	 * ({@code text/html} by default).
	 * @since 4.2.1
	 */
	public void setContentType(@Nullable String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Return the content type to use for the response.
	 * @since 4.2.1
	 */
	@Override
	public @Nullable String getContentType() {
		return this.contentType;
	}

	/**
	 * Set the charset used to read script and template files.
	 * ({@code UTF-8} by default).
	 */
	public void setCharset(@Nullable Charset charset) {
		this.charset = charset;
	}

	@Override
	public @Nullable Charset getCharset() {
		return this.charset;
	}

	/**
	 * Set the resource loader path(s) via a Spring resource location.
	 * Accepts multiple locations as a comma-separated list of paths.
	 * Standard URLs like "file:" and "classpath:" and pseudo URLs are supported
	 * as understood by Spring's {@link org.springframework.core.io.ResourceLoader}.
	 * Relative paths are allowed when running in an ApplicationContext.
	 * <p>Default is "classpath:".
	 */
	public void setResourceLoaderPath(@Nullable String resourceLoaderPath) {
		this.resourceLoaderPath = resourceLoaderPath;
	}

	@Override
	public @Nullable String getResourceLoaderPath() {
		return this.resourceLoaderPath;
	}

}
