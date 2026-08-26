package pl.szymtrener.admin;

import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Service;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository repository;

    public AdminUserDetailsService(AdminUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser u = repository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Nie ma konta: " + username));
        return User.withUsername(u.getEmail())
                .password(u.getPasswordHash())
                .roles(u.getRole())
                .disabled(!u.isEnabled())
                .build();
    }

    /** Nieuzywane — pomocnicze, gdyby trzeba bylo trybu awaryjnego bez bazy. */
    static InMemoryUserDetailsManager empty() { return new InMemoryUserDetailsManager(); }
}
