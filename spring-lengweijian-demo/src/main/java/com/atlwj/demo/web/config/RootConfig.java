package com.atlwj.demo.web.config;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;


/**
 * root context
 */
@Configurable
@ComponentScans(
		@ComponentScan(value = "com.atlwj.demo.web",excludeFilters = {
				@ComponentScan.Filter(type = FilterType.ANNOTATION,classes = {Controller.class})
		})
)
public class RootConfig {


}
