package org.springframework.docs.testing.mockmvc.mockmvcserverplaywright

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.FilePayload
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.web.servlet.playwright.MockMvcPlaywrightHandler
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.util.StreamUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartResolver
import org.springframework.web.multipart.support.StandardServletMultipartResolver
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.charset.StandardCharsets

@SpringJUnitWebConfig
class MockMvcPlaywrightHandlerTests(wac: WebApplicationContext) {

	private lateinit var page: Page

	private val handler: MockMvcPlaywrightHandler

	// tag::init[]
	init {
		val mockMvc = MockMvcBuilders.webAppContextSetup(wac).build()
		handler = MockMvcPlaywrightHandler.builder(mockMvc).build()
	}
	// end::init[]

	private fun filePayload(name: String, content: String): FilePayload {
		return FilePayload(name, MediaType.TEXT_PLAIN_VALUE, content.toByteArray(StandardCharsets.UTF_8))
	}

	// tag::setup[]
	@BeforeEach
	fun initPage() {
		page = browser.newPage()
		page.setDefaultTimeout(500.0)
		page.route({ url -> url.startsWith("http://localhost") }, handler)
		page.navigate("http://localhost/index.html")
	}
	// end::setup[]

	// tag::close[]
	@AfterEach
	fun closePage() {
		page.close()
	}
	// end::close[]

	@Test
	fun testLoadSimpleHtmlPage() {
		assertThat(page).hasTitle("Playwright-MockMvc integration Test Web Page")
	}

	@Test
	fun getForm() {
		page.locator("#query").fill("spring")
		page.locator("#getForm button[type='submit']").click()

		assertThat(page.locator("#getResult")).hasText("query=spring")
	}

	@Test
	fun postForm() {
		page.locator("#username").fill("joe")
		page.locator("#message").fill("hello")
		page.locator("#postForm button[type='submit']").click()

		assertThat(page.locator("body")).hasText("username=joe,message=hello")
	}

	@Test
	fun singleFileForm() {
		page.locator("#singleFile").setInputFiles(filePayload("playwright-single.txt", "single-content"))
		page.locator("#singleFileForm button[type='submit']").click()

		assertThat(page.locator("body")).hasText("file=playwright-single.txt,content=single-content")
	}

	@Test
	fun multipleFilesForm() {
		val fileOne = filePayload("playwright-multiple-1.txt", "first-content")
		val fileTwo = filePayload("playwright-multiple-2.txt", "second-content")
		page.locator("#multipleFiles").setInputFiles(arrayOf(fileOne, fileTwo))
		page.locator("#multipleFilesForm button[type='submit']").click()

		assertThat(page.locator("body")).hasText("files=playwright-multiple-1.txt:first-content;playwright-multiple-2.txt:second-content")
	}

	@Test
	fun mixedMultipartForm() {
		page.locator("#mixedFile").setInputFiles(filePayload("playwright-mixed.txt", "mixed-content"))
		page.locator("#description").fill("sample")
		page.locator("#version").fill("7")
		page.locator("#mixedMultipartForm button[type='submit']").click()

		assertThat(page.locator("body")).hasText("file=playwright-mixed.txt,description=sample,version=7,content=mixed-content")
	}

	@Configuration
	@EnableWebMvc
	internal class Config : WebMvcConfigurer {

		@Bean
		fun multipartResolver(): MultipartResolver {
			return StandardServletMultipartResolver()
		}

		override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
			registry.addResourceHandler("/**")
				.addResourceLocations("classpath:org/springframework/test/web/servlet/playwright/content/")
		}

		@RestController
		internal class FormController {

			@GetMapping(path = ["/search"], produces = [MediaType.TEXT_PLAIN_VALUE])
			fun search(@RequestParam query: String): String {
				return "query=$query"
			}

			@PostMapping(path = ["/submit"], produces = [MediaType.TEXT_PLAIN_VALUE])
			fun submit(@RequestParam username: String, @RequestParam message: String): String {
				return "username=$username,message=$message"
			}

			@PostMapping(path = ["/upload-single"], produces = [MediaType.TEXT_PLAIN_VALUE])
			fun uploadSingle(@RequestParam("file") file: MultipartFile): String {
				return "file=${file.originalFilename},content=" +
						StreamUtils.copyToString(file.inputStream, StandardCharsets.UTF_8)
			}

			@PostMapping(path = ["/upload-multiple"], produces = [MediaType.TEXT_PLAIN_VALUE])
			fun uploadMultiple(@RequestParam("files") files: Array<MultipartFile>): String {
				val response = StringBuilder("files=")
				for (i in files.indices) {
					if (i > 0) {
						response.append(';')
					}
					response.append(files[i].originalFilename)
						.append(':')
						.append(StreamUtils.copyToString(files[i].inputStream, StandardCharsets.UTF_8))
				}
				return response.toString()
			}

			@PostMapping(path = ["/upload-mixed"], produces = [MediaType.TEXT_PLAIN_VALUE])
			fun uploadMixed(
                @RequestParam("file") file: MultipartFile,
                @RequestParam description: String,
                @RequestParam version: Int,
			): String {
				return "file=${file.originalFilename},description=$description,version=$version,content=" +
						StreamUtils.copyToString(file.inputStream, StandardCharsets.UTF_8)
			}
		}
	}

	companion object {

		private lateinit var playwright: Playwright
		private lateinit var browser: Browser

		@BeforeAll
		@JvmStatic
		fun initPlaywright() {
			playwright = Playwright.create()
			browser = playwright.chromium().launch()
		}

		@AfterAll
		@JvmStatic
		fun closePlaywright() {
			browser.close()
			playwright.close()
		}
	}
}