/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.support;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

/**
 * A base {@link HeaderMapper} implementation.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @param <T> type of the instance to and from which headers will be mapped
 */
public abstract class AbstractHeaderMapper<T> implements HeaderMapper<T> {

	protected final Log logger = LogFactory.getLog(getClass());

	private String inboundPrefix = "";

	private String outboundPrefix = "";


	/**
	 * Specify a prefix to be appended to the message header name for any
	 * user-defined property that is being mapped into the MessageHeaders.
	 * The default is an empty String (no prefix).
	 */
	public void setInboundPrefix(@Nullable String inboundPrefix) {
		this.inboundPrefix = (inboundPrefix != null ? inboundPrefix : "");
	}

	/**
	 * Specify a prefix to be appended to the protocol property name for any
	 * user-defined message header that is being mapped into the protocol-specific
	 * Message. The default is an empty String (no prefix).
	 */
	public void setOutboundPrefix(@Nullable String outboundPrefix) {
		this.outboundPrefix = (outboundPrefix != null ? outboundPrefix : "");
	}


	/**
	 * Generate the name to use to set the header defined by the specified
	 * {@code headerName} to the protocol specific message.
	 * @see #setOutboundPrefix
	 */
	protected String fromHeaderName(String headerName) {
		String propertyName = headerName;
		if (StringUtils.hasText(this.outboundPrefix) && !propertyName.startsWith(this.outboundPrefix)) {
			propertyName = this.outboundPrefix + headerName;
		}
		return propertyName;
	}

	/**
	 * Generate the name to use to set the header defined by the specified
	 * {@code propertyName} to the {@link MessageHeaders} instance.
	 * @see #setInboundPrefix(String)
	 */
	protected String toHeaderName(String propertyName) {
		String headerName = propertyName;
		if (StringUtils.hasText(this.inboundPrefix) && !headerName.startsWith(this.inboundPrefix)) {
			headerName = this.inboundPrefix + propertyName;
		}
		return headerName;
	}

	/**
	 * Return the header value, or {@code null} if it does not exist
	 * or does not match the requested {@code type}.
	 */
	protected <V> @Nullable V getHeaderIfAvailable(Map<String, Object> headers, String name, Class<V> type) {
		Object value = headers.get(name);
		if (value == null) {
			return null;
		}
		if (!type.isAssignableFrom(value.getClass())) {
			if (logger.isDebugEnabled()) {
				logger.debug("Skipping header '" + name + "': expected type [" + type + "], but got [" +
						value.getClass() + "]");
			}
			return null;
		}
		else {
			return type.cast(value);
		}
	}

}
