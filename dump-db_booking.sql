-- 1. Kích hoạt extension để hỗ trợ UUID (nếu dùng Postgres < 13, nhưng tốt nhất cứ bật)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. Tạo hàm để tự động cập nhật thời gian 'updated_at'
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- ==========================================
-- 3. TẠO CÁC BẢNG (Theo thứ tự phụ thuộc)
-- ==========================================

-- Bảng: Users (Bảng gốc, không phụ thuộc ai)
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    avatar_url TEXT,
    phone_number VARCHAR(20),
    role VARCHAR(50) DEFAULT 'PATIENT', -- Có thể là 'patient', 'doctor', 'admin'
    google_id VARCHAR(255),
    email_verified BOOLEAN DEFAULT FALSE,
    dob DATE,
    address TEXT,
    gender VARCHAR(20), -- 'male', 'female', 'other'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng: Specialties (Chuyên khoa)
CREATE TABLE specialties (
    specialty_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng: Doctors (Phụ thuộc Users và Specialties)
CREATE TABLE doctors (
    doctor_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    specialty_id UUID REFERENCES specialties(specialty_id) ON DELETE SET NULL,
    bio TEXT,
    experience TEXT,
    treatment_scope TEXT,
    CONSTRAINT uq_doctor_user UNIQUE (user_id) -- Một user chỉ làm 1 doctor
);

-- Bảng: Doctor_Schedules (Lịch làm việc chung)
CREATE TABLE doctor_schedules (
    schedule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL REFERENCES doctors(doctor_id) ON DELETE CASCADE,
    work_date TEXT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

-- Bảng: Doctor_Available_Slot (Các slot cụ thể có thể đặt)
CREATE TABLE doctor_available_slot (
    slot_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL REFERENCES doctors(doctor_id) ON DELETE CASCADE,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng: Appointments (Lịch hẹn)
CREATE TABLE appointments (
    appointment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES doctors(doctor_id) ON DELETE CASCADE,
    slot_id UUID REFERENCES doctor_available_slot(slot_id),
    appointment_date DATE NOT NULL,
    expected_time TIME,
    status VARCHAR(50) DEFAULT 'PENDING', -- 'pending', 'confirmed', 'completed', 'cancelled'
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng: Medical_Records (Hồ sơ bệnh án)
CREATE TABLE medical_records (
    record_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID REFERENCES appointments(appointment_id) ON DELETE SET NULL,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES doctors(doctor_id) ON DELETE CASCADE,
    diagnosis TEXT,
    prescription TEXT,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng: Review_Ratings (Đánh giá)
CREATE TABLE review_ratings (
    rating_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES doctors(doctor_id) ON DELETE CASCADE,
    rating INTEGER CHECK (rating >= 1 AND rating <= 5), -- Constraint điểm từ 1 đến 5
    review TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng: Notifications (Thông báo)
CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    type VARCHAR(50),
    title VARCHAR(255),
    message TEXT,
    related_id UUID, -- ID của đối tượng liên quan (ví dụ appointment_id)
    related_type VARCHAR(50), -- Tên bảng liên quan (ví dụ 'appointments')
    is_read BOOLEAN DEFAULT FALSE,
    is_sent BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 4. TẠO TRIGGER ĐỂ AUTO UPDATE 'updated_at'
-- ==========================================

CREATE TRIGGER set_timestamp_users
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

CREATE TRIGGER set_timestamp_appointments
BEFORE UPDATE ON appointments
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

CREATE TRIGGER set_timestamp_review_ratings
BEFORE UPDATE ON review_ratings
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();