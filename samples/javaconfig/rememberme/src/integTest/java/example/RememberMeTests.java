/*
 * Copyright 2002-2018 the original author or authors.
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

import example.pages.HomePage;
import example.pages.LoginPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

/**
 * @author Michael Simons
 */
public class RememberMeTests {

	private WebDriver driver;

	private WebDriver driver2;

	private int port;

	@BeforeEach
	public void setup() {
		this.port = Integer.parseInt(System.getProperty("app.httpPort"));
		this.driver = new HtmlUnitDriver();
		this.driver2= new HtmlUnitDriver();
	}

	@AfterEach
	public void tearDown() {
		this.driver.quit();
		this.driver2.quit();
	}

	@Test
	public void authenticateWhenRememberMeThenRemembers() {
		final HomePage homePage = HomePage.to(this.driver, this.port, LoginPage.class)
				.loginForm()
				.username("user")
				.password("password")
				.rememberme()
				.submit();
		homePage
				.assertAt();

		this.driver.manage().deleteCookieNamed("JSESSIONID");

		HomePage.to(this.driver, this.port, HomePage.class).assertAt();
	}

	@Test
	public void authenticateWhenNotRememberMeThenNotRemembered() {
		final HomePage homePage = HomePage.to(this.driver, this.port, LoginPage.class)
				.loginForm()
				.username("user")
				.password("password")
				.submit();
		homePage
				.assertAt();

		this.driver.manage().deleteCookieNamed("JSESSIONID");

		HomePage.to(this.driver, this.port, LoginPage.class).assertAt();
	}
}
