package initbean;


import org.springframework.context.annotation.*;

@EnableAspectJAutoProxy
@Configuration
@ComponentScan("initbean")
public class AppConfigInit {

	@Bean(initMethod = "initPerson", destroyMethod = "destroyPerson")
	@Scope("prototype")
	public Person person(){
		return new Person();
	}


}
