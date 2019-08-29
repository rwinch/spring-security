/*
 * Copyright 2002-2019 the original author or authors.
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

package sample.web;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sample.Saml2LoginApplication;

@Controller
public class SampleSaml2AppController {

	private static final Log logger = LogFactory.getLog(Saml2LoginApplication.class);

	@RequestMapping(value = { "/logged-in" })
	public String loggedIn() {
		logger.info("Sample SP Application - You are logged in!");
		return "logged-in";
	}

	public String loggedOut() {
		return "local-logout";
	}

	@RequestMapping(value = { "/", "/index", "/logged-out" })
	public String landingPage() {
		logger.info("Sample SP Application - Landing Page");
		if (isLoggedIn()) {
			return loggedIn();
		}
		else {
			return loggedOut();
		}
	}

	private boolean isLoggedIn() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && !(authentication instanceof AnonymousAuthenticationToken)
				&& authentication.isAuthenticated();
	}

}
