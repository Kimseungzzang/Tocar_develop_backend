package Capstone.Capstone.servicelmpl;

import Capstone.Capstone.Service.UserService;
import Capstone.Capstone.dto.SignUpDto;
import Capstone.Capstone.dto.UserDto;
import Capstone.Capstone.entity.Community;
import Capstone.Capstone.entity.User;
import Capstone.Capstone.utils.SmsUtil;
import Capstone.Capstone.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsUtil smsUtil;


    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, SmsUtil smsUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.smsUtil=smsUtil;
    }

    @Override
    @Transactional // 트랜잭션 추가
    public void saveUser(SignUpDto user) {

       User savedUser= User.builder()
               .id(user.getId())
                .nickname(user.getNickname())
                .password(passwordEncoder.encode(user.getPassword()))
                .name(user.getName())
                .birthdate(user.getBirthdate())
                .phoneNumber(user.getPhoneNumber())
                .profileImage(user.getProfileImage())
               .email(user.getEmail())
                .build();


        userRepository.save(savedUser);

    }

    @Override
    @Transactional()
    public User getUserById(String Id) {
        Optional<User> optionalUser = userRepository.findById(Id);
        return optionalUser.orElse(null);

    }

    @Override
    @Transactional
    public void updateUserPassword(String id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        // ⚠️ 기존 코드 버그: 평문 저장하고 있었음
        user.setPassword(passwordEncoder.encode(newPassword));
        // 저장은 트랜잭션 종료 시 flush
    }



    @Override
    public String sendSms(User user) {
        return "";
    }


    /*
        @Override
        @Transactional
        public String sendSms(User user) {


                //수신번호 형태에 맞춰 "-"을 ""로 변환
                String phoneNum = user.getPhoneNumber().replaceAll("-","");



                String verificationCode = smsUtil.generateStoreVerificationCode(phoneNum);
                smsUtil.sendOne(phoneNum, verificationCode);


                return "SMS 전송 성공";
            }
    */
    @Override
    @Transactional
    public User getUserByNickName(String nickName) {
        return userRepository.findByNickname(nickName);
    }

    @Override
    @Transactional
        public boolean checkVerificationCode(String phoneNum,String verificationCode){
            phoneNum=phoneNum.replaceAll("-","");

           return smsUtil.checkVerificationCode(phoneNum,verificationCode);

        }
    @Override
    @Transactional
    public void switchToDriverMode(User user) {
        if (user.getDriverLicense() == null || user.getDriverLicense().isEmpty()) {
            throw new IllegalStateException("운전면허증을 등록해야 운전자 모드를 사용할 수 있습니다.");
        }
        user.setIsDriver(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void registerDriverLicense(User user, String driverLicense) { //면허증 등록
        user.setDriverLicense(driverLicense);
        userRepository.save(user);
    }



    @Override
    @Transactional
    public void UpdateUserInform(UserDto userDto) {
        Optional<User> user=userRepository.findById(userDto.getId());
        if (user.isPresent()){
           User findUser=user.get();


           findUser.setNickname(userDto.getNickname());
           findUser.setBirthdate(userDto.getBirthdate());
           findUser.setPhoneNumber(userDto.getPhoneNumber());
           findUser.setName(userDto.getName());
           findUser.setId(userDto.getId());
           findUser.setAvgStar(userDto.getStar());
           findUser.setProfileImage(saveImage(userDto.getProfileImage()));
           findUser.setEmail(userDto.getEmail());
           userRepository.save(findUser);
        }

    }

    @Override
    public void checkOutUser() {


    }

    @Override
    public User convertToEntity(UserDto userDto) {
        User user = new User();
        user.setId(userDto.getId());
        user.setNickname(userDto.getNickname());
        return user;
    }
@Override
    public UserDto convertToDto(User user){

        UserDto userDto=new UserDto();
        userDto.setName(user.getName());
        userDto.setId(user.getId());
        userDto.setBirthdate(user.getBirthdate());
        userDto.setNickname(user.getNickname());
        userDto.setPhoneNumber(user.getPhoneNumber());
        userDto.setProfileImage(user.getProfileImage());
        userDto.setAvgStar(user.getAvgStar());
        userDto.setEmail(user.getEmail());
        return userDto;
}
    @Override
    public String saveImage(String image) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "image_" + timeStamp + ".jpg"; // 파일 확장자에 맞게 변경

        // 이미지 데이터(dataURL) 추출
        String imageData = image;

        // dataURL에서 실제 데이터 부분만 분리 (콤마 이후의 부분)
        String base64Image = imageData.split(",")[1];

        // base64로 인코딩된 데이터를 디코딩하여 바이너리 데이터로 변환
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        String imageUrl="/home/ubuntu/images/"+fileName;

        // 저장할 파일 경로 지정
        File outputFile = new File(imageUrl);

        try (OutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(imageBytes);
        }

        catch (Exception e) {
            e.printStackTrace();

        }
        return  imageUrl;
    }

    // 별점 누락 방지를 위해 재시도 전략
    @Override
    public void addRating(User user, double star) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                addRatingOnce(user.getId(), star);
                return; // 성공
            } catch (org.springframework.dao.OptimisticLockingFailureException e) {
                if (attempt == maxRetries) throw e; // 재시도 한계 도달
                // 짧은 대기 후 재시도 (백오프)
                try { Thread.sleep(50L * attempt); } catch (InterruptedException ignored) {}
            }
        }
    }

    @Transactional
    public void addRatingOnce(String userId, double star) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        double totalStar = u.getAvgStar() * u.getStar();
        totalStar += star;
        int newStarCount = u.getStar() + 1;
        double newAverageStar = totalStar / newStarCount;

        u.setStar(newStarCount);
        u.setAvgStar(newAverageStar);
        // flush 시 version 비교 → 충돌 시 예외
    }




}

