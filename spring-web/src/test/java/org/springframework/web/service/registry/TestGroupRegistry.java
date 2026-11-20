/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.registry;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.ClassUtils;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar.GroupRegistry;

/**
 * A {@link GroupRegistry} that records the inputs given and creates {@link TestGroup}s
 *
 * @author Rossen Stoyanchev
 */
class TestGroupRegistry implements GroupRegistry {

	private final Map<String, TestGroup> groupMap = new LinkedHashMap<>();

	public Map<String, TestGroup> groupMap() {
		return this.groupMap;
	}

	@Override
	public GroupSpec forGroup(String name, HttpServiceGroup.ClientType clientType) {
		return new TestGroupSpec(this.groupMap, name, clientType);
	}


	private record TestGroupSpec(
			Map<String, TestGroup> groupMap, String groupName,
			HttpServiceGroup.ClientType clientType) implements GroupSpec {

		@Override
		public GroupSpec register(Class<?>... serviceTypes) {
			getOrCreateGroup().httpServiceTypes().addAll(Arrays.asList(serviceTypes));
			return this;
		}

		@Override
		public GroupSpec registerTypeNames(String... serviceTypes) {
			return register(Arrays.stream(serviceTypes)
					.map(className -> ClassUtils.resolveClassName(className, getClass().getClassLoader()))
					.toArray(Class[]::new));
		}

		@Override
		public GroupSpec detectInBasePackages(Class<?>... packageClasses) {
			getOrCreateGroup().packageClasses().addAll(Arrays.asList(packageClasses));
			return this;
		}

		@Override
		public GroupSpec detectInBasePackages(String... packageNames) {
			getOrCreateGroup().packageNames().addAll(Arrays.asList(packageNames));
			return this;
		}

		private TestGroup getOrCreateGroup() {
			return this.groupMap.computeIfAbsent(this.groupName, name -> new TestGroup(name, this.clientType));
		}
	}

}
