package com.IMIC.booking_care.appointment.dto.response;

import com.IMIC.booking_care.appointment.enums.AppointmentStatus;
import com.IMIC.booking_care.user.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayAppointmentResponse {
    private UUID appointmentId;
    private String patientName;
    private String phoneNumber;
    private String email;
    private LocalDate dob;
    private String address;
    private Gender gender;
    private LocalDate appointmentDate;
    private LocalTime expectedTime;
    private AppointmentStatus status;
    private String notes;
}
