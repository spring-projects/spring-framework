/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.service.invoker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.reactivex.rxjava3.core.Observable;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.HttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link PathVariableArgumentResolver}.
 *
 * @author Olga Maciaszek-Sharma
 */
class PathVariableArgumentResolverTests extends HttpServiceMethodTestSupport {

	private final Service service = createService(Service.class,
			Collections.singletonList(new PathVariableArgumentResolver(null)));

	@Test
	void shouldResolvePathVariableWithNameFromParameter() {
		Mono<Void> execution = this.service.execute("test");

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo("test");
	}

	@Test
	void shouldResolvePathVariableWithNameFromAnnotationName() {
		Mono<Void> execution = this.service.executeNamed("test");

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo("test");
	}

	@Test
	void shouldResolvePathVariableNameFromValue() {
		Mono<Void> execution = this.service.executeNamedWithValue("test");

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo("test");
	}

	@Test
	void shouldOverrideNameIfValuePresentInAnnotation() {
		Mono<Void> execution = this.service.executeValueNamed("test");

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo("test");
	}

	@Test
	void shouldResolvePathVariableWithNameFromObject() {
		Mono<Void> execution = this.service.execute(new TestObject("test"));

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo("test");
	}

	@Test
	void shouldResolvePathVariableWithConversionService() {
		Service service = createService(Service.class,
				Collections.singletonList(new PathVariableArgumentResolver(
						new DefaultConversionService())));
		Mono<Void> execution = service.execute(Boolean.TRUE);

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo("true");
	}

	@Test
	void shouldResolvePathVariableFromOptionalArgumentWithConversionService() {
		Service service = createService(Service.class,
				Collections.singletonList(new PathVariableArgumentResolver(
						new DefaultConversionService())));
		Mono<Void> execution = service.executeOptional(Optional.of(Boolean.TRUE));

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo("true");
	}

	@Test
	void shouldResolvePathVariableFromOptionalArgument() {
		Mono<Void> execution = this.service.execute(Optional.of("test"));

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo("test");
	}

	@Test
	void shouldThrowExceptionForNullWithConversionService() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Service service = createService(Service.class,
					Collections.singletonList(new PathVariableArgumentResolver(
							new DefaultConversionService())));
			Mono<Void> execution = service.executeNamedWithValue(null);

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldThrowExceptionForNull() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Mono<Void> execution = this.service.executeNamedWithValue(null);

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldThrowExceptionForEmptyOptional() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Service service = createService(Service.class, Collections.singletonList(new PathVariableArgumentResolver(new DefaultConversionService())));
			Mono<Void> execution = service.execute(Optional.empty());

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldThrowExceptionForEmptyOptionalWithoutConversionService() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Mono<Void> execution = this.service.execute(Optional.empty());

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldNotThrowExceptionForNullWithConversionServiceWhenNotRequired() {
		Service service = createService(Service.class, Collections.singletonList(new PathVariableArgumentResolver(new DefaultConversionService())));
		Mono<Void> execution = service.executeNotRequired(null);

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isNull();
	}

	@Test
	void shouldNotThrowExceptionForNullWhenNotRequired() {
		Mono<Void> execution = this.service.executeNotRequired(null);

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo(null);
	}

	@Test
	void shouldNotThrowExceptionForEmptyOptionalWhenNotRequired() {
		Service service = createService(Service.class, Collections.singletonList(new PathVariableArgumentResolver(new DefaultConversionService())));
		Mono<Void> execution = service.executeOptionalNotRequired(Optional.empty());

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo(null);
	}

	@Test
	void shouldNotThrowExceptionForEmptyOptionalWithoutConversionServiceWhenNotRequired() {
		Mono<Void> execution = this.service.executeOptionalNotRequired(Optional.empty());

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo(null);
	}

	@Test
	void shouldThrowExceptionForReactorWrapper() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Mono<Void> execution = this.service.executeMono(Mono.just("test"));

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldThrowExceptionForRXWrapper() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Mono<Void> execution = this.service.executeObservable(Observable.just("test"));

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldThrowExceptionForOptionalReactorWrapper() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Mono<Void> execution = this.service.executeOptionalMono(Optional.of(Mono.just("test")));

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldThrowExceptionForOptionalRXWrapper() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Mono<Void> execution = this.service.executeOptionalObservable(Optional.of(Observable.just("test")));

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldResolvePathVariableFromNamedMap() {
		Mono<Void> execution = this.service.executeNamedMap(Map.of("id", "test"));

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo("test");
	}

	@Test
	void shouldResolvePathVariableFromMapWithAnnotationValue() {
		Mono<Void> execution = this.service.executeNamedValueMap(Map.of("id", "test"));

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo("test");
	}

	@Test
	void shouldThrowExceptionForReactorWrapperInNamedMap() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Mono<Void> execution = this.service.executeNamedReactorMap(Map.of("id", Flux.just("test")));

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldResolveOptionalPathVariableFromNamedMap() {
		Mono<Void> execution = this.service.executeOptionalValueNamedMap(Map.of("id", Optional.of("test")));

		StepVerifier.create(execution).verifyComplete();

		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo("test");
	}

	@Test
	void shouldResolveNamedPathVariableFromNamedMapWithConversionService() {
		Service service = createService(Service.class, Collections.singletonList(new PathVariableArgumentResolver(new DefaultConversionService())));
		Mono<Void> execution = service.executeNamedBooleanMap(Map.of("id", Boolean.TRUE));

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo("true");
	}

	@Test
	void shouldThrowExceptionForNullNamedMap() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Mono<Void> execution = this.service.executeNamedValueMap(null);

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldThrowExceptionForNullNamedMapValue() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Mono<Void> execution = this.service.executeNamedValueMap(new HashMap<>());

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldThrowExceptionForEmptyOptionalNamedMapValue() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Service service = createService(Service.class, Collections.singletonList(new PathVariableArgumentResolver(new DefaultConversionService())));
			Mono<Void> execution = service.executeOptionalValueNamedMap(Map.of("id", Optional.empty()));

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldNotThrowExceptionForNullNamedMapValueWhenNotRequired() {
		Mono<Void> execution = this.service.executeNamedValueMapNotRequired(null);

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo(null);
	}

	@Test
	void shouldNotThrowExceptionForEmptyOptionalNamedMapValueWhenNotRequired() {
		Service service = createService(Service.class, Collections.singletonList(new PathVariableArgumentResolver(new DefaultConversionService())));
		Mono<Void> execution = service.executeOptionalValueMapNotRequired(Map.of("id", Optional.empty()));

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo(null);
	}

	@Test
	void shouldResolvePathVariablesFromMap() {
		Mono<Void> execution = this.service.executeValueMap(Map.of("id", "test"));

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo("test");
	}

	@Test
	void shouldThrowExceptionForReactorWrapperValueInMap() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Mono<Void> execution = this.service.executeReactorValueMap(Map.of("id", Flux.just("test")));

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldThrowExceptionForReactorWrapperKeyInMap() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Mono<Void> execution = this.service.executeReactorKeyMap(Map.of(Flux.just("id"), "test"));

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldResolvePathVariableFromOptionalMapValue() {
		Mono<Void> execution = this.service.executeOptionalValueMap(Map.of("id", Optional.of("test")));

		StepVerifier.create(execution).verifyComplete();

		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo("test");
	}

	@Test
	void shouldResolvePathVariableFromOptionalMapKey() {
		Mono<Void> execution = this.service.executeOptionalKeyMap(Map.of(Optional.of("id"), "test"));

		StepVerifier.create(execution).verifyComplete();

		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo("test");
	}

	@Test
	void shouldResolvePathVariableFromMapWithConversionService() {
		Service service = createService(Service.class, Collections.singletonList(new PathVariableArgumentResolver(new DefaultConversionService())));
		Mono<Void> execution = service.executeBooleanMap(Map.of(Boolean.TRUE, Boolean.TRUE));

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables()
				.get("true")).isEqualTo("true");
	}

	@Test
	void shouldThrowExceptionForNullMapValue() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Mono<Void> execution = this.service.executeValueMap(null);

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldThrowExceptionForEmptyOptionalMapValue() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Service service = createService(Service.class, Collections.singletonList(new PathVariableArgumentResolver(new DefaultConversionService())));
			Mono<Void> execution = service.executeOptionalValueMap(Map.of("id", Optional.empty()));

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldThrowExceptionForEmptyOptionalMapKey() {
		assertThatIllegalStateException().isThrownBy(() -> {
			Service service = createService(Service.class, Collections.singletonList(new PathVariableArgumentResolver(new DefaultConversionService())));
			Mono<Void> execution = service.executeOptionalKeyMap(Map.of(Optional.empty(), "test"));

			StepVerifier.create(execution).verifyComplete();
		});
	}

	@Test
	void shouldNotThrowExceptionForNullMapValueWhenNotRequired() {
		Mono<Void> execution = this.service.executeValueMapNotRequired(null);

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo(null);
	}

	@Test
	void shouldNotThrowExceptionForEmptyOptionalMapValueWhenNotRequired() {
		Service service = createService(Service.class, Collections.singletonList(new PathVariableArgumentResolver(new DefaultConversionService())));
		Mono<Void> execution = service.executeOptionalValueMapNotRequired(Map.of("id", Optional.empty()));

		StepVerifier.create(execution).verifyComplete();
		assertThat(getRequestDefinition().getUriVariables().get("id")).isEqualTo(null);
	}


	private interface Service {

		@HttpRequest
		Mono<Void> execute(@PathVariable String id);

		@HttpRequest
		Mono<Void> executeNotRequired(@Nullable @PathVariable(required = false) String id);

		@HttpRequest
		Mono<Void> executeOptional(@PathVariable Optional<Boolean> id);

		@HttpRequest
		Mono<Void> executeOptionalNotRequired(@PathVariable(required = false) Optional<String> id);

		@HttpRequest
		Mono<Void> executeNamedWithValue(@Nullable @PathVariable(name = "test", value = "id") String employeeId);

		@HttpRequest
		Mono<Void> executeNamed(@PathVariable(name = "id") String employeeId);

		@HttpRequest
		Mono<Void> executeValueNamed(@PathVariable("id") String employeeId);

		@HttpRequest
		Mono<Void> execute(@PathVariable Object id);

		@HttpRequest
		Mono<Void> execute(@PathVariable Boolean id);

		@HttpRequest
		Mono<Void> executeMono(@PathVariable Mono<String> id);

		@HttpRequest
		Mono<Void> executeObservable(@PathVariable Observable<String> id);

		@HttpRequest
		Mono<Void> executeOptionalMono(@PathVariable Optional<Mono<String>> id);

		@HttpRequest
		Mono<Void> executeOptionalObservable(@PathVariable Optional<Observable<String>> id);

		@HttpRequest
		Mono<Void> executeNamedMap(@PathVariable(name = "id") Map<String, String> map);

		@HttpRequest
		Mono<Void> executeNamedValueMap(@Nullable @PathVariable("id") Map<String, String> map);

		@HttpRequest
		Mono<Void> executeNamedBooleanMap(@PathVariable("id") Map<String, Boolean> map);

		@HttpRequest
		Mono<Void> executeNamedReactorMap(@PathVariable(name = "id") Map<String, Flux<String>> map);

		@HttpRequest
		Mono<Void> executeOptionalValueNamedMap(@PathVariable("id") Map<String, Optional<String>> map);

		@HttpRequest
		Mono<Void> executeNamedValueMapNotRequired(@Nullable @PathVariable(name = "id", required = false) Map<String, String> map);

		@HttpRequest
		Mono<Void> executeOptionalValueMapNotRequired(@PathVariable(name = "id", required = false) Map<String, Optional<String>> map);

		@HttpRequest
		Mono<Void> executeValueMap(@Nullable @PathVariable Map<String, String> map);

		@HttpRequest
		Mono<Void> executeReactorValueMap(@PathVariable Map<String, Flux<String>> map);

		@HttpRequest
		Mono<Void> executeReactorKeyMap(@PathVariable Map<Flux<String>, String> map);

		@HttpRequest
		Mono<Void> executeOptionalValueMap(@PathVariable Map<String, Optional<String>> map);

		@HttpRequest
		Mono<Void> executeOptionalKeyMap(@PathVariable Map<Optional<String>, String> map);

		@HttpRequest
		Mono<Void> executeBooleanMap(@PathVariable Map<Boolean, Boolean> map);

		@HttpRequest
		Mono<Void> executeValueMapNotRequired(@Nullable @PathVariable(required = false) Map<String, String> map);
	}

	static class TestObject {

		TestObject(String value) {
			this.value = value;
		}

		String value;

		@Override
		public String toString() {
			return value;
		}
	}

}
