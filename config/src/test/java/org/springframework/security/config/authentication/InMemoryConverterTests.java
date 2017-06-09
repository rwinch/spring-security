/*
 *
 *  * Copyright 2002-2017 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.security.config.authentication;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import java.beans.PropertyEditorSupport;
import java.util.Collection;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Winch
 * @since 5.0
 */
@RunWith(SpringRunner.class)
public class InMemoryConverterTests {
	@Value("#{@aString}")
	Properties aString;

	@Value("classpath:users.properties")
	Resource userResource;

	@Value("classpath:users.properties")
	Properties users;

	@Value("bar")
	Foo foo;

	@Test
	public void aString() {
		assertThat(aString).containsEntry("a","b");
	}

	@Test
	public void userResource() {
		assertThat(userResource).isNotNull();
		assertThat(userResource.exists()).isTrue();
	}

	@Test
	public void users() {
		assertThat(users).isNotNull();
		System.out.println(users);
	}

	@Test
	public void bar() {
		assertThat(foo).isNotNull();
		assertThat(foo.getBar()).isEqualTo("bar");
	}

	@Configuration
	static class Config {
		@Bean
		public String aString() {
			return "a=b";
		}

		@Bean
		FooRegistrar fooRegistrar() {
			return new FooRegistrar();
		}
	}


	static class Foo {
		private String bar;

		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

		public String toString() {
			return "Foo " + bar;
		}
	}

	static class Foo2Editor extends PropertyEditorSupport {
		public void setAsText(String text) {
			Foo foo = new Foo();
			foo.setBar(text);
			setValue(foo);
		}
	}

	static class FooRegistrar implements PropertyEditorRegistrar {

		@Override
		public void registerCustomEditors(PropertyEditorRegistry registry) {
			registry.registerCustomEditor(Foo.class, new Foo2Editor());
		}
	}
}
