/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Rob Winch
 */
@SpringJUnitWebConfig({ SecurityConfiguration.class, WebMvcConfiguration.class })
public class SecurityConfigurationTests {
	private MockMvc mvc;

	@BeforeEach
	public void setup(@Autowired WebApplicationContext context) {
		this.mvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.defaultRequest(get("/").accept(MediaType.TEXT_HTML))
				.build();
	}

	@Test
	public void requestProtectedResourceRequiresAuthentication() throws Exception {
		this.mvc.perform(get("/"))
				.andExpect(redirectedUrl("http://localhost/login"));
	}

	@Test
	public void loginSuccess() throws Exception {
		this.mvc.perform(formLogin())
				.andExpect(redirectedUrl("/"));
	}

	@Test
	public void loginFailure() throws Exception {
		this.mvc.perform(formLogin().password("invalid"))
				.andExpect(redirectedUrl("/login?error"));
	}

	@Test
	@WithMockUser
	public void requestProtectedResourceWithUser() throws Exception {
		this.mvc.perform(get("/"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser
	public void logoutSuccess() throws Exception {
		this.mvc.perform(logout())
				.andExpect(redirectedUrl("/login?logout"))
				.andExpect(unauthenticated());
	}
}
