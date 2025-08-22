/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.service.registry;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A stub implementation of {@link HttpServiceGroup}.
 *
 * @author Rossen Stoyanchev
 */
record TestGroup(
		String name, ClientType clientType, Set<Class<?>> httpServiceTypes,
		Set<Class<?>> packageClasses, Set<String> packageNames) implements HttpServiceGroup {

	TestGroup(String name, ClientType clientType) {
		this(name, clientType, new LinkedHashSet<>(), new LinkedHashSet<>(), new LinkedHashSet<>());
	}

	public static TestGroup ofListing(String name, Class<?>... httpServiceTypes) {
		return ofListing(name, ClientType.UNSPECIFIED, httpServiceTypes);
	}

	public static TestGroup ofListing(String name, ClientType clientType, Class<?>... httpServiceTypes) {
		TestGroup group = new TestGroup(name, clientType);
		group.httpServiceTypes().addAll(Arrays.asList(httpServiceTypes));
		return group;
	}

	public static TestGroup ofPackageClasses(String name, Class<?>... packageClasses) {
		return ofPackageClasses(name, ClientType.UNSPECIFIED, packageClasses);
	}

	public static TestGroup ofPackageClasses(String name, ClientType clientType, Class<?>... packageClasses) {
		TestGroup group = new TestGroup(name, clientType);
		group.packageClasses().addAll(Arrays.asList(packageClasses));
		return group;
	}

}
