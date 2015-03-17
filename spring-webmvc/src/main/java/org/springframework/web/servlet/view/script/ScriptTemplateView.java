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
import java.nio.charset.Charset;
import java.util.Map;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * An {@link org.springframework.web.servlet.view.AbstractUrlBasedView AbstractUrlBasedView}
 * designed to run any template library based on a JSR-223 script engine.
 *
 * <p>Nashorn Javascript engine requires Java 8+.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 * @see ScriptTemplateConfigurer
 * @see ScriptTemplateViewResolver
 */
public class ScriptTemplateView extends AbstractUrlBasedView {

	private ScriptEngine engine;

	private String renderObject;

	private String renderFunction;

	private Charset charset;

	private ResourceLoader resourceLoader;

	/**
	 * Set the {@link ScriptEngine} to use in this view.
	 * <p>If not set, the engine is auto-detected by looking up up a single
	 * {@link ScriptTemplateConfig} bean in the web application context and using
	 * it to obtain the configured {@code ScriptEngine} instance.
	 * @see ScriptTemplateConfig
	 */
	public void setEngine(ScriptEngine engine) {
		this.engine = engine;
	}

	/**
	 * Set the render function name. This function will be called with the
	 * following parameters:
	 * <ol>
	 *     <li>{@code template}: the view template content (String)</li>
	 *     <li>{@code model}: the view model (Map)</li>
	 * </ol>
	 * <p>If not set, the function name is auto-detected by looking up up a single
	 * {@link ScriptTemplateConfig} bean in the web application context and using
	 * it to obtain the configured {@code functionName} property.
	 * @see ScriptTemplateConfig
	 */
	public void setRenderFunction(String functionName) {
		this.renderFunction = functionName;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	protected void initApplicationContext(ApplicationContext context) {
		super.initApplicationContext(context);
		ScriptTemplateConfig viewConfig = autodetectViewConfig();
		if (this.engine == null) {
			this.engine = viewConfig.getEngine();
			Assert.state(this.engine != null, "Script engine should not be null.");
			Assert.state(this.engine instanceof Invocable, "Script engine should be instance of Invocable");
		}
		if (this.resourceLoader == null) {
			this.resourceLoader = viewConfig.getResourceLoader();
		}
		if (this.renderObject == null) {
			this.renderObject = viewConfig.getRenderObject();
		}
		if (this.renderFunction == null) {
			this.renderFunction = viewConfig.getRenderFunction();
		}
		if (this.charset == null) {
			this.charset = viewConfig.getCharset();
		}
	}

	protected ScriptTemplateConfig autodetectViewConfig() throws BeansException {
		try {
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(getApplicationContext(), ScriptTemplateConfig.class, true, false);
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException("Expected a single ScriptTemplateConfig bean in the current " +
					"Servlet web application context or the parent root context: ScriptTemplateConfigurer is " +
					"the usual implementation. This bean may have any name.", ex);
		}
	}

	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Assert.notNull("Render function must not be null", this.renderFunction);
		try {
			String template = getTemplate(getUrl());
			Object html = null;
			if (this.renderObject != null) {
				Object thiz = engine.eval(this.renderObject);
				html = ((Invocable)this.engine).invokeMethod(thiz, this.renderFunction, template, model);
			}
			else {
				html = ((Invocable)this.engine).invokeFunction(this.renderFunction, template, model);
			}
			response.getWriter().write(String.valueOf(html));
		}
		catch (Exception e) {
			throw new IllegalStateException("failed to render template", e);
		}
	}

	protected String getTemplate(String path) throws IOException {
		Resource resource = this.resourceLoader.getResource(path);
		Assert.state(resource.exists(), "Resource " + path + " not found.");
		return StreamUtils.copyToString(resource.getInputStream(), this.charset);
	}

}