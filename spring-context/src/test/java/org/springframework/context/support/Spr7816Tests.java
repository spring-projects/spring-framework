/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.support;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class Spr7816Tests {

	@Test
	public void spr7816() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("spr7816.xml", getClass());
		FilterAdapter adapter = ctx.getBean(FilterAdapter.class);
		assertThat(adapter.getSupportedTypes().get("Building")).isEqualTo(Building.class);
		assertThat(adapter.getSupportedTypes().get("Entrance")).isEqualTo(Entrance.class);
		assertThat(adapter.getSupportedTypes().get("Dwelling")).isEqualTo(Dwelling.class);
	}

	public static class FilterAdapter {

		private String extensionPrefix;

		private Map<String, Class<? extends DomainEntity>> supportedTypes;

		public FilterAdapter(final String extensionPrefix, final Map<String, Class<? extends DomainEntity>> supportedTypes) {
			this.extensionPrefix = extensionPrefix;
			this.supportedTypes = supportedTypes;
		}

		public String getExtensionPrefix() {
			return extensionPrefix;
		}

		public Map<String, Class<? extends DomainEntity>> getSupportedTypes() {
			return supportedTypes;
		}

	}

	public static class Building extends DomainEntity {
	}

	public static class Entrance extends DomainEntity {
	}

	public static class Dwelling extends DomainEntity {
	}

	public abstract static class DomainEntity {

	}
}
