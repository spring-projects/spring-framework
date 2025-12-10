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
