/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.jsf;

import java.util.Iterator;

import jakarta.faces.application.Application;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.application.FacesMessage.Severity;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseStream;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.render.RenderKit;

/**
 * Mock implementation of the {@code FacesContext} class to facilitate
 * standalone Action unit tests.
 *
 * @author Ulrik Sandberg
 * @see jakarta.faces.context.FacesContext
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
	public Iterator<String> getClientIdsWithMessages() {
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
	public Iterator<FacesMessage> getMessages() {
		return null;
	}

	@Override
	public Iterator<FacesMessage> getMessages(String clientId) {
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
