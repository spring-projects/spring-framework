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

package org.springframework.web.servlet.tags;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockPageContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Scott Andrews
 */
class UrlTagTests extends AbstractTagTests {

	private UrlTag tag;

	private MockPageContext context;


	@BeforeEach
	void setup() {
		context = createPageContext();
		tag = new UrlTag();
		tag.setPageContext(context);
	}


	@Test
	void paramSupport() {
		assertThat(tag).isInstanceOf(ParamAware.class);
	}

	@Test
	void doStartTag() throws JspException {
		int action = tag.doStartTag();

		assertThat(action).isEqualTo(Tag.EVAL_BODY_INCLUDE);
	}

	@Test
	void doEndTag() throws JspException {
		tag.setValue("url/path");
		tag.doStartTag();
		int action = tag.doEndTag();

		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
	}

	@Test
	void varDefaultScope() throws JspException {
		tag.setValue("url/path");
		tag.setVar("var");
		tag.doStartTag();
		tag.doEndTag();

		assertThat(context.getAttribute("var", PageContext.PAGE_SCOPE)).isEqualTo("url/path");
	}

	@Test
	void varExplicitScope() throws JspException {
		tag.setValue("url/path");
		tag.setVar("var");
		tag.setScope("request");
		tag.doStartTag();
		tag.doEndTag();

		assertThat(context.getAttribute("var", PageContext.REQUEST_SCOPE)).isEqualTo("url/path");
	}

	@Test
	void setHtmlEscapeDefault() throws JspException {
		tag.setValue("url/path");
		tag.setVar("var");
		tag.doStartTag();

		Param param = new Param();
		param.setName("n me");
		param.setValue("v&l=e");
		tag.addParam(param);

		param = new Param();
		param.setName("name");
		param.setValue("value2");
		tag.addParam(param);

		tag.doEndTag();
		assertThat(context.getAttribute("var")).isEqualTo("url/path?n%20me=v%26l%3De&name=value2");
	}

	@Test
	void setHtmlEscapeFalse() throws JspException {
		tag.setValue("url/path");
		tag.setVar("var");
		tag.setHtmlEscape(false);

		tag.doStartTag();

		Param param = new Param();
		param.setName("n me");
		param.setValue("v&l=e");
		tag.addParam(param);

		param = new Param();
		param.setName("name");
		param.setValue("value2");
		tag.addParam(param);

		tag.doEndTag();
		assertThat(context.getAttribute("var")).isEqualTo("url/path?n%20me=v%26l%3De&name=value2");
	}

	@Test
	void setHtmlEscapeTrue() throws JspException {
		tag.setValue("url/path");
		tag.setVar("var");
		tag.setHtmlEscape(true);
		tag.doStartTag();

		Param param = new Param();
		param.setName("n me");
		param.setValue("v&l=e");
		tag.addParam(param);

		param = new Param();
		param.setName("name");
		param.setValue("value2");
		tag.addParam(param);

		tag.doEndTag();
		assertThat(context.getAttribute("var")).isEqualTo("url/path?n%20me=v%26l%3De&amp;name=value2");
	}

	@Test
	void setJavaScriptEscapeTrue() throws JspException {
		tag.setValue("url/path");
		tag.setVar("var");
		tag.setJavaScriptEscape(true);
		tag.doStartTag();

		Param param = new Param();
		param.setName("n me");
		param.setValue("v&l=e");
		tag.addParam(param);

		param = new Param();
		param.setName("name");
		param.setValue("value2");
		tag.addParam(param);

		tag.doEndTag();
		assertThat(context.getAttribute("var")).isEqualTo("url\\/path?n%20me=v%26l%3De&name=value2");
	}

	@Test
	void setHtmlAndJavaScriptEscapeTrue() throws JspException {
		tag.setValue("url/path");
		tag.setVar("var");
		tag.setHtmlEscape(true);
		tag.setJavaScriptEscape(true);
		tag.doStartTag();

		Param param = new Param();
		param.setName("n me");
		param.setValue("v&l=e");
		tag.addParam(param);

		param = new Param();
		param.setName("name");
		param.setValue("value2");
		tag.addParam(param);

		tag.doEndTag();
		assertThat(context.getAttribute("var")).isEqualTo("url\\/path?n%20me=v%26l%3De&amp;name=value2");
	}

	@Test
	void createQueryStringNoParams() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		String queryString = tag.createQueryString(params, usedParams, true);
		assertThat(queryString).isEmpty();
	}

	@Test
	void createQueryStringOneParam() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		Param param = new Param();
		param.setName("name");
		param.setValue("value");
		params.add(param);

		String queryString = tag.createQueryString(params, usedParams, true);
		assertThat(queryString).isEqualTo("?name=value");
	}

	@Test
	void createQueryStringOneParamForExistingQueryString() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		Param param = new Param();
		param.setName("name");
		param.setValue("value");
		params.add(param);

		String queryString = tag.createQueryString(params, usedParams, false);
		assertThat(queryString).isEqualTo("&name=value");
	}

	@Test
	void createQueryStringOneParamEmptyValue() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		Param param = new Param();
		param.setName("name");
		param.setValue("");
		params.add(param);

		String queryString = tag.createQueryString(params, usedParams, true);
		assertThat(queryString).isEqualTo("?name=");
	}

	@Test
	void createQueryStringOneParamNullValue() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		Param param = new Param();
		param.setName("name");
		param.setValue(null);
		params.add(param);

		String queryString = tag.createQueryString(params, usedParams, true);
		assertThat(queryString).isEqualTo("?name");
	}

	@Test
	void createQueryStringOneParamAlreadyUsed() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		Param param = new Param();
		param.setName("name");
		param.setValue("value");
		params.add(param);
		usedParams.add("name");

		String queryString = tag.createQueryString(params, usedParams, true);
		assertThat(queryString).isEmpty();
	}

	@Test
	void createQueryStringTwoParams() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		Param param = new Param();
		param.setName("name");
		param.setValue("value");
		params.add(param);

		param = new Param();
		param.setName("name");
		param.setValue("value2");
		params.add(param);

		String queryString = tag.createQueryString(params, usedParams, true);
		assertThat(queryString).isEqualTo("?name=value&name=value2");
	}

	@Test
	void createQueryStringUrlEncoding() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		Param param = new Param();
		param.setName("n me");
		param.setValue("v&l=e");
		params.add(param);

		param = new Param();
		param.setName("name");
		param.setValue("value2");
		params.add(param);

		String queryString = tag.createQueryString(params, usedParams, true);
		assertThat(queryString).isEqualTo("?n%20me=v%26l%3De&name=value2");
	}

	@Test
	void createQueryStringParamNullName() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		Param param = new Param();
		param.setName(null);
		param.setValue("value");
		params.add(param);

		String queryString = tag.createQueryString(params, usedParams, true);
		assertThat(queryString).isEmpty();
	}

	@Test
	void createQueryStringParamEmptyName() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		Param param = new Param();
		param.setName("");
		param.setValue("value");
		params.add(param);

		String queryString = tag.createQueryString(params, usedParams, true);
		assertThat(queryString).isEmpty();
	}

	@Test
	void replaceUriTemplateParamsNoParams() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		String uri = tag.replaceUriTemplateParams("url/path", params, usedParams);
		assertThat(uri).isEqualTo("url/path");
		assertThat(usedParams).isEmpty();
	}

	@Test
	void replaceUriTemplateParamsTemplateWithoutParamMatch() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		String uri = tag.replaceUriTemplateParams("url/{path}", params, usedParams);
		assertThat(uri).isEqualTo("url/{path}");
		assertThat(usedParams).isEmpty();
	}

	@Test
	void replaceUriTemplateParamsTemplateWithParamMatch() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		Param param = new Param();
		param.setName("name");
		param.setValue("value");
		params.add(param);

		String uri = tag.replaceUriTemplateParams("url/{name}", params, usedParams);
		assertThat(uri).isEqualTo("url/value");
		assertThat(usedParams).hasSize(1);
		assertThat(usedParams).contains("name");
	}

	@Test
	void replaceUriTemplateParamsTemplateWithParamMatchNamePreEncoding() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		Param param = new Param();
		param.setName("n me");
		param.setValue("value");
		params.add(param);

		String uri = tag.replaceUriTemplateParams("url/{n me}", params, usedParams);
		assertThat(uri).isEqualTo("url/value");
		assertThat(usedParams).hasSize(1);
		assertThat(usedParams).contains("n me");
	}

	@Test
	void replaceUriTemplateParamsTemplateWithParamMatchValueEncoded() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		Param param = new Param();
		param.setName("name");
		param.setValue("v lue");
		params.add(param);

		String uri = tag.replaceUriTemplateParams("url/{name}", params,
				usedParams);

		assertThat(uri).isEqualTo("url/v%20lue");
		assertThat(usedParams).hasSize(1);
		assertThat(usedParams).contains("name");
	}

	@Test  // SPR-11401
	public void replaceUriTemplateParamsTemplateWithPathSegment() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		Param param = new Param();
		param.setName("name");
		param.setValue("my/Id");
		params.add(param);

		String uri = tag.replaceUriTemplateParams("url/{/name}", params, usedParams);

		assertThat(uri).isEqualTo("url/my%2FId");
		assertThat(usedParams).hasSize(1);
		assertThat(usedParams).contains("name");
	}

	@Test
	void replaceUriTemplateParamsTemplateWithPath() throws JspException {
		List<Param> params = new ArrayList<>();
		Set<String> usedParams = new HashSet<>();

		Param param = new Param();
		param.setName("name");
		param.setValue("my/Id");
		params.add(param);

		String uri = tag.replaceUriTemplateParams("url/{name}", params, usedParams);
		assertThat(uri).isEqualTo("url/my/Id");
		assertThat(usedParams).hasSize(1);
		assertThat(usedParams).contains("name");
	}

	@Test
	void createUrlRemoteServer() throws JspException {
		tag.setValue("https://www.springframework.org/");
		tag.doStartTag();

		String uri = tag.createUrl();
		assertThat(uri).isEqualTo("https://www.springframework.org/");
	}

	@Test
	void createUrlRelative() throws JspException {
		tag.setValue("url/path");
		tag.doStartTag();

		String uri = tag.createUrl();
		assertThat(uri).isEqualTo("url/path");
	}

	@Test
	void createUrlLocalContext() throws JspException {
		((MockHttpServletRequest) context.getRequest()).setContextPath("/app-context");

		tag.setValue("/url/path");
		tag.doStartTag();

		String uri = tag.createUrl();
		assertThat(uri).isEqualTo("/app-context/url/path");
	}

	@Test
	void createUrlRemoteContext() throws JspException {
		((MockHttpServletRequest) context.getRequest()).setContextPath("/app-context");

		tag.setValue("/url/path");
		tag.setContext("some-other-context");
		tag.doStartTag();

		String uri = tag.createUrl();
		assertThat(uri).isEqualTo("/some-other-context/url/path");
	}

	@Test
	void createUrlRemoteContextWithSlash() throws JspException {
		((MockHttpServletRequest) context.getRequest()).setContextPath("/app-context");

		tag.setValue("/url/path");
		tag.setContext("/some-other-context");
		tag.doStartTag();

		String uri = tag.createUrl();
		assertThat(uri).isEqualTo("/some-other-context/url/path");
	}

	@Test
	void createUrlRemoteContextSingleSlash() throws JspException {
		((MockHttpServletRequest) context.getRequest()).setContextPath("/app-context");

		tag.setValue("/url/path");
		tag.setContext("/");
		tag.doStartTag();

		String uri = tag.createUrl();
		assertThat(uri).isEqualTo("/url/path");
	}

	@Test
	void createUrlWithParams() throws JspException {
		tag.setValue("url/path");
		tag.doStartTag();

		Param param = new Param();
		param.setName("name");
		param.setValue("value");
		tag.addParam(param);

		param = new Param();
		param.setName("n me");
		param.setValue("v lue");
		tag.addParam(param);

		String uri = tag.createUrl();
		assertThat(uri).isEqualTo("url/path?name=value&n%20me=v%20lue");
	}

	@Test
	void createUrlWithTemplateParams() throws JspException {
		tag.setValue("url/{name}");
		tag.doStartTag();

		Param param = new Param();
		param.setName("name");
		param.setValue("value");
		tag.addParam(param);

		param = new Param();
		param.setName("n me");
		param.setValue("v lue");
		tag.addParam(param);

		String uri = tag.createUrl();
		assertThat(uri).isEqualTo("url/value?n%20me=v%20lue");
	}

	@Test
	void createUrlWithParamAndExistingQueryString() throws JspException {
		tag.setValue("url/path?foo=bar");
		tag.doStartTag();

		Param param = new Param();
		param.setName("name");
		param.setValue("value");
		tag.addParam(param);

		String uri = tag.createUrl();
		assertThat(uri).isEqualTo("url/path?foo=bar&name=value");
	}

}
