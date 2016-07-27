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

package org.springframework.web.reactive.function;

import java.time.Duration;
import java.util.Objects;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.bootstrap.HttpServer;
import org.springframework.http.server.reactive.bootstrap.ReactorHttpServer;

import static org.springframework.web.reactive.function.RequestPredicates.GET;
import static org.springframework.web.reactive.function.RequestPredicates.POST;
import static org.springframework.web.reactive.function.Router.route;
import static org.springframework.web.reactive.function.Router.toHttpHandler;

/**
 * @author Arjen Poutsma
 */
public class Driver {

    public static void main(String[] args) throws Exception {

        PersonHandler handler = new PersonHandler();

        RoutingFunction<Publisher<Person>> personRoute =
                route(GET("/person/{id}"), handler::person)
                        .and(route(GET("/people"), handler::people))
                        .filter((request, next) -> {
                            System.out.println("In publisher filter");
                            Response<Publisher<Person>> handlerResponse = next.handle(request);
                            Publisher<Person> s = Flux.from(handlerResponse.body())
                                    .map(person -> new Person(person.name.toUpperCase()));
                            return Response.from(handlerResponse).stream(s, Person.class);
                        });

        RoutingFunction<Publisher<String>> stringRoute = route(POST("/string"), request -> {
            Flux<String> requestBody = request.body().convertTo(String.class);
            Flux<String> responseBody = Flux.concat(Mono.just("Hello "), requestBody);
            return Response.ok().stream(responseBody, String.class);
        });

        RoutingFunction<?> sseRoute = route(GET("/sse"), request -> {
            Flux<ServerSentEvent<Person>> eventFlux = Flux.interval(Duration.ofMillis(100)).map(l -> {
                Person person = new Person("Person " + l);
                return ServerSentEvent.<Person>builder().data(person)
                        .id(Long.toString(l))
                        .comment("bar")
                        .build();
            }).take(20);

            return Response.ok().sse(eventFlux);
        }).andOther(route(GET("/sse-string"), request -> {
            Flux<String> flux = Flux.interval(Duration.ofMillis(100)).map(l -> Long.toString(l)).take(20);
            return Response.ok().sse(flux, String.class);
        }));

        createServer(toHttpHandler(
                personRoute.andOther(stringRoute).andOther(sseRoute)
                        .filter((request, next) -> {
                            System.out.println("Before");
                            Response<?> result = next.handle(request);
                            System.out.println("After");
                            return result;
                        })
        ));

        System.out.println("Press ENTER to exit.");
        System.in.read();
    }

    private static HttpServer createServer(HttpHandler httpHandler) throws Exception {
        HttpServer server = new ReactorHttpServer();
        int port = 8080;
        server.setPort(port);
        server.setHandler(httpHandler);
        server.afterPropertiesSet();
        server.start();
        System.out.println("Server started at http://localhost:" + port + "/");
        return server;
    }

    private static class PersonHandler {

        public Response<Publisher<Person>> person(Request r) {
            System.out.println("r.pathVariable(id) = " + r.pathVariable("id"));
            return Response.ok().contentType(MediaType.APPLICATION_JSON)
                    .stream(Mono.just(new Person("John")), Person.class);
        }

        public Response<Publisher<Person>> people(Request r) {
            return Response.ok().contentType(MediaType.APPLICATION_JSON)
                    .stream(Flux.just(new Person("Jane"), new Person("John")), Person.class);
        }


    }

    private static class Person {

        private String name;

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
            if (o instanceof Person) {
                Person other = (Person) o;
                return Objects.equals(this.name, other.name);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.name);
        }

        @Override
        public String toString() {
            return "Person{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}

