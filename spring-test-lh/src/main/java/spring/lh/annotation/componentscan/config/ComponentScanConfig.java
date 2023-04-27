package spring.lh.annotation.componentscan.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import spring.lh.annotation.componentscan.filter.ComponentScanFilter;

@Configuration
@ComponentScan(value = "spring.lh.annotation.componentscan", includeFilters = {
		@ComponentScan.Filter(type = FilterType.CUSTOM, classes = {ComponentScanFilter.class})
}, useDefaultFilters = false)
public class ComponentScanConfig {
}
