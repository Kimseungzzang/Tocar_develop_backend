package Capstone.Capstone.controller;

import Capstone.Capstone.Service.UserService;
import Capstone.Capstone.dto.LoginDto;
import Capstone.Capstone.dto.SignUpDto;
import Capstone.Capstone.dto.SmsDto;
import Capstone.Capstone.dto.UserDto;
import Capstone.Capstone.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@Tag(name="User API", description = "User API입니다.")
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public UserController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    // ----------------------------------------------------------------------
    // 1) Auth (회원가입/로그인/로그아웃)
    // ----------------------------------------------------------------------

    @PostMapping("/signUp")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "유저 생성"))
    @Operation(summary = "회원가입", description = "User 정보를 저장합니다.")
    public ResponseEntity<Void> signUp(@RequestBody SignUpDto signUpDto) {
        userService.saveUser(signUpDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "id와 password를 기반으로 로그인합니다.")
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getId(), loginDto.getPassword())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            // ★ 핵심: 세션에 SPRING_SECURITY_CONTEXT로 저장
            request.getSession(true)
                    .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            // (선택) 네가 따로 쓰는 세션 키도 유지
            User findUser = userService.getUserById(loginDto.getId());
            UserDto dto = userService.convertToDto(findUser);
            request.getSession().setAttribute("LOGIN_USER_ID", dto.getId());

            return ResponseEntity.ok(dto);
        } catch (BadCredentialsException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    @GetMapping("/logout")
    @Operation(summary = "로그아웃", description = "사용자 세션을 무효화하여 로그아웃시킵니다.")
    public ResponseEntity<Void> logOut(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate(); // Redis 세션 삭제
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    // ----------------------------------------------------------------------
    // 2) User 조회/검증/수정 (CRUD 성격)
    // ----------------------------------------------------------------------

    @GetMapping("/getUser/{userId}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "유저 확인"),
            @ApiResponse(responseCode = "404", description = "유효하지 않은 유저")
    })
    @Operation(summary = "회원 찾기", description = "id를 기반으로 user를 찾습니다.")
    public ResponseEntity<UserDto> getUserById(@PathVariable("userId") String userId) {
        User user = userService.getUserById(userId);
        if (user != null) {
            UserDto userDto = userService.convertToDto(user);
            return new ResponseEntity<>(userDto, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/checkId")
    @Operation(summary = "아이디 중복 확인")
    public ResponseEntity<Boolean> checkId(@RequestParam String id) {
        boolean available = (userService.getUserById(id) == null);
        return new ResponseEntity<>(available, HttpStatus.OK);
    }

    @PutMapping("/changeInform")
    @Operation(summary = "회원 정보 변경", description = "UserDto 기반으로 정보를 수정합니다.")
    public ResponseEntity<Void> changeUserInform(@RequestBody UserDto userDto) {
        userService.UpdateUserInform(userDto);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // ----------------------------------------------------------------------
    // 3) 비밀번호 (찾기/변경)
    // ----------------------------------------------------------------------

    @PostMapping("/findPw")
    @Operation(summary = "비밀번호 찾기", description = "비밀번호 찾기(SMS/이메일 인증 등).")
    public ResponseEntity<Void> findPassword(@RequestBody User user) {
        userService.sendSms(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/changePassword/{userId}")
    @Operation(summary = "비밀번호 변경", description = "password를 변경합니다.")
    public ResponseEntity<Void> changePassword(@PathVariable("userId") String userId,
                                               @RequestParam String newPassword) {
        userService.updateUserPassword(userId, newPassword);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // ----------------------------------------------------------------------
    // 4) SMS 인증
    // ----------------------------------------------------------------------

    @PostMapping("/sendSMS")
    @Operation(summary = "SMS 발송", description = "userDto의 번호로 인증 문자를 보냅니다.")
    public ResponseEntity<Void> sendSMS(@RequestBody User user) {
        userService.sendSms(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/sendSMS/check")
    @Operation(summary = "인증문자 확인", description = "입력한 번호가 인증 문자가 맞는지 확인합니다.")
    public boolean checkVerificationCode(@RequestBody SmsDto smsDto) {
        return userService.checkVerificationCode(smsDto.getPhoneNumber(), smsDto.getVerificationCode());
    }

    // ----------------------------------------------------------------------
    // 5) 운전자 기능
    // ----------------------------------------------------------------------

    @PostMapping("/{userId}/switchToDriverMode")
    @Operation(summary = "운전자모드 변경", description = "운전자모드로 전환합니다.")
    public ResponseEntity<Void> switchToDriverMode(@PathVariable String userId) {
        User user = userService.getUserById(userId);
        userService.switchToDriverMode(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/registerDriverLicense")
    @Operation(summary = "운전면허증 등록", description = "운전면허증을 등록합니다.")
    public ResponseEntity<Void> registerDriverLicense(@PathVariable String userId,
                                                      @RequestBody String driverLicense) {
        User user = userService.getUserById(userId);
        userService.registerDriverLicense(user, driverLicense);
        return ResponseEntity.ok().build();
    }

    // ----------------------------------------------------------------------
    // 6) 평점
    // ----------------------------------------------------------------------

    @PostMapping("/{userId}/star/")
    @Operation(summary = "평점 주기", description = "해당 사용자에게 별점을 부여합니다.")
    public ResponseEntity<Void> rateUser(@PathVariable String userId, @RequestParam double star) {
        User user = userService.getUserById(userId);
        userService.addRating(user, star);
        return ResponseEntity.ok().build();
    }
}
