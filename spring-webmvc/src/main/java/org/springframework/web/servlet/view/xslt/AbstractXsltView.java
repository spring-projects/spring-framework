/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.view.xslt;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Node;

import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.xml.SimpleTransformErrorListener;
import org.springframework.util.xml.TransformerUtils;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.util.NestedServletException;

/**
 * Convenient superclass for views rendered using an XSLT stylesheet.
 *
 * <p>Subclasses typically must provide the {@link Source} to transform
 * by overriding {@link #createXsltSource}. Subclasses do not need to
 * concern themselves with XSLT other than providing a valid stylesheet location.
 *
 * <p>Properties:
 * <ul>
 * <li>{@link #setStylesheetLocation(org.springframework.core.io.Resource) stylesheetLocation}:
 * a {@link Resource} pointing to the XSLT stylesheet
 * <li>{@link #setRoot(String) root}: the name of the root element; defaults to {@link #DEFAULT_ROOT "DocRoot"}
 * <li>{@link #setUriResolver(javax.xml.transform.URIResolver) uriResolver}:
 * the {@link URIResolver} to be used in the transform
 * <li>{@link #setErrorListener(javax.xml.transform.ErrorListener) errorListener} (optional):
 * the {@link ErrorListener} implementation instance for custom handling of warnings and errors during TransformerFactory operations
 * <li>{@link #setIndent(boolean) indent} (optional): whether additional whitespace
 * may be added when outputting the result; defaults to <code>true</code>
 * <li>{@link #setCache(boolean) cache} (optional): are templates to be cached; debug setting only; defaults to <code>true</code>
 * </ul>
 *
 * <p>Note that setting {@link #setCache(boolean) "cache"} to <code>false</code>
 * will cause the template objects to be reloaded for each rendering. This is
 * useful during development, but will seriously affect performance in production
 * and is not thread-safe.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Darren Davison
 * @deprecated since Spring 2.5; superseded by {@link XsltView} and its
 * more flexible {@link XsltView#locateSource} mechanism
 */
@Deprecated
public abstract class AbstractXsltView extends AbstractView {

	/** The default content type if no stylesheet specified */
	public static final String XML_CONTENT_TYPE = "text/xml;charset=ISO-8859-1";

	/** The default document root name */
	public static final String DEFAULT_ROOT = "DocRoot";


	private boolean customContentTypeSet = false;

	private Class<?> transformerFactoryClass;

	private Resource stylesheetLocation;

	private String root = DEFAULT_ROOT;

	private boolean useSingleModelNameAsRoot = true;

	private URIResolver uriResolver;

	private ErrorListener errorListener = new SimpleTransformErrorListener(logger);

	private boolean indent = true;

	private Properties outputProperties;

	private boolean cache = true;

	private TransformerFactory transformerFactory;

	private volatile Templates cachedTemplates;


	/**
	 * This constructor sets the content type to "text/xml;charset=ISO-8859-1"
	 * by default. This will be switched to the standard web view default
	 * "text/html;charset=ISO-8859-1" if a stylesheet location has been specified.
	 * <p>A specific content type can be configured via the
	 * {@link #setContentType "contentType"} bean property.
	 */
	protected AbstractXsltView() {
		super.setContentType(XML_CONTENT_TYPE);
	}


	@Override
	public void setContentType(String contentType) {
		super.setContentType(contentType);
		this.customContentTypeSet = true;
	}

	/**
	 * Specify the XSLT TransformerFactory class to use.
	 * <p>The default constructor of the specified class will be called
	 * to build the TransformerFactory for this view.
	 */
	public void setTransformerFactoryClass(Class<?> transformerFactoryClass) {
		Assert.isAssignable(TransformerFactory.class, transformerFactoryClass);
		this.transformerFactoryClass = transformerFactoryClass;
	}

	/**
	 * Set the location of the XSLT stylesheet.
	 * <p>If the {@link TransformerFactory} used by this instance has already
	 * been initialized then invoking this setter will result in the
	 * {@link TransformerFactory#newTemplates(javax.xml.transform.Source) attendant templates}
	 * being re-cached.
	 * @param stylesheetLocation the location of the XSLT stylesheet
	 * @see org.springframework.context.ApplicationContext#getResource
	 */
	public void setStylesheetLocation(Resource stylesheetLocation) {
		this.stylesheetLocation = stylesheetLocation;
		// Re-cache templates if transformer factory already initialized.
		resetCachedTemplates();
	}

	/**
	 * Return the location of the XSLT stylesheet, if any.
	 */
	protected Resource getStylesheetLocation() {
		return this.stylesheetLocation;
	}

	/**
	 * The document root element name. Default is {@link #DEFAULT_ROOT "DocRoot"}.
	 * <p>Only used if we're not passed a single {@link Node} as the model.
	 * @param root the document root element name
	 * @see #DEFAULT_ROOT
	 */
	public void setRoot(String root) {
		this.root = root;
	}

	/**
	 * Set whether to use the name of a given single model object as the
	 * document root element name.
	 * <p>Default is <code>true</code> : If you pass in a model with a single object
	 * named "myElement", then the document root will be named "myElement"
	 * as well. Set this flag to <code>false</code> if you want to pass in a single
	 * model object while still using the root element name configured
	 * through the {@link #setRoot(String) "root" property}.
	 * @param useSingleModelNameAsRoot <code>true</code> if the name of a given single
	 * model object is to be used as the document root element name
	 * @see #setRoot
	 */
	public void setUseSingleModelNameAsRoot(boolean useSingleModelNameAsRoot) {
		this.useSingleModelNameAsRoot = useSingleModelNameAsRoot;
	}

	/**
	 * Set the URIResolver used in the transform.
	 * <p>The URIResolver handles calls to the XSLT <code>document()</code> function.
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
	 * <p>Default is <code>true</code> (on); set this to <code>false</code> (off)
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
	 * Set whether to activate the template cache for this view.
	 * <p>Default is <code>true</code>. Turn this off to refresh
	 * the Templates object on every access, e.g. during development.
	 * @see #resetCachedTemplates()
	 */
	public void setCache(boolean cache) {
		this.cache = cache;
	}

	/**
	 * Reset the cached Templates object, if any.
	 * <p>The Templates object will subsequently be rebuilt on next
	 * {@link #getTemplates() access}, if caching is enabled.
	 * @see #setCache
	 */
	public final void resetCachedTemplates() {
		this.cachedTemplates = null;
	}


	/**
	 * Here we load our template, as we need the
	 * {@link org.springframework.context.ApplicationContext} to do it.
	 */
	@Override
	protected final void initApplicationContext() throws ApplicationContextException {
		this.transformerFactory = newTransformerFactory(this.transformerFactoryClass);
		this.transformerFactory.setErrorListener(this.errorListener);
		if (this.uriResolver != null) {
			this.transformerFactory.setURIResolver(this.uriResolver);
		}
		if (getStylesheetLocation() != null && !this.customContentTypeSet) {
			// Use "text/html" as default (instead of "text/xml") if a stylesheet
			// has been configured but no custom content type has been set.
			super.setContentType(DEFAULT_CONTENT_TYPE);
		}
		try {
			getTemplates();
		}
		catch (TransformerConfigurationException ex) {
			throw new ApplicationContextException("Cannot load stylesheet for XSLT view '" + getBeanName() + "'", ex);
		}
	}

	/**
	 * Instantiate a new TransformerFactory for this view.
	 * <p>The default implementation simply calls
	 * {@link javax.xml.transform.TransformerFactory#newInstance()}.
	 * If a {@link #setTransformerFactoryClass "transformerFactoryClass"}
	 * has been specified explicitly, the default constructor of the
	 * specified class will be called instead.
	 * <p>Can be overridden in subclasses.
	 * @param transformerFactoryClass the specified factory class (if any)
	 * @return the new TransactionFactory instance
	 * @throws TransformerFactoryConfigurationError in case of instantiation failure
	 * @see #setTransformerFactoryClass
	 * @see #getTransformerFactory()
	 */
	protected TransformerFactory newTransformerFactory(Class<?> transformerFactoryClass) {
		if (transformerFactoryClass != null) {
			try {
				return (TransformerFactory) transformerFactoryClass.newInstance();
			}
			catch (Exception ex) {
				throw new TransformerFactoryConfigurationError(ex, "Could not instantiate TransformerFactory");
			}
		}
		else {
			return TransformerFactory.newInstance();
		}
	}

	/**
	 * Return the TransformerFactory used by this view.
	 * Available once the View object has been fully initialized.
	 */
	protected final TransformerFactory getTransformerFactory() {
		return this.transformerFactory;
	}


	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		response.setContentType(getContentType());

		Source source = null;
		String docRoot = null;
		// Value of a single element in the map, if there is one.
		Object singleModel = null;

		if (this.useSingleModelNameAsRoot && model.size() == 1) {
			docRoot = model.keySet().iterator().next();
			if (logger.isDebugEnabled()) {
				logger.debug("Single model object received, key [" + docRoot + "] will be used as root tag");
			}
			singleModel = model.get(docRoot);
		}

		// Handle special case when we have a single node.
		if (singleModel instanceof Node || singleModel instanceof Source) {
			// Don't domify if the model is already an XML node/source.
			// We don't need to worry about model name, either:
			// we leave the Node alone.
			logger.debug("No need to domify: was passed an XML Node or Source");
			source = (singleModel instanceof Node ? new DOMSource((Node) singleModel) : (Source) singleModel);
		}
		else {
			// docRoot local variable takes precedence
			source = createXsltSource(model, (docRoot != null ? docRoot : this.root), request, response);
		}

		doTransform(model, source, request, response);
	}

	/**
	 * Return the XML {@link Source} to transform.
	 * @param model the model Map
	 * @param root name for root element. This can be supplied as a bean property
	 * to concrete subclasses within the view definition file, but will be overridden
	 * in the case of a single object in the model map to be the key for that object.
	 * If no root property is specified and multiple model objects exist, a default
	 * root tag name will be supplied.
	 * @param request HTTP request. Subclasses won't normally use this, as
	 * request processing should have been complete. However, we might want to
	 * create a RequestContext to expose as part of the model.
	 * @param response HTTP response. Subclasses won't normally use this,
	 * however there may sometimes be a need to set cookies.
	 * @return the XSLT Source to transform
	 * @throws Exception if an error occurs
	 */
	protected Source createXsltSource(
			Map<String, Object> model, String root, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		return null;
	}

	/**
	 * Perform the actual transformation, writing to the HTTP response.
	 * <p>The default implementation delegates to the
	 * {@link #doTransform(javax.xml.transform.Source, java.util.Map, javax.xml.transform.Result, String)}
	 * method, building a StreamResult for the ServletResponse OutputStream
	 * or for the ServletResponse Writer (according to {@link #useWriter()}).
	 * @param model the model Map
	 * @param source the Source to transform
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception if an error occurs
	 * @see javax.xml.transform.stream.StreamResult
	 * @see javax.servlet.ServletResponse#getOutputStream()
	 * @see javax.servlet.ServletResponse#getWriter()
	 * @see #useWriter()
	 */
	protected void doTransform(
			Map<String, Object> model, Source source, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		Map<String, ?> parameters = getParameters(model, request);
		Result result = (useWriter() ?
				new StreamResult(response.getWriter()) :
				new StreamResult(new BufferedOutputStream(response.getOutputStream())));
		String encoding = response.getCharacterEncoding();
		doTransform(source, parameters, result, encoding);
	}

	/**
	 * Return a Map of transformer parameters to be applied to the stylesheet.
	 * <p>Subclasses can override this method in order to apply one or more
	 * parameters to the transformation process.
	 * <p>The default implementation delegates to the
	 * {@link #getParameters(HttpServletRequest)} variant.
	 * @param model the model Map
	 * @param request current HTTP request
	 * @return a Map of parameters to apply to the transformation process
	 * @see javax.xml.transform.Transformer#setParameter
	 */
	protected Map<String, ?> getParameters(Map<String, Object> model, HttpServletRequest request) {
		return getParameters(request);
	}

	/**
	 * Return a Map of transformer parameters to be applied to the stylesheet.
	 * <p>Subclasses can override this method in order to apply one or more
	 * parameters to the transformation process.
	 * <p>The default implementation simply returns <code>null</code>.
	 * @param request current HTTP request
	 * @return a Map of parameters to apply to the transformation process
	 * @see #getParameters(Map, HttpServletRequest)
	 * @see javax.xml.transform.Transformer#setParameter
	 */
	protected Map<String, ?> getParameters(HttpServletRequest request) {
		return null;
	}

	/**
	 * Return whether to use a <code>java.io.Writer</code> to write text content
	 * to the HTTP response. Else, a <code>java.io.OutputStream</code> will be used,
	 * to write binary content to the response.
	 * <p>The default implementation returns <code>false</code>, indicating a
	 * a <code>java.io.OutputStream</code>.
	 * @return whether to use a Writer (<code>true</code>) or an OutputStream
	 * (<code>false</code>)
	 * @see javax.servlet.ServletResponse#getWriter()
	 * @see javax.servlet.ServletResponse#getOutputStream()
	 */
	protected boolean useWriter() {
		return false;
	}


	/**
	 * Perform the actual transformation, writing to the given result.
	 * @param source the Source to transform
	 * @param parameters a Map of parameters to be applied to the stylesheet
	 * (as determined by {@link #getParameters(Map, HttpServletRequest)})
	 * @param result the result to write to
	 * @param encoding the preferred character encoding that the underlying Transformer should use
	 * @throws Exception if an error occurs
	 */
	protected void doTransform(Source source, Map<String, ?> parameters, Result result, String encoding)
			throws Exception {

		try {
			Transformer trans = buildTransformer(parameters);

			// Explicitly apply URIResolver to every created Transformer.
			if (this.uriResolver != null) {
				trans.setURIResolver(this.uriResolver);
			}

			// Specify default output properties.
			trans.setOutputProperty(OutputKeys.ENCODING, encoding);
			if (this.indent) {
				TransformerUtils.enableIndenting(trans);
			}

			// Apply any arbitrary output properties, if specified.
			if (this.outputProperties != null) {
				Enumeration<?> propsEnum = this.outputProperties.propertyNames();
				while (propsEnum.hasMoreElements()) {
					String propName = (String) propsEnum.nextElement();
					trans.setOutputProperty(propName, this.outputProperties.getProperty(propName));
				}
			}

			// Perform the actual XSLT transformation.
			trans.transform(source, result);
		}
		catch (TransformerConfigurationException ex) {
			throw new NestedServletException(
					"Couldn't create XSLT transformer in XSLT view with name [" + getBeanName() + "]", ex);
		}
		catch (TransformerException ex) {
			throw new NestedServletException(
					"Couldn't perform transform in XSLT view with name [" + getBeanName() + "]", ex);
		}
	}

	/**
	 * Build a Transformer object for immediate use, based on the
	 * given parameters.
	 * @param parameters a Map of parameters to be applied to the stylesheet
	 * (as determined by {@link #getParameters(Map, HttpServletRequest)})
	 * @return the Transformer object (never <code>null</code>)
	 * @throws TransformerConfigurationException if the Transformer object
	 * could not be built
	 */
	protected Transformer buildTransformer(Map<String, ?> parameters) throws TransformerConfigurationException {
		Templates templates = getTemplates();
		Transformer transformer =
				(templates != null ? templates.newTransformer() : getTransformerFactory().newTransformer());
		applyTransformerParameters(parameters, transformer);
		return transformer;
	}

	/**
	 * Obtain the Templates object to use, based on the configured
	 * stylesheet, either a cached one or a freshly built one.
	 * <p>Subclasses may override this method e.g. in order to refresh
	 * the Templates instance, calling {@link #resetCachedTemplates()}
	 * before delegating to this <code>getTemplates()</code> implementation.
	 * @return the Templates object (or <code>null</code> if there is
	 * no stylesheet specified)
	 * @throws TransformerConfigurationException if the Templates object
	 * could not be built
	 * @see #setStylesheetLocation
	 * @see #setCache
	 * @see #resetCachedTemplates
	 */
	protected Templates getTemplates() throws TransformerConfigurationException {
		if (this.cachedTemplates != null) {
			return this.cachedTemplates;
		}
		Resource location = getStylesheetLocation();
		if (location != null) {
			Templates templates = getTransformerFactory().newTemplates(getStylesheetSource(location));
			if (this.cache) {
				this.cachedTemplates = templates;
			}
			return templates;
		}
		return null;
	}

	/**
	 * Apply the specified parameters to the given Transformer.
	 * @param parameters the transformer parameters
	 * (as determined by {@link #getParameters(Map, HttpServletRequest)})
	 * @param transformer the Transformer to aply the parameters
	 */
	protected void applyTransformerParameters(Map<String, ?> parameters, Transformer transformer) {
		if (parameters != null) {
			for (Map.Entry<String, ?> entry : parameters.entrySet()) {
				transformer.setParameter(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Load the stylesheet from the specified location.
	 * @param stylesheetLocation the stylesheet resource to be loaded
	 * @return the stylesheet source
	 * @throws ApplicationContextException if the stylesheet resource could not be loaded
	 */
	protected Source getStylesheetSource(Resource stylesheetLocation) throws ApplicationContextException {
		if (logger.isDebugEnabled()) {
			logger.debug("Loading XSLT stylesheet from " + stylesheetLocation);
		}
		try {
			URL url = stylesheetLocation.getURL();
			String urlPath = url.toString();
			String systemId = urlPath.substring(0, urlPath.lastIndexOf('/') + 1);
			return new StreamSource(url.openStream(), systemId);
		}
		catch (IOException ex) {
			throw new ApplicationContextException("Can't load XSLT stylesheet from " + stylesheetLocation, ex);
		}
	}

}
