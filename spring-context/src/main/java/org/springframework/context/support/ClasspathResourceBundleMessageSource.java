package org.springframework.context.support;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

public class ClasspathResourceBundleMessageSource extends ResourceBundleMessageSource {
	
    private static final String PROPERTIES_SUFFIX = ".properties";
    private static final String XML_SUFFIX = ".xml";
    
    private static final Pattern SPRING_CLASSPATH_PREFIX_PATTERN = Pattern.compile("^classpath(?:\\*)?:.+");
    private static final Pattern CLASSPATH_SEARCH_PATTERN_FILTER = Pattern.compile("^classpath(?:\\*)?:((?:/)?[^\\*]+)(?:\\*|).+$");

    @Override
    public void setBasename(String basename) {
        try {
            super.setBasenames(getBaseNamesFromClasspath(basename).toArray(new String[] {}));
        } catch (IOException ex) {
            logger.error("Error loading the resource files from " + basename, ex);
        }
    }

    @Override
    public void setBasenames(String... basenames) {
        if (basenames != null) {
            Set<String> basenameSet = new HashSet<String>();
            for (String basename : basenames) {
                if (SPRING_CLASSPATH_PREFIX_PATTERN.matcher(basename).matches()) {
                    try {
                        basenameSet.addAll(getBaseNamesFromClasspath(basename));
                    } catch (IOException ex) {
                    	logger.error("Error loading the resource files from " + basename, ex);
                    }
                } else {
                    basenameSet.add(basename);
                }
            }

            if(logger.isDebugEnabled()) {
            	logger.debug("Returning basename set [ " + basenameSet + " ]");
            }
            
            String[] array = basenameSet.toArray(new String[] {});
      
            super.setBasenames(array);
        }
    }

    private Set<String> getBaseNamesFromClasspath(String searchPattern) throws IOException {
        Set<String> basenameSet = new HashSet<String>();
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        String resourcesBaseName;
        for (Resource resource : resourcePatternResolver.getResources(searchPattern + PROPERTIES_SUFFIX)) {
            String externalForm = resource.getURL().toExternalForm();
            logger.debug("File in classpath: " + externalForm);
            resourcesBaseName = getResourcesBaseNameBySeachPattern(externalForm, searchPattern);
            if (resourcesBaseName != null) {
                basenameSet.add(resourcesBaseName);
            }
        }

        for (Resource resource : resourcePatternResolver.getResources(searchPattern + XML_SUFFIX)) {
            resourcesBaseName = getResourcesBaseNameBySeachPattern(resource.getURL().toExternalForm(), searchPattern);
            if (resourcesBaseName != null) {
                basenameSet.add(resourcesBaseName);
            }
        }

        logger.debug("Returning basename set " + basenameSet + " for seach pattern " + searchPattern);
        return basenameSet;
    }

    private String getResourcesBaseNameBySeachPattern(String externalResourceUrl, String searchPattern) {
        Matcher matcher = CLASSPATH_SEARCH_PATTERN_FILTER.matcher(searchPattern);
        if(matcher.matches()) {
            int startIndex = externalResourceUrl.lastIndexOf(matcher.group(1));
            int lastIndex = externalResourceUrl.lastIndexOf(".");
            String fileName = externalResourceUrl.substring(startIndex, lastIndex);
            return fileName;
        }
        
        return null;
    }
    
}
