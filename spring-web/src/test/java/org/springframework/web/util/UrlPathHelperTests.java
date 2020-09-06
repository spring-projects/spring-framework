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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

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

		assertThat(helper.getPathWithinApplication(request)).isEqualTo("/welcome.html");
	}

	@Test
	public void getPathWithinApplicationForRootWithNoLeadingSlash() {
		request.setContextPath("/petclinic");
		request.setRequestURI("/petclinic");

		assertThat(helper.getPathWithinApplication(request)).as("Incorrect root path returned").isEqualTo("/");
	}

	@Test
	public void getPathWithinApplicationForSlashContextPath() {
		request.setContextPath("/");
		request.setRequestURI("/welcome.html");

		assertThat(helper.getPathWithinApplication(request)).isEqualTo("/welcome.html");
	}

	@Test
	public void getPathWithinServlet() {
		request.setContextPath("/petclinic");
		request.setServletPath("/main");
		request.setRequestURI("/petclinic/main/welcome.html");

		assertThat(helper.getPathWithinServletMapping(request)).isEqualTo("/welcome.html");
	}

	@Test
	public void alwaysUseFullPath() {
		helper.setAlwaysUseFullPath(true);
		request.setContextPath("/petclinic");
		request.setServletPath("/main");
		request.setRequestURI("/petclinic/main/welcome.html");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/main/welcome.html");
	}

	@Test // SPR-11101
	public void getPathWithinServletWithoutUrlDecoding() {
		request.setContextPath("/SPR-11101");
		request.setServletPath("/test_url_decoding/a/b");
		request.setRequestURI("/test_url_decoding/a%2Fb");

		helper.setUrlDecode(false);
		String actual = helper.getPathWithinServletMapping(request);

		assertThat(actual).isEqualTo("/test_url_decoding/a%2Fb");
	}

	@Test
	public void getRequestUri() {
		request.setRequestURI("/welcome.html");
		assertThat(helper.getRequestUri(request)).isEqualTo("/welcome.html");

		request.setRequestURI("/foo%20bar");
		assertThat(helper.getRequestUri(request)).isEqualTo("/foo bar");

		request.setRequestURI("/foo+bar");
		assertThat(helper.getRequestUri(request)).isEqualTo("/foo+bar");
	}

	@Test
	public void getRequestRemoveSemicolonContent() {
		helper.setRemoveSemicolonContent(true);
		request.setRequestURI("/foo;f=F;o=O;o=O/bar;b=B;a=A;r=R");
		assertThat(helper.getRequestUri(request)).isEqualTo("/foo/bar");

		// SPR-13455
		request.setRequestURI("/foo/;test/1");
		request.setServletPath("/foo/1");
		assertThat(helper.getRequestUri(request)).isEqualTo("/foo/1");
	}

	@Test
	public void getRequestKeepSemicolonContent() {
		helper.setRemoveSemicolonContent(false);

		request.setRequestURI("/foo;a=b;c=d");
		assertThat(helper.getRequestUri(request)).isEqualTo("/foo;a=b;c=d");

		request.setRequestURI("/foo;jsessionid=c0o7fszeb1");
		assertThat(helper.getRequestUri(request)).as("jsessionid should always be removed").isEqualTo("/foo");

		request.setRequestURI("/foo;a=b;jsessionid=c0o7fszeb1;c=d");
		assertThat(helper.getRequestUri(request)).as("jsessionid should always be removed").isEqualTo("/foo;a=b;c=d");

		// SPR-10398
		request.setRequestURI("/foo;a=b;JSESSIONID=c0o7fszeb1;c=d");
		assertThat(helper.getRequestUri(request)).as("JSESSIONID should always be removed").isEqualTo("/foo;a=b;c=d");
	}

	@Test
	public void getLookupPathWithSemicolonContent() {
		helper.setRemoveSemicolonContent(false);

		request.setContextPath("/petclinic");
		request.setServletPath("/main");
		request.setRequestURI("/petclinic;a=b/main;b=c/welcome.html;c=d");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/welcome.html;c=d");
	}

	@Test
	public void getLookupPathWithSemicolonContentAndNullPathInfo() {
		helper.setRemoveSemicolonContent(false);

		request.setContextPath("/petclinic");
		request.setServletPath("/welcome.html");
		request.setRequestURI("/petclinic;a=b/welcome.html;c=d");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/welcome.html;c=d");
	}


	//
	// suite of tests root requests for default servlets (SRV 11.2) on Websphere vs Tomcat and other containers
	// see: https://jira.springframework.org/browse/SPR-7064
	//


	//
	// / mapping (default servlet)
	//

	@Test
	public void tomcatDefaultServletRoot() {
		request.setContextPath("/test");
		request.setServletPath("/");
		request.setPathInfo(null);
		request.setRequestURI("/test/");
		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/");
	}

	@Test
	public void tomcatDefaultServletFile() {
		request.setContextPath("/test");
		request.setServletPath("/foo");
		request.setPathInfo(null);
		request.setRequestURI("/test/foo");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo");
	}

	@Test
	public void tomcatDefaultServletFolder() {
		request.setContextPath("/test");
		request.setServletPath("/foo/");
		request.setPathInfo(null);
		request.setRequestURI("/test/foo/");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo/");
	}

	@Test //SPR-12372, SPR-13455
	public void removeDuplicateSlashesInPath() {
		request.setContextPath("/SPR-12372");
		request.setPathInfo(null);
		request.setServletPath("/foo/bar/");
		request.setRequestURI("/SPR-12372/foo//bar/");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo/bar/");

		request.setServletPath("/foo/bar/");
		request.setRequestURI("/SPR-12372/foo/bar//");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo/bar/");

		// "normal" case
		request.setServletPath("/foo/bar//");
		request.setRequestURI("/SPR-12372/foo/bar//");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo/bar//");
	}

	@Test
	public void wasDefaultServletRoot() {
		request.setContextPath("/test");
		request.setPathInfo("/");
		request.setServletPath("");
		request.setRequestURI("/test/");
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/");
	}

	@Test
	public void wasDefaultServletRootWithCompliantSetting() {
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/");
		tomcatDefaultServletRoot();
	}

	@Test
	public void wasDefaultServletFile() {
		request.setContextPath("/test");
		request.setPathInfo("/foo");
		request.setServletPath("");
		request.setRequestURI("/test/foo");
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo");
	}

	@Test
	public void wasDefaultServletFileWithCompliantSetting() {
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo");
		tomcatDefaultServletFile();
	}

	@Test
	public void wasDefaultServletFolder() {
		request.setContextPath("/test");
		request.setPathInfo("/foo/");
		request.setServletPath("");
		request.setRequestURI("/test/foo/");
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo/");
	}

	@Test
	public void wasDefaultServletFolderWithCompliantSetting() {
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
	public void tomcatCasualServletRoot() {
		request.setContextPath("/test");
		request.setPathInfo("/");
		request.setServletPath("/foo");
		request.setRequestURI("/test/foo/");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/");
	}

	@Disabled
	// test the root mapping for /foo/* w/o a trailing slash - <host>/<context>/foo
	@Test
	public void tomcatCasualServletRootWithMissingSlash() {
		request.setContextPath("/test");
		request.setPathInfo(null);
		request.setServletPath("/foo");
		request.setRequestURI("/test/foo");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/");
	}

	@Test
	public void tomcatCasualServletFile() {
		request.setContextPath("/test");
		request.setPathInfo("/foo");
		request.setServletPath("/foo");
		request.setRequestURI("/test/foo/foo");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo");
	}

	@Test
	public void tomcatCasualServletFolder() {
		request.setContextPath("/test");
		request.setPathInfo("/foo/");
		request.setServletPath("/foo");
		request.setRequestURI("/test/foo/foo/");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo/");
	}

	@Test
	public void wasCasualServletRoot() {
		request.setContextPath("/test");
		request.setPathInfo(null);
		request.setServletPath("/foo/");
		request.setRequestURI("/test/foo/");
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/");
	}

	@Test
	public void wasCasualServletRootWithCompliantSetting() {
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/");
		tomcatCasualServletRoot();
	}

	@Disabled
	// test the root mapping for /foo/* w/o a trailing slash - <host>/<context>/foo
	@Test
	public void wasCasualServletRootWithMissingSlash() {
		request.setContextPath("/test");
		request.setPathInfo(null);
		request.setServletPath("/foo");
		request.setRequestURI("/test/foo");
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/");
	}

	@Disabled
	@Test
	public void wasCasualServletRootWithMissingSlashWithCompliantSetting() {
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo");
		tomcatCasualServletRootWithMissingSlash();
	}

	@Test
	public void wasCasualServletFile() {
		request.setContextPath("/test");
		request.setPathInfo("/foo");
		request.setServletPath("/foo");
		request.setRequestURI("/test/foo/foo");
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/foo");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo");
	}

	@Test
	public void wasCasualServletFileWithCompliantSetting() {
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/foo");
		tomcatCasualServletFile();
	}

	@Test
	public void wasCasualServletFolder() {
		request.setContextPath("/test");
		request.setPathInfo("/foo/");
		request.setServletPath("/foo");
		request.setRequestURI("/test/foo/foo/");
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/foo/");

		assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo/");
	}

	@Test
	public void wasCasualServletFolderWithCompliantSetting() {
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/test/foo/foo/");
		tomcatCasualServletFolder();
	}

	@Test
	public void getOriginatingRequestUri() {
		request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/path");
		request.setRequestURI("/forwarded");
		assertThat(helper.getOriginatingRequestUri(request)).isEqualTo("/path");
	}

	@Test
	public void getOriginatingRequestUriWebsphere() {
		request.setAttribute(WEBSPHERE_URI_ATTRIBUTE, "/path");
		request.setRequestURI("/forwarded");
		assertThat(helper.getOriginatingRequestUri(request)).isEqualTo("/path");
	}

	@Test
	public void getOriginatingRequestUriDefault() {
		request.setRequestURI("/forwarded");
		assertThat(helper.getOriginatingRequestUri(request)).isEqualTo("/forwarded");
	}

	@Test
	public void getOriginatingQueryString() {
		request.setQueryString("forward=on");
		request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/path");
		request.setAttribute(WebUtils.FORWARD_QUERY_STRING_ATTRIBUTE, "original=on");
		assertThat(this.helper.getOriginatingQueryString(request)).isEqualTo("original=on");
	}

	@Test
	public void getOriginatingQueryStringNotPresent() {
		request.setQueryString("forward=true");
		assertThat(this.helper.getOriginatingQueryString(request)).isEqualTo("forward=true");
	}

	@Test
	public void getOriginatingQueryStringIsNull() {
		request.setQueryString("forward=true");
		request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/path");
		assertThat(this.helper.getOriginatingQueryString(request)).isNull();
	}

}
