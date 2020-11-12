/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.util;

import java.io.UnsupportedEncodingException;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link UrlPathHelper}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Costin Leau
 */
public class UrlPathHelperTests {

	private static final String WEBSPHERE_URI_ATTRIBUTE = "com.ibm.websphere.servlet.uri_non_decoded";

	private final UrlPathHelper helper = new UrlPathHelper();

	private final MockHttpServletRequest request = new MockHttpServletRequest();


	@Test
	public void getPathWithinApplication() {
		request.setContextPath("/petclinic");
		request.setRequestURI("/petclinic/welcome.html");

		assertEquals("Incorrect path returned", "/welcome.html", helper.getPathWithinApplication(request));
	}

	@Test
	public void getPathWithinApplicationForRootWithNoLeadingSlash() {
		request.setContextPath("/petclinic");
		request.setRequestURI("/petclinic");

		assertEquals("Incorrect root path returned", "/", helper.getPathWithinApplication(request));
	}

	@Test
	public void getPathWithinApplicationForSlashContextPath() {
		request.setContextPath("/");
		request.setRequestURI("/welcome.html");

		assertEquals("Incorrect path returned", "/welcome.html", helper.getPathWithinApplication(request));
	}

	@Test
	public void getPathWithinServlet() {
		request.setContextPath("/petclinic");
		request.setServletPath("/main");
		request.setRequestURI("/petclinic/main/welcome.html");

		assertEquals("Incorrect path returned", "/welcome.html", helper.getPathWithinServletMapping(request));
	}

	@Test
	public void alwaysUseFullPath() {
		helper.setAlwaysUseFullPath(true);
		request.setContextPath("/petclinic");
		request.setServletPath("/main");
		request.setRequestURI("/petclinic/main/welcome.html");

		assertEquals("Incorrect path returned", "/main/welcome.html", helper.getLookupPathForRequest(request));
	}

	// SPR-11101

	@Test
	public void getPathWithinServletWithoutUrlDecoding() {
		request.setContextPath("/SPR-11101");
		request.setServletPath("/test_url_decoding/a/b");
		request.setRequestURI("/test_url_decoding/a%2Fb");

		helper.setUrlDecode(false);
		String actual = helper.getPathWithinServletMapping(request);

		assertEquals("/test_url_decoding/a%2Fb", actual);
	}

	@Test
	public void getRequestUri() {
		request.setRequestURI("/welcome.html");
		assertEquals("Incorrect path returned", "/welcome.html", helper.getRequestUri(request));

		request.setRequestURI("/foo%20bar");
		assertEquals("Incorrect path returned", "/foo bar", helper.getRequestUri(request));

		request.setRequestURI("/foo+bar");
		assertEquals("Incorrect path returned", "/foo+bar", helper.getRequestUri(request));
	}

	@Test
	public void getRequestRemoveSemicolonContent() throws UnsupportedEncodingException {
		helper.setRemoveSemicolonContent(true);

		request.setRequestURI("/foo;f=F;o=O;o=O/bar;b=B;a=A;r=R");
		assertEquals("/foo/bar", helper.getRequestUri(request));

		// SPR-13455

		request.setServletPath("/foo/1");
		request.setRequestURI("/foo/;test/1");

		assertEquals("/foo/1", helper.getRequestUri(request));
	}

	@Test
	public void getRequestKeepSemicolonContent() {
		helper.setRemoveSemicolonContent(false);

		testKeepSemicolonContent("/foo;a=b;c=d", "/foo;a=b;c=d");
		testKeepSemicolonContent("/test;jsessionid=1234", "/test");
		testKeepSemicolonContent("/test;JSESSIONID=1234", "/test");
		testKeepSemicolonContent("/test;jsessionid=1234;a=b", "/test;a=b");
		testKeepSemicolonContent("/test;a=b;jsessionid=1234;c=d", "/test;a=b;c=d");
		testKeepSemicolonContent("/test;jsessionid=1234/anotherTest", "/test/anotherTest");
		testKeepSemicolonContent("/test;jsessionid=;a=b", "/test;a=b");
		testKeepSemicolonContent("/somethingLongerThan12;jsessionid=1234", "/somethingLongerThan12");
	}

	private void testKeepSemicolonContent(String requestUri, String expectedPath) {
		request.setRequestURI(requestUri);
		assertEquals(expectedPath, helper.getRequestUri(request));
	}

	@Test
	public void getLookupPathWithSemicolonContent() {
		helper.setRemoveSemicolonContent(false);

		request.setContextPath("/petclinic");
		request.setServletPath("/main");
		request.setRequestURI("/petclinic;a=b/main;b=c/welcome.html;c=d");

		assertEquals("/welcome.html;c=d", helper.getLookupPathForRequest(request));
	}

	@Test
	public void getLookupPathWithSemicolonContentAndNullPathInfo() {
		helper.setRemoveSemicolonContent(false);

		request.setContextPath("/petclinic");
		request.setServletPath("/welcome.html");
		request.setRequestURI("/petclinic;a=b/welcome.html;c=d");

		assertEquals("/welcome.html;c=d", helper.getLookupPathForRequest(request));
	}


	//
	// suite of tests root requests for default servlets (SRV 11.2) on Websphere vs Tomcat and other containers
	// see: https://jira.springframework.org/browse/SPR-7064
	//


	//
	// / mapping (default servlet)
	//

	@Test
	public void tomcatDefaultServletRoot() throws Exception {
		request.setContextPath("/test");
		request.setPathInfo(null);
		request.setServletPath("/");
		request.setRequestURI("/test/");
		assertEquals("/", helper.getLookupPathForRequest(request));
	}

	@Test
	public void tomcatDefaultServletFile() throws Exception {
		request.setContextPath("/test");
		request.setPathInfo(null);
		request.setServletPath("/foo");
		request.setRequestURI("/test/foo");

		assertEquals("/foo", helper.getLookupPathForRequest(request));
	}

	@Test
	public void tomcatDefaultServletFolder() throws Exception {
		request.setContextPath("/test");
		request.setPathInfo(null);
		request.setServletPath("/foo/");
		request.setRequestURI("/test/foo/");

		assertEquals("/foo/", helper.getLookupPathForRequest(request));
	}

	//SPR-12372 & SPR-13455
	@Test
	public void removeDuplicateSlashesInPath() throws Exception {
		request.setContextPath("/SPR-12372");
		request.setPathInfo(null);
		request.setServletPath("/foo/bar/");
		request.setRequestURI("/SPR-12372/foo//bar/");

		assertEquals("/foo/bar/", helper.getLookupPathForRequest(request));

		request.setServletPath("/foo/bar/");
		request.setRequestURI("/SPR-12372/foo/bar//");

		assertEquals("/foo/bar/", helper.getLookupPathForRequest(request));

		// "normal" case
		request.setServletPath("/foo/bar//");
		request.setRequestURI("/SPR-12372/foo/bar//");

		assertEquals("/foo/bar//", helper.getLookupPathForRequest(request));
	}

	@Test
	public void wasDefaultServletRoot() throws Exception {
		request.setContextPath("/test");
		request.setPathInfo("/");
		request.setServletPath("");
		request.setRequestURI("/test/");
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/");

		assertEquals("/", helper.getLookupPathForRequest(request));
	}

	@Test
	public void wasDefaultServletRootWithCompliantSetting() throws Exception {
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/");
		tomcatDefaultServletRoot();
	}

	@Test
	public void wasDefaultServletFile() throws Exception {
		request.setContextPath("/test");
		request.setPathInfo("/foo");
		request.setServletPath("");
		request.setRequestURI("/test/foo");
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo");

		assertEquals("/foo", helper.getLookupPathForRequest(request));
	}

	@Test
	public void wasDefaultServletFileWithCompliantSetting() throws Exception {
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo");
		tomcatDefaultServletFile();
	}

	@Test
	public void wasDefaultServletFolder() throws Exception {
		request.setContextPath("/test");
		request.setPathInfo("/foo/");
		request.setServletPath("");
		request.setRequestURI("/test/foo/");
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/");

		assertEquals("/foo/", helper.getLookupPathForRequest(request));
	}

	@Test
	public void wasDefaultServletFolderWithCompliantSetting() throws Exception {
		UrlPathHelper.websphereComplianceFlag = true;
		try {
			request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/");
			tomcatDefaultServletFolder();
		}
		finally {
			UrlPathHelper.websphereComplianceFlag = false;
		}
	}


	//
	// /foo/* mapping
	//

	@Test
	public void tomcatCasualServletRoot() throws Exception {
		request.setContextPath("/test");
		request.setPathInfo("/");
		request.setServletPath("/foo");
		request.setRequestURI("/test/foo/");

		assertEquals("/", helper.getLookupPathForRequest(request));
	}

	// test the root mapping for /foo/* w/o a trailing slash - <host>/<context>/foo
	@Test @Ignore
	public void tomcatCasualServletRootWithMissingSlash() throws Exception {
		request.setContextPath("/test");
		request.setPathInfo(null);
		request.setServletPath("/foo");
		request.setRequestURI("/test/foo");

		assertEquals("/", helper.getLookupPathForRequest(request));
	}

	@Test
	public void tomcatCasualServletFile() throws Exception {
		request.setContextPath("/test");
		request.setPathInfo("/foo");
		request.setServletPath("/foo");
		request.setRequestURI("/test/foo/foo");

		assertEquals("/foo", helper.getLookupPathForRequest(request));
	}

	@Test
	public void tomcatCasualServletFolder() throws Exception {
		request.setContextPath("/test");
		request.setPathInfo("/foo/");
		request.setServletPath("/foo");
		request.setRequestURI("/test/foo/foo/");

		assertEquals("/foo/", helper.getLookupPathForRequest(request));
	}

	@Test
	public void wasCasualServletRoot() throws Exception {
		request.setContextPath("/test");
		request.setPathInfo(null);
		request.setServletPath("/foo/");
		request.setRequestURI("/test/foo/");
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/");

		assertEquals("/", helper.getLookupPathForRequest(request));
	}

	@Test
	public void wasCasualServletRootWithCompliantSetting() throws Exception {
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/");
		tomcatCasualServletRoot();
	}

	// test the root mapping for /foo/* w/o a trailing slash - <host>/<context>/foo
	@Ignore
	@Test
	public void wasCasualServletRootWithMissingSlash() throws Exception {
		request.setContextPath("/test");
		request.setPathInfo(null);
		request.setServletPath("/foo");
		request.setRequestURI("/test/foo");
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo");

		assertEquals("/", helper.getLookupPathForRequest(request));
	}

	@Ignore
	@Test
	public void wasCasualServletRootWithMissingSlashWithCompliantSetting() throws Exception {
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo");
		tomcatCasualServletRootWithMissingSlash();
	}

	@Test
	public void wasCasualServletFile() throws Exception {
		request.setContextPath("/test");
		request.setPathInfo("/foo");
		request.setServletPath("/foo");
		request.setRequestURI("/test/foo/foo");
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/foo");

		assertEquals("/foo", helper.getLookupPathForRequest(request));
	}

	@Test
	public void wasCasualServletFileWithCompliantSetting() throws Exception {
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/foo");
		tomcatCasualServletFile();
	}

	@Test
	public void wasCasualServletFolder() throws Exception {
		request.setContextPath("/test");
		request.setPathInfo("/foo/");
		request.setServletPath("/foo");
		request.setRequestURI("/test/foo/foo/");
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/foo/");

		assertEquals("/foo/", helper.getLookupPathForRequest(request));
	}

	@Test
	public void wasCasualServletFolderWithCompliantSetting() throws Exception {
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/foo/");
		tomcatCasualServletFolder();
	}

	@Test
	public void getOriginatingRequestUri() {
		request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/path");
		request.setRequestURI("/forwarded");
		assertEquals("/path", helper.getOriginatingRequestUri(request));
	}

	@Test
	public void getOriginatingRequestUriWebsphere() {
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/path");
		request.setRequestURI("/forwarded");
		assertEquals("/path", helper.getOriginatingRequestUri(request));
	}

	@Test
	public void getOriginatingRequestUriDefault() {
		request.setRequestURI("/forwarded");
		assertEquals("/forwarded", helper.getOriginatingRequestUri(request));
	}

	@Test
	public void getOriginatingQueryString() {
		request.setQueryString("forward=on");
		request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/path");
		request.setAttribute(WebUtils.FORWARD_QUERY_STRING_ATTRIBUTE, "original=on");
		assertEquals("original=on", this.helper.getOriginatingQueryString(request));
	}

	@Test
	public void getOriginatingQueryStringNotPresent() {
		request.setQueryString("forward=true");
		assertEquals("forward=true", this.helper.getOriginatingQueryString(request));
	}

	@Test
	public void getOriginatingQueryStringIsNull() {
		request.setQueryString("forward=true");
		request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/path");
		assertNull(this.helper.getOriginatingQueryString(request));
	}

}
