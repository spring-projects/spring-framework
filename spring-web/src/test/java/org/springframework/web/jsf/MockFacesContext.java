/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.web.jsf;

import java.util.Iterator;

import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseStream;
import javax.faces.context.ResponseWriter;
import javax.faces.render.RenderKit;

/**
 * Mock implementation of the {@code FacesContext} class to facilitate
 * standalone Action unit tests.
 *
 * @author Ulrik Sandberg
 * @see javax.faces.context.FacesContext
 */
public class MockFacesContext extends FacesContext {

	private ExternalContext externalContext;

	private Application application;

	private UIViewRoot viewRoot;


	@Override
	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}

	@Override
	public Iterator getClientIdsWithMessages() {
		return null;
	}

	@Override
	public ExternalContext getExternalContext() {
		return externalContext;
	}

	public void setExternalContext(ExternalContext externalContext) {
		this.externalContext = externalContext;
	}

	@Override
	public Severity getMaximumSeverity() {
		return null;
	}

	@Override
	public Iterator getMessages() {
		return null;
	}

	@Override
	public Iterator getMessages(String arg0) {
		return null;
	}

	@Override
	public RenderKit getRenderKit() {
		return null;
	}

	@Override
	public boolean getRenderResponse() {
		return false;
	}

	@Override
	public boolean getResponseComplete() {
		return false;
	}

	@Override
	public ResponseStream getResponseStream() {
		return null;
	}

	@Override
	public void setResponseStream(ResponseStream arg0) {
	}

	@Override
	public ResponseWriter getResponseWriter() {
		return null;
	}

	@Override
	public void setResponseWriter(ResponseWriter arg0) {
	}

	@Override
	public UIViewRoot getViewRoot() {
		return viewRoot;
	}

	@Override
	public void setViewRoot(UIViewRoot viewRoot) {
		this.viewRoot = viewRoot;
	}

	@Override
	public void addMessage(String arg0, FacesMessage arg1) {
	}

	@Override
	public void release() {
	}

	@Override
	public void renderResponse() {
	}

	@Override
	public void responseComplete() {
	}

}