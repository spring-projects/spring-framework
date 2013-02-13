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

package org.springframework.web.servlet.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.activation.FileTypeMap;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * Implementation of {@link ViewResolver} that resolves a view based on the request file name or {@code Accept} header.
 *
 * <p>The {@code ContentNegotiatingViewResolver} does not resolve views itself, but delegates to other {@link
 * ViewResolver}s. By default, these other view resolvers are picked up automatically from the application context,
 * though they can also be set explicitly by using the {@link #setViewResolvers(List) viewResolvers} property.
 * <strong>Note</strong> that in order for this view resolver to work properly, the {@link #setOrder(int) order}
 * property needs to be set to a higher precedence than the others (the default is {@link Ordered#HIGHEST_PRECEDENCE}.)
 *
 * <p>This view resolver uses the requested {@linkplain MediaType media type} to select a suitable {@link View} for a
 * request. The requested media type is determined through the configured {@link ContentNegotiationManager}.
 * Once the requested media type has been determined, this resolver queries each delegate view resolver for a
 * {@link View} and determines if the requested media type is {@linkplain MediaType#includes(MediaType) compatible}
 * with the view's {@linkplain View#getContentType() content type}). The most compatible view is returned.
 *
 * <p>Additionally, this view resolver exposes the {@link #setDefaultViews(List) defaultViews} property, allowing you to
 * override the views provided by the view resolvers. Note that these default views are offered as candicates, and
 * still need have the content type requested (via file extension, parameter, or {@code Accept} header, described above).
 * You can also set the {@linkplain #setDefaultContentType(MediaType) default content type} directly, which will be
 * returned when the other mechanisms ({@code Accept} header, file extension or parameter) do not result in a match.
 *
 * <p>For example, if the request path is {@code /view.html}, this view resolver will look for a view that has the
 * {@code text/html} content type (based on the {@code html} file extension). A request for {@code /view} with a {@code
 * text/html} request {@code Accept} header has the same result.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 * @see ViewResolver
 * @see InternalResourceViewResolver
 * @see BeanNameViewResolver
 */
public class ContentNegotiatingViewResolver extends WebApplicationObjectSupport implements ViewResolver, Ordered, InitializingBean {

	private static final Log logger = LogFactory.getLog(ContentNegotiatingViewResolver.class);

	private int order = Ordered.HIGHEST_PRECEDENCE;

	private ContentNegotiationManager contentNegotiationManager;

	private ContentNegotiationManagerFactoryBean cnManagerFactoryBean = new ContentNegotiationManagerFactoryBean();

	private boolean useNotAcceptableStatusCode = false;

	private List<View> defaultViews;

	private List<ViewResolver> viewResolvers;


	public ContentNegotiatingViewResolver() {
		super();
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the {@link ContentNegotiationManager} to use to determine requested media types.
	 * If not set, the default constructor is used.
	 */
	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * Indicate whether the extension of the request path should be used to determine the requested media type,
	 * in favor of looking at the {@code Accept} header. The default value is {@code true}.
	 * <p>For instance, when this flag is {@code true} (the default), a request for {@code /hotels.pdf}
	 * will result in an {@code AbstractPdfView} being resolved, while the {@code Accept} header can be the
	 * browser-defined {@code text/html,application/xhtml+xml}.
	 *
	 * @deprecated use {@link #setContentNegotiationManager(ContentNegotiationManager)}
	 */
	@Deprecated
	public void setFavorPathExtension(boolean favorPathExtension) {
		this.cnManagerFactoryBean.setFavorParameter(favorPathExtension);
	}

	/**
	 * Indicate whether to use the Java Activation Framework to map from file extensions to media types.
	 * <p>Default is {@code true}, i.e. the Java Activation Framework is used (if available).
	 *
	 * @deprecated use {@link #setContentNegotiationManager(ContentNegotiationManager)}
	 */
	@Deprecated
	public void setUseJaf(boolean useJaf) {
		this.cnManagerFactoryBean.setUseJaf(useJaf);
	}

	/**
	 * Indicate whether a request parameter should be used to determine the requested media type,
	 * in favor of looking at the {@code Accept} header. The default value is {@code false}.
	 * <p>For instance, when this flag is {@code true}, a request for {@code /hotels?format=pdf} will result
	 * in an {@code AbstractPdfView} being resolved, while the {@code Accept} header can be the browser-defined
	 * {@code text/html,application/xhtml+xml}.
	 *
	 * @deprecated use {@link #setContentNegotiationManager(ContentNegotiationManager)}
	 */
	@Deprecated
	public void setFavorParameter(boolean favorParameter) {
		this.cnManagerFactoryBean.setFavorParameter(favorParameter);
	}

	/**
	 * Set the parameter name that can be used to determine the requested media type if the {@link
	 * #setFavorParameter} property is {@code true}. The default parameter name is {@code format}.
	 *
	 * @deprecated use {@link #setContentNegotiationManager(ContentNegotiationManager)}
	 */
	@Deprecated
	public void setParameterName(String parameterName) {
		this.cnManagerFactoryBean.setParameterName(parameterName);
	}

	/**
	 * Indicate whether the HTTP {@code Accept} header should be ignored. Default is {@code false}.
	 * <p>If set to {@code true}, this view resolver will only refer to the file extension and/or
	 * parameter, as indicated by the {@link #setFavorPathExtension favorPathExtension} and
	 * {@link #setFavorParameter favorParameter} properties.
	 *
	 * @deprecated use {@link #setContentNegotiationManager(ContentNegotiationManager)}
	 */
	@Deprecated
	public void setIgnoreAcceptHeader(boolean ignoreAcceptHeader) {
		this.cnManagerFactoryBean.setIgnoreAcceptHeader(ignoreAcceptHeader);
	}

	/**
	 * Set the mapping from file extensions to media types.
	 * <p>When this mapping is not set or when an extension is not present, this view resolver
	 * will fall back to using a {@link FileTypeMap} when the Java Action Framework is available.
	 *
	 * @deprecated use {@link #setContentNegotiationManager(ContentNegotiationManager)}
	 */
	@Deprecated
	public void setMediaTypes(Map<String, String> mediaTypes) {
		if (mediaTypes != null) {
			Properties props = new Properties();
			props.putAll(mediaTypes);
			this.cnManagerFactoryBean.setMediaTypes(props);
		}
	}

	/**
	 * Set the default content type.
	 * <p>This content type will be used when file extension, parameter, nor {@code Accept}
	 * header define a content-type, either through being disabled or empty.
	 *
	 * @deprecated use {@link #setContentNegotiationManager(ContentNegotiationManager)}
	 */
	@Deprecated
	public void setDefaultContentType(MediaType defaultContentType) {
		this.cnManagerFactoryBean.setDefaultContentType(defaultContentType);
	}

	/**
	 * Indicate whether a {@link HttpServletResponse#SC_NOT_ACCEPTABLE 406 Not Acceptable}
	 * status code should be returned if no suitable view can be found.
	 * <p>Default is {@code false}, meaning that this view resolver returns {@code null} for
	 * {@link #resolveViewName(String, Locale)} when an acceptable view cannot be found.
	 * This will allow for view resolvers chaining. When this property is set to {@code true},
	 * {@link #resolveViewName(String, Locale)} will respond with a view that sets the
	 * response status to {@code 406 Not Acceptable} instead.
	 */
	public void setUseNotAcceptableStatusCode(boolean useNotAcceptableStatusCode) {
		this.useNotAcceptableStatusCode = useNotAcceptableStatusCode;
	}

	/**
	 * Set the default views to use when a more specific view can not be obtained
	 * from the {@link ViewResolver} chain.
	 */
	public void setDefaultViews(List<View> defaultViews) {
		this.defaultViews = defaultViews;
	}

	/**
	 * Sets the view resolvers to be wrapped by this view resolver.
	 * <p>If this property is not set, view resolvers will be detected automatically.
	 */
	public void setViewResolvers(List<ViewResolver> viewResolvers) {
		this.viewResolvers = viewResolvers;
	}


	@Override
	protected void initServletContext(ServletContext servletContext) {
		Collection<ViewResolver> matchingBeans =
				BeanFactoryUtils.beansOfTypeIncludingAncestors(getApplicationContext(), ViewResolver.class).values();
		if (this.viewResolvers == null) {
			this.viewResolvers = new ArrayList<ViewResolver>(matchingBeans.size());
			for (ViewResolver viewResolver : matchingBeans) {
				if (this != viewResolver) {
					this.viewResolvers.add(viewResolver);
				}
			}
		}
		else {
			for (int i=0; i < viewResolvers.size(); i++) {
				if (matchingBeans.contains(viewResolvers.get(i))) {
					continue;
				}
				String name = viewResolvers.get(i).getClass().getName() + i;
				getApplicationContext().getAutowireCapableBeanFactory().initializeBean(viewResolvers.get(i), name);
			}

		}
		if (this.viewResolvers.isEmpty()) {
			logger.warn("Did not find any ViewResolvers to delegate to; please configure them using the " +
					"'viewResolvers' property on the ContentNegotiatingViewResolver");
		}
		OrderComparator.sort(this.viewResolvers);
		this.cnManagerFactoryBean.setServletContext(servletContext);
	}

	public void afterPropertiesSet() throws Exception {
		if (this.contentNegotiationManager == null) {
			this.cnManagerFactoryBean.afterPropertiesSet();
			this.contentNegotiationManager = this.cnManagerFactoryBean.getObject();
		}
	}

	public View resolveViewName(String viewName, Locale locale) throws Exception {
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		Assert.isInstanceOf(ServletRequestAttributes.class, attrs);
		List<MediaType> requestedMediaTypes = getMediaTypes(((ServletRequestAttributes) attrs).getRequest());
		if (requestedMediaTypes != null) {
			List<View> candidateViews = getCandidateViews(viewName, locale, requestedMediaTypes);
			View bestView = getBestView(candidateViews, requestedMediaTypes, attrs);
			if (bestView != null) {
				return bestView;
			}
		}
		if (this.useNotAcceptableStatusCode) {
			if (logger.isDebugEnabled()) {
				logger.debug("No acceptable view found; returning 406 (Not Acceptable) status code");
			}
			return NOT_ACCEPTABLE_VIEW;
		}
		else {
			logger.debug("No acceptable view found; returning null");
			return null;
		}
	}

	/**
	 * Determines the list of {@link MediaType} for the given {@link HttpServletRequest}.
	 * @param request the current servlet request
	 * @return the list of media types requested, if any
	 */
	protected List<MediaType> getMediaTypes(HttpServletRequest request) {
		try {
			ServletWebRequest webRequest = new ServletWebRequest(request);
			List<MediaType> acceptableMediaTypes = this.contentNegotiationManager.resolveMediaTypes(webRequest);
			List<MediaType> producibleMediaTypes = getProducibleMediaTypes(request);
			Set<MediaType> compatibleMediaTypes = new LinkedHashSet<MediaType>();
			for (MediaType acceptable : acceptableMediaTypes) {
				for (MediaType producible : producibleMediaTypes) {
					if (acceptable.isCompatibleWith(producible)) {
						compatibleMediaTypes.add(getMostSpecificMediaType(acceptable, producible));
					}
				}
			}
			List<MediaType> selectedMediaTypes = new ArrayList<MediaType>(compatibleMediaTypes);
			MediaType.sortBySpecificityAndQuality(selectedMediaTypes);
			if (logger.isDebugEnabled()) {
				logger.debug("Requested media types are " + selectedMediaTypes + " based on Accept header types " +
						"and producible media types " + producibleMediaTypes + ")");
			}
			return selectedMediaTypes;
		}
		catch (HttpMediaTypeNotAcceptableException ex) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private List<MediaType> getProducibleMediaTypes(HttpServletRequest request) {
		Set<MediaType> mediaTypes = (Set<MediaType>)
				request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			return new ArrayList<MediaType>(mediaTypes);
		}
		else {
			return Collections.singletonList(MediaType.ALL);
		}
	}

	/**
	 * Return the more specific of the acceptable and the producible media types
	 * with the q-value of the former.
	 */
	private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
		produceType = produceType.copyQualityValue(acceptType);
		return MediaType.SPECIFICITY_COMPARATOR.compare(acceptType, produceType) < 0 ? acceptType : produceType;
	}

	private List<View> getCandidateViews(String viewName, Locale locale, List<MediaType> requestedMediaTypes)
			throws Exception {

		List<View> candidateViews = new ArrayList<View>();
		for (ViewResolver viewResolver : this.viewResolvers) {
			View view = viewResolver.resolveViewName(viewName, locale);
			if (view != null) {
				candidateViews.add(view);
			}
			for (MediaType requestedMediaType : requestedMediaTypes) {
				List<String> extensions = this.contentNegotiationManager.resolveFileExtensions(requestedMediaType);
				for (String extension : extensions) {
					String viewNameWithExtension = viewName + "." + extension;
					view = viewResolver.resolveViewName(viewNameWithExtension, locale);
					if (view != null) {
						candidateViews.add(view);
					}
				}
			}
		}
		if (!CollectionUtils.isEmpty(this.defaultViews)) {
			candidateViews.addAll(this.defaultViews);
		}
		return candidateViews;
	}

	private View getBestView(List<View> candidateViews, List<MediaType> requestedMediaTypes, RequestAttributes attrs) {
		for (View candidateView : candidateViews) {
			if (candidateView instanceof SmartView) {
				SmartView smartView = (SmartView) candidateView;
				if (smartView.isRedirectView()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Returning redirect view [" + candidateView + "]");
					}
					return candidateView;
				}
			}
		}
		for (MediaType mediaType : requestedMediaTypes) {
			for (View candidateView : candidateViews) {
				if (StringUtils.hasText(candidateView.getContentType())) {
					MediaType candidateContentType = MediaType.parseMediaType(candidateView.getContentType());
					if (mediaType.isCompatibleWith(candidateContentType)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Returning [" + candidateView + "] based on requested media type '"
									+ mediaType + "'");
						}
						attrs.setAttribute(View.SELECTED_CONTENT_TYPE, mediaType, RequestAttributes.SCOPE_REQUEST);
						return candidateView;
					}
				}
			}
		}
		return null;
	}


	private static final View NOT_ACCEPTABLE_VIEW = new View() {

		public String getContentType() {
			return null;
		}

		public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
			response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
		}
	};

}
