package com.IMIC.booking_care.appointment.dto.request;

import com.IMIC.booking_care.user.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequest {
    private UUID doctorId;
    private UUID slotId;
    private LocalDate appointmentDate;
    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDate dob;
    private String address;
    private Gender gender;
    private String notes;
}
