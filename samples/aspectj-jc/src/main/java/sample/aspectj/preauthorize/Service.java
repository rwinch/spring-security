package sample.aspectj.preauthorize;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service which is secured on method level
 *
 * @author Rob Winch
 * @since 4.0.2
 */
public class Service {

	@PreAuthorize("hasRole('USER')")
	public void secureMethod() {
		// nothing
	}

	public void publicMethod() {
		// nothing
	}

}
