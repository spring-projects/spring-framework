package org.springframework.core.type.filter;

import org.springframework.core.type.ClassMetadata;
import org.springframework.util.Assert;
import java.util.regex.Pattern;

/**
 * Abstract base class for type filters that perform class testing.
 */
public abstract class AbstractClassTestingTypeFilter implements TypeFilter {

    // Other existing methods and fields...

    /**
     * Checks if the class metadata matches the given regex pattern.
     * 
     * @param metadata the class metadata to be checked
     * @param pattern the regex pattern against which the class name is checked
     * @return true if the class name matches the pattern
     */
    protected boolean matchesPattern(ClassMetadata metadata, Pattern pattern) {
        Assert.notNull(metadata, "ClassMetadata must not be null");
        Assert.notNull(pattern, "Pattern must not be null");
        return pattern.matcher(metadata.getClassName()).matches();
    }

    // Rest of the class...
}

/**
 * A type filter that uses a regex pattern to match class names.
 */
public class RegexPatternTypeFilter extends AbstractClassTestingTypeFilter {

    private final Pattern pattern;

    /**
     * Creates a new RegexPatternTypeFilter with the given pattern.
     * 
     * @param pattern the regex pattern to be used for matching class names
     */
    public RegexPatternTypeFilter(Pattern pattern) {
        Assert.notNull(pattern, "Pattern must not be null");
        this.pattern = pattern;
    }

    /**
     * Checks if the given class metadata matches the regex pattern.
     * 
     * @param metadata the class metadata to be checked
     * @return true if the class name matches the pattern
     */
    @Override
    protected boolean match(ClassMetadata metadata) {
        return matchesPattern(metadata, this.pattern);
    }

}
