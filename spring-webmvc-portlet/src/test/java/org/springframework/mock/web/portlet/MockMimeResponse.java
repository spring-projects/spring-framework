/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.mock.web.portlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import javax.portlet.CacheControl;
import javax.portlet.MimeResponse;
import javax.portlet.PortalContext;
import javax.portlet.PortletRequest;
import javax.portlet.PortletURL;
import javax.portlet.ResourceURL;

import org.springframework.util.CollectionUtils;
import org.springframework.web.util.WebUtils;

/**
 * Mock implementation of the {@link javax.portlet.MimeResponse} interface.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class MockMimeResponse extends MockPortletResponse implements MimeResponse {

	private PortletRequest request;

	private String contentType;

	private String characterEncoding = WebUtils.DEFAULT_CHARACTER_ENCODING;

	private PrintWriter writer;

	private Locale locale = Locale.getDefault();

	private int bufferSize = 4096;

	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

	private final CacheControl cacheControl = new MockCacheControl();

	private boolean committed;

	private String includedUrl;

	private String forwardedUrl;


	/**
	 * Create a new MockMimeResponse with a default {@link MockPortalContext}.
	 * @see org.springframework.mock.web.portlet.MockPortalContext
	 */
	public MockMimeResponse() {
		super();
	}

	/**
	 * Create a new MockMimeResponse.
	 * @param portalContext the PortalContext defining the supported
	 * PortletModes and WindowStates
	 */
	public MockMimeResponse(PortalContext portalContext) {
		super(portalContext);
	}

	/**
	 * Create a new MockMimeResponse.
	 * @param portalContext the PortalContext defining the supported
	 * PortletModes and WindowStates
	 * @param request the corresponding render/resource request that this response
	 * is being generated for
	 */
	public MockMimeResponse(PortalContext portalContext, PortletRequest request) {
		super(portalContext);
		this.request = request;
	}


	//---------------------------------------------------------------------
	// RenderResponse methods
	//---------------------------------------------------------------------

	@Override
	public void setContentType(String contentType) {
		if (this.request != null) {
			Enumeration<String> supportedTypes = this.request.getResponseContentTypes();
			if (!CollectionUtils.contains(supportedTypes, contentType)) {
				throw new IllegalArgumentException("Content type [" + contentType + "] not in supported list: " +
						Collections.list(supportedTypes));
			}
		}
		this.contentType = contentType;
	}

	@Override
	public String getContentType() {
		return this.contentType;
	}

	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}

	@Override
	public String getCharacterEncoding() {
		return this.characterEncoding;
	}

	@Override
	public PrintWriter getWriter() throws UnsupportedEncodingException {
		if (this.writer == null) {
			Writer targetWriter = (this.characterEncoding != null
					? new OutputStreamWriter(this.outputStream, this.characterEncoding)
					: new OutputStreamWriter(this.outputStream));
			this.writer = new PrintWriter(targetWriter);
		}
		return this.writer;
	}

	public byte[] getContentAsByteArray() {
		flushBuffer();
		return this.outputStream.toByteArray();
	}

	public String getContentAsString() throws UnsupportedEncodingException {
		flushBuffer();
		return (this.characterEncoding != null)
				? this.outputStream.toString(this.characterEncoding)
				: this.outputStream.toString();
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	@Override
	public Locale getLocale() {
		return this.locale;
	}

	@Override
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	@Override
	public int getBufferSize() {
		return this.bufferSize;
	}

	@Override
	public void flushBuffer() {
		if (this.writer != null) {
			this.writer.flush();
		}
		if (this.outputStream != null) {
			try {
				this.outputStream.flush();
			}
			catch (IOException ex) {
				throw new IllegalStateException("Could not flush OutputStream: " + ex.getMessage());
			}
		}
		this.committed = true;
	}

	@Override
	public void resetBuffer() {
		if (this.committed) {
			throw new IllegalStateException("Cannot reset buffer - response is already committed");
		}
		this.outputStream.reset();
	}

	public void setCommitted(boolean committed) {
		this.committed = committed;
	}

	@Override
	public boolean isCommitted() {
		return this.committed;
	}

	@Override
	public void reset() {
		resetBuffer();
		this.characterEncoding = null;
		this.contentType = null;
		this.locale = null;
	}

	@Override
	public OutputStream getPortletOutputStream() throws IOException {
		return this.outputStream;
	}

	@Override
	public PortletURL createRenderURL() {
		return new MockPortletURL(getPortalContext(), MockPortletURL.URL_TYPE_RENDER);
	}

	@Override
	public PortletURL createActionURL() {
		return new MockPortletURL(getPortalContext(), MockPortletURL.URL_TYPE_ACTION);
	}

	@Override
	public ResourceURL createResourceURL() {
		return new MockResourceURL();
	}

	@Override
	public CacheControl getCacheControl() {
		return this.cacheControl;
	}


	//---------------------------------------------------------------------
	// Methods for MockPortletRequestDispatcher
	//---------------------------------------------------------------------

	public void setIncludedUrl(String includedUrl) {
		this.includedUrl = includedUrl;
	}

	public String getIncludedUrl() {
		return this.includedUrl;
	}

	public void setForwardedUrl(String forwardedUrl) {
		this.forwardedUrl = forwardedUrl;
	}

	public String getForwardedUrl() {
		return this.forwardedUrl;
	}

}
