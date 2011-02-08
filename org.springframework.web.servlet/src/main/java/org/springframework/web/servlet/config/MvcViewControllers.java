/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.web.servlet.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.parsing.SimpleProblemCollector;
import org.springframework.context.config.AbstractFeatureSpecification;
import org.springframework.context.config.FeatureSpecificationExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;

/**
 * Specifies the Spring MVC "View Controllers" container feature. The
 * feature allows specifying one or more path to view name mappings.
 * It sets up the following fine-grained configuration:
 *
 * <ul>
 * <li>{@link ParameterizableViewController} for each path/view name pair.
 * <li>{@link SimpleUrlHandlerMapping} mapping each view controller to its path.
 * <li>{@link SimpleControllerHandlerAdapter} to enable the DispatcherServlet
 *     to invoke the view controllers.
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author Chris Beams
 * @since 3.1
 */
public final class MvcViewControllers extends AbstractFeatureSpecification {

	private static final Class<? extends FeatureSpecificationExecutor> EXECUTOR_TYPE = MvcViewControllersExecutor.class;

	private Map<String, String> mappings = new HashMap<String, String>();

	public MvcViewControllers(String path) {
		this(path, null);
	}

	public MvcViewControllers(String path, String viewName) {
		super(EXECUTOR_TYPE);
		this.mappings.put(path, viewName);
	}

	public MvcViewControllers viewController(String path) {
		return this.viewController(path, null);
	}

	public MvcViewControllers viewController(String path, String viewName) {
		this.mappings.put(path, viewName);
		return this;
	}

	Map<String, String> mappings() {
		return Collections.unmodifiableMap(mappings);
	}

	@Override
	protected void doValidate(SimpleProblemCollector problems) {
		if (mappings.size() == 0) {
			problems.error("At least one ViewController must be defined");
		}
		for (String path : mappings.keySet()) {
			if (!StringUtils.hasText(path)) {
				problems.error("The path attribute in a ViewController is required");
			}
			String viewName = mappings.get(path);
			if (viewName != null && viewName.isEmpty()) {
				problems.error("The view name in a ViewController may be null but not empty.");
			}
		}
	}

}
