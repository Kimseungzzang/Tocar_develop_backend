package Capstone.Capstone.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SignUpDto {


    private String id;
    private String name;
    private LocalDate birthdate;
    private String phoneNumber;
    private String profileImage;
    private String password;
    private String nickname;

}
