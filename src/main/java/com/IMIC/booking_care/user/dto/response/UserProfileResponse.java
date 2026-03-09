package com.IMIC.booking_care.user.dto.response;

import com.IMIC.booking_care.appointment.enums.AppointmentStatus;
import com.IMIC.booking_care.user.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDate dob;
    private Gender gender;
    private String address;
}
