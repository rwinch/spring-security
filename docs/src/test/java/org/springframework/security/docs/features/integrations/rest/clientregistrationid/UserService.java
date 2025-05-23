package org.springframework.security.docs.features.integrations.rest.clientregistrationid;

import org.springframework.security.oauth2.client.annotation.ClientRegistrationId;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/user")
public interface UserService {

	// tag::getAuthenticatedUser[]
	@GetExchange
	@ClientRegistrationId("github")
	User getAuthenticatedUser();
	// end::getAuthenticatedUser[]

}
