package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;

/**
 * A {@code VersionStrategy} that relies on a fixed version applied as a request
 * parameter
 *
 * <p>This is useful for example when {@link ContentVersionStrategy} cannot be
 * used such as when using JavaScript and css module loaders which are in charge of
 * loading the JavaScript and css resources and need to know their relative paths.
 *
 * @author Zsolt Fatér
 * @see VersionResourceResolver
 */
public class UrlParameterFixedVersionStrategy extends AbstractVersionStrategy {

    private final String version;

    public UrlParameterFixedVersionStrategy(String version) {
        super(new UrlParameterVersionStrategy());
        this.version = version;
    }

	public UrlParameterFixedVersionStrategy(String version, String attributeName) {
		super(new UrlParameterVersionStrategy(attributeName));
		this.version = version;
	}

    @Override
    public String getResourceVersion(Resource resource) {
        return this.version;
    }

}
