package com.IMIC.booking_care.appointment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DoctorResponse {
    private UUID doctorId;
    private UUID userId;
    private String fullName;
    private String specialty;
    private String bio;
    private String experience;
    private String treatmentScope;
}
