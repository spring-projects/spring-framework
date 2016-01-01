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

package org.springframework.web.servlet.view.velocity;

import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;

/**
 * @author Juergen Hoeller
 * @since 09.10.2004
 */
class TestVelocityEngine extends VelocityEngine {

	private final Map<String, Template> templates = new HashMap<>();


	public TestVelocityEngine() {
	}

	public TestVelocityEngine(String expectedName, Template template) {
		addTemplate(expectedName, template);
	}

	public void addTemplate(String expectedName, Template template) {
		this.templates.put(expectedName, template);
	}


	@Override
	public Template getTemplate(String name) throws ResourceNotFoundException {
		Template template = this.templates.get(name);
		if (template == null) {
			throw new ResourceNotFoundException("No template registered for name [" + name + "]");
		}
		return template;
	}

}
