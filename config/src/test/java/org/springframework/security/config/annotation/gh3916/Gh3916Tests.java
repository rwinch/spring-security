/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.config.annotation.gh3916;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author rwinch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Configuration
public class Gh3916Tests {
	@Autowired
	FilterChainProxy springSecurityFilterChain;

	@Test
	public void loads() {
		assertThat(this.springSecurityFilterChain).isNotNull();
	}

	@Configuration
	static class BootGlobalAuthenticationConfigurationAdapter
			extends GlobalAuthenticationConfigurerAdapter {
		private final ApplicationContext context;

		@Autowired
		BootGlobalAuthenticationConfigurationAdapter(ApplicationContext context) {
			this.context = context;
		}

		@Override
		public void init(AuthenticationManagerBuilder auth) throws Exception {
			AuthenticationConfiguration configuration = this.context
					.getBean(AuthenticationConfiguration.class);
			configuration.getAuthenticationManager();
		}
	}

	@EnableWebSecurity
	@EnableGlobalMethodSecurity(prePostEnabled = true)
	static class WebSecurity extends WebSecurityConfigurerAdapter {
	}

	@PreAuthorize("hasRole('ADMIN')")
	static class Service {
	}

	@Configuration
	static class ServiceConfig {
		@Bean
		Service service() {
			return new Service();
		}

		@Bean
		UserDetailsService uds() {
			return new UserDetailsService() {

				@Override
				public UserDetails loadUserByUsername(String username)
						throws UsernameNotFoundException {
					// TODO Auto-generated method stub
					throw new UnsupportedOperationException("Auto-generated method stub");
				}
			};
		}
	}
}
