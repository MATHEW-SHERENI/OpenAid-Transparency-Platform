package com.mathewshereni.open_aid_transparency.security;

import com.mathewshereni.open_aid_transparency.user.AppUser;
import com.mathewshereni.open_aid_transparency.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Bridges OUR AppUser entity to Spring Security's authentication machinery.
 *
 * Spring Security never sees our AppUser directly - it works with its own
 * UserDetails interface. This service loads an AppUser by username and adapts it
 * into a Spring "User" (a built-in UserDetails) carrying the password hash and
 * the role as an authority. Spring then BCrypt-compares the submitted password
 * against that hash for us.
 *
 * Note the "ROLE_" prefix: that's Spring's convention for roles, and it lets us
 * later write rules like hasRole("ADMIN").
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = repository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User '" + username + "' not found"));

        return User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                .disabled(!user.isEnabled())
                .build();
    }
}
