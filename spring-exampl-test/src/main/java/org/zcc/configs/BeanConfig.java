package org.zcc.configs;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.zcc.beans.SpringCustomBean;

@Configuration
@ComponentScan(basePackageClasses = Math.class)
public class BeanConfig {

	@Bean(name = "customBean")
	SpringCustomBean springCustomBean() {
		SpringCustomBean springCustomBean = new SpringCustomBean();
		springCustomBean.setName("zcc");
		return springCustomBean;
	}
}
