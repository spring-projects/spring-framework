/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.servlet.view;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

/**
 * Implementation of {@link ViewResolver} that resolves a view based on the request file name or {@code Accept} header.
 *
 * <p>The {@code ContentNegotiatingViewResolver} does not resolve views itself, but delegates to other {@link
 * ViewResolver}s. By default, these other view resolvers are picked up automatically from the application context,
 * though they can also be set explicitely by using the {@link #setViewResolvers(List) viewResolvers} property.
 * <strong>Note</strong> that in order for this view resolver to work properly, the {@link #setOrder(int) order}
 * property needs to be set to a higher precedence than the others (the default is {@link Ordered#HIGHEST_PRECEDENCE}.)
 *
 * <p>This view resolver uses the requested {@linkplain MediaType media type} to select a suitable {@link View} for a
 * request. This media type is determined by using the following criteria: <ol> <li>If the requested path has a file
 * extension and if the {@link #setFavorPathExtension(boolean)} property is <code>true</code>, the {@link
 * #setMediaTypes(Map)  mediaTypes} property is inspected for a matching media type.</li> <li>If there is no match and
 * if the Java Activation Framework (JAF) is present on the class path, {@link FileTypeMap#getContentType(String)} is
 * used.</li> <li>If the previous steps did not result in a media type, the request {@code Accept} header is used.</li>
 * </ol> Once the requested media type has been determined, this resolver queries each delegate view resolver for a
 * {@link View} and determines if the requested media type is {@linkplain MediaType#includes(MediaType) compatible} with
 * the view's {@linkplain View#getContentType() content type}). The most compatible view is returned.
 *
 * <p>For example, if the request path is {@code /view.html}, this view resolver will look for a view that has the
 * {@code text/html} content type (based on the {@code html} file extension). A request for {@code /view} with a {@code
 * text/html} request {@code Accept} header has the same result.
 *
 * @author Arjen Poutsma
 * @see ViewResolver
 * @see InternalResourceViewResolver
 * @see BeanNameViewResolver
 * @since 3.0
 */
public class ContentNegotiatingViewResolver extends WebApplicationObjectSupport implements ViewResolver, Ordered {

	private static final boolean jafPresent =
			ClassUtils.isPresent("javax.activation.FileTypeMap", ContentNegotiatingViewResolver.class.getClassLoader());

	private static final String ACCEPT_HEADER = "Accept";

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private boolean favorPathExtension = true;

	private int order = Ordered.HIGHEST_PRECEDENCE;

	private ConcurrentMap<String, MediaType> mediaTypes = new ConcurrentHashMap<String, MediaType>();

	private List<ViewResolver> viewResolvers;

	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return order;
	}

	/**
	 * Indicates whether the extension of the request path should be used to determine the requested media type, in favor
	 * of looking at the {@code Accept} header.
	 *
	 * <p>For instance, when this flag is <code>true</code> (the default), a request for {@code /hotels.pdf} will result in
	 * an {@code AbstractPdfView} being resolved, while the {@code Accept} header can be the browser-defined {@code
	 * text/html,application/xhtml+xml}.
	 */
	public void setFavorPathExtension(boolean favorPathExtension) {
		this.favorPathExtension = favorPathExtension;
	}

	/**
	 * Sets the mapping from file extensions to media types.
	 *
	 * <p>When this mapping is not set or when an extension is not present, this view resolver will fall back to using a
	 * {@link FileTypeMap} when the Java Action Framework is available.
	 */
	public void setMediaTypes(Map<String, String> mediaTypes) {
		Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
		for (Map.Entry<String, String> entry : mediaTypes.entrySet()) {
			String extension = entry.getKey().toLowerCase(Locale.ENGLISH);
			MediaType mediaType = MediaType.parseMediaType(entry.getValue());
			this.mediaTypes.put(extension, mediaType);
		}
	}

	/**
	 * Sets the view resolvers to be wrapped by this view resolver.
	 *
	 * <p>If this property is not set, view resolvers will be detected automatically.
	 */
	public void setViewResolvers(List<ViewResolver> viewResolvers) {
		this.viewResolvers = viewResolvers;
	}

	@Override
	protected void initServletContext(ServletContext servletContext) {
		if (viewResolvers == null) {
			Map<String, ViewResolver> matchingBeans = BeanFactoryUtils
					.beansOfTypeIncludingAncestors(getApplicationContext(), ViewResolver.class, true, false);
			this.viewResolvers = new ArrayList<ViewResolver>(matchingBeans.size());
			for (ViewResolver viewResolver : matchingBeans.values()) {
				if (this != viewResolver) {
					this.viewResolvers.add(viewResolver);
				}
			}
		}
		if (this.viewResolvers.isEmpty()) {
			logger.warn("Did not find any ViewResolvers to delegate to; please configure them using the " +
					"'viewResolvers' property on the ContentNegotiatingViewResolver");
		}
		OrderComparator.sort(this.viewResolvers);
	}

	/**
	 * Determines the list of {@link MediaType} for the given {@link HttpServletRequest}.
	 *
	 * <p>The default implementation invokes {@link #getMediaTypeFromFilename(String)} if {@linkplain
	 * #setFavorPathExtension(boolean) favorPathExtension} property is <code>true</code>. If the property is
	 * <code>false</code>, or when a media type cannot be determined from the request path, this method will inspect the
	 * {@code Accept} header of the request.
	 *
	 * <p>This method can be overriden to provide a different algorithm.
	 *
	 * @param request the current servlet request
	 * @return the list of media types requested, if any
	 */
	protected List<MediaType> getMediaTypes(HttpServletRequest request) {
		if (favorPathExtension) {
			String requestUri = urlPathHelper.getRequestUri(request);
			String filename = WebUtils.extractFullFilenameFromUrlPath(requestUri);
			MediaType mediaType = getMediaTypeFromFilename(filename);
			if (mediaType != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Requested media type is '" + mediaType + "' (based on filename '" + filename + "')");
				}
				List<MediaType> mediaTypes = new ArrayList<MediaType>();
				mediaTypes.add(mediaType);
				return mediaTypes;
			}
		}
		String acceptHeader = request.getHeader(ACCEPT_HEADER);
		if (StringUtils.hasText(acceptHeader)) {
			List<MediaType> mediaTypes = MediaType.parseMediaTypes(acceptHeader);
			if (logger.isDebugEnabled()) {
				logger.debug("Requested media types are " + mediaTypes + " (based on Accept header)");
			}
			return mediaTypes;
		}
		else {
			return Collections.emptyList();
		}
	}

	/**
	 * Determines the {@link MediaType} for the given filename.
	 *
	 * <p>The default implementation will check the {@linkplain #setMediaTypes(Map) media types} property first for a
	 * defined mapping. If not present, and if the Java Activation Framework can be found on the class path, it will call
	 * {@link FileTypeMap#getContentType(String)}
	 *
	 * <p>This method can be overriden to provide a different algorithm.
	 *
	 * @param filename the current request file name (i.e. {@code hotels.html})
	 * @return the media type, if any
	 */
	protected MediaType getMediaTypeFromFilename(String filename) {
		String extension = StringUtils.getFilenameExtension(filename);
		if (!StringUtils.hasText(extension)) {
			return null;
		}
		extension = extension.toLowerCase(Locale.ENGLISH);
		MediaType mediaType = mediaTypes.get(extension);
		if (mediaType == null && jafPresent) {
			mediaType = ActivationMediaTypeFactory.getMediaType(filename);
			if (mediaType != null) {
				mediaTypes.putIfAbsent(extension, mediaType);
			}
		}
		return mediaType;
	}

	public View resolveViewName(String viewName, Locale locale) throws Exception {
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		Assert.isInstanceOf(ServletRequestAttributes.class, attrs);
		ServletRequestAttributes servletAttrs = (ServletRequestAttributes) attrs;
		List<MediaType> requestedMediaTypes = getMediaTypes(servletAttrs.getRequest());
		Collections.sort(requestedMediaTypes);

		SortedMap<MediaType, View> views = new TreeMap<MediaType, View>();
		for (ViewResolver viewResolver : viewResolvers) {
			View view = viewResolver.resolveViewName(viewName, locale);
			if (view != null) {
				MediaType viewMediaType = MediaType.parseMediaType(view.getContentType());
				for (MediaType requestedMediaType : requestedMediaTypes) {
					if (requestedMediaType.includes(viewMediaType)) {
						if (!views.containsKey(requestedMediaType)) {
							views.put(requestedMediaType, view);
							break;
						}
					}
				}
			}
		}
		if (!views.isEmpty()) {
			MediaType mediaType = views.firstKey();
			View view = views.get(mediaType);
			if (logger.isDebugEnabled()) {
				logger.debug("Returning [" + view + "] based on requested media type '" + mediaType + "'");
			}
			return view;
		}
		else {
			return null;
		}
	}

	/**
	 * Inner class to avoid hard-coded JAF dependency.
	 */
	private static class ActivationMediaTypeFactory {

		private static final FileTypeMap fileTypeMap;

		static {
			fileTypeMap = loadFileTypeMapFromContextSupportModule();
		}

		private static FileTypeMap loadFileTypeMapFromContextSupportModule() {
			// see if we can find the extended mime.types from the context-support module
			Resource mappingLocation = new ClassPathResource("org/springframework/mail/javamail/mime.types");
			if (mappingLocation.exists()) {
				InputStream inputStream = null;
				try {
					inputStream = mappingLocation.getInputStream();
					return new MimetypesFileTypeMap(inputStream);
				}
				catch (IOException ex) {
					// ignore
				}
				finally {
					if (inputStream != null) {
						try {
							inputStream.close();
						}
						catch (IOException ex) {
							// ignore
						}
					}
				}
			}
			return FileTypeMap.getDefaultFileTypeMap();
		}

		public static MediaType getMediaType(String fileName) {
			String mediaType = fileTypeMap.getContentType(fileName);
			return StringUtils.hasText(mediaType) ? MediaType.parseMediaType(mediaType) : null;
		}
	}

}
