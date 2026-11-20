/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.annotation.reactive;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.invocation.ResolvableMethod;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.messaging.handler.annotation.MessagingPredicates.destinationVar;

/**
 * Test fixture for {@link DestinationVariableMethodArgumentResolver} tests.
 * @author Rossen Stoyanchev
 */
class DestinationVariableMethodArgumentResolverTests {

	private final DestinationVariableMethodArgumentResolver resolver =
			new DestinationVariableMethodArgumentResolver(new DefaultConversionService());

	private final ResolvableMethod resolvable =
			ResolvableMethod.on(getClass()).named("handleMessage").build();


	@Test
	void supportsParameter() {
		assertThat(resolver.supportsParameter(this.resolvable.annot(destinationVar().noValue()).arg())).isTrue();
		assertThat(resolver.supportsParameter(this.resolvable.annotNotPresent(DestinationVariable.class).arg())).isFalse();
	}

	@Test
	void resolveArgument() {

		Map<String, Object> vars = new HashMap<>();
		vars.put("foo", "bar");
		vars.put("name", "value");

		Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).setHeader(
			DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER, vars).build();

		Object result = resolveArgument(this.resolvable.annot(destinationVar().noValue()).arg(), message);
		assertThat(result).isEqualTo("bar");

		result = resolveArgument(this.resolvable.annot(destinationVar("name")).arg(), message);
		assertThat(result).isEqualTo("value");
	}

	@Test
	void resolveArgumentNotFound() {
		Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
		assertThatExceptionOfType(MessageHandlingException.class).isThrownBy(() ->
				resolveArgument(this.resolvable.annot(destinationVar().noValue()).arg(), message));
	}

	@SuppressWarnings({"unchecked", "ConstantConditions"})
	private <T> T resolveArgument(MethodParameter param, Message<?> message) {
		return (T) this.resolver.resolveArgument(param, message).block(Duration.ofSeconds(5));
	}


	@SuppressWarnings("unused")
	private void handleMessage(
			@DestinationVariable String foo,
			@DestinationVariable(value = "name") String param1,
			String param3) {
	}

}
