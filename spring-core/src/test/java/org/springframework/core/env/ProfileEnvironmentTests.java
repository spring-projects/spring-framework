/**
 * 
 */
package org.springframework.core.env;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests covering the profile functionality of {@link AbstractEnvironment}.
 * 
 * @author Sami Nieminen
 */
public class ProfileEnvironmentTests {

    private static final String DEVELOPMENT_PROFILE = "development-profile";
    private static final String PRODUCTION_PROFILE1 = "production-profile1";
    private static final String PRODUCTION_PROFILE2 = "production-profile2";
    
    private static final String DEVELOPMENT_WILDCARD = "development-*";
    private static final String PRODUCTION_WILDCARD = "production-*";
    
    private static final String PROFILE1_WILDCARD = "*-profile1";
    private static final String PROFILE2_WILDCARD = "*-profile2";
    
    private static final String INVALID_PROFILE = "*-profile-*";

    @Test
    public void defaultProfile() {
        AbstractEnvironment env = new ProfileTestEnvironment();
        env.setDefaultProfiles(DEVELOPMENT_PROFILE);
        
        assertTrue(env.acceptsProfiles(DEVELOPMENT_PROFILE));
        assertFalse(env.acceptsProfiles(PRODUCTION_PROFILE1));
        assertFalse(env.acceptsProfiles(PRODUCTION_PROFILE2));
    }
    
    @Test
    public void defaultProfileWithWildcard() {
        AbstractEnvironment env = new ProfileTestEnvironment();
        env.setDefaultProfiles(DEVELOPMENT_PROFILE);
        
        assertTrue(env.acceptsProfiles(DEVELOPMENT_WILDCARD));
        assertFalse(env.acceptsProfiles(PRODUCTION_WILDCARD));
    }
    
    @Test
    public void activeProfile() {
        AbstractEnvironment env = new ProfileTestEnvironment();
        env.setActiveProfiles(PRODUCTION_PROFILE2);
        
        assertTrue(env.acceptsProfiles(PRODUCTION_PROFILE2));
        assertFalse(env.acceptsProfiles(PRODUCTION_PROFILE1));
        assertFalse(env.acceptsProfiles(DEVELOPMENT_PROFILE));
    }
    
    @Test
    public void activeProfiles() {
        AbstractEnvironment env = new ProfileTestEnvironment();
        env.setActiveProfiles(PRODUCTION_PROFILE1, PRODUCTION_PROFILE2);
        
        assertTrue(env.acceptsProfiles(PRODUCTION_PROFILE1));
        assertTrue(env.acceptsProfiles(PRODUCTION_PROFILE2));
        assertFalse(env.acceptsProfiles(DEVELOPMENT_PROFILE));
    }
    
    @Test
    public void activeProfileOverridesDefaultProfile() {
        AbstractEnvironment env = new ProfileTestEnvironment();
        env.setDefaultProfiles(DEVELOPMENT_PROFILE);
        env.setActiveProfiles(PRODUCTION_PROFILE2);
        
        assertTrue(env.acceptsProfiles(PRODUCTION_PROFILE2));
        assertFalse(env.acceptsProfiles(PRODUCTION_PROFILE1));
        assertFalse(env.acceptsProfiles(DEVELOPMENT_PROFILE));
    }
    
    @Test
    public void activeProfileWithWildcard() {
        AbstractEnvironment env = new ProfileTestEnvironment();
        env.setActiveProfiles(PRODUCTION_PROFILE2);
        
        assertTrue(env.acceptsProfiles(PRODUCTION_WILDCARD));
        assertTrue(env.acceptsProfiles(PROFILE2_WILDCARD));
        assertFalse(env.acceptsProfiles(PROFILE1_WILDCARD));
        assertFalse(env.acceptsProfiles(DEVELOPMENT_WILDCARD));
    }
    
    @Test
    public void activeProfilesWithWildcard() {
        AbstractEnvironment env = new ProfileTestEnvironment();
        env.setActiveProfiles(PRODUCTION_PROFILE1, PRODUCTION_PROFILE2);
        
        assertTrue(env.acceptsProfiles(PRODUCTION_WILDCARD));
        assertTrue(env.acceptsProfiles(PROFILE1_WILDCARD));
        assertTrue(env.acceptsProfiles(PROFILE2_WILDCARD));
        assertFalse(env.acceptsProfiles(DEVELOPMENT_WILDCARD));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void invalidProfile() {
        AbstractEnvironment env = new ProfileTestEnvironment();
        
        env.acceptsProfiles(INVALID_PROFILE);
    }
    
    @Test
    public void negation() {
        AbstractEnvironment env = new ProfileTestEnvironment();
        env.setActiveProfiles(PRODUCTION_PROFILE1);
        
        assertFalse(env.acceptsProfiles("!" + PRODUCTION_PROFILE1));
        assertFalse(env.acceptsProfiles("!" + PRODUCTION_WILDCARD));
        assertTrue(env.acceptsProfiles("!" + PRODUCTION_PROFILE2));
        assertFalse(env.acceptsProfiles(DEVELOPMENT_WILDCARD));
        assertTrue(env.acceptsProfiles(PRODUCTION_PROFILE1));
    }
    
    private static class ProfileTestEnvironment extends AbstractEnvironment {}
}
