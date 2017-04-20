/*
 * Copyright 2012-2017 the original author or authors.
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
package org.springframework.security.oauth2.core;

import org.springframework.util.Assert;

/**
 * @author Joe Grandja
 */
public class OAuth2Error {
	private final String errorCode;
	private final String description;
	private final String uri;

	public OAuth2Error(String errorCode) {
		this(errorCode, null, null);
	}

	public OAuth2Error(String errorCode, String description, String uri) {
		Assert.hasText(errorCode, "errorCode cannot be empty");
		this.errorCode = errorCode;
		this.description = description;
		this.uri = uri;
	}

	public String getErrorCode() {
		return this.errorCode;
	}

	public String getDescription() {
		return this.description;
	}

	public String getUri() {
		return this.uri;
	}

	@Override
	public String toString() {
		return "[" + this.getErrorCode() + "] " +
				(this.getDescription() != null ? this.getDescription() : "");
	}
}
