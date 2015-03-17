/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.view.script;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An implementation of Spring MVC's {@link ScriptTemplateConfig} for creating
 * a {@code ScriptEngine} for use in a web application.
 *
 * <pre class="code">
 *
 * // Add the following to an &#64;Configuration class
 *
 * &#64;Bean
 * public ScriptTemplateConfigurer mustacheConfigurer() {
 *
 *    ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
 *    configurer.setEngineName("nashorn");
 *    configurer.setScripts("mustache.js");
 *    configurer.setRenderObject("Mustache");
 *    configurer.setRenderFunction("render");
 *    return configurer;
 * }
 * </pre>
 *
 * @author Sebastien Deleuze
 * @since 4.2
 * @see ScriptTemplateView
 */
public class ScriptTemplateConfigurer implements ScriptTemplateConfig, ApplicationContextAware, InitializingBean {

	private ScriptEngine engine;

	private String engineName;

	private ApplicationContext applicationContext;

	private String[] scripts;

	private String renderObject;

	private String renderFunction;

	private Charset charset = Charset.forName("UTF-8");

	private ResourceLoader resourceLoader;

	private String resourceLoaderPath = "classpath:";

	/**
	 * Set the {@link ScriptEngine} to use by the view.
	 * The script engine must implement {@code Invocable}.
	 * You must define {@code engine} or {@code engineName}, not both.
	 */
	public void setEngine(ScriptEngine engine) {
		Assert.isInstanceOf(Invocable.class, engine);
		this.engine = engine;
	}

	@Override
	public ScriptEngine getEngine() {
		return this.engine;
	}

	/**
	 * Set the engine name that will be used to instantiate the {@link ScriptEngine}.
	 * The script engine must implement {@code Invocable}.
	 * You must define {@code engine} or {@code engineName}, not both.
	 */
	public void setEngineName(String engineName) {
		this.engineName = engineName;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	protected ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * Set the scripts to be loaded by the script engine (library or user provided).
	 * Since {@code resourceLoaderPath} default value is "classpath:", you can load easily
	 * any script available on the classpath.
	 *
	 * For example, in order to use a Javascript library available as a WebJars dependency
	 * and a custom "render.js" file, you should call
	 * {@code configurer.setScripts("/META-INF/resources/webjars/library/version/library.js",
	 * "com/myproject/script/render.js");}.
	 *
	 * @see #setResourceLoaderPath(String)
	 * @see <a href="http://www.webjars.org">WebJars</a>
	 */
	public void setScripts(String... scriptNames) {
		this.scripts = scriptNames;
	}

	@Override
	public String getRenderObject() {
		return renderObject;
	}

	/**
	 * Set the object where belongs the render function (optional).
	 * For example, in order to call {@code Mustache.render()}, {@code renderObject}
	 * should be set to {@code "Mustache"} and {@code renderFunction} to {@code "render"}.
	 */
	public void setRenderObject(String renderObject) {
		this.renderObject = renderObject;
	}

	@Override
	public String getRenderFunction() {
		return renderFunction;
	}

	/**
	 * Set the render function name (mandatory). This function will be called with the
	 * following parameters:
	 * <ol>
	 *     <li>{@code template}: the view template content (String)</li>
	 *     <li>{@code model}: the view model (Map)</li>
	 * </ol>
	 */
	public void setRenderFunction(String renderFunction) {
		this.renderFunction = renderFunction;
	}

	/**
	 * Set the charset used to read script and template files.
	 * ({@code UTF-8} by default).
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	@Override
	public Charset getCharset() {
		return this.charset;
	}

	/**
	 * Set the resource loader path(s) via a Spring resource location.
	 * Accepts multiple locations as a comma-separated list of paths.
	 * Standard URLs like "file:" and "classpath:" and pseudo URLs are supported
	 * as understood by Spring's {@link org.springframework.core.io.ResourceLoader}.
	 * Relative paths are allowed when running in an ApplicationContext.
	 * Default is "classpath:".
	 */
	public void setResourceLoaderPath(String resourceLoaderPath) {
		this.resourceLoaderPath = resourceLoaderPath;
	}

	public String getResourceLoaderPath() {
		return resourceLoaderPath;
	}

	@Override
	public ResourceLoader getResourceLoader() {
		return resourceLoader;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.engine == null) {
			this.engine = createScriptEngine();
		}
		Assert.state(this.renderFunction != null, "renderFunction property must be defined.");
		this.resourceLoader  = new DefaultResourceLoader(createClassLoader());
		if (this.scripts != null) {
			try {
				for (String script : this.scripts) {
					this.engine.eval(read(script));
				}
			}
			catch (ScriptException e) {
				throw new IllegalStateException("could not load script", e);
			}
		}
	}

	protected ClassLoader createClassLoader() throws IOException {
		String[] paths = StringUtils.commaDelimitedListToStringArray(this.resourceLoaderPath);
		List<URL> urls = new ArrayList<URL>();
		for (String path : paths) {
			Resource[] resources = getApplicationContext().getResources(path);
			if (resources.length > 0) {
				for (Resource resource : resources) {
					if (resource.exists()) {
						urls.add(resource.getURL());
					}
				}
			}
		}
		ClassLoader classLoader = getApplicationContext().getClassLoader();
		return (urls.size() > 0 ? new URLClassLoader(urls.toArray(new URL[urls.size()]), classLoader) : classLoader);
	}

	private Reader read(String path) throws IOException {
		Resource resource = this.resourceLoader.getResource(path);
		Assert.state(resource.exists(), "Resource " + path + " not found.");
		return new InputStreamReader(resource.getInputStream());
	}

	protected ScriptEngine createScriptEngine() throws IOException {
		if (this.engine != null && this.engineName != null) {
			throw new IllegalStateException("You should define engine or engineName properties, not both.");
		}
		if (this.engineName != null) {
			ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName(this.engineName);
			Assert.state(scriptEngine != null, "No engine \"" + this.engineName + "\" found.");
			Assert.state(scriptEngine instanceof Invocable, "Script engine should be instance of Invocable");
			this.engine = scriptEngine;
		}
		Assert.state(this.engine != null, "No script engine found, please specify valid engine or engineName properties.");
		return this.engine;
	}

}
