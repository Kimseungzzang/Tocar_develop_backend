package Capstone.Capstone.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder //빌더 추가
public class Recruit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idxNum;
    private String title;
    private String contents;
    private double star=0.0;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id") // PK(id) 기준 권장
    private User author;
    @ElementCollection
    private List<String> keywords = new ArrayList<>(); //키워드
    private boolean isDriverPost = false;
    private String destination; //목적지
    private String departure;
    private LocalDate departureDate; //출발일자
    private double distance2; //사용자 실시간 위치에서 모집글 사용자 위치까지 거리
    private String time;
    private double distance; //사용자 실시간 위치에서 모집글 사용자 위치까지 거리


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PostType postType = PostType.GENERAL; //포스트 종류

    private int participant;
    private int maxParticipant;

    private String message;
    @ElementCollection
    private List<String> users = new ArrayList<>();
    @ElementCollection
    private List<String> bookingUsers=new ArrayList<>();
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; //생성날짜
    @Column(nullable = false)
    private LocalDateTime updatedAt; //수정날짜
    private double  departureX;
    private double  departureY;
    private double arrivalX;
    private double arrivalY;
    @ManyToMany
    @JoinTable(name = "record",
            joinColumns = @JoinColumn(name = "recruit_id"),
            inverseJoinColumns = @JoinColumn(name = "user_nickname"))
    private List<User> bookedUsers;

    private boolean Full=false;

    private double currentX;
    private double currentY;
    private double timeTaxi;
    private int fare;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }






}

