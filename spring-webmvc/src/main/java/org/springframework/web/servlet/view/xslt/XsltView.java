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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
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
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.SimpleTransformErrorListener;
import org.springframework.util.xml.TransformerUtils;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.util.WebUtils;

/**
 * XSLT-driven View that allows for response context to be rendered as the
 * result of an XSLT transformation.
 *
 * <p>The XSLT Source object is supplied as a parameter in the model and then
 * {@link #locateSource detected} during response rendering. Users can either specify
 * a specific entry in the model via the {@link #setSourceKey sourceKey} property or
 * have Spring locate the Source object. This class also provides basic conversion
 * of objects into Source implementations. See {@link #getSourceTypes() here}
 * for more details.
 *
 * <p>All model parameters are passed to the XSLT Transformer as parameters.
 * In addition the user can configure {@link #setOutputProperties output properties}
 * to be passed to the Transformer.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class XsltView extends AbstractUrlBasedView {

	private Class<?> transformerFactoryClass;

	private String sourceKey;

	private URIResolver uriResolver;

	private ErrorListener errorListener = new SimpleTransformErrorListener(logger);

	private boolean indent = true;

	private Properties outputProperties;

	private boolean cacheTemplates = true;

	private TransformerFactory transformerFactory;

	private Templates cachedTemplates;


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
	 * Set the name of the model attribute that represents the XSLT Source.
	 * If not specified, the model map will be searched for a matching value type.
	 * <p>The following source types are supported out of the box:
	 * {@link Source}, {@link Document}, {@link Node}, {@link Reader},
	 * {@link InputStream} and {@link Resource}.
	 * @see #getSourceTypes
	 * @see #convertSource
	 */
	public void setSourceKey(String sourceKey) {
		this.sourceKey = sourceKey;
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
		this.errorListener = (errorListener != null ? errorListener : new SimpleTransformErrorListener(logger));
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
	 * Turn on/off the caching of the XSLT {@link Templates} instance.
	 * <p>The default value is "true". Only set this to "false" in development,
	 * where caching does not seriously impact performance.
	 */
	public void setCacheTemplates(boolean cacheTemplates) {
		this.cacheTemplates = cacheTemplates;
	}


	/**
	 * Initialize this XsltView's TransformerFactory.
	 */
	@Override
	protected void initApplicationContext() throws BeansException {
		this.transformerFactory = newTransformerFactory(this.transformerFactoryClass);
		this.transformerFactory.setErrorListener(this.errorListener);
		if (this.uriResolver != null) {
			this.transformerFactory.setURIResolver(this.uriResolver);
		}
		if (this.cacheTemplates) {
			this.cachedTemplates = loadTemplates();
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
	 * Return the TransformerFactory that this XsltView uses.
	 * @return the TransformerFactory (never <code>null</code>)
	 */
	protected final TransformerFactory getTransformerFactory() {
		return this.transformerFactory;
	}


	@Override
	protected void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		Templates templates = this.cachedTemplates;
		if (templates == null) {
			templates = loadTemplates();
		}

		Transformer transformer = createTransformer(templates);
		configureTransformer(model, response, transformer);
		configureResponse(model, response, transformer);
		Source source = null;
		try {
			source = locateSource(model);
			if (source == null) {
				throw new IllegalArgumentException("Unable to locate Source object in model: " + model);
			}
			transformer.transform(source, createResult(response));
		}
		finally {
			closeSourceIfNecessary(source);
		}
	}

	/**
	 * Create the XSLT {@link Result} used to render the result of the transformation.
	 * <p>The default implementation creates a {@link StreamResult} wrapping the supplied
	 * HttpServletResponse's {@link HttpServletResponse#getOutputStream() OutputStream}.
	 * @param response current HTTP response
	 * @return the XSLT Result to use
	 * @throws Exception if the Result cannot be built
	 */
	protected Result createResult(HttpServletResponse response) throws Exception {
		return new StreamResult(response.getOutputStream());
	}

	/**
	 * <p>Locate the {@link Source} object in the supplied model,
	 * converting objects as required.
	 * The default implementation first attempts to look under the configured
	 * {@link #setSourceKey source key}, if any, before attempting to locate
	 * an object of {@link #getSourceTypes() supported type}.
	 * @param model the merged model Map
	 * @return the XSLT Source object (or <code>null</code> if none found)
	 * @throws Exception if an error occured during locating the source
	 * @see #setSourceKey
	 * @see #convertSource
	 */
	protected Source locateSource(Map<String, Object> model) throws Exception {
		if (this.sourceKey != null) {
			return convertSource(model.get(this.sourceKey));
		}
		Object source = CollectionUtils.findValueOfType(model.values(), getSourceTypes());
		return (source != null ? convertSource(source) : null);
	}

	/**
	 * Return the array of {@link Class Classes} that are supported when converting to an
	 * XSLT {@link Source}.
	 * <p>Currently supports {@link Source}, {@link Document}, {@link Node},
	 * {@link Reader}, {@link InputStream} and {@link Resource}.
	 * @return the supported source types
	 */
	protected Class<?>[] getSourceTypes() {
		return new Class[] {Source.class, Document.class, Node.class, Reader.class, InputStream.class, Resource.class};
	}

	/**
	 * Convert the supplied {@link Object} into an XSLT {@link Source} if the
	 * {@link Object} type is {@link #getSourceTypes() supported}.
	 * @param source the original source object
	 * @return the adapted XSLT Source
	 * @throws IllegalArgumentException if the given Object is not of a supported type
	 */
	protected Source convertSource(Object source) throws Exception {
		if (source instanceof Source) {
			return (Source) source;
		}
		else if (source instanceof Document) {
			return new DOMSource(((Document) source).getDocumentElement());
		}
		else if (source instanceof Node) {
			return new DOMSource((Node) source);
		}
		else if (source instanceof Reader) {
			return new StreamSource((Reader) source);
		}
		else if (source instanceof InputStream) {
			return new StreamSource((InputStream) source);
		}
		else if (source instanceof Resource) {
			Resource resource = (Resource) source;
			return new StreamSource(resource.getInputStream(), resource.getURI().toASCIIString());
		}
		else {
			throw new IllegalArgumentException("Value '" + source + "' cannot be converted to XSLT Source");
		}
	}

	/**
	 * Configure the supplied {@link Transformer} instance.
	 * <p>The default implementation copies parameters from the model into the
	 * Transformer's {@link Transformer#setParameter parameter set}.
	 * This implementation also copies the {@link #setOutputProperties output properties}
	 * into the {@link Transformer} {@link Transformer#setOutputProperty output properties}.
	 * Indentation properties are set as well.
	 * @param model merged output Map (never <code>null</code>)
	 * @param response current HTTP response
	 * @param transformer the target transformer
	 * @see #copyModelParameters(Map, Transformer)
	 * @see #copyOutputProperties(Transformer)
	 * @see #configureIndentation(Transformer)
	 */
	protected void configureTransformer(Map<String, Object> model, HttpServletResponse response, Transformer transformer) {
		copyModelParameters(model, transformer);
		copyOutputProperties(transformer);
		configureIndentation(transformer);
	}

	/**
	 * Configure the indentation settings for the supplied {@link Transformer}.
	 * @param transformer the target transformer
	 * @see org.springframework.util.xml.TransformerUtils#enableIndenting(javax.xml.transform.Transformer)
	 * @see org.springframework.util.xml.TransformerUtils#disableIndenting(javax.xml.transform.Transformer)
	 */
	protected final void configureIndentation(Transformer transformer) {
		if (this.indent) {
			TransformerUtils.enableIndenting(transformer);
		}
		else {
			TransformerUtils.disableIndenting(transformer);
		}
	}

	/**
	 * Copy the configured output {@link Properties}, if any, into the
	 * {@link Transformer#setOutputProperty output property set} of the supplied
	 * {@link Transformer}.
	 * @param transformer the target transformer
	 */
	protected final void copyOutputProperties(Transformer transformer) {
		if (this.outputProperties != null) {
			Enumeration<?> en = this.outputProperties.propertyNames();
			while (en.hasMoreElements()) {
				String name = (String) en.nextElement();
				transformer.setOutputProperty(name, this.outputProperties.getProperty(name));
			}
		}
	}

	/**
	 * Copy all entries from the supplied Map into the
	 * {@link Transformer#setParameter(String, Object) parameter set}
	 * of the supplied {@link Transformer}.
	 * @param model merged output Map (never <code>null</code>)
	 * @param transformer the target transformer
	 */
	protected final void copyModelParameters(Map<String, Object> model, Transformer transformer) {
		for (Map.Entry<String, Object> entry : model.entrySet()) {
			transformer.setParameter(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Configure the supplied {@link HttpServletResponse}.
	 * <p>The default implementation of this method sets the
	 * {@link HttpServletResponse#setContentType content type} and
	 * {@link HttpServletResponse#setCharacterEncoding encoding}
	 * from the "media-type" and "encoding" output properties
	 * specified in the {@link Transformer}.
	 * @param model merged output Map (never <code>null</code>)
	 * @param response current HTTP response
	 * @param transformer the target transformer
	 */
	protected void configureResponse(Map<String, Object> model, HttpServletResponse response, Transformer transformer) {
		String contentType = getContentType();
		String mediaType = transformer.getOutputProperty(OutputKeys.MEDIA_TYPE);
		String encoding = transformer.getOutputProperty(OutputKeys.ENCODING);
		if (StringUtils.hasText(mediaType)) {
			contentType = mediaType;
		}
		if (StringUtils.hasText(encoding)) {
			// Only apply encoding if content type is specified but does not contain charset clause already.
			if (contentType != null && !contentType.toLowerCase().contains(WebUtils.CONTENT_TYPE_CHARSET_PREFIX)) {
				contentType = contentType + WebUtils.CONTENT_TYPE_CHARSET_PREFIX + encoding;
			}
		}
		response.setContentType(contentType);
	}

	/**
	 * Load the {@link Templates} instance for the stylesheet at the configured location.
	 */
	private Templates loadTemplates() throws ApplicationContextException {
		Source stylesheetSource = getStylesheetSource();
		try {
			Templates templates = this.transformerFactory.newTemplates(stylesheetSource);
			if (logger.isDebugEnabled()) {
				logger.debug("Loading templates '" + templates + "'");
			}
			return templates;
		}
		catch (TransformerConfigurationException ex) {
			throw new ApplicationContextException("Can't load stylesheet from '" + getUrl() + "'", ex);
		}
		finally {
			closeSourceIfNecessary(stylesheetSource);
		}
	}

	/**
	 * Create the {@link Transformer} instance used to prefer the XSLT transformation.
	 * <p>The default implementation simply calls {@link Templates#newTransformer()}, and
	 * configures the {@link Transformer} with the custom {@link URIResolver} if specified.
	 * @param templates the XSLT Templates instance to create a Transformer for
	 * @return the Transformer object
	 * @throws TransformerConfigurationException in case of creation failure
	 */
	protected Transformer createTransformer(Templates templates) throws TransformerConfigurationException {
		Transformer transformer = templates.newTransformer();
		if (this.uriResolver != null) {
			transformer.setURIResolver(this.uriResolver);
		}
		return transformer;
	}

	/**
	 * Get the XSLT {@link Source} for the XSLT template under the {@link #setUrl configured URL}.
	 * @return the Source object
	 */
	protected Source getStylesheetSource() {
		String url = getUrl();
		if (logger.isDebugEnabled()) {
			logger.debug("Loading XSLT stylesheet from '" + url + "'");
		}
		try {
			Resource resource = getApplicationContext().getResource(url);
			return new StreamSource(resource.getInputStream(), resource.getURI().toASCIIString());
		}
		catch (IOException ex) {
			throw new ApplicationContextException("Can't load XSLT stylesheet from '" + url + "'", ex);
		}
	}

	/**
	 * Close the underlying resource managed by the supplied {@link Source} if applicable.
	 * <p>Only works for {@link StreamSource StreamSources}.
	 * @param source the XSLT Source to close (may be <code>null</code>)
	 */
	private void closeSourceIfNecessary(Source source) {
		if (source instanceof StreamSource) {
			StreamSource streamSource = (StreamSource) source;
			if (streamSource.getReader() != null) {
				try {
					streamSource.getReader().close();
				}
				catch (IOException ex) {
					// ignore
				}
			}
			if (streamSource.getInputStream() != null) {
				try {
					streamSource.getInputStream().close();
				}
				catch (IOException ex) {
					// ignore
				}
			}
		}
	}

}
