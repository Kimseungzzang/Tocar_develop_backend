package Capstone.Capstone.Security;

import jakarta.servlet.http.HttpServletResponse;
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
                // JSON API라면 CSRF 토큰 준비가 번거롭습니다. 우선 /login, /signUp만 예외 처리
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/user/login", "/api/user/signUp", "/swagger-ui/**", "/api-docs/**").permitAll()
                        .requestMatchers("/api/recruits/**").hasRole("USER")   // ← 경로 앞에 `/` 빼먹지 않게 수정
                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        // 로그인 안 됐을 때 (401)
                        .authenticationEntryPoint((request, response, e) -> {
                            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                            System.out.println("[401 Unauthorized] auth=" + auth);
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                        // 로그인은 됐는데 권한이 부족할 때 (403)
                        .accessDeniedHandler((request, response, e) -> {
                            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                            System.out.println("[403 Forbidden]");
                            if (auth != null) {
                                System.out.println(" - name       : " + auth.getName());
                                System.out.println(" - principal  : " + auth.getPrincipal());
                                System.out.println(" - authorities: " + auth.getAuthorities());
                                System.out.println(" - class      : " + auth.getClass().getName());
                            } else {
                                System.out.println(" - auth is null");
                            }
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                        })
                )

                // 세션 기반 관리 (기본은 IF_REQUIRED)
                .sessionManagement(sess -> sess.sessionFixation().migrateSession());

        return http.build();
    }

}
