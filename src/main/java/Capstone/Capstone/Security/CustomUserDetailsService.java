package Capstone.Capstone.Security;

import Capstone.Capstone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository; // getUserById와 같은 역할

    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        Capstone.Capstone.entity.User u = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("user not found: " + id));

        // 권한이 없으면 ROLE_USER 하나 부여하거나 빈 리스트로
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getId())
                .password(u.getPassword()) // 반드시 BCrypt 해시여야 함
                .authorities("ROLE_USER")
                .build();
    }
}
