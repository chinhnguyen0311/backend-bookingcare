package com.IMIC.booking_care.appointment.dto.response;

import com.IMIC.booking_care.appointment.enums.AppointmentStatus;
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
public class AppointmentResponse {
    private UUID appointmentId;
    private UUID doctorId;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalTime expectedTime;
    private AppointmentStatus status;
    private String notes;
    private OffsetDateTime createdAt;
}
