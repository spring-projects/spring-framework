package org.springframework.web.servlet.resource;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link AbstractVersionStrategy.UrlParameterVersionStrategy}.
 *
 * @author Zsolt Fatér
 */
public class AbstractVersionStrategyUrlParameterVersionStrategyTest {

	private static final String VERSION = "1.0.0";
	private static final String VERSION_ATTRIBUTE = "v=" + VERSION;
	private static final String JSESSIONID = ";jsessionid=123456";
	private static final String OTHER_ATTRIBUTE = "other=other";

	private static final String ADD_URL_SIMPLE = "http://localhost/project/css/test.css";
	private static final String EXPECTED_ADD_URL_SIMPLE = ADD_URL_SIMPLE + "?" + VERSION_ATTRIBUTE;
	private static final String ADD_URL_JSESSION = ADD_URL_SIMPLE + JSESSIONID;
	private static final String EXPECTED_ADD_URL_JSESSION = ADD_URL_SIMPLE + "?" + VERSION_ATTRIBUTE + JSESSIONID;
	private static final String ADD_URL_OTHER = ADD_URL_SIMPLE + "?" + OTHER_ATTRIBUTE;
	private static final String EXPECTED_ADD_URL_OTHER = ADD_URL_OTHER + "&" + VERSION_ATTRIBUTE;
	private static final String ADD_URL_OTHER_JSESSION = ADD_URL_SIMPLE + "?" + OTHER_ATTRIBUTE + JSESSIONID;
	private static final String EXPECTED_ADD_URL_OTHER_JSESSION = ADD_URL_OTHER + "&" + VERSION_ATTRIBUTE + JSESSIONID;
	private static final String ADD_URL_OTHER_OTHER = ADD_URL_SIMPLE + "?" + OTHER_ATTRIBUTE + "&" + OTHER_ATTRIBUTE;
	private static final String EXPECTED_ADD_URL_OTHER_OTHER = ADD_URL_OTHER_OTHER + "&" + VERSION_ATTRIBUTE;
	private static final String ADD_URL_OTHER_OTHER_JSESSION = ADD_URL_OTHER_OTHER + JSESSIONID;
	private static final String EXPECTED_ADD_URL_OTHER_OTHER_JSESSION = ADD_URL_OTHER_OTHER + "&" + VERSION_ATTRIBUTE + JSESSIONID;

	private static final String EXTRACT_URL = ADD_URL_OTHER + "&" + VERSION_ATTRIBUTE + "&" + OTHER_ATTRIBUTE;
	private static final String EXTRACT_URL_JSESSION = EXTRACT_URL + JSESSIONID;

	private static final String REMOVE_URL_VERSION_OTHER = EXPECTED_ADD_URL_SIMPLE + "&" + OTHER_ATTRIBUTE;
	private static final String REMOVE_URL_VERSION_OTHER_JSESSION = REMOVE_URL_VERSION_OTHER + JSESSIONID;
	private static final String REMOVE_URL_OTHER_VERSION_OTHER = EXPECTED_ADD_URL_OTHER + "&" + OTHER_ATTRIBUTE;
	private static final String REMOVE_URL_OTHER_VERSION_OTHER_JSESSION = REMOVE_URL_OTHER_VERSION_OTHER + JSESSIONID;

	private static final AbstractVersionStrategy.UrlParameterVersionStrategy URL_PARAMETER_VERSION_STRATEGY = new AbstractVersionStrategy.UrlParameterVersionStrategy();

	@Test
	public void addTest() {
		Assert.assertEquals(EXPECTED_ADD_URL_SIMPLE, URL_PARAMETER_VERSION_STRATEGY.addVersion(ADD_URL_SIMPLE, VERSION));
		Assert.assertEquals(EXPECTED_ADD_URL_JSESSION, URL_PARAMETER_VERSION_STRATEGY.addVersion(ADD_URL_JSESSION, VERSION));
		Assert.assertEquals(EXPECTED_ADD_URL_OTHER, URL_PARAMETER_VERSION_STRATEGY.addVersion(ADD_URL_OTHER, VERSION));
		Assert.assertEquals(EXPECTED_ADD_URL_OTHER_JSESSION, URL_PARAMETER_VERSION_STRATEGY.addVersion(ADD_URL_OTHER_JSESSION, VERSION));
		Assert.assertEquals(EXPECTED_ADD_URL_OTHER_OTHER, URL_PARAMETER_VERSION_STRATEGY.addVersion(ADD_URL_OTHER_OTHER, VERSION));
		Assert.assertEquals(EXPECTED_ADD_URL_OTHER_OTHER_JSESSION, URL_PARAMETER_VERSION_STRATEGY.addVersion(ADD_URL_OTHER_OTHER_JSESSION, VERSION));
	}

	@Test
	public void extractTest() {
		Assert.assertNull(URL_PARAMETER_VERSION_STRATEGY.extractVersion(ADD_URL_SIMPLE));
		Assert.assertEquals(VERSION, URL_PARAMETER_VERSION_STRATEGY.extractVersion(EXPECTED_ADD_URL_SIMPLE));
		Assert.assertEquals(VERSION, URL_PARAMETER_VERSION_STRATEGY.extractVersion(EXPECTED_ADD_URL_JSESSION));
		Assert.assertEquals(VERSION, URL_PARAMETER_VERSION_STRATEGY.extractVersion(EXPECTED_ADD_URL_OTHER));
		Assert.assertEquals(VERSION, URL_PARAMETER_VERSION_STRATEGY.extractVersion(EXPECTED_ADD_URL_OTHER_JSESSION));
		Assert.assertEquals(VERSION, URL_PARAMETER_VERSION_STRATEGY.extractVersion(EXTRACT_URL));
		Assert.assertEquals(VERSION, URL_PARAMETER_VERSION_STRATEGY.extractVersion(EXTRACT_URL_JSESSION));
	}

	@Test
	public void removeTest() {
		Assert.assertEquals(ADD_URL_SIMPLE, URL_PARAMETER_VERSION_STRATEGY.removeVersion(ADD_URL_SIMPLE, VERSION));
		Assert.assertEquals(ADD_URL_SIMPLE, URL_PARAMETER_VERSION_STRATEGY.removeVersion(EXPECTED_ADD_URL_SIMPLE, VERSION));
		Assert.assertEquals(ADD_URL_JSESSION, URL_PARAMETER_VERSION_STRATEGY.removeVersion(EXPECTED_ADD_URL_JSESSION, VERSION));
		Assert.assertEquals(ADD_URL_OTHER, URL_PARAMETER_VERSION_STRATEGY.removeVersion(EXPECTED_ADD_URL_OTHER, VERSION));
		Assert.assertEquals(ADD_URL_OTHER_JSESSION, URL_PARAMETER_VERSION_STRATEGY.removeVersion(EXPECTED_ADD_URL_OTHER_JSESSION, VERSION));
		Assert.assertEquals(ADD_URL_OTHER, URL_PARAMETER_VERSION_STRATEGY.removeVersion(REMOVE_URL_VERSION_OTHER, VERSION));
		Assert.assertEquals(ADD_URL_OTHER_JSESSION, URL_PARAMETER_VERSION_STRATEGY.removeVersion(REMOVE_URL_VERSION_OTHER_JSESSION, VERSION));
		Assert.assertEquals(ADD_URL_OTHER_OTHER, URL_PARAMETER_VERSION_STRATEGY.removeVersion(REMOVE_URL_OTHER_VERSION_OTHER, VERSION));
		Assert.assertEquals(ADD_URL_OTHER_OTHER_JSESSION, URL_PARAMETER_VERSION_STRATEGY.removeVersion(REMOVE_URL_OTHER_VERSION_OTHER_JSESSION, VERSION));
	}
}
