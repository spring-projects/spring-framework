/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp;

import java.security.Principal;
import java.util.Collections;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.security.TestPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SimpMessageHeaderAccessor}.
 *
 * @author Rossen Stoyanchev
 */
class SimpMessageHeaderAccessorTests {


	@Test
	void getShortLogMessage() {
		assertThat(SimpMessageHeaderAccessor.create().getShortLogMessage("p"))
				.isEqualTo("MESSAGE session=null payload=p");
	}

	@Test
	void getLogMessageWithValuesSet() {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
		accessor.setDestination("/destination");
		accessor.setSubscriptionId("subscription");
		accessor.setSessionId("session");
		accessor.setUser(new TestPrincipal("user"));
		accessor.setSessionAttributes(Collections.singletonMap("key", "value"));

		assertThat(accessor.getShortLogMessage("p"))
				.isEqualTo(("MESSAGE destination=/destination subscriptionId=subscription " +
						"session=session user=user attributes[1] payload=p"));
	}

	@Test
	void getDetailedLogMessageWithValuesSet() {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
		accessor.setDestination("/destination");
		accessor.setSubscriptionId("subscription");
		accessor.setSessionId("session");
		accessor.setUser(new TestPrincipal("user"));
		accessor.setSessionAttributes(Collections.singletonMap("key", "value"));
		accessor.setNativeHeader("nativeKey", "nativeValue");

		assertThat(accessor.getDetailedLogMessage("p"))
				.isEqualTo(("MESSAGE destination=/destination subscriptionId=subscription " +
						"session=session user=user attributes={key=value} nativeHeaders=" +
						"{nativeKey=[nativeValue]} payload=p"));
	}

	@Test
	void userChangeCallback() {
		UserCallback userCallback = new UserCallback();
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
		accessor.setUserChangeCallback(userCallback);

		Principal user1 = mock();
		accessor.setUser(user1);
		assertThat(userCallback.getUser()).isEqualTo(user1);

		Principal user2 = mock();
		accessor.setUser(user2);
		assertThat(userCallback.getUser()).isEqualTo(user2);
	}


	private static class UserCallback implements Consumer<Principal> {

		private Principal user;


		public Principal getUser() {
			return this.user;
		}

		@Override
		public void accept(Principal principal) {
			this.user = principal;
		}
	}

}
