/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.view.xslt;

import java.util.Properties;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.URIResolver;

import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * {@link org.springframework.web.servlet.ViewResolver} implementation that
 * resolves instances of {@link XsltView} by translating the supplied view name
 * into the URL of the XSLT stylesheet.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class XsltViewResolver extends UrlBasedViewResolver {

	private String sourceKey;

	private URIResolver uriResolver;

	private ErrorListener errorListener;

	private boolean indent = true;

	private Properties outputProperties;

	private boolean cacheTemplates = true;


	public XsltViewResolver() {
		setViewClass(requiredViewClass());
	}


	@Override
	protected Class<?> requiredViewClass() {
		return XsltView.class;
	}

	/**
	 * Set the name of the model attribute that represents the XSLT Source.
	 * If not specified, the model map will be searched for a matching value type.
	 * <p>The following source types are supported out of the box:
	 * {@link javax.xml.transform.Source}, {@link org.w3c.dom.Document},
	 * {@link org.w3c.dom.Node}, {@link java.io.Reader}, {@link java.io.InputStream}
	 * and {@link org.springframework.core.io.Resource}.
	 */
	public void setSourceKey(String sourceKey) {
		this.sourceKey = sourceKey;
	}

	/**
	 * Set the URIResolver used in the transform.
	 * <p>The URIResolver handles calls to the XSLT {@code document()} function.
	 */
	public void setUriResolver(URIResolver uriResolver) {
		this.uriResolver = uriResolver;
	}

	/**
	 * Set an implementation of the {@link javax.xml.transform.ErrorListener}
	 * interface for custom handling of transformation errors and warnings.
	 * <p>If not set, a default
	 * {@link org.springframework.util.xml.SimpleTransformErrorListener} is
	 * used that simply logs warnings using the logger instance of the view class,
	 * and rethrows errors to discontinue the XML transformation.
	 * @see org.springframework.util.xml.SimpleTransformErrorListener
	 */
	public void setErrorListener(ErrorListener errorListener) {
		this.errorListener = errorListener;
	}

	/**
	 * Set whether the XSLT transformer may add additional whitespace when
	 * outputting the result tree.
	 * <p>Default is {@code true} (on); set this to {@code false} (off)
	 * to not specify an "indent" key, leaving the choice up to the stylesheet.
	 * @see javax.xml.transform.OutputKeys#INDENT
	 */
	public void setIndent(boolean indent) {
		this.indent = indent;
	}

	/**
	 * Set arbitrary transformer output properties to be applied to the stylesheet.
	 * <p>Any values specified here will override defaults that this view sets
	 * programmatically.
	 * @see javax.xml.transform.Transformer#setOutputProperty
	 */
	public void setOutputProperties(Properties outputProperties) {
		this.outputProperties = outputProperties;
	}

	/**
	 * Turn on/off the caching of the XSLT templates.
	 * <p>The default value is "true". Only set this to "false" in development,
	 * where caching does not seriously impact performance.
	 */
	public void setCacheTemplates(boolean cacheTemplates) {
		this.cacheTemplates = cacheTemplates;
	}


	@Override
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		XsltView view = (XsltView) super.buildView(viewName);
		view.setSourceKey(this.sourceKey);
		if (this.uriResolver != null) {
			view.setUriResolver(this.uriResolver);
		}
		if (this.errorListener != null) {
			view.setErrorListener(this.errorListener);
		}
		view.setIndent(this.indent);
		view.setOutputProperties(this.outputProperties);
		view.setCacheTemplates(this.cacheTemplates);
		return view;
	}

}
