package com.IMIC.booking_care.authentication.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {

    /** Optional: nếu gửi kèm thì refresh token sẽ bị thu hồi trong DB. */
    private String refreshToken;
}
