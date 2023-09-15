/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.codec.multipart.PartEvent;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.UndertowHttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class MultipartWebClientIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private WebClient webClient;


	@Override
	protected HttpHandler createHttpHandler() {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(TestConfiguration.class);
		wac.refresh();
		return WebHttpHandlerBuilder.webHandler(new DispatcherHandler(wac)).build();
	}

	@Override
	protected void startServer(HttpServer httpServer) throws Exception {
		super.startServer(httpServer);
		this.webClient = WebClient.create("http://localhost:" + this.port);
	}


	@ParameterizedHttpServerTest
	void requestPart(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<ResponseEntity<Void>> result = webClient
				.post()
				.uri("/requestPart")
				.bodyValue(generateBody())
				.retrieve()
				.toBodilessEntity();

		StepVerifier
				.create(result)
				.consumeNextWith(entity -> assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK))
				.verifyComplete();
	}

	@ParameterizedHttpServerTest
	void requestBodyMap(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<String> result = webClient
				.post()
				.uri("/requestBodyMap")
				.bodyValue(generateBody())
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.consumeNextWith(body -> assertThat(body).isEqualTo("Map[[fieldPart],[fileParts:foo.txt,fileParts:logo.png],[jsonPart]]"))
				.verifyComplete();
	}

	@ParameterizedHttpServerTest
	void requestBodyFlux(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<String> result = webClient
				.post()
				.uri("/requestBodyFlux")
				.bodyValue(generateBody())
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.consumeNextWith(body -> assertThat(body).isEqualTo("[fieldPart,fileParts:foo.txt,fileParts:logo.png,jsonPart]"))
				.verifyComplete();
	}

	@ParameterizedHttpServerTest
	void filePartsFlux(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<String> result = webClient
				.post()
				.uri("/filePartFlux")
				.bodyValue(generateBody())
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.consumeNextWith(body -> assertThat(body).isEqualTo("[fileParts:foo.txt,fileParts:logo.png]"))
				.verifyComplete();
	}

	@ParameterizedHttpServerTest
	void filePartsMono(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<String> result = webClient
				.post()
				.uri("/filePartMono")
				.bodyValue(generateBody())
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.consumeNextWith(body -> assertThat(body).isEqualTo("[fileParts:foo.txt]"))
				.verifyComplete();
	}

	@ParameterizedHttpServerTest
	void transferTo(HttpServer httpServer) throws Exception {
		// TODO Determine why Undertow fails: https://github.com/spring-projects/spring-framework/issues/25310
		assumeFalse(httpServer instanceof UndertowHttpServer, "Undertow currently fails with transferTo");
		startServer(httpServer);

		Flux<String> result = webClient
				.post()
				.uri("/transferTo")
				.bodyValue(generateBody())
				.retrieve()
				.bodyToFlux(String.class);

		StepVerifier.create(result)
				.consumeNextWith(filename -> verifyContents(Paths.get(filename), new ClassPathResource("foo.txt", MultipartHttpMessageReader.class)))
				.consumeNextWith(filename -> verifyContents(Paths.get(filename), new ClassPathResource("logo.png", getClass())))
				.verifyComplete();

	}

	@ParameterizedHttpServerTest
	void modelAttribute(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<String> result = webClient
				.post()
				.uri("/modelAttribute")
				.bodyValue(generateBody())
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.consumeNextWith(body -> assertThat(body).isEqualTo("FormBean[fieldValue,[fileParts:foo.txt,fileParts:logo.png]]"))
				.verifyComplete();
	}

	@ParameterizedHttpServerTest
	void partData(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<String> result = webClient
				.post()
				.uri("/partData")
				.bodyValue(generateBody())
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.consumeNextWith(body -> assertThat(body).isEqualTo("fieldPart,foo.txt:fileParts,logo.png:fileParts,jsonPart,"))
				.verifyComplete();
	}

	private MultiValueMap<String, HttpEntity<?>> generateBody() {
		MultipartBodyBuilder builder = new MultipartBodyBuilder();
		builder.part("fieldPart", "fieldValue");
		builder.part("fileParts", new ClassPathResource("foo.txt", MultipartHttpMessageReader.class));
		builder.part("fileParts", new ClassPathResource("logo.png", getClass()));
		builder.part("jsonPart", new Person("Jason"));
		return builder.build();
	}

	private static void verifyContents(Path tempFile, Resource resource) {
		try {
			// Use FileCopyUtils since the resource might reside in a JAR instead of in the file system.
			byte[] resourceBytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
			assertThat(tempFile).hasBinaryContent(resourceBytes);
		}
		catch (IOException ex) {
			throw new AssertionError(ex);
		}
	}


	@Configuration
	@EnableWebFlux
	@SuppressWarnings("unused")
	static class TestConfiguration {

		@Bean
		public MultipartController testController() {
			return new MultipartController();
		}
	}

	@RestController
	@SuppressWarnings("unused")
	static class MultipartController {

		@PostMapping("/requestPart")
		void requestPart(@RequestPart FormFieldPart fieldPart,
				@RequestPart("fileParts") FilePart fileParts,
				@RequestPart("jsonPart") Mono<Person> personMono) {

			assertThat(fieldPart.value()).isEqualTo("fieldValue");
			assertThat(partDescription(fileParts)).isEqualTo("fileParts:foo.txt");

			StepVerifier.create(personMono)
					.consumeNextWith(p -> assertThat(p.getName()).isEqualTo("Jason"))
					.verifyComplete();
		}

		@PostMapping("/requestBodyMap")
		Mono<String> requestBodyMap(@RequestBody Mono<MultiValueMap<String, Part>> partsMono) {
			return partsMono.map(MultipartWebClientIntegrationTests::partMapDescription);
		}

		@PostMapping("/requestBodyFlux")
		Mono<String> requestBodyFlux(@RequestBody Flux<Part> parts) {
			return partFluxDescription(parts);
		}

		@PostMapping("/filePartFlux")
		Mono<String> filePartsFlux(@RequestPart("fileParts") Flux<FilePart> parts) {
			return partFluxDescription(parts);
		}

		@PostMapping("/filePartMono")
		Mono<String> filePartsFlux(@RequestPart("fileParts") Mono<FilePart> parts) {
			return partFluxDescription(Flux.from(parts));
		}

		@PostMapping("/transferTo")
		Flux<String> transferTo(@RequestPart("fileParts") Flux<FilePart> parts) {
			return parts.concatMap(filePart -> createTempFile(filePart.filename())
					.flatMap(tempFile -> filePart.transferTo(tempFile)
							.then(Mono.just(tempFile.toString() + "\n"))));
		}

		private Mono<Path> createTempFile(String suffix) {
			return Mono.defer(() -> {
						try {
							return Mono.just(Files.createTempFile("MultipartIntegrationTests", suffix));
						}
						catch (IOException ex) {
							return Mono.error(ex);
						}
					})
					.subscribeOn(Schedulers.boundedElastic());
		}

		@PostMapping("/modelAttribute")
		String modelAttribute(@ModelAttribute FormBean formBean) {
			return formBean.toString();
		}

		@PostMapping("/partData")
		Flux<String> tokens(@RequestBody Flux<PartEvent> partData) {
			return partData.map(data -> {
				if (data.isLast()) {
					ContentDisposition cd = data.headers().getContentDisposition();
					StringBuilder sb = new StringBuilder();
					if (cd.getFilename() != null) {
						sb.append(cd.getFilename())
								.append(':')
								.append(cd.getName());
					}
					else if (cd.getName() != null) {
						sb.append(cd.getName());
					}
					sb.append(',');
					DataBufferUtils.release(data.content());
					return sb.toString();
				}
				else {
					return "";
				}
			});
		}

	}

	private static String partMapDescription(MultiValueMap<String, Part> partsMap) {
		return partsMap.keySet().stream().sorted()
				.map(key -> partListDescription(partsMap.get(key)))
				.collect(Collectors.joining(",", "Map[", "]"));
	}

	private static Mono<String> partFluxDescription(Flux<? extends Part> partsFlux) {
		return partsFlux.collectList().map(MultipartWebClientIntegrationTests::partListDescription);
	}

	private static String partListDescription(List<? extends Part> parts) {
		return parts.stream().map(MultipartWebClientIntegrationTests::partDescription)
				.collect(Collectors.joining(",", "[", "]"));
	}

	private static String partDescription(Part part) {
		return part instanceof FilePart filePart ? part.name() + ":" + filePart.filename() : part.name();
	}

	static class FormBean {

		private String fieldPart;

		private List<FilePart> fileParts;


		public String getFieldPart() {
			return this.fieldPart;
		}

		public void setFieldPart(String fieldPart) {
			this.fieldPart = fieldPart;
		}

		public List<FilePart> getFileParts() {
			return this.fileParts;
		}

		public void setFileParts(List<FilePart> fileParts) {
			this.fileParts = fileParts;
		}

		@Override
		public String toString() {
			return "FormBean[" + getFieldPart() + "," + partListDescription(getFileParts()) + "]";
		}
	}

	private static class Person {

		private String name;

		@JsonCreator
		public Person(@JsonProperty("name") String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

}
