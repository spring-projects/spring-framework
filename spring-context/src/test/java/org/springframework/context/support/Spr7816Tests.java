/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.support;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
class Spr7816Tests {

	@Test
	void spr7816() {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext("spr7816.xml", getClass());
		FilterAdapter adapter = ctx.getBean(FilterAdapter.class);
		assertThat(adapter.getSupportedTypes().get("Building")).isEqualTo(Building.class);
		assertThat(adapter.getSupportedTypes().get("Entrance")).isEqualTo(Entrance.class);
		assertThat(adapter.getSupportedTypes().get("Dwelling")).isEqualTo(Dwelling.class);
		ctx.close();
	}

	static class FilterAdapter {

		private String extensionPrefix;

		private Map<String, Class<? extends DomainEntity>> supportedTypes;

		FilterAdapter(final String extensionPrefix, final Map<String, Class<? extends DomainEntity>> supportedTypes) {
			this.extensionPrefix = extensionPrefix;
			this.supportedTypes = supportedTypes;
		}

		String getExtensionPrefix() {
			return extensionPrefix;
		}

		Map<String, Class<? extends DomainEntity>> getSupportedTypes() {
			return supportedTypes;
		}

	}

	abstract static sealed class DomainEntity permits Building, Entrance, Dwelling {
	}

	static final class Building extends DomainEntity {
	}

	static final class Entrance extends DomainEntity {
	}

	static final class Dwelling extends DomainEntity {
	}

}
