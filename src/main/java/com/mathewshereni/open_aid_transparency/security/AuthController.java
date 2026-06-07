package com.mathewshereni.open_aid_transparency.security;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * POST /api/auth/login
     * 1. Hand the username+password to the AuthenticationManager, which loads the
     *    user (via AppUserDetailsService) and BCrypt-checks the password.
     * 2. If it throws, the user is rejected (handled as 401 by our advice).
     * 3. If it succeeds, mint a signed JWT carrying their username + role.
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)         // e.g. "ROLE_CITIZEN"
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .orElse("UNKNOWN");

        String token = jwtService.generateToken(authentication.getName(), role);
        return new LoginResponse(token, "Bearer", authentication.getName(), role);
    }
}
