/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.model.ui;

/**
 * A factory for domain object PresentationModels.
 * Makes it easy for clients to lookup PresentationModels for domain objects they need to bind to.
 * @author Keith Donald
 * @since 3.0
 */
public interface PresentationModelFactory {
	
	/**
	 * Get the PresentationModel for the domain object.
	 * If none exists, one is created and cached.
	 * Never returns <code>null</code>.
	 * @param domainObject the model object
	 * @return the presentation model
	 */
	public PresentationModel getPresentationModel(Object domainObject);
}