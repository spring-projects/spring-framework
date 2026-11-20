/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.client.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.Person;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.test.web.servlet.samples.context.PersonDao;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.BDDMockito.given;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.context.XmlConfigTests}.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration("src/test/resources/META-INF/web-resources")
@ContextHierarchy({
	@ContextConfiguration("../../context/root-context.xml"),
	@ContextConfiguration("../../context/servlet-context.xml")
})
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
class XmlConfigTests {

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private PersonDao personDao;

	private WebTestClient testClient;


	@BeforeEach
	void setup() {
		this.testClient = MockMvcWebTestClient.bindToApplicationContext(this.wac).build();
		given(this.personDao.getPerson(5L)).willReturn(new Person("Joe"));
	}


	@Test
	void person() {
		testClient.get().uri("/person/5")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
	}

}
