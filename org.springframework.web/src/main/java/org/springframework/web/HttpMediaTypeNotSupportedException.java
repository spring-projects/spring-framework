package org.springframework.web;

import java.util.List;
import javax.servlet.ServletException;

import org.springframework.http.MediaType;

/**
 * Exception thrown when a client POSTs or PUTs content 
 * not supported by request handler does not support a
 * specific request method.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class HttpMediaTypeNotSupportedException extends ServletException {

	private MediaType contentType;

	private List<MediaType> supportedMediaTypes;

	/**
	 * Create a new HttpMediaTypeNotSupportedException.
	 * @param contentType the unsupported content type
	 * @param supportedMediaTypes the list of supported media types
	 */
	public HttpMediaTypeNotSupportedException(MediaType contentType, List<MediaType> supportedMediaTypes) {
		this(contentType, supportedMediaTypes, "Content type '" + contentType + "' not supported");
	}

	/**
	 * Create a new HttpMediaTypeNotSupportedException.
	 * @param contentType the unsupported content type
	 * @param supportedMediaTypes the list of supported media types
	 * @param msg the detail message
	 */
	public HttpMediaTypeNotSupportedException(MediaType contentType, List<MediaType> supportedMediaTypes, String msg) {
		super(msg);
		this.contentType = contentType;
		this.supportedMediaTypes = supportedMediaTypes;
	}

	/**
	 * Return the HTTP request content type method that caused the failure.
	 */
	public MediaType getContentType() {
		return contentType;
	}

	/**
	 * Return the list of supported media types.
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return supportedMediaTypes;
	}
}
