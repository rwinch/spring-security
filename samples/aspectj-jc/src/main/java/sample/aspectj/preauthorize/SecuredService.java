package sample.aspectj.preauthorize;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service which is secured on the class level
 *
 * @author Rob Winch
 * @since 4.0.2
 */
@PreAuthorize("hasRole('USER')")
public class SecuredService {

	public void secureMethod() {
		// nothing
	}

}
