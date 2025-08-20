package Capstone.Capstone.entity;



import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Builder
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ToCar_user")
public class User {
    @Id
    @Column(name = "id")
    private String id;

    private String name;
    private LocalDate birthdate; // 타임존이 항상 들어가는 Date 대신 LocalDate로 변경
    private String phoneNumber;
    private String profileImage;
    private String password;
    private String email; // email 추가

    @Column(unique = true, nullable = false)  //nickname에 unique 제약 조건 추가.
    private String nickname;

    private boolean isDriver; //현재 운전자 모드인지 아닌지
    private String driverLicense; //운전면허증 등록여부
    private int star=0;
    private double avgStar = 0.0; //별점 평균

    @ManyToMany(mappedBy = "bookedUsers")
    private List<Recruit> recruits;

    public void setIsDriver(boolean isDriver) {
        this.isDriver = isDriver;
    }


    // 생성자, getter, setter 등 필요한 코드 추가


    @OneToMany(mappedBy = "user")
    private List<Like> likes;

    @ManyToMany(mappedBy = "users")
    private Set<ChatRoom> chatRooms = new HashSet<>();

    //낙관적 락을 위한 version 추가
    @Version
    private long version;

}
