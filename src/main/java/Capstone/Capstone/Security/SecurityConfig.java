package Capstone.Capstone.Security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity // @PreAuthorize 등 쓰고 싶을 때
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // JSON API라면 CSRF 토큰 준비가 번거롭습니다. 우선 /login만 예외로.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/user/login","/api/user/signUp"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/user/login", "/api/user/signUp", "/swagger-ui/**", "/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 세션 기반(기본: IF_REQUIRED). 필요시 정책 명시 가능
                .sessionManagement(sess -> sess.sessionFixation().migrateSession());

        return http.build();
    }
}
