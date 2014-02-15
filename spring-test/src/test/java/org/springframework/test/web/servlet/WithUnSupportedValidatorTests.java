package org.springframework.test.web.servlet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class WithUnSupportedValidatorTests {
	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@Before
	public void setup() {
		this.mockMvc = webAppContextSetup(this.wac).build();
	}
	
	@Test
	public void testAFlowWithPartlyUnsupportedValidators() throws Exception {
		this.mockMvc.perform(post("/sampleRequest").param("id", "1").param("message", "message"))
			.andExpect(view().name("view"));
	}
	
	@Configuration
	@EnableWebMvc
	public static class WebConfiguration {
		@Bean
		public SampleController sampleController() {
			return new SampleController();
		}
	}
}


@Controller
class SampleController {
	
	@RequestMapping("/sampleRequest")
	public String sampleRequest(Request request, BindingResult bindingResult1,
			GeneralHolder genHolder, BindingResult bindingResult2, Model model) {
		model.addAttribute("moreinmodel", new GeneralHolder());
		return "view";
	}
	
	@InitBinder
	public void initBinder(WebDataBinder dataBinder) {
		dataBinder.addValidators(new Validator() {
			
			@Override
			public void validate(Object target, Errors errors) {
				//
			}
			
			@Override
			public boolean supports(Class<?> clazz) {
				return Request.class.equals(clazz);
			}
		});
	}
}

class Request {
	private int id;
	private String message;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}

class GeneralHolder {
	private int message;

	public int getMessage() {
		return message;
	}

	public void setMessage(int message) {
		this.message = message;
	}
}
