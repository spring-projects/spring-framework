/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.servlet.playwright;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.FilePayload;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@SpringJUnitWebConfig(resourcePath = "classpath:org/springframework/test/web/servlet/playwright/content")
class MockMvcPlaywrightHandlerTests {

	private static final String LOCALHOST = "http://localhost";
	private static Playwright playwright;
	private static Browser browser;
	private Page page;

	private final MockMvcPlaywrightHandler handler;

	public MockMvcPlaywrightHandlerTests(WebApplicationContext wac) {
		var mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
		this.handler = MockMvcPlaywrightHandler.builder(mockMvc).build();
	}

	private static FilePayload filePayload(String name, String content) {
		return new FilePayload(name, MediaType.TEXT_PLAIN_VALUE, content.getBytes(StandardCharsets.UTF_8));
	}

	@BeforeAll
	static void initPlaywright() {
		playwright = Playwright.create();
		browser = playwright.chromium().launch();
	}

	@BeforeEach
	void initPage() {
		page = browser.newPage();
		page.setDefaultTimeout(500);
		page.route(url -> url.startsWith(LOCALHOST), handler);
		page.navigate(LOCALHOST+"/index.html");
	}

	@AfterEach
	void closePage() {
		page.close();
	}

	@AfterAll
	static void closePlaywright() {
		browser.close();
		playwright.close();
	}

	@Test
	void testLoadSimpleHtmlPage() {
		assertThat(page).hasTitle("Playwright-MockMvc integration Test Web Page");
	}

	@Test
	void getForm() {
		page.locator("#query").fill("spring");
		page.locator("#getForm button[type='submit']").click();

		assertThat(page.locator("#getResult")).hasText("query=spring");
	}

	@Test
	void postForm() {
		page.locator("#username").fill("joe");
		page.locator("#message").fill("hello");
		page.locator("#postForm button[type='submit']").click();

		assertThat(page.locator("body")).hasText("username=joe,message=hello");
	}

	@Test
	void singleFileForm() throws IOException {
		page.locator("#singleFile").setInputFiles(filePayload("playwright-single.txt", "single-content"));
		page.locator("#singleFileForm button[type='submit']").click();

		assertThat(page.locator("body")).hasText("file=playwright-single.txt,content=single-content");
	}

	@Test
	void multipleFilesForm() throws IOException {
		var fileOne = filePayload("playwright-multiple-1.txt", "first-content");
		var fileTwo = filePayload("playwright-multiple-2.txt", "second-content");
		page.locator("#multipleFiles").setInputFiles(new FilePayload[]{fileOne, fileTwo});
		page.locator("#multipleFilesForm button[type='submit']").click();

		assertThat(page.locator("body")).hasText("files=playwright-multiple-1.txt:first-content;playwright-multiple-2.txt:second-content");
	}

	@Test
	void mixedMultipartForm() throws IOException {
		page.locator("#mixedFile").setInputFiles(filePayload("playwright-mixed.txt", "mixed-content"));
		page.locator("#description").fill("sample");
		page.locator("#version").fill("7");
		page.locator("#mixedMultipartForm button[type='submit']").click();

		assertThat(page.locator("body")).hasText("file=playwright-mixed.txt,description=sample,version=7,content=mixed-content");
	}

	@Configuration
	@EnableWebMvc
	static class Config implements WebMvcConfigurer {

		@Bean
		MultipartResolver multipartResolver() {
			return new StandardServletMultipartResolver();
		}

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/**").addResourceLocations("classpath:org/springframework/test/web/servlet/playwright/content/");
		}

		@RestController
		static class FormController {

			@GetMapping(path = "/search", produces = MediaType.TEXT_PLAIN_VALUE)
			String search(@RequestParam String query) {
				return "query=" + query;
			}

			@PostMapping(path = "/submit", produces = MediaType.TEXT_PLAIN_VALUE)
			String submit(@RequestParam String username, @RequestParam String message) {
				return "username=" + username + ",message=" + message;
			}

			@PostMapping(path = "/upload-single", produces = MediaType.TEXT_PLAIN_VALUE)
			String uploadSingle(@RequestParam("file") MultipartFile file) throws IOException {
				return "file=" + file.getOriginalFilename() + ",content=" +
						StreamUtils.copyToString(file.getInputStream(), StandardCharsets.UTF_8);
			}

			@PostMapping(path = "/upload-multiple", produces = MediaType.TEXT_PLAIN_VALUE)
			String uploadMultiple(@RequestParam("files") MultipartFile[] files) throws IOException {
				StringBuilder response = new StringBuilder("files=");
				for (int i = 0; i < files.length; i++) {
					if (i > 0) {
						response.append(';');
					}
					response.append(files[i].getOriginalFilename())
							.append(':')
							.append(StreamUtils.copyToString(files[i].getInputStream(), StandardCharsets.UTF_8));
				}
				return response.toString();
			}

			@PostMapping(path = "/upload-mixed", produces = MediaType.TEXT_PLAIN_VALUE)
			String uploadMixed(@RequestParam("file") MultipartFile file, @RequestParam String description,
								@RequestParam Integer version) throws IOException {
				return "file=" + file.getOriginalFilename() + ",description=" + description + ",version=" + version +
						",content=" + StreamUtils.copyToString(file.getInputStream(), StandardCharsets.UTF_8);
			}
		}
	}
}
