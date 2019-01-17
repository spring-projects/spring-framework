/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.support.JspAwareRequestContext;
import org.springframework.web.servlet.support.RequestContext;

/**
 * Superclass for all tags that require a {@link RequestContext}.
 *
 * <p>The {@code RequestContext} instance provides easy access
 * to current state like the
 * {@link org.springframework.web.context.WebApplicationContext},
 * the {@link java.util.Locale}, the
 * {@link org.springframework.ui.context.Theme}, etc.
 *
 * <p>Mainly intended for
 * {@link org.springframework.web.servlet.DispatcherServlet} requests;
 * will use fallbacks when used outside {@code DispatcherServlet}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.servlet.support.RequestContext
 * @see org.springframework.web.servlet.DispatcherServlet
 */
@SuppressWarnings("serial")
public abstract class RequestContextAwareTag extends TagSupport implements TryCatchFinally {

	/**
	 * {@link javax.servlet.jsp.PageContext} attribute for the
	 * page-level {@link RequestContext} instance.
	 */
	public static final String REQUEST_CONTEXT_PAGE_ATTRIBUTE =
			"org.springframework.web.servlet.tags.REQUEST_CONTEXT";


	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());


	@Nullable
	private RequestContext requestContext;


	/**
	 * Create and expose the current RequestContext.
	 * Delegates to {@link #doStartTagInternal()} for actual work.
	 * @see #REQUEST_CONTEXT_PAGE_ATTRIBUTE
	 * @see org.springframework.web.servlet.support.JspAwareRequestContext
	 */
	@Override
	public final int doStartTag() throws JspException {
		try {
			this.requestContext = (RequestContext) this.pageContext.getAttribute(REQUEST_CONTEXT_PAGE_ATTRIBUTE);
			if (this.requestContext == null) {
				this.requestContext = new JspAwareRequestContext(this.pageContext);
				this.pageContext.setAttribute(REQUEST_CONTEXT_PAGE_ATTRIBUTE, this.requestContext);
			}
			return doStartTagInternal();
		}
		catch (JspException | RuntimeException ex) {
			logger.error(ex.getMessage(), ex);
			throw ex;
		}
		catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			throw new JspTagException(ex.getMessage());
		}
	}

	/**
	 * Return the current RequestContext.
	 */
	protected final RequestContext getRequestContext() {
		Assert.state(this.requestContext != null, "No current RequestContext");
		return this.requestContext;
	}

	/**
	 * Called by doStartTag to perform the actual work.
	 * @return same as TagSupport.doStartTag
	 * @throws Exception any exception, any checked one other than
	 * a JspException gets wrapped in a JspException by doStartTag
	 * @see javax.servlet.jsp.tagext.TagSupport#doStartTag
	 */
	protected abstract int doStartTagInternal() throws Exception;


	@Override
	public void doCatch(Throwable throwable) throws Throwable {
		throw throwable;
	}

	@Override
	public void doFinally() {
		this.requestContext = null;
	}

}
