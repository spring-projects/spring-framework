/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
