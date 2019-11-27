package aspectdemo;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy
@Configuration
@ComponentScan("aspectdemo")
public class AppConfigAspect {

	@Bean(value = "", initMethod = "", destroyMethod = "")
	public String getName(){
		return "abel";
	}

}
