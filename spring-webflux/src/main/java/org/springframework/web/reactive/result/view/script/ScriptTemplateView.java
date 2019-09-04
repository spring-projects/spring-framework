/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.result.view.script;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.scripting.support.StandardScriptEvalException;
import org.springframework.scripting.support.StandardScriptUtils;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.server.ServerWebExchange;

/**
 * An {@link AbstractUrlBasedView} subclass designed to run any template library
 * based on a JSR-223 script engine.
 *
 * <p>If not set, each property is auto-detected by looking up a single
 * {@link ScriptTemplateConfig} bean in the web application context and using
 * it to obtain the configured properties.
 *
 * <p>The Nashorn JavaScript engine requires Java 8+ and may require setting the
 * {@code sharedEngine} property to {@code false} in order to run properly. See
 * {@link ScriptTemplateConfigurer#setSharedEngine(Boolean)} for more details.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 5.0
 * @see ScriptTemplateConfigurer
 * @see ScriptTemplateViewResolver
 */
public class ScriptTemplateView extends AbstractUrlBasedView {

	private static final String DEFAULT_RESOURCE_LOADER_PATH = "classpath:";


	@Nullable
	private ScriptEngine engine;

	@Nullable
	private Supplier<ScriptEngine> engineSupplier;

	@Nullable
	private String engineName;

	@Nullable
	private Boolean sharedEngine;

	@Nullable
	private String[] scripts;

	@Nullable
	private String renderObject;

	@Nullable
	private String renderFunction;

	@Nullable
	private String[] resourceLoaderPaths;

	@Nullable
	private volatile ScriptEngineManager scriptEngineManager;


	/**
	 * Constructor for use as a bean.
	 * @see #setUrl
	 */
	public ScriptTemplateView() {
	}

	/**
	 * Create a new ScriptTemplateView with the given URL.
	 */
	public ScriptTemplateView(String url) {
		super(url);
	}


	/**
	 * See {@link ScriptTemplateConfigurer#setEngine(ScriptEngine)} documentation.
	 */
	public void setEngine(ScriptEngine engine) {
		this.engine = engine;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setEngineSupplier(Supplier)} documentation.
	 * @since 5.2
	 */
	public void setEngineSupplier(Supplier<ScriptEngine> engineSupplier) {
		this.engineSupplier = engineSupplier;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setEngineName(String)} documentation.
	 */
	public void setEngineName(String engineName) {
		this.engineName = engineName;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setSharedEngine(Boolean)} documentation.
	 */
	public void setSharedEngine(Boolean sharedEngine) {
		this.sharedEngine = sharedEngine;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setScripts(String...)} documentation.
	 */
	public void setScripts(String... scripts) {
		this.scripts = scripts;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setRenderObject(String)} documentation.
	 */
	public void setRenderObject(String renderObject) {
		this.renderObject = renderObject;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setRenderFunction(String)} documentation.
	 */
	public void setRenderFunction(String functionName) {
		this.renderFunction = functionName;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setResourceLoaderPath(String)} documentation.
	 */
	public void setResourceLoaderPath(String resourceLoaderPath) {
		String[] paths = StringUtils.commaDelimitedListToStringArray(resourceLoaderPath);
		this.resourceLoaderPaths = new String[paths.length + 1];
		this.resourceLoaderPaths[0] = "";
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];
			if (!path.endsWith("/") && !path.endsWith(":")) {
				path = path + "/";
			}
			this.resourceLoaderPaths[i + 1] = path;
		}
	}

	@Override
	public void setApplicationContext(@Nullable ApplicationContext context) {
		super.setApplicationContext(context);

		ScriptTemplateConfig viewConfig = autodetectViewConfig();
		if (this.engine == null && viewConfig.getEngine() != null) {
			this.engine = viewConfig.getEngine();
		}
		if (this.engineSupplier == null && viewConfig.getEngineSupplier() != null) {
			this.engineSupplier = viewConfig.getEngineSupplier();
		}
		if (this.engineName == null && viewConfig.getEngineName() != null) {
			this.engineName = viewConfig.getEngineName();
		}
		if (this.scripts == null && viewConfig.getScripts() != null) {
			this.scripts = viewConfig.getScripts();
		}
		if (this.renderObject == null && viewConfig.getRenderObject() != null) {
			this.renderObject = viewConfig.getRenderObject();
		}
		if (this.renderFunction == null && viewConfig.getRenderFunction() != null) {
			this.renderFunction = viewConfig.getRenderFunction();
		}
		if (viewConfig.getCharset() != null) {
			setDefaultCharset(viewConfig.getCharset());
		}
		if (this.resourceLoaderPaths == null) {
			String resourceLoaderPath = viewConfig.getResourceLoaderPath();
			setResourceLoaderPath(resourceLoaderPath != null ? resourceLoaderPath : DEFAULT_RESOURCE_LOADER_PATH);
		}
		if (this.sharedEngine == null && viewConfig.isSharedEngine() != null) {
			this.sharedEngine = viewConfig.isSharedEngine();
		}

		int engineCount = 0;
		if (this.engine != null) {
			engineCount++;
		}
		if (this.engineSupplier != null) {
			engineCount++;
		}
		if (this.engineName != null) {
			engineCount++;
		}
		Assert.isTrue(engineCount == 1,
				"You should define either 'engine', 'engineSupplier' or 'engineName'.");

		if (Boolean.FALSE.equals(this.sharedEngine)) {
			Assert.isTrue(this.engine == null,
					"When 'sharedEngine' is set to false, you should specify the " +
					"script engine using 'engineName' or 'engineSupplier' , not 'engine'.");
		}
		else if (this.engine != null) {
			loadScripts(this.engine);
		}
		else if (this.engineName != null) {
			setEngine(createEngineFromName(this.engineName));
		}
		else {
			setEngine(createEngineFromSupplier());
		}

		if (this.renderFunction != null && this.engine != null) {
			Assert.isInstanceOf(Invocable.class, this.engine,
					"ScriptEngine must implement Invocable when 'renderFunction' is specified");
		}
	}

	protected ScriptEngine getEngine() {
		if (Boolean.FALSE.equals(this.sharedEngine)) {
			if (this.engineName != null) {
				return createEngineFromName(this.engineName);
			}
			else {
				return createEngineFromSupplier();
			}
		}
		else {
			Assert.state(this.engine != null, "No shared engine available");
			return this.engine;
		}
	}

	protected ScriptEngine createEngineFromName(String engineName) {
		ScriptEngineManager scriptEngineManager = this.scriptEngineManager;
		if (scriptEngineManager == null) {
			scriptEngineManager = new ScriptEngineManager(obtainApplicationContext().getClassLoader());
			this.scriptEngineManager = scriptEngineManager;
		}

		ScriptEngine engine = StandardScriptUtils.retrieveEngineByName(scriptEngineManager, engineName);
		loadScripts(engine);
		return engine;
	}

	private ScriptEngine createEngineFromSupplier() {
		Assert.state(this.engineSupplier != null, "No engine supplier available");
		ScriptEngine engine = this.engineSupplier.get();
		if (this.renderFunction != null) {
			Assert.isInstanceOf(Invocable.class, engine,
					"ScriptEngine must implement Invocable when 'renderFunction' is specified");
		}
		loadScripts(engine);
		return engine;
	}

	protected void loadScripts(ScriptEngine engine) {
		if (!ObjectUtils.isEmpty(this.scripts)) {
			for (String script : this.scripts) {
				Resource resource = getResource(script);
				if (resource == null) {
					throw new IllegalStateException("Script resource [" + script + "] not found");
				}
				try {
					engine.eval(new InputStreamReader(resource.getInputStream()));
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Failed to evaluate script [" + script + "]", ex);
				}
			}
		}
	}

	@Nullable
	protected Resource getResource(String location) {
		if (this.resourceLoaderPaths != null) {
			for (String path : this.resourceLoaderPaths) {
				Resource resource = obtainApplicationContext().getResource(path + location);
				if (resource.exists()) {
					return resource;
				}
			}
		}
		return null;
	}

	protected ScriptTemplateConfig autodetectViewConfig() throws BeansException {
		try {
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(
					obtainApplicationContext(), ScriptTemplateConfig.class, true, false);
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException("Expected a single ScriptTemplateConfig bean in the current " +
					"web application context or the parent root context: ScriptTemplateConfigurer is " +
					"the usual implementation. This bean may have any name.", ex);
		}
	}

	@Override
	public boolean checkResourceExists(Locale locale) throws Exception {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");
		return (getResource(url) != null);
	}

	@Override
	protected Mono<Void> renderInternal(
			Map<String, Object> model, @Nullable MediaType contentType, ServerWebExchange exchange) {

		return exchange.getResponse().writeWith(Mono.fromCallable(() -> {
			try {
				ScriptEngine engine = getEngine();
				String url = getUrl();
				Assert.state(url != null, "'url' not set");
				String template = getTemplate(url);

				Function<String, String> templateLoader = path -> {
					try {
						return getTemplate(path);
					}
					catch (IOException ex) {
						throw new IllegalStateException(ex);
					}
				};

				Locale locale = LocaleContextHolder.getLocale(exchange.getLocaleContext());
				RenderingContext context = new RenderingContext(
						obtainApplicationContext(), locale, templateLoader, url);

				Object html;
				if (this.renderFunction == null) {
					SimpleBindings bindings = new SimpleBindings();
					bindings.putAll(model);
					model.put("renderingContext", context);
					html = engine.eval(template, bindings);
				}
				else if (this.renderObject != null) {
					Object thiz = engine.eval(this.renderObject);
					html = ((Invocable) engine).invokeMethod(thiz, this.renderFunction, template, model, context);
				}
				else {
					html = ((Invocable) engine).invokeFunction(this.renderFunction, template, model, context);
				}

				byte[] bytes = String.valueOf(html).getBytes(StandardCharsets.UTF_8);
				return exchange.getResponse().bufferFactory().wrap(bytes); // just wrapping, no allocation
			}
			catch (ScriptException ex) {
				throw new IllegalStateException("Failed to render script template", new StandardScriptEvalException(ex));
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to render script template", ex);
			}
		}));
	}

	protected String getTemplate(String path) throws IOException {
		Resource resource = getResource(path);
		if (resource == null) {
			throw new IllegalStateException("Template resource [" + path + "] not found");
		}
		InputStreamReader reader = new InputStreamReader(resource.getInputStream(), getDefaultCharset());
		return FileCopyUtils.copyToString(reader);
	}

}
