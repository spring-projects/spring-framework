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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * Extends {@link AbstractMessageConverterMethodArgumentResolver} with the ability to handle
 * method return values by writing to the response with {@link HttpMessageConverter}s.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class AbstractMessageConverterMethodProcessor extends AbstractMessageConverterMethodArgumentResolver
		implements HandlerMethodReturnValueHandler {

	private static final MediaType MEDIA_TYPE_APPLICATION = new MediaType("application");

	private static final UrlPathHelper RAW_URL_PATH_HELPER = new UrlPathHelper();

	private static final UrlPathHelper DECODING_URL_PATH_HELPER = new UrlPathHelper();

	static {
		RAW_URL_PATH_HELPER.setRemoveSemicolonContent(false);
		RAW_URL_PATH_HELPER.setUrlDecode(false);
	}

	/* Extensions associated with the built-in message converters */
	private static final Set<String> WHITELISTED_EXTENSIONS = new HashSet<String>(Arrays.asList(
			"txt", "text", "yml", "properties", "csv",
			"json", "xml", "atom", "rss",
			"png", "jpe", "jpeg", "jpg", "gif", "wbmp", "bmp"));

	private static final Set<String> WHITELISTED_MEDIA_BASE_TYPES = new HashSet<String>(
			Arrays.asList("audio", "image", "video"));


	private final ContentNegotiationManager contentNegotiationManager;

	private final PathExtensionContentNegotiationStrategy pathStrategy;

	private final Set<String> safeExtensions = new HashSet<String>();



	/**
	 * Constructor with list of converters only.
	 */
	protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters) {
		this(converters, null);
	}

	/**
	 * Constructor with list of converters and ContentNegotiationManager.
	 */
	protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters,
			ContentNegotiationManager contentNegotiationManager) {

		this(converters, contentNegotiationManager, null);
	}

	/**
	 * Constructor with list of converters and ContentNegotiationManager as well
	 * as request/response body advice instances.
	 */
	protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters,
			ContentNegotiationManager manager, List<Object> requestResponseBodyAdvice) {

		super(converters, requestResponseBodyAdvice);
		this.contentNegotiationManager = (manager != null ? manager : new ContentNegotiationManager());
		this.pathStrategy = initPathStrategy(this.contentNegotiationManager);
		this.safeExtensions.addAll(this.contentNegotiationManager.getAllFileExtensions());
		this.safeExtensions.addAll(WHITELISTED_EXTENSIONS);
	}

	private static PathExtensionContentNegotiationStrategy initPathStrategy(ContentNegotiationManager manager) {
		for (ContentNegotiationStrategy strategy : manager.getStrategies()) {
			if (strategy instanceof PathExtensionContentNegotiationStrategy) {
				return (PathExtensionContentNegotiationStrategy) strategy;
			}
		}
		return new PathExtensionContentNegotiationStrategy();
	}


	/**
	 * Creates a new {@link HttpOutputMessage} from the given {@link NativeWebRequest}.
	 * @param webRequest the web request to create an output message from
	 * @return the output message
	 */
	protected ServletServerHttpResponse createOutputMessage(NativeWebRequest webRequest) {
		HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
		return new ServletServerHttpResponse(response);
	}

	/**
	 * Writes the given return value to the given web request. Delegates to
	 * {@link #writeWithMessageConverters(Object, MethodParameter, ServletServerHttpRequest, ServletServerHttpResponse)}
	 */
	protected <T> void writeWithMessageConverters(T returnValue, MethodParameter returnType, NativeWebRequest webRequest)
			throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {

		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
		ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);
		writeWithMessageConverters(returnValue, returnType, inputMessage, outputMessage);
	}

	/**
	 * Writes the given return type to the given output message.
	 * @param returnValue the value to write to the output message
	 * @param returnType the type of the value
	 * @param inputMessage the input messages. Used to inspect the {@code Accept} header.
	 * @param outputMessage the output message to write to
	 * @throws IOException thrown in case of I/O errors
	 * @throws HttpMediaTypeNotAcceptableException thrown when the conditions indicated by {@code Accept} header on
	 * the request cannot be met by the message converters
	 */
	@SuppressWarnings("unchecked")
	protected <T> void writeWithMessageConverters(T returnValue, MethodParameter returnType,
			ServletServerHttpRequest inputMessage, ServletServerHttpResponse outputMessage)
			throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {

		Class<?> returnValueClass = getReturnValueType(returnValue, returnType);
		Type returnValueType = getGenericType(returnType);
		HttpServletRequest servletRequest = inputMessage.getServletRequest();
		List<MediaType> requestedMediaTypes = getAcceptableMediaTypes(servletRequest);
		List<MediaType> producibleMediaTypes = getProducibleMediaTypes(servletRequest, returnValueClass, returnValueType);

		if (returnValue != null && producibleMediaTypes.isEmpty()) {
			throw new IllegalArgumentException("No converter found for return value of type: " + returnValueClass);
		}

		Set<MediaType> compatibleMediaTypes = new LinkedHashSet<MediaType>();
		for (MediaType requestedType : requestedMediaTypes) {
			for (MediaType producibleType : producibleMediaTypes) {
				if (requestedType.isCompatibleWith(producibleType)) {
					compatibleMediaTypes.add(getMostSpecificMediaType(requestedType, producibleType));
				}
			}
		}
		if (compatibleMediaTypes.isEmpty()) {
			if (returnValue != null) {
				throw new HttpMediaTypeNotAcceptableException(producibleMediaTypes);
			}
			return;
		}

		List<MediaType> mediaTypes = new ArrayList<MediaType>(compatibleMediaTypes);
		MediaType.sortBySpecificityAndQuality(mediaTypes);

		MediaType selectedMediaType = null;
		for (MediaType mediaType : mediaTypes) {
			if (mediaType.isConcrete()) {
				selectedMediaType = mediaType;
				break;
			}
			else if (mediaType.equals(MediaType.ALL) || mediaType.equals(MEDIA_TYPE_APPLICATION)) {
				selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;
				break;
			}
		}

		if (selectedMediaType != null) {
			selectedMediaType = selectedMediaType.removeQualityValue();
			for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
				if (messageConverter instanceof GenericHttpMessageConverter) {
					if (((GenericHttpMessageConverter<T>) messageConverter).canWrite(returnValueType,
							returnValueClass, selectedMediaType)) {
						returnValue = (T) getAdvice().beforeBodyWrite(returnValue, returnType, selectedMediaType,
								(Class<? extends HttpMessageConverter<?>>) messageConverter.getClass(),
								inputMessage, outputMessage);
						if (returnValue != null) {
							addContentDispositionHeader(inputMessage, outputMessage);
							((GenericHttpMessageConverter<T>) messageConverter).write(returnValue,
									returnValueType, selectedMediaType, outputMessage);
							if (logger.isDebugEnabled()) {
								logger.debug("Written [" + returnValue + "] as \"" +
										selectedMediaType + "\" using [" + messageConverter + "]");
							}
						}
						return;
					}
				}
				else if (messageConverter.canWrite(returnValueClass, selectedMediaType)) {
					returnValue = (T) getAdvice().beforeBodyWrite(returnValue, returnType, selectedMediaType,
							(Class<? extends HttpMessageConverter<?>>) messageConverter.getClass(),
							inputMessage, outputMessage);
					if (returnValue != null) {
						addContentDispositionHeader(inputMessage, outputMessage);
						((HttpMessageConverter<T>) messageConverter).write(returnValue,
								selectedMediaType, outputMessage);
						if (logger.isDebugEnabled()) {
							logger.debug("Written [" + returnValue + "] as \"" +
									selectedMediaType + "\" using [" + messageConverter + "]");
						}
					}
					return;
				}
			}
		}

		if (returnValue != null) {
			throw new HttpMediaTypeNotAcceptableException(this.allSupportedMediaTypes);
		}
	}

	/**
	 * Return the type of the value to be written to the response. Typically this
	 * is a simple check via getClass on the returnValue but if the returnValue is
	 * null, then the returnType needs to be examined possibly including generic
	 * type determination (e.g. {@code ResponseEntity<T>}).
	 */
	protected Class<?> getReturnValueType(Object returnValue, MethodParameter returnType) {
		return (returnValue != null ? returnValue.getClass() : returnType.getParameterType());
	}

	/**
	 * Return the generic type of the {@code returnType} (or of the nested type if it is
	 * a {@link HttpEntity}).
	 */
	private Type getGenericType(MethodParameter returnType) {
		Type type;
		if (HttpEntity.class.isAssignableFrom(returnType.getParameterType())) {
			returnType.increaseNestingLevel();
			type = returnType.getNestedGenericParameterType();
		}
		else {
			type = returnType.getGenericParameterType();
		}
		return type;
	}

	/**
	 * @see #getProducibleMediaTypes(HttpServletRequest, Class, Type)
	 */
	@SuppressWarnings({"unchecked", "unused"})
	protected List<MediaType> getProducibleMediaTypes(HttpServletRequest request, Class<?> returnValueClass) {
		return getProducibleMediaTypes(request, returnValueClass, null);
	}

	/**
	 * Returns the media types that can be produced:
	 * <ul>
	 * <li>The producible media types specified in the request mappings, or
	 * <li>Media types of configured converters that can write the specific return value, or
	 * <li>{@link MediaType#ALL}
	 * </ul>
	 * @since 4.2
	 */
	@SuppressWarnings("unchecked")
	protected List<MediaType> getProducibleMediaTypes(HttpServletRequest request, Class<?> returnValueClass, Type returnValueType) {
		Set<MediaType> mediaTypes = (Set<MediaType>) request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			return new ArrayList<MediaType>(mediaTypes);
		}
		else if (!this.allSupportedMediaTypes.isEmpty()) {
			List<MediaType> result = new ArrayList<MediaType>();
			for (HttpMessageConverter<?> converter : this.messageConverters) {
				if (converter instanceof GenericHttpMessageConverter && returnValueType != null) {
					if (((GenericHttpMessageConverter<?>) converter).canWrite(returnValueType, returnValueClass, null)) {
						result.addAll(converter.getSupportedMediaTypes());
					}
				}
				else if (converter.canWrite(returnValueClass, null)) {
					result.addAll(converter.getSupportedMediaTypes());
				}
			}
			return result;
		}
		else {
			return Collections.singletonList(MediaType.ALL);
		}
	}

	private List<MediaType> getAcceptableMediaTypes(HttpServletRequest request) throws HttpMediaTypeNotAcceptableException {
		List<MediaType> mediaTypes = this.contentNegotiationManager.resolveMediaTypes(new ServletWebRequest(request));
		return (mediaTypes.isEmpty() ? Collections.singletonList(MediaType.ALL) : mediaTypes);
	}

	/**
	 * Return the more specific of the acceptable and the producible media types
	 * with the q-value of the former.
	 */
	private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
		MediaType produceTypeToUse = produceType.copyQualityValue(acceptType);
		return (MediaType.SPECIFICITY_COMPARATOR.compare(acceptType, produceTypeToUse) <= 0 ? acceptType : produceTypeToUse);
	}

	/**
	 * Check if the path has a file extension and whether the extension is
	 * either {@link #WHITELISTED_EXTENSIONS whitelisted} or explicitly
	 * {@link ContentNegotiationManager#getAllFileExtensions() registered}.
	 * If not, and the status is in the 2xx range, a 'Content-Disposition'
	 * header with a safe attachment file name ("f.txt") is added to prevent
	 * RFD exploits.
	 */
	private void addContentDispositionHeader(ServletServerHttpRequest request,
			ServletServerHttpResponse response) {

		HttpHeaders headers = response.getHeaders();
		if (headers.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
			return;
		}

		try {
			int status = response.getServletResponse().getStatus();
			if (status < 200 || status > 299) {
				return;
			}
		}
		catch (Throwable ex) {
			// Ignore
		}

		HttpServletRequest servletRequest = request.getServletRequest();
		String requestUri = RAW_URL_PATH_HELPER.getOriginatingRequestUri(servletRequest);

		int index = requestUri.lastIndexOf('/') + 1;
		String filename = requestUri.substring(index);
		String pathParams = "";

		index = filename.indexOf(';');
		if (index != -1) {
			pathParams = filename.substring(index);
			filename = filename.substring(0, index);
		}

		filename = DECODING_URL_PATH_HELPER.decodeRequestString(servletRequest, filename);
		String ext = StringUtils.getFilenameExtension(filename);

		pathParams = DECODING_URL_PATH_HELPER.decodeRequestString(servletRequest, pathParams);
		String extInPathParams = StringUtils.getFilenameExtension(pathParams);

		if (!safeExtension(servletRequest, ext) || !safeExtension(servletRequest, extInPathParams)) {
			headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=f.txt");
		}
	}

	@SuppressWarnings("unchecked")
	private boolean safeExtension(HttpServletRequest request, String extension) {
		if (!StringUtils.hasText(extension)) {
			return true;
		}
		extension = extension.toLowerCase(Locale.ENGLISH);
		if (this.safeExtensions.contains(extension)) {
			return true;
		}
		String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		if (pattern != null && pattern.endsWith("." + extension)) {
			return true;
		}
		if (extension.equals("html")) {
			String name = HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;
			Set<MediaType> mediaTypes = (Set<MediaType>) request.getAttribute(name);
			if (!CollectionUtils.isEmpty(mediaTypes) && mediaTypes.contains(MediaType.TEXT_HTML)) {
				return true;
			}
		}
		return safeMediaTypesForExtension(extension);
	}

	private boolean safeMediaTypesForExtension(String extension) {
		List<MediaType> mediaTypes = null;
		try {
			mediaTypes = this.pathStrategy.resolveMediaTypeKey(null, extension);
		}
		catch (HttpMediaTypeNotAcceptableException e) {
			// Ignore
		}
		if (CollectionUtils.isEmpty(mediaTypes)) {
			return false;
		}
		for (MediaType mediaType : mediaTypes) {
			if (!safeMediaType(mediaType)) {
				return false;
			}
		}
		return true;
	}

	private boolean safeMediaType(MediaType mediaType) {
		return (WHITELISTED_MEDIA_BASE_TYPES.contains(mediaType.getType()) ||
				mediaType.getSubtype().endsWith("+xml"));
	}

}
