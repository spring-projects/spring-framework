/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.method.annotation;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Ignore;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rx.Fluxion;
import reactor.rx.Promise;
import rx.Observable;
import rx.Single;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.support.ByteBufferEncoder;
import org.springframework.core.codec.support.JacksonJsonEncoder;
import org.springframework.core.codec.support.JsonObjectEncoder;
import org.springframework.core.codec.support.StringEncoder;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.convert.support.ReactiveStreamsToCompletableFutureConverter;
import org.springframework.core.convert.support.ReactiveStreamsToReactorFluxionConverter;
import org.springframework.core.convert.support.ReactiveStreamsToRxJava1Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.core.io.buffer.DefaultDataBufferAllocator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.ViewResolver;
import org.springframework.web.reactive.handler.SimpleHandlerResultHandler;
import org.springframework.web.reactive.view.ViewResolverResultHandler;
import org.springframework.web.reactive.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.reactive.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Stephane Maldini
 */
public class RequestMappingIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private AnnotationConfigApplicationContext wac;


	@Override
	protected HttpHandler createHttpHandler() {
		this.wac = new AnnotationConfigApplicationContext();
		this.wac.register(FrameworkConfig.class, ApplicationConfig.class);
		this.wac.refresh();

		DispatcherHandler webHandler = new DispatcherHandler();
		webHandler.setApplicationContext(this.wac);

		return WebHttpHandlerBuilder.webHandler(webHandler).build();
	}

	@Test
	public void helloWithQueryParam() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI("http://localhost:" + port + "/param?name=George");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		ResponseEntity<String> response = restTemplate.exchange(request, String.class);

		assertEquals("Hello George!", response.getBody());
	}

	@Test
	public void rawPojoResponse() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI("http://localhost:" + port + "/raw");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		Person person = restTemplate.exchange(request, Person.class).getBody();

		assertEquals(new Person("Robert"), person);
	}

	@Test
	public void rawFluxResponse() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI("http://localhost:" + port + "/raw-flux");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		ResponseEntity<String> response = restTemplate.exchange(request, String.class);

		assertEquals("Hello!", response.getBody());
	}

	@Test
	public void rawObservableResponse() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI("http://localhost:" + port + "/raw-observable");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		ResponseEntity<String> response = restTemplate.exchange(request, String.class);

		assertEquals("Hello!", response.getBody());
	}

	@Test
	public void handleWithThrownException() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI("http://localhost:" + port + "/thrown-exception");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		ResponseEntity<String> response = restTemplate.exchange(request, String.class);

		assertEquals("Recovered from error: Boo", response.getBody());
	}

	@Test
	public void handleWithErrorSignal() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI("http://localhost:" + port + "/error-signal");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		ResponseEntity<String> response = restTemplate.exchange(request, String.class);

		assertEquals("Recovered from error: Boo", response.getBody());
	}

	@Test
	@Ignore
	//FIXME Fail with Jetty and Tomcat
	public void streamResult() throws Exception {
		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI("http://localhost:" + port + "/stream-result");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		ResponseEntity<String[]> response = restTemplate.exchange(request, String[].class);

		assertArrayEquals(new String[]{"0", "1", "2", "3", "4"}, response.getBody());
	}

	@Test
	public void serializeAsPojo() throws Exception {
		serializeAsPojo("http://localhost:" + port + "/person");
	}

	@Test
	public void serializeAsCompletableFuture() throws Exception {
		serializeAsPojo("http://localhost:" + port + "/completable-future");
	}

	@Test
	public void serializeAsMono() throws Exception {
		serializeAsPojo("http://localhost:" + port + "/mono");
	}

	@Test
	public void serializeAsSingle() throws Exception {
		serializeAsPojo("http://localhost:" + port + "/single");
	}

	@Test
	public void serializeAsPromise() throws Exception {
		serializeAsPojo("http://localhost:" + port + "/promise");
	}

	@Test
	public void serializeAsList() throws Exception {
		serializeAsCollection("http://localhost:" + port + "/list");
	}

	@Test
	public void serializeAsPublisher() throws Exception {
		serializeAsCollection("http://localhost:" + port + "/publisher");
	}

	@Test
	public void serializeAsFlux() throws Exception {
		serializeAsCollection("http://localhost:" + port + "/flux");
	}

	@Test
	public void serializeAsObservable() throws Exception {
		serializeAsCollection("http://localhost:" + port + "/observable");
	}

	@Test
	public void serializeAsReactorStream() throws Exception {
		serializeAsCollection("http://localhost:" + port + "/stream");
	}

	@Test
	public void publisherCapitalize() throws Exception {
		capitalizeCollection("http://localhost:" + port + "/publisher-capitalize");
	}

	@Test
	public void fluxCapitalize() throws Exception {
		capitalizeCollection("http://localhost:" + port + "/flux-capitalize");
	}

	@Test
	public void observableCapitalize() throws Exception {
		capitalizeCollection("http://localhost:" + port + "/observable-capitalize");
	}

	@Test
	public void streamCapitalize() throws Exception {
		capitalizeCollection("http://localhost:" + port + "/stream-capitalize");
	}

	@Test
	public void personCapitalize() throws Exception {
		capitalizePojo("http://localhost:" + port + "/person-capitalize");
	}
	
	@Test
	public void completableFutureCapitalize() throws Exception {
		capitalizePojo("http://localhost:" + port + "/completable-future-capitalize");
	}

	@Test
	public void monoCapitalize() throws Exception {
		capitalizePojo("http://localhost:" + port + "/mono-capitalize");
	}

	@Test
	public void singleCapitalize() throws Exception {
		capitalizePojo("http://localhost:" + port + "/single-capitalize");
	}

	@Test
	public void promiseCapitalize() throws Exception {
		capitalizePojo("http://localhost:" + this.port + "/promise-capitalize");
	}

	@Test
	public void publisherCreate() throws Exception {
		create("http://localhost:" + this.port + "/publisher-create");
	}

	@Test
	public void fluxCreate() throws Exception {
		create("http://localhost:" + this.port + "/flux-create");
	}

	@Test
	public void streamCreate() throws Exception {
		create("http://localhost:" + this.port + "/stream-create");
	}

	@Test
	public void observableCreate() throws Exception {
		create("http://localhost:" + this.port + "/observable-create");
	}

	@Test
	public void html() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI("http://localhost:" + port + "/html?name=Jason");
		RequestEntity<Void> request = RequestEntity.get(url).accept(MediaType.TEXT_HTML).build();
		ResponseEntity<String> response = restTemplate.exchange(request, String.class);

		assertEquals("<html><body>Hello: Jason!</body></html>", response.getBody());
	}


	private void serializeAsPojo(String requestUrl) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		RequestEntity<Void> request = RequestEntity.get(new URI(requestUrl))
				.accept(MediaType.APPLICATION_JSON)
				.build();
		ResponseEntity<Person> response = restTemplate.exchange(request, Person.class);

		assertEquals(new Person("Robert"), response.getBody());
	}

	private void serializeAsCollection(String requestUrl) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		RequestEntity<Void> request = RequestEntity.get(new URI(requestUrl))
				.accept(MediaType.APPLICATION_JSON)
				.build();
		List<Person> results = restTemplate.exchange(request,
				new ParameterizedTypeReference<List<Person>>(){}).getBody();

		assertEquals(2, results.size());
		assertEquals(new Person("Robert"), results.get(0));
		assertEquals(new Person("Marie"), results.get(1));
	}


	private void capitalizePojo(String requestUrl) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		RequestEntity<Person> request = RequestEntity.post(new URI(requestUrl))
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(new Person("Robert"));
		ResponseEntity<Person> response = restTemplate.exchange(request, Person.class);

		assertEquals(new Person("ROBERT"), response.getBody());
	}

	private void capitalizeCollection(String requestUrl) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		RequestEntity<List<Person>> request = RequestEntity.post(new URI(requestUrl))
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Arrays.asList(new Person("Robert"), new Person("Marie")));
		List<Person> results = restTemplate.exchange(request,
				new ParameterizedTypeReference<List<Person>>(){}).getBody();

		assertEquals(2, results.size());
		assertEquals("ROBERT", results.get(0).getName());
		assertEquals("MARIE", results.get(1).getName());
	}

	private void create(String requestUrl) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		URI url = new URI(requestUrl);
		RequestEntity<List<Person>> request = RequestEntity.post(url)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Arrays.asList(new Person("Robert"), new Person("Marie")));
		ResponseEntity<Void> response = restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(2, this.wac.getBean(TestRestController.class).persons.size());
	}


	@Configuration
	@SuppressWarnings("unused")
	static class FrameworkConfig {

		private DataBufferAllocator allocator = new DefaultDataBufferAllocator();


		@Bean
		public RequestMappingHandlerMapping handlerMapping() {
			return new RequestMappingHandlerMapping();
		}

		@Bean
		public RequestMappingHandlerAdapter handlerAdapter() {
			RequestMappingHandlerAdapter handlerAdapter = new RequestMappingHandlerAdapter();
			handlerAdapter.setConversionService(conversionService());
			return handlerAdapter;
		}

		@Bean
		public ConversionService conversionService() {
			// TODO: test failures with DefaultConversionService
			GenericConversionService service = new GenericConversionService();
			service.addConverter(new ReactiveStreamsToCompletableFutureConverter());
			service.addConverter(new ReactiveStreamsToReactorFluxionConverter());
			service.addConverter(new ReactiveStreamsToRxJava1Converter());
			return service;
		}

		@Bean
		public ResponseBodyResultHandler responseBodyResultHandler() {
			List<Encoder<?>> encoders = Arrays.asList(
					new ByteBufferEncoder(this.allocator), new StringEncoder(this.allocator),
					new JacksonJsonEncoder(this.allocator, new JsonObjectEncoder(this.allocator)));
			ResponseBodyResultHandler resultHandler = new ResponseBodyResultHandler(encoders, conversionService());
			resultHandler.setOrder(1);
			return resultHandler;
		}

		@Bean
		public SimpleHandlerResultHandler simpleHandlerResultHandler() {
			SimpleHandlerResultHandler resultHandler = new SimpleHandlerResultHandler(conversionService());
			resultHandler.setOrder(2);
			return resultHandler;
		}

		@Bean
		public ViewResolverResultHandler viewResolverResultHandler() {
			List<ViewResolver> resolvers = Collections.singletonList(freeMarkerViewResolver());
			ViewResolverResultHandler resultHandler = new ViewResolverResultHandler(resolvers, conversionService());
			resultHandler.setOrder(3);
			return resultHandler;
		}

		@Bean
		public ViewResolver freeMarkerViewResolver() {
			FreeMarkerViewResolver viewResolver = new FreeMarkerViewResolver("", ".ftl");
			viewResolver.setBufferAllocator(this.allocator);
			return viewResolver;
		}

		@Bean
		public FreeMarkerConfigurer freeMarkerConfig() {
			FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
			configurer.setPreferFileSystemAccess(false);
			configurer.setTemplateLoaderPath("classpath*:org/springframework/web/reactive/view/freemarker/");
			return configurer;
		}

	}

	@Configuration
	@SuppressWarnings("unused")
	static class ApplicationConfig {

		@Bean
		public TestRestController testRestController() {
			return new TestRestController();
		}

		@Bean
		public TestController testController() {
			return new TestController();
		}
	}


	@RestController
	@SuppressWarnings("unused")
	private static class TestRestController {

		final List<Person> persons = new ArrayList<>();

		@RequestMapping("/param")
		public Publisher<String> handleWithParam(@RequestParam String name) {
			return Flux.just("Hello ", name, "!");
		}

		@RequestMapping("/person")
		public Person personResponseBody() {
			return new Person("Robert");
		}

		@RequestMapping("/completable-future")
		public CompletableFuture<Person> completableFutureResponseBody() {
			return CompletableFuture.completedFuture(new Person("Robert"));
		}

		@RequestMapping("/raw")
		public Publisher<ByteBuffer> rawResponseBody() {
			JacksonJsonEncoder encoder = new JacksonJsonEncoder(new DefaultDataBufferAllocator());
			return encoder.encode(Mono.just(new Person("Robert")),
					ResolvableType.forClass(Person.class), MediaType.APPLICATION_JSON).map(DataBuffer::asByteBuffer);
		}

		@RequestMapping("/stream-result")
		public Publisher<Long> stringStreamResponseBody() {
			return Flux.interval(1).as(Fluxion::from).take(5);
		}

		@RequestMapping("/raw-flux")
		public Flux<ByteBuffer> rawFluxResponseBody() {
			return Flux.just(ByteBuffer.wrap("Hello!".getBytes()));
		}

		@RequestMapping("/raw-observable")
		public Observable<ByteBuffer> rawObservableResponseBody() {
			return Observable.just(ByteBuffer.wrap("Hello!".getBytes()));
		}

		@RequestMapping("/mono")
		public Mono<Person> monoResponseBody() {
			return Mono.just(new Person("Robert"));
		}

		@RequestMapping("/single")
		public Single<Person> singleResponseBody() {
			return Single.just(new Person("Robert"));
		}

		@RequestMapping("/promise")
		public Promise<Person> promiseResponseBody() {
			return Promise.success(new Person("Robert"));
		}

		@RequestMapping("/list")
		public List<Person> listResponseBody() {
			return Arrays.asList(new Person("Robert"), new Person("Marie"));
		}

		@RequestMapping("/publisher")
		public Publisher<Person> publisherResponseBody() {
			return Flux.just(new Person("Robert"), new Person("Marie"));
		}

		@RequestMapping("/flux")
		public Flux<Person> fluxResponseBody() {
			return Flux.just(new Person("Robert"), new Person("Marie"));
		}

		@RequestMapping("/observable")
		public Observable<Person> observableResponseBody() {
			return Observable.just(new Person("Robert"), new Person("Marie"));
		}

		@RequestMapping("/stream")
		public Fluxion<Person> reactorStreamResponseBody() {
			return Fluxion.just(new Person("Robert"), new Person("Marie"));
		}

		@RequestMapping("/publisher-capitalize")
		public Publisher<Person> publisherCapitalize(@RequestBody Publisher<Person> persons) {
			return Flux
					.from(persons)
					.map(person -> new Person(person.getName().toUpperCase()));
		}

		@RequestMapping("/flux-capitalize")
		public Flux<Person> fluxCapitalize(@RequestBody Flux<Person> persons) {
			return persons.map(person -> new Person(person.getName().toUpperCase()));
		}

		@RequestMapping("/observable-capitalize")
		public Observable<Person> observableCapitalize(@RequestBody Observable<Person> persons) {
			return persons.map(person -> new Person(person.getName().toUpperCase()));
		}

		@RequestMapping("/stream-capitalize")
		public Fluxion<Person> streamCapitalize(@RequestBody Fluxion<Person> persons) {
			return persons.map(person -> new Person(person.getName().toUpperCase()));
		}

		@RequestMapping("/person-capitalize")
		public Person personCapitalize(@RequestBody Person person) {
			return new Person(person.getName().toUpperCase());
		}
		
		@RequestMapping("/completable-future-capitalize")
		public CompletableFuture<Person> completableFutureCapitalize(
				@RequestBody CompletableFuture<Person> personFuture) {
			return personFuture.thenApply(person -> new Person(person.getName().toUpperCase()));
		}

		@RequestMapping("/mono-capitalize")
		public Mono<Person> monoCapitalize(@RequestBody Mono<Person> personFuture) {
			return personFuture.map(person -> new Person(person.getName().toUpperCase()));
		}

		@RequestMapping("/single-capitalize")
		public Single<Person> singleCapitalize(@RequestBody Single<Person> personFuture) {
			return personFuture.map(person -> new Person(person.getName().toUpperCase()));
		}

		@RequestMapping("/promise-capitalize")
		public Promise<Person> promiseCapitalize(@RequestBody Promise<Person> personFuture) {
			return Fluxion.from(personFuture.map(person -> new Person(person.getName().toUpperCase()))).promise();
		}

		@RequestMapping("/publisher-create")
		public Publisher<Void> publisherCreate(@RequestBody Publisher<Person> personStream) {
			return Flux.from(personStream).doOnNext(persons::add).after();
		}

		@RequestMapping("/flux-create")
		public Mono<Void> fluxCreate(@RequestBody Flux<Person> personStream) {
			return personStream.doOnNext(persons::add).after();
		}

		@RequestMapping("/stream-create")
		public Publisher<Void> streamCreate(@RequestBody Fluxion<Person> personStream) {
			return personStream.toList().doOnSuccess(persons::addAll).after();
		}

		@RequestMapping("/observable-create")
		public Observable<Void> observableCreate(@RequestBody Observable<Person> personStream) {
			return personStream.toList().doOnNext(persons::addAll).flatMap(document -> Observable.empty());
		}

		@RequestMapping("/thrown-exception")
		public Publisher<String> handleAndThrowException() {
			throw new IllegalStateException("Boo");
		}

		@RequestMapping("/error-signal")
		public Publisher<String> handleWithError() {
			return Mono.error(new IllegalStateException("Boo"));
		}

		@ExceptionHandler
		public Publisher<String> handleException(IllegalStateException ex) {
			return Mono.just("Recovered from error: " + ex.getMessage());
		}

		//TODO add mixed and T request mappings tests

	}

	@Controller
	@SuppressWarnings("unused")
	private static class TestController {

		@RequestMapping("/html")
		public String getHtmlPage(@RequestParam String name, Model model) {
			model.addAttribute("hello", "Hello: " + name + "!");
			return "test";
		}

	}

	private static class Person {

		private String name;

		@SuppressWarnings("unused")
		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Person person = (Person) o;
			return !(this.name != null ? !this.name.equals(person.name) : person.name != null);
		}

		@Override
		public int hashCode() {
			return this.name != null ? this.name.hashCode() : 0;
		}

		@Override
		public String toString() {
			return "Person{" +
					"name='" + name + '\'' +
					'}';
		}
	}

}
