/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.filter;

import java.io.IOException;
import java.nio.charset.Charset;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Servlet Filter that allows one to specify a character encoding for requests.
 * This is useful because current browsers typically do not set a character
 * encoding even if specified in the HTML page or form.
 *
 * <p>This filter can either apply its encoding if the request does not already
 * specify an encoding, or enforce this filter's encoding in any case
 * ("forceEncoding"="true"). In the latter case, the encoding will also be
 * applied as default response encoding (although this will usually be overridden
 * by a full content type set in the view).
 *
 * @author Juergen Hoeller
 * @since 15.03.2004
 * @see #setEncoding
 * @see #setForceEncoding
 * @see jakarta.servlet.http.HttpServletRequest#setCharacterEncoding(Charset)
 * @see jakarta.servlet.http.HttpServletResponse#setCharacterEncoding(Charset)
 */
public class CharacterEncodingFilter extends OncePerRequestFilter {

	private @Nullable Charset charset;

	private boolean forceRequestEncoding = false;

	private boolean forceResponseEncoding = false;


	/**
	 * Create a default {@code CharacterEncodingFilter},
	 * with the encoding to be set via {@link #setEncoding}.
	 * @see #setEncoding
	 */
	public CharacterEncodingFilter() {
	}

	/**
	 * Create a {@code CharacterEncodingFilter} for the given charset.
	 * @param charset the charset to apply as an encoding
	 * @since 7.1.1
	 * @see #setCharset
	 * @see #setForceEncoding
	 */
	public CharacterEncodingFilter(Charset charset) {
		this.charset = charset;
	}

	/**
	 * Create a {@code CharacterEncodingFilter} for the given encoding.
	 * @param encoding the encoding to apply
	 * @since 4.2.3
	 * @see #setEncoding
	 */
	public CharacterEncodingFilter(String encoding) {
		this(encoding, false);
	}

	/**
	 * Create a {@code CharacterEncodingFilter} for the given encoding.
	 * @param encoding the encoding to apply
	 * @param forceEncoding whether the specified encoding is supposed to
	 * override existing request and response encodings
	 * @since 4.2.3
	 * @see #setEncoding
	 * @see #setForceEncoding
	 */
	public CharacterEncodingFilter(String encoding, boolean forceEncoding) {
		this(encoding, forceEncoding, forceEncoding);
	}

	/**
	 * Create a {@code CharacterEncodingFilter} for the given encoding.
	 * @param encoding the encoding to apply
	 * @param forceRequestEncoding whether the specified encoding is supposed to
	 * override existing request encodings
	 * @param forceResponseEncoding whether the specified encoding is supposed to
	 * override existing response encodings
	 * @since 4.3
	 * @see #setEncoding
	 * @see #setForceRequestEncoding
	 * @see #setForceResponseEncoding
	 */
	public CharacterEncodingFilter(String encoding, boolean forceRequestEncoding, boolean forceResponseEncoding) {
		Assert.hasLength(encoding, "Encoding must not be empty");
		this.charset = Charset.forName(encoding);
		this.forceRequestEncoding = forceRequestEncoding;
		this.forceResponseEncoding = forceResponseEncoding;
	}


	/**
	 * Set the charset to apply as an encoding. This charset will be passed into
	 * {@link jakarta.servlet.http.HttpServletRequest#setCharacterEncoding(Charset)}.
	 * <p>Whether this encoding will override existing request encodings
	 * (and whether it will be applied as default response encoding as well)
	 * depends on the {@link #setForceEncoding "forceEncoding"} flag.
	 * @since 7.1.1
	 * @see #setEncoding
	 */
	public void setCharset(@Nullable Charset charset) {
		this.charset = charset;
	}

	/**
	 * Set the encoding to use for requests. As of 7.1.1, this encoding will be
	 * pre-resolved into a {@link Charset} instance and then passed into
	 * {@link jakarta.servlet.http.HttpServletRequest#setCharacterEncoding(Charset)}.
	 * <p>Whether this encoding will override existing request encodings
	 * (and whether it will be applied as default response encoding as well)
	 * depends on the {@link #setForceEncoding "forceEncoding"} flag.
	 * @see #setCharset
	 */
	public void setEncoding(@Nullable String encoding) {
		this.charset = (encoding != null ? Charset.forName(encoding) : null);
	}

	/**
	 * Return the configured encoding for requests and/or responses.
	 * @since 4.3
	 */
	public @Nullable String getEncoding() {
		return (this.charset != null ? this.charset.name() : null);
	}

	/**
	 * Set whether the configured {@link #setEncoding encoding} of this filter
	 * is supposed to override existing request and response encodings.
	 * <p>Default is "false", i.e. do not modify the encoding if
	 * {@link jakarta.servlet.http.HttpServletRequest#getCharacterEncoding()}
	 * returns a non-null value. Switch this to "true" to enforce the specified
	 * encoding in any case, applying it as default response encoding as well.
	 * <p>This is the equivalent to setting both {@link #setForceRequestEncoding(boolean)}
	 * and {@link #setForceResponseEncoding(boolean)}.
	 * @see #setForceRequestEncoding
	 * @see #setForceResponseEncoding
	 */
	public void setForceEncoding(boolean forceEncoding) {
		this.forceRequestEncoding = forceEncoding;
		this.forceResponseEncoding = forceEncoding;
	}

	/**
	 * Set whether the configured {@link #setEncoding encoding} of this filter
	 * is supposed to override existing request encodings.
	 * <p>Default is "false", i.e. do not modify the encoding if
	 * {@link jakarta.servlet.http.HttpServletRequest#getCharacterEncoding()}
	 * returns a non-null value. Switch this to "true" to enforce the specified
	 * encoding in any case.
	 * @since 4.3
	 */
	public void setForceRequestEncoding(boolean forceRequestEncoding) {
		this.forceRequestEncoding = forceRequestEncoding;
	}

	/**
	 * Return whether the encoding should be forced on requests.
	 * @since 4.3
	 */
	public boolean isForceRequestEncoding() {
		return this.forceRequestEncoding;
	}

	/**
	 * Set whether the configured {@link #setEncoding encoding} of this filter
	 * is supposed to override existing response encodings.
	 * <p>Default is "false", i.e. do not modify the encoding.
	 * Switch this to "true" to enforce the specified encoding
	 * for responses in any case.
	 * @since 4.3
	 */
	public void setForceResponseEncoding(boolean forceResponseEncoding) {
		this.forceResponseEncoding = forceResponseEncoding;
	}

	/**
	 * Return whether the encoding should be forced on responses.
	 * @since 4.3
	 */
	public boolean isForceResponseEncoding() {
		return this.forceResponseEncoding;
	}


	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		Charset charset = this.charset;
		if (charset != null) {
			if (isForceRequestEncoding() || request.getCharacterEncoding() == null) {
				request.setCharacterEncoding(charset);
			}
			if (isForceResponseEncoding()) {
				response.setCharacterEncoding(charset);
			}
		}
		filterChain.doFilter(request, response);
	}

}
