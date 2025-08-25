package Capstone.Capstone.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecruitRequest {

    private String title;
    private String contents;
    private double distance;
    private double  distance2;
    private List<String> keywords;
    private String destination;
    private String departure;
    private LocalDate departureDate;
    private String time;
    private String message;
    private int maxParticipant;
    private double  departureX;
    private double  departureY;
    private double arrivalX;
    private double arrivalY;
    private double currentX;
    private double currentY;


}
