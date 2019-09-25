package com.atlwj.demo.web.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;

/**
 * 只扫描Controller
 */
@ComponentScan(value = "com.atlwj.demo.web",includeFilters = {
		@ComponentScan.Filter(type = FilterType.ANNOTATION,classes = {Controller.class})
},useDefaultFilters = false)
public class WebConfig {
}
