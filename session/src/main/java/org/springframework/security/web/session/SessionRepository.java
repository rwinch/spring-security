package org.springframework.security.web.session;

/**
 * @author Rob Winch
 */
public interface SessionRepository<S extends Session> {
    void save(S session);

    Session getSession(String id);

    void delete(String id);

    S createSession();
}
