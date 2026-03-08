# Hướng dẫn test API Auth bằng Postman

**Base URL:** `http://localhost:8080`

**Lưu ý:** Đảm bảo backend đang chạy (port 8080) và **đã tạo bảng `refresh_tokens`** trong PostgreSQL (xem mục "Chuẩn bị database" bên dưới). URL đúng có **v1**: `/api/v1/auth/...` (không phải `/api/auth/...`).

---

## Phân quyền: Bệnh nhân, Bác sĩ, Admin

| Vai trò      | Đăng ký (tự tạo tài khoản) | Đăng nhập / Đăng xuất |
|-------------|-----------------------------|-------------------------|
| **Bệnh nhân** | Có — dùng `POST /api/v1/auth/register` | Cùng endpoint: `POST /api/v1/auth/login`, `POST /api/v1/auth/logout` |
| **Bác sĩ**  | Không — do **Admin** tạo qua `POST /api/v1/auth/admin/users` | Cùng endpoint: `login`, `logout` (username + password) |
| **Admin**   | Không — tạo bằng seed SQL hoặc Admin khác tạo | Cùng endpoint: `login`, `logout` |

- **Đăng nhập / Đăng xuất** dùng chung cho cả 3 vai trò: cùng URL, cùng body. Response có trường `role` (PATIENT / DOCTOR / ADMIN) để app phân biệt.
- **Đăng ký** chỉ dành cho bệnh nhân (tạo tài khoản role PATIENT).
- **Bác sĩ và Admin** không có form “đăng ký” công khai. Tài khoản do Admin tạo (mục 5 bên dưới) hoặc insert/seed trong DB (Admin đầu tiên).

---

## Chuẩn bị database (bắt buộc trước khi đăng ký)

Nếu chưa có bảng `refresh_tokens`, mở PostgreSQL (pgAdmin hoặc psql), kết nối tới database **bookingcare_db**, rồi chạy file:

**`scripts/create_refresh_tokens.sql`**

Hoặc copy và chạy đoạn SQL sau:

```sql
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token TEXT NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```

---

## 1. Đăng ký (Register)

**Method:** `POST`  
**URL:** `http://localhost:8080/api/v1/auth/register`

**Headers:**
| Key          | Value            |
|-------------|------------------|
| Content-Type | application/json |

**Body** (chọn **raw** → **JSON**):

```json
{
    "username": "patient01",
    "email": "patient01@gmail.com",
    "password": "123456",
    "fullName": "Nguyễn Văn A",
    "phoneNumber": "0901234567"
}
```

**Ví dụ response thành công (200):**

```json
{
    "success": true,
    "code": 200,
    "message": "Đăng ký thành công",
    "data": {
        "userId": "uuid-here",
        "accessToken": "eyJ...",
        "refreshToken": "eyJ...",
        "role": "PATIENT",
        "fullName": "Nguyễn Văn A",
        "email": "patient01@gmail.com"
    },
    "timestamp": "2025-03-08T..."
}
```

**Lưu lại:** Copy `accessToken` và `refreshToken` để dùng cho bước **Logout** và **Đổi mật khẩu**.

---

## 2. Đăng nhập (Login)

**Method:** `POST`  
**URL:** `http://localhost:8080/api/v1/auth/login`

**Headers:**
| Key          | Value            |
|-------------|------------------|
| Content-Type | application/json |

**Body** (raw → JSON):

```json
{
    "username": "patient01",
    "password": "123456"
}
```

**Response thành công (200):** Giống đăng ký, có `accessToken`, `refreshToken`, `userId`, `role`, `fullName`, `email`.

**Lỗi thường gặp:**
- **404 / 1001:** Tài khoản không tồn tại → kiểm tra `username`.
- **401 / 1002:** Mật khẩu không chính xác.

---

## 3. Đăng xuất (Logout)

**Method:** `POST`  
**URL:** `http://localhost:8080/api/v1/auth/logout`

**Headers:**
| Key           | Value                    |
|---------------|--------------------------|
| Content-Type  | application/json         |
| Authorization | Bearer \<accessToken\>   |

Thay `\<accessToken\>` bằng token lấy từ **Login** hoặc **Register** (không có dấu `<` `>`).

**Body:** Có thể dùng **một trong hai cách**:
- **Chỉ cần Bearer token:** Để trống body (hoặc không chọn Body). Server sẽ blacklist access token hiện tại.
- **Gửi kèm refresh token:** Chọn raw → JSON và gửi `{"refreshToken": "eyJ..."}` để thu hồi luôn refresh token trong DB.

**Response thành công (200):**

```json
{
    "success": true,
    "code": 200,
    "message": "Đăng xuất thành công",
    "data": null,
    "timestamp": "..."
}
```

---

## 4. Đổi mật khẩu (Change Password)

**Method:** `POST`  
**URL:** `http://localhost:8080/api/v1/auth/change-password`

**Headers (bắt buộc):**
| Key           | Value                  |
|---------------|------------------------|
| Content-Type  | application/json       |
| Authorization | Bearer \<accessToken\> |

**Body** (raw → JSON):

```json
{
    "oldPassword": "123456",
    "newPassword": "654321"
}
```

**Response thành công (200):**

```json
{
    "success": true,
    "code": 200,
    "message": "Đổi mật khẩu thành công",
    "data": null,
    "timestamp": "..."
}
```

Sau khi đổi mật khẩu, dùng mật khẩu mới (`654321`) để **Đăng nhập**. Các refresh token cũ sẽ bị thu hồi.

**Lỗi:**
- **401:** Thiếu hoặc sai `Authorization` header.
- **1005:** Mật khẩu hiện tại không đúng.

---

## 5. Admin tạo tài khoản (Bác sĩ hoặc Admin)

**Chỉ tài khoản có role ADMIN** mới gọi được endpoint này để tạo tài khoản **Bác sĩ** hoặc **Admin** (không dùng để tạo bệnh nhân — bệnh nhân tự đăng ký).

**Method:** `POST`  
**URL:** `http://localhost:8080/api/v1/auth/admin/users`

**Headers (bắt buộc):**
| Key           | Value                    |
|---------------|--------------------------|
| Content-Type  | application/json         |
| Authorization | Bearer \<accessToken_của_Admin\> |

**Body** (raw → JSON):

```json
{
    "username": "doctor01",
    "email": "doctor01@gmail.com",
    "password": "123456",
    "fullName": "Bác sĩ Nguyễn Văn B",
    "phoneNumber": "0912345678",
    "role": "DOCTOR"
}
```

- Để tạo **Admin** khác: đổi `"role": "ADMIN"`.
- Giá trị `role` hợp lệ: `PATIENT`, `DOCTOR`, `ADMIN`.

**Response thành công (200):** Trả về thông tin user vừa tạo (userId, role, fullName, email), không trả về token. Bác sĩ/Admin đó dùng **Login** (mục 2) với username/password vừa tạo để lấy token và đăng nhập.

**Lỗi:**
- **403 / 1008:** Không phải Admin (chỉ ADMIN mới gọi được).
- **401:** Token hết hạn hoặc không hợp lệ.
- **1003 / 1004:** Username hoặc email đã tồn tại.

**Tạo Admin đầu tiên (khi chưa có Admin nào):**  
- **Cách 1:** Gọi **Register** tạo một bệnh nhân (username/email/password tùy chọn), sau đó vào bảng `users` trong DB đổi cột `role` từ `PATIENT` thành `ADMIN` cho user đó. Từ lần sau dùng **Login** với tài khoản đó và gọi **Admin tạo tài khoản** để tạo Bác sĩ/Admin khác.  
- **Cách 2:** Insert trực tiếp trong DB: thêm một dòng vào `users` với `role = 'ADMIN'`, cột `password_hash` phải là chuỗi BCrypt (có thể tạm dùng cách 1 để tạo user rồi copy `password_hash` từ user đó sang user admin mới).

---

## Thứ tự test gợi ý

**Luồng Bệnh nhân:**  
1. **Register** → lưu `accessToken`, `refreshToken`.  
2. **Logout** (dùng token vừa lưu).  
3. **Login** lại → lấy token mới.  
4. **Change password** (header `Authorization: Bearer <accessToken>`).  
5. **Login** lại bằng mật khẩu mới để xác nhận.

**Luồng Bác sĩ / Admin:**  
1. Có sẵn tài khoản (do Admin tạo qua **Admin tạo tài khoản** hoặc seed DB).  
2. **Login** với username/password của tài khoản đó (cùng endpoint như bệnh nhân).  
3. **Logout** khi cần (cùng endpoint, gửi Bearer token).

---

## Cấu hình nhanh trong Postman

1. Tạo **Environment** (biến):
   - `baseUrl` = `http://localhost:8080`
   - `accessToken` = (để trống, paste sau khi login)
   - `refreshToken` = (để trống, paste sau khi login)

2. URL request: `{{baseUrl}}/api/v1/auth/login`

3. Header Authorization: `Bearer {{accessToken}}`

4. Sau khi gọi Login/Register, dùng **Tests** trong Postman để lưu token tự động:

   ```js
   if (pm.response.code === 200) {
       const json = pm.response.json();
       if (json.data && json.data.accessToken) {
           pm.environment.set("accessToken", json.data.accessToken);
           pm.environment.set("refreshToken", json.data.refreshToken);
       }
   }
   ```

Sau đó các request **Logout** và **Change password** chỉ cần dùng `{{accessToken}}` và `{{refreshToken}}` trong Body/Headers.
