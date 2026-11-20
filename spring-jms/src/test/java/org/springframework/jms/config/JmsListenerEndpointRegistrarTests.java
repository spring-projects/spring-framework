/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Stephane Nicoll
 */
class JmsListenerEndpointRegistrarTests {

	private final JmsListenerEndpointRegistrar registrar = new JmsListenerEndpointRegistrar();

	private final JmsListenerEndpointRegistry registry = new JmsListenerEndpointRegistry();

	private final JmsListenerContainerTestFactory containerFactory = new JmsListenerContainerTestFactory();


	@BeforeEach
	void setup() {
		this.registrar.setEndpointRegistry(this.registry);
		this.registrar.setBeanFactory(new StaticListableBeanFactory());
	}


	@Test
	void registerNullEndpoint() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.registrar.registerEndpoint(null, this.containerFactory));
	}

	@Test
	void registerNullEndpointId() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.registrar.registerEndpoint(new SimpleJmsListenerEndpoint(), this.containerFactory));
	}

	@Test
	void registerEmptyEndpointId() {
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setId("");

		assertThatIllegalArgumentException().isThrownBy(() ->
				this.registrar.registerEndpoint(endpoint, this.containerFactory));
	}

	@Test
	void registerNullContainerFactoryIsAllowed() {
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setId("some id");
		this.registrar.setContainerFactory(this.containerFactory);
		this.registrar.registerEndpoint(endpoint, null);
		this.registrar.afterPropertiesSet();
		assertThat(this.registry.getListenerContainer("some id")).as("Container not created").isNotNull();
		assertThat(this.registry.getListenerContainers()).hasSize(1);
		assertThat(this.registry.getListenerContainerIds()).containsOnly("some id");
	}

	@Test
	void registerNullContainerFactoryWithNoDefault() {
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setId("some id");
		this.registrar.registerEndpoint(endpoint, null);

		assertThatIllegalStateException().isThrownBy(() ->
				this.registrar.afterPropertiesSet())
			.withMessageContaining(endpoint.toString());
	}

	@Test
	void registerContainerWithoutFactory() {
		SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
		endpoint.setId("myEndpoint");
		this.registrar.setContainerFactory(this.containerFactory);
		this.registrar.registerEndpoint(endpoint);
		this.registrar.afterPropertiesSet();
		assertThat(this.registry.getListenerContainer("myEndpoint")).as("Container not created").isNotNull();
		assertThat(this.registry.getListenerContainers()).hasSize(1);
		assertThat(this.registry.getListenerContainerIds()).containsOnly("myEndpoint");
	}

}
