package org.springframework.test.web.servlet.setup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Test for SPR-10277 (Multiple method chaining when building MockMvc).
 *
 * @author Wesley Hall
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration
public class Spr10277Tests {

    @Autowired
    private WebApplicationContext wac;

    @Test
    public void chainMultiple() {

        //Reproduction code from https://jira.springsource.org/browse/SPR-10277
        MockMvcBuilders
                .webAppContextSetup(wac)
                .addFilter( new CharacterEncodingFilter() )
                .alwaysDo(print())
                .defaultRequest(get("/").contextPath("/mywebapp"))
                .build();


    }

    @Configuration
    @EnableWebMvc
    static class WebConfig extends WebMvcConfigurerAdapter {
    }
}
