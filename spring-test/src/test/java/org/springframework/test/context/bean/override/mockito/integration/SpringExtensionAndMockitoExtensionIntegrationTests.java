/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Integration tests that verify support for {@link MockitoBean @MockitoBean}
 * and {@link MockitoSpyBean @MockitoSpyBean} when the {@link MockitoExtension}
 * is registered alongside the {@link SpringExtension}.
 *
 * <p>This test class currently verifies explicit support for {@link Captor @Captor},
 * but we may extend the scope of this test class in the future.
 *
 * @author Sam Brannen
 * @since 6.2
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
public class SpringExtensionAndMockitoExtensionIntegrationTests {

	@MockitoSpyBean
	RegistrationService registrationService;

	@MockitoBean
	UserService userService;

	@Captor
	ArgumentCaptor<User> userCaptor;


	@Test
	void test() {
		registrationService.registerUser("Duke");
		verify(registrationService).registerUser("Duke");
		verify(userService).validateUser(userCaptor.capture());
		assertThat(userCaptor.getValue().name).isEqualTo("Duke");
	}

	@Configuration
	static class Config {

		@Bean
		RegistrationService registrationService(UserService userService) {
			return new RegistrationService(userService);
		}
	}

	interface UserService {

		void validateUser(User user);
	}

	record RegistrationService(UserService userService) {

		void registerUser(String name) {
			User user = new User(name);
			this.userService.validateUser(user);
			// Register user...
		}
	}

	record User(String name) {
	}

}
