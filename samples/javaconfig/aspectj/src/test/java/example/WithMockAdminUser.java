package example;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.test.context.support.WithMockUser;

/**
 * @author Rob Winch
 */
@WithMockUser(roles = "ADMIN")
@Retention(RetentionPolicy.RUNTIME)
public @interface WithMockAdminUser {}
