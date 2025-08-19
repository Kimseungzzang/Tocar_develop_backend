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
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;



@Slf4j
@RestController
@RequestMapping("/api/user")
@Tag(name="User API",description = "User API입니다.")
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public UserController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/signUp")
    @Tag(name = "User API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "유저 셍성")
    })
    @Operation(summary = "회원가입", description = "User 정보를 저장합니다.")
    //기존 User 로 받던 signup 요청 body를 signUpDto로 변경
    public ResponseEntity<Void> signUp(@RequestBody SignUpDto signUpDto) {
        userService.saveUser(signUpDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }


    @GetMapping("/getUser/{userId}")
    @Tag(name = "User API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "유저 확인"),
            @ApiResponse(responseCode = "404", description = "유효하지 않은 유저"),
    })
    @Operation(summary = "회원 찾기", description = "id를 기반으로 user를 찾습니다.")
    public ResponseEntity<UserDto> getUserById(@PathVariable("userId") String userId) {
        User user=userService.getUserById(userId);
       if(user!=null){
           UserDto userDto=userService.convertToDto(user);
           return new ResponseEntity<>(userDto, HttpStatus.OK);
       }
       else{
           return new ResponseEntity<>(HttpStatus.NOT_FOUND);
       }

    }

    @PostMapping("/login")
    @Tag(name = "User API")
    @Operation(summary = "로그인", description = "id와 password를 기반으로 로그인합니다.")
    //Spring Security 적용
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto, HttpServletRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getId(), loginDto.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.getSession(true); // JSESSIONID 발급

            User findUser = userService.getUserById(loginDto.getId());
            UserDto dto = userService.convertToDto(findUser);

            return ResponseEntity.ok(dto);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 또는 비밀번호가 올바르지 않습니다.");
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자를 찾을 수 없습니다.");
        }
    }


    @GetMapping("/logout")
    @Tag(name="User API")
    @Operation(summary="로그아웃",description = "사용자 세션을 무효화하여 로그아웃시킵니다,")
    //Spring Security 적용
    public ResponseEntity<Void> logOut(HttpServletRequest request){
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // 세션 무효화
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }



    @GetMapping("/checkId")
    public ResponseEntity<Boolean> checkId(@RequestParam String id){
        if(userService.getUserById(id)!=null)
        {
            return new ResponseEntity<>(false,HttpStatus.OK);
        }
        return new ResponseEntity<>(true,HttpStatus.OK);
    }


    @PostMapping("/findPw")
    @Tag(name = "User API")
    @Operation(summary = "비밀번호 찾기", description = "비밀번호를 찾습니다.")
    public ResponseEntity<Void> findPassword(@RequestBody User user) {
        userService.sendSms(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/changePassword/{userId}")
    @Tag(name = "User API")
    @Operation(summary = "비밀번호 변경", description = "password를 변경합니다")
    public ResponseEntity<Void> changePassword(@PathVariable("userId") String userId, @RequestParam String newPassword) {
        userService.updateUserPassword(userId, newPassword);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    @PutMapping("/changeInform")
    public  ResponseEntity<Void> changeUserInform(@RequestBody UserDto userDto)
    {
        userService.UpdateUserInform(userDto);
        return  new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/sendSMS")
    @Tag(name = "User API")
    @Operation(summary = "sms 발송", description = "userDto의 번호로 인증 문자를 보냅니다")
    public ResponseEntity<Void> sendSMS(@RequestBody User user) {
        userService.sendSms(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @PostMapping("/sendSMS/check")
    @Tag(name = "User API")
    @Operation(summary = "인증문자 확인", description = "입력한 번호가 인증 문자가 맞는지 확인합니다.")
    public boolean checkVerificationCode(@RequestBody SmsDto smsDto) {

        return userService.checkVerificationCode(smsDto.getPhoneNumber(),smsDto.getVerificationCode()) ;


    }

    @PostMapping("/{userId}/switchToDriverMode")
    @Tag(name="User API")
    @Operation(summary = "운전자모드 변경",description = "운전자모드로 전환합니다.")
    public ResponseEntity<Void> switchToDriverMode(@PathVariable String userId) {
        User user = userService.getUserById(userId);
        userService.switchToDriverMode(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/registerDriverLicense")
    @Tag(name="User API")
    @Operation(summary = "운전면허증 등록",description = "운전면허증을 등록합니다.")
    public ResponseEntity<Void> registerDriverLicense(@PathVariable String userId, @RequestBody String driverLicense) {
        User user = userService.getUserById(userId);
        userService.registerDriverLicense(user, driverLicense);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/star/")
    public ResponseEntity<User> rateUser(@PathVariable String userId, @RequestParam double star) {
        User user = userService.getUserById(userId);

        userService.addRating(user, star);

        return ResponseEntity.ok().build();
    }




}
