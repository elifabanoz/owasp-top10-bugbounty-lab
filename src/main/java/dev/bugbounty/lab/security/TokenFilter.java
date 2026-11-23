package dev.bugbounty.lab.security;

import dev.bugbounty.lab.repository.UserRepository;
import dev.bugbounty.lab.model.Actor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TokenFilter extends OncePerRequestFilter {
    private final TokenService tokens;
    private final UserRepository users;

    public TokenFilter(TokenService tokens, UserRepository users) {
        this.tokens = tokens;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Arrays.stream(cookies).filter(cookie -> "LAB_TOKEN".equals(cookie.getName())).findFirst()
                    .ifPresent(cookie -> authenticate(cookie.getValue()));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            users.byId(tokens.verify(token)).ifPresent(user -> SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(Actor.from(user), null,
                            java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.role())))));
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }
    }
}
