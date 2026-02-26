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
    gender VARCHAR(20), -- 'MALE', 'FEMALE', 'other'
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

INSERT INTO users (username, email, full_name, password_hash, role, gender, email_verified)
VALUES
('nguyenthihoaian', 'hoaian.nguyen@clinic.com', 'Bác sĩ Nguyễn Thị Hoài An', 'd12345', 'DOCTOR', 'FEMALE', TRUE),
('nguyenxuandat', 'xuandat.nguyen@clinic.com', 'Bác sĩ Nguyễn Xuân Đạt', 'd12345', 'DOCTOR', 'MALE', TRUE),
('ngotrongthe', 'trongthe.ngo@clinic.com', 'Bác sĩ Ngô Trọng Thế', 'd12345', 'DOCTOR', 'MALE', TRUE),
('phammanhthan', 'manhthan.pham@clinic.com', 'Bác sĩ Phạm Mạnh Thân', 'd12345', 'DOCTOR', 'MALE', TRUE),
('leduchinh', 'duchinh.le@clinic.com', 'GS. TS. Lê Đức Hinh', 'd12345', 'DOCTOR', 'MALE', TRUE),
('nguyenthitam', 'tam.nguyen@clinic.com', 'TS.BS Nguyễn Thị Tâm', 'd12345', 'DOCTOR', 'FEMALE', TRUE),
('phamthiquynh', 'quynh.pham@clinic.com', 'Bác sĩ Phạm Thị Quỳnh', 'd12345', 'DOCTOR', 'FEMALE', TRUE),
('caochitrung', 'chitrung.cao@clinic.com', 'Bác sĩ Cao Chí Trung', 'd12345', 'DOCTOR', 'MALE', TRUE),
('vuthaiha', 'thaiha.vu@clinic.com', 'Bác sĩ Vũ Thái Hà', 'd12345', 'DOCTOR', 'FEMALE', TRUE),
('nguyentienthanh', 'tienthanh.nguyen@clinic.com', 'Bác sĩ Nguyễn Tiến Thành', 'd12345', 'DOCTOR', 'MALE', TRUE),
('nguyenthilanhuong', 'lanhuong.nguyen@clinic.com', 'Bác sĩ Nguyễn Thị Lan Hương', 'd12345', 'DOCTOR', 'FEMALE', TRUE),
('thaivanthanh', 'vanthanh.thai@clinic.com', 'Bác sĩ Thái Văn Thành', 'd12345', 'DOCTOR', 'MALE', TRUE),
('dangbahiep', 'bahiep.dang@clinic.com', 'Ths. BSCKII Đặng Bá Hiệp', 'd12345', 'DOCTOR', 'MALE', TRUE),
('dangvangiap', 'vangiap.dang@clinic.com', 'BS. CKI Đặng Văn Giáp', 'd12345', 'DOCTOR', 'MALE', TRUE);
('admin', 'admin@admin.com', 'Admin', 'admin123', 'ADMIN', 'MALE', TRUE);
INSERT INTO specialties (name, description)
VALUES
('Tai mũi họng', 'Khám và điều trị các bệnh lý về Tai - Mũi - Họng.'),
('Nhi khoa', 'Khám và điều trị các bệnh lý cho trẻ sơ sinh, trẻ em và trẻ vị thành niên.'),
('Thần kinh', 'Chẩn đoán và điều trị các rối loạn, bệnh lý liên quan đến hệ thần kinh.'),
('Sản phụ khoa', 'Chăm sóc sức khỏe sinh sản nữ giới, theo dõi thai kỳ và các bệnh lý phụ khoa.'),
('Da liễu', 'Khám và điều trị các bệnh lý liên quan đến da, lông, tóc và móng.'),
('Nội khoa', 'Chẩn đoán và điều trị các bệnh lý nội tạng không yêu cầu can thiệp phẫu thuật.'),
('Ngoại khoa', 'Khám và điều trị các bệnh lý, chấn thương cần can thiệp bằng phương pháp phẫu thuật.');


INSERT INTO doctors (user_id, specialty_id, bio, experience, treatment_scope)
VALUES
-- 1. Bác sĩ Nguyễn Thị Hoài An -> Tai mũi họng
((SELECT user_id FROM users WHERE username = 'nguyenthihoaian'), (SELECT specialty_id FROM specialties WHERE name = 'Tai mũi họng'), 'Gần 40 năm kinh nghiệm điều trị và phẫu thuật thành công hàng ngàn ca viêm Amidan, viêm VA, viêm xoang, u thanh quản, u hạ họng, viêm tai giữa, ù tai, nghe kém, điếc đột ngột, thủng màng nhĩ...', 'Nguyên Trưởng khoa Tai mũi họng trẻ em, Bệnh viện Tai Mũi Họng Trung ương', 'Chuyên khám và điều trị các bệnh lý Tai Mũi Họng trẻ em
Nội soi Tai Mũi Họng. Thực hiện các qui trình kỹ thuật Tai Mũi Họng'),

-- 2. Bác sĩ Nguyễn Xuân Đạt -> Tai mũi họng
((SELECT user_id FROM users WHERE username = 'nguyenxuandat'), (SELECT specialty_id FROM specialties WHERE name = 'Tai mũi họng'), 'Với 15 năm kinh nghiệm trong khám chữa bệnh Tai Mũi Họng. Có nhiều kinh nghiệm trong khám, điều trị và phẫu thuật Tai Mũi Họng, đặc biệt chuyên sâu về các bệnh lý ở trẻ em.', 'Sáng lập, Quản lý & vận hành, phụ trách chuyên môn – Phòng khám chuyên khoa tai mũi họng Entic (11/2017 – 03/2025)
Giám đốc chuyên môn kiêm Giám đốc vận hành – Doctor Anywhere Vietnam (05/2019 – 09/2020).
Chuyên viên cấp cao thẩm định và giải quyết quyền lợi bảo hiểm – Công ty bảo hiểm nhân thọ BIDV MetLife (09/2016 – 05/2019).
Giảng viên bộ môn Tai Mũi Họng – Đại học Y Dược Thái Nguyên (2011 – 2016).
Bác sĩ Khoa Tai Mũi Họng – Bệnh viện Trung ương Thái Nguyên (2011 – 2016).
Tốt nghiệp Thạc sĩ chuyên ngành Tai Mũi Họng, Đại học Y Hà Nội (2014 - 2016).
Tốt nghiệp Bác sĩ chuyên khoa Tai Mũi Họng, Đại học Y Hà Nội (2005 - 2011).', 'Chuyên khám và điều trị các bệnh lý Tai Mũi Họng trẻ em
Các bệnh về tai, mũi xoang và thanh quản'),

-- 3. Bác sĩ Ngô Trọng Thế -> Nhi khoa
((SELECT user_id FROM users WHERE username = 'ngotrongthe'), (SELECT specialty_id FROM specialties WHERE name = 'Nhi khoa'), 'Bác sĩ có nhiều năm kinh nghiệm thăm khám và điều trị khoa Nhi ', 'Công tác tại Bệnh viện Nhi Trung ương (2022 - 2024).
Tốt nghiệp Đại học Y Hà Nội.', 'Bệnh lý sơ sinh.
Bệnh tiêu hóa.
Bệnh tuần hoàn.
Bệnh hô hấp.
Bệnh huyết học.'),

-- 4. Bác sĩ Phạm Mạnh Thân -> Nhi khoa
((SELECT user_id FROM users WHERE username = 'phammanhthan'), (SELECT specialty_id FROM specialties WHERE name = 'Nhi khoa'), 'Bác sĩ có kinh nghiệm chuyên sâu khám chữa nhiều mặt bệnh Nội khoa của trẻ em như: Bệnh hô hấp, tiêu hoá, dinh dưỡng…. Và các bệnh lý Nhi khoa khác.', 'Bác sĩ Nhi & kiêm Trưởng phòng kế hoạch tổng hợp, Bệnh viện Xanh Pôn (1992 - 2016).
Bác sĩ Nội trú, Bệnh viện ST Charle charles - Montpellier - Cộng hoà Pháp (1991 – 1992).
Bác sĩ chuyên khoa Nhi, Bệnh viện Xanh Pôn (1983 - 1991).', 'Nhi khoa tổng quát.
Nhi chuyên sâu (phổi, tiêu hoá, hô hấp, nội tiết, thận, thần kinh).
Nhi khoa sơ sinh.
Các bệnh truyền nhiễm và tiêm chủng.
Dinh dưỡng.'),

-- 5. GS. TS. Lê Đức Hinh -> Thần kinh
((SELECT user_id FROM users WHERE username = 'leduchinh'), (SELECT specialty_id FROM specialties WHERE name = 'Thần kinh'), 'Giáo sư - Tiến sỹ, Chuyên gia đầu ngành về Thần kinh.
Danh hiệu Thầy thuốc Nhân dân.', 'Chủ tịch Hội Thần kinh học Việt Nam.
Nguyên Trưởng khoa Thần kinh - Bệnh viện Bạch Mai.', 'Giáo sư khám và điều trị các bệnh lý về Thần kinh.
Bệnh Parkinson, chẩn đoán và điều trị các cơn co giật.'),

-- 6. TS.BS Nguyễn Thị Tâm -> Thần kinh
((SELECT user_id FROM users WHERE username = 'nguyenthitam'), (SELECT specialty_id FROM specialties WHERE name = 'Thần kinh'), 'Bác sĩ có 40 năm kinh nghiệm thăm khám và điều trị chuyên khoa Nội Thần kinh
Chuyên gia Thần kinh.', 'Nguyên Trưởng Khoa Nội Thần kinh, Bệnh viện Trung ương Quân đội 108', 'Các bệnh đau đầu: Chứng đau nửa đầu, đau đầu căn nguyên mạch máu, đau đầu mạn tính hàng ngày,..
Bệnh đau vai gáy do thoái hóa cột sống cổ, thoát vị đĩa đệm cột sống cổ, …
Đau thắt lưng hông do thoái hóa, thoát vị, đau do viêm khớp cùng chậu…
Điều trị chóng mặt do thiếu máu não.'),

-- 7. Bác sĩ Phạm Thị Quỳnh -> Sản phụ khoa
((SELECT user_id FROM users WHERE username = 'phamthiquynh'), (SELECT specialty_id FROM specialties WHERE name = 'Sản phụ khoa'), 'Bác sĩ có hơn 40 năm kinh nghiệm trong lĩnh vực Sản Phụ khoa', 'Nguyên Trưởng khoa Sản, Bệnh viện E Hà Nội', 'Khám thai và theo dõi thai kì.
Chẩn đoán trước sinh và quản lý thai.
Khám về điều trị các bệnh lý phụ khoa.
Tư vấn phẫu thuật sản khoa.'),

-- 8. Bác sĩ Cao Chí Trung -> Sản phụ khoa
((SELECT user_id FROM users WHERE username = 'caochitrung'), (SELECT specialty_id FROM specialties WHERE name = 'Sản phụ khoa'), '25 năm kinh nghiệm khám và điều trị Sản phụ khoa - Vô sinh hiếm muộn. Phó Trưởng khoa sản - BV quân Y 354.', 'Phó Trưởng khoa sản - BV quân Y 354.
Bác sĩ có các chứng chỉ chuyên môn từ Bệnh viện Phụ sản Trung Ương và Bệnh viện Từ Dũ.', 'Khám và điều trị vô sinh hiếm muộn.
Khám, siêu âm và theo dõi tự nhiên, IUI, IVF.
Khám, theo dõi và tư vấn thai kỳ.
Khám, phát hiện và điều trị các bệnh lý phụ khoa, tầm soát ung thư cổ tử cung.
Tư vấn các biện pháp tránh thai: Cấy que tránh thai, đặt vòng…'),

-- 9. Bác sĩ Vũ Thái Hà -> Da liễu
((SELECT user_id FROM users WHERE username = 'vuthaiha'), (SELECT specialty_id FROM specialties WHERE name = 'Da liễu'), 'Bác sĩ có hơn 16 năm kinh nghiệm trong khám và điều trị Da liễu, Da liễu Thẩm mỹ.
Trưởng khoa Khoa nghiên cứu và ứng dụng công nghệ tế bào gốc - Bệnh viện Da liễu Trung ương.', 'Trưởng khoa Khoa nghiên cứu và ứng dụng công nghệ tế bào gốc - Bệnh viện Da liễu Trung ương (2016 - nay).
Phó trưởng khoa phụ trách Khoa nghiên cứu và ứng dụng công nghệ tế bào gốc - Bệnh viện Da liễu Trung ương (4/2016 - 6/2016).
Phó trưởng khoa Phẫu thuật tạo hình và Phục hồi chức năng - Bệnh viện Da liễu Trung ương (7/2015 - 4/2016).
Phó trường khoa Laser phẫu thuật (D1) - Bệnh viện Da liễu Trung ương (5/2013 - 7/2015).
Giáo vụ bộ môn Da liễu - Đại học Y Hà Nội (6/2012 - 4/2013).
Bác sĩ Da liễu tại khoa Laser - phẫu thuật bệnh viện Da liễu Trung ương (từ 1/2013).
Giảng viện bộ môn da liễu - Trường Đại học Y Hà Nội (từ 1/2013).', 'Viêm da cơ địa: Tổn thương da khô, ban đỏ kèm theo hiện tượng ngứa.
Viêm da tiếp xúc: Tổn thương vùng bị tiếp xúc, dát đỏ, mụn nước , có thể loét , kèm ngứa.
Viêm da dầu: Các mảng dát màu đỏ hồng, có vảy da trắng mỏng nhờn dính kèm ngứa ở nếp gấp, rãnh mũi má.
Mày đay: Sẩn phù, ngứa, phù mạch, nguyên nhân chủ yếu do dị ứng.
Zona: Ban đỏ, đám mụn nước ,vết loét, cảm giác ngứa rát dọc theo dây thần kinh.
Thủy đậu: Sốt, mụn nước to dịch trong lan toàn thân , sau vỡ vảy tiết, sẹo.
Nấm da: Mảng nổi nhẹ, có hình vòng hoặc bầu dục, có màu đỏ hoặc nâu, tróc vảy và gây ngứa.
U mềm lây: Nốt sẩn nhỏ, lõm trung tâm, rải rác toàn thân, không ngứa, không đau.
Viêm hạch lan tỏa. '),

-- 10. Bác sĩ Nguyễn Tiến Thành -> Da liễu
((SELECT user_id FROM users WHERE username = 'nguyentienthanh'), (SELECT specialty_id FROM specialties WHERE name = 'Da liễu'), 'Hơn 15 năm kinh nghiệm trong chuyên khoa Da liễu và thế mạnh chính về Laser sắc tố da.
Phó Trưởng phòng Quản lý chất lượng, Bệnh viện Da liễu Trung ương', 'Phó Trưởng phòng Quản lý chất lượng, Bệnh viện Da liễu Trung ương (2022 - Nay).
Bác sĩ khoa Laser và săn sóc da, Bệnh viện Da liễu Trung ương (2015 - Nay).
Phó Trưởng khoa Laser và săn sóc da, Bệnh viện Da liễu Trung ương (2019 - 2022).
Phó Trưởng phòng Tổ chức cán bộ, Bệnh viện Da liễu Trung ương (2015 - 2018).
Bác sĩ khoa Điều trị bệnh da nữ và trẻ em, Bệnh viện Da liễu Trung ương (2010 - 2015).', 'Viêm da cơ địa, mày đay, nấm da, nấm móng, vảy nến,…
Trứng cá, thâm mụn, sẹo lõm, sẹo lồi, sẹo xấu,…
Zona, herpes, hạt cơm,…
Điều trị các bệnh lây truyền qua đường tình dục: Sùi mào gà, u mềm lây, lậu, ghẻ,…'),

-- 11. Bác sĩ Nguyễn Thị Lan Hương -> Nội khoa
((SELECT user_id FROM users WHERE username = 'nguyenthilanhuong'), (SELECT specialty_id FROM specialties WHERE name = 'Nội khoa'), 'Đạt danh hiệu Thầy thuốc Ưu tú.
Gần 40 năm kinh nghiệm trong lĩnh vực Nội tổng hợp', 'Tốt nghiệp Bác sĩ chuyên khoa I Nội tổng hợp, Học viện Quân Y .
Học chuyên ngành  Nội Nhi, Đại học Y khoa Hà Nội (1974 - 1980).
Từng làm việc tại Phòng khám Bộ Công an (1980 - 2012).', 'Các bệnh Nội thận, tiết niệu: Phù, thiếu máu, đau thắt lưng, tiểu buốt, tiểu máu...
Các bệnh về máu: Thiếu máu, xuất huyết, sốt, hạch to, lách to...đau đầu, chóng mặt.
Các bệnh Nội Tiêu hóa: Đau bụng, vàng da, tiểu vàng, rối loạn tiêu hóa.
Các bệnh Xương khớp: Sưng đau khớp, hội chứng cổ vai, đau cột sống.
Các bệnh nội tiết: Đái tháo đường, Basedow...'),

-- 12. Bác sĩ Thái Văn Thành -> Ngoại khoa
((SELECT user_id FROM users WHERE username = 'thaivanthanh'), (SELECT specialty_id FROM specialties WHERE name = 'Nội khoa'), '20 năm kinh nghiệm lĩnh vực Nội Tổng quát
Tham gia nhiều khóa học nâng cao tại các đơn vị, bệnh viện lớn: Bệnh viện Chợ Rẫy', 'Giám đốc, Bác sĩ Nội Tổng quát - Phòng khám đa khoa Quốc tế Việt Healthcare (2020 - Nay).
Giám đốc, Bác sĩ Nội Tổng quát - Phòng khám đa khoa Pháp Anh (2009 - 2019).
Bác sĩ Nội Thận - Phòng khám Đa khoa Medic Hòa Hảo (2003 - 2009).', 'Bệnh lý chuyên khoa Nội thận và Nội tiêu hóa:
Viêm loét dạ dày: đau bụng, nôn ói, đầy bụng.
Viêm thực quản trào ngược: nghẹn, nuốt nghẹn, khó thở, ho kéo dài.
Nhiễm HP dạ dày: đau bụng, nôn ói, đầy bụng.
Đái tháo đường.
Nội soi tiêu hóa.
Rối loạn tiêu hóa: rối loạn đi tiêu.
Viêm đại tràng: rối loạn đi tiêu lúc táo bón lúc tiêu chảy.'),

-- 13. Ths. BSCKII Đặng Bá Hiệp -> Ngoại khoa
((SELECT user_id FROM users WHERE username = 'dangbahiep'), (SELECT specialty_id FROM specialties WHERE name = 'Ngoại khoa'), 'Bác sĩ có gần 20 năm kinh nghiệm về chuyên khoa Ngoại Vú - Phụ Khoa.
Phó trưởng khoa chuyên khoa Ngoại Vú – Phụ Khoa.
Bác sĩ nhận khám từ 15 tuổi trở lên.', 'Giảng viên thỉnh giảng, Đại học Y dược Thái Nguyên (2022 - nay).
Giảng viên thỉnh giảng, Khoa Y Đại học Kinh doanh và Công nghệ (2021 - nay).
Bác sĩ Phó Trưởng Khoa, Khoa Ngoại Vú Phòng khám Bệnh viện Ung Bướu Hà Nội (2017 - nay).
Bác sĩ Khoa Ngoại Vú,  Phòng khám Bệnh viện Ung Bướu Hà Nội (2011 - 2017).
Bác sĩ Khoa ngoại Tổng Hợp, Bệnh viện Ung Bướu Hà Nội (2007 - 2011).', 'Khám và phẫu thuật ung thư vú.
Khám và phẫu thuật ung thư phụ khoa.
Khám và phẫu thuật u lành tính vú và phụ khoa.
Khám và điều trị bệnh lành tính tuyến vú.
Phẫu thuật bảo tồn và tạo hình điều trị ung thư vú.'),

-- 14. BS. CKI Đặng Văn Giáp -> Ngoại khoa
((SELECT user_id FROM users WHERE username = 'dangvangiap'), (SELECT specialty_id FROM specialties WHERE name = 'Ngoại khoa'), 'Bác sĩ có hơn 40 năm kinh nghiệm trong lĩnh vực Ngoại khoa.
Bác sĩ đang làm việc tại Đa khoa Quốc tế Việt Nga (VRR).
Bác sĩ nhận khám từ 18 tuổi.', 'đang công tác tại phòng khám Đa khoa Quốc Tế Việt - Nga (VRR).', 'Bác sĩ chuyên khám và chẩn đoán và điều trị bệnh lý ngoại khoa tổng quát.');

-- 1. Bác sĩ Nguyễn Thị Hoài An
WITH target_doctor AS (
    SELECT d.doctor_id FROM doctors d JOIN users u ON d.user_id = u.user_id WHERE u.username = 'nguyenthihoaian'
)
INSERT INTO doctor_schedules (doctor_id, work_date, start_time, end_time)
SELECT doctor_id, work_date, CAST(start_time AS TIME), CAST(end_time AS TIME)
FROM target_doctor CROSS JOIN (
    VALUES ('Thứ 2', '08:00', '12:00'), ('Thứ 5', '08:00', '12:00'), ('Thứ 6', '08:00', '12:00'),
           ('Thứ 2', '13:00', '17:00'), ('Thứ 3', '13:00', '17:00'), ('Thứ 4', '13:00', '17:00'), ('Thứ 6', '13:00', '17:00'), ('Thứ 7', '13:00', '17:00'), ('Chủ nhật', '13:00', '17:00')
) AS schedule(work_date, start_time, end_time);

-- 2. Bác sĩ Nguyễn Xuân Đạt
WITH target_doctor AS (
    SELECT d.doctor_id FROM doctors d JOIN users u ON d.user_id = u.user_id WHERE u.username = 'nguyenxuandat'
)
INSERT INTO doctor_schedules (doctor_id, work_date, start_time, end_time)
SELECT doctor_id, work_date, CAST(start_time AS TIME), CAST(end_time AS TIME)
FROM target_doctor CROSS JOIN (
    VALUES ('Thứ 3', '08:00', '12:00'), ('Thứ 4', '08:00', '12:00'), ('Thứ 5', '08:00', '12:00'), ('Thứ 6', '08:00', '12:00'), ('Thứ 7', '08:00', '12:00'), ('Chủ nhật', '08:00', '12:00'),
           ('Thứ 2', '13:00', '17:00'), ('Thứ 5', '13:00', '17:00')
) AS schedule(work_date, start_time, end_time);

-- 3. Bác sĩ Ngô Trọng Thế
WITH target_doctor AS (
    SELECT d.doctor_id FROM doctors d JOIN users u ON d.user_id = u.user_id WHERE u.username = 'ngotrongthe'
)
INSERT INTO doctor_schedules (doctor_id, work_date, start_time, end_time)
SELECT doctor_id, work_date, CAST(start_time AS TIME), CAST(end_time AS TIME)
FROM target_doctor CROSS JOIN (
    VALUES ('Thứ 4', '08:00', '12:00'), ('Thứ 6', '08:00', '12:00'), ('Chủ nhật', '08:00', '12:00'),
           ('Thứ 2', '13:00', '17:00'), ('Thứ 3', '13:00', '17:00'), ('Thứ 4', '13:00', '17:00'), ('Thứ 5', '13:00', '17:00'), ('Thứ 6', '13:00', '17:00')
) AS schedule(work_date, start_time, end_time);

-- 4. Bác sĩ Phạm Mạnh Thân
WITH target_doctor AS (
    SELECT d.doctor_id FROM doctors d JOIN users u ON d.user_id = u.user_id WHERE u.username = 'phammanhthan'
)
INSERT INTO doctor_schedules (doctor_id, work_date, start_time, end_time)
SELECT doctor_id, work_date, CAST(start_time AS TIME), CAST(end_time AS TIME)
FROM target_doctor CROSS JOIN (
    VALUES ('Thứ 2', '08:00', '12:00'), ('Thứ 3', '08:00', '12:00'), ('Thứ 4', '08:00', '12:00'), ('Thứ 5', '08:00', '12:00'), ('Thứ 7', '08:00', '12:00'),
           ('Thứ 2', '13:00', '17:00'), ('Thứ 3', '13:00', '17:00'), ('Thứ 6', '13:00', '17:00'), ('Thứ 7', '13:00', '17:00')
) AS schedule(work_date, start_time, end_time);

-- 5. GS. TS. Lê Đức Hinh
WITH target_doctor AS (
    SELECT d.doctor_id FROM doctors d JOIN users u ON d.user_id = u.user_id WHERE u.username = 'leduchinh'
)
INSERT INTO doctor_schedules (doctor_id, work_date, start_time, end_time)
SELECT doctor_id, work_date, CAST(start_time AS TIME), CAST(end_time AS TIME)
FROM target_doctor CROSS JOIN (
    VALUES ('Thứ 3', '08:00', '12:00'), ('Thứ 4', '08:00', '12:00'), ('Thứ 5', '08:00', '12:00'), ('Thứ 6', '08:00', '12:00'), ('Thứ 7', '08:00', '12:00'), ('Chủ nhật', '08:00', '12:00'),
           ('Thứ 2', '13:00', '17:00'), ('Thứ 4', '13:00', '17:00')
) AS schedule(work_date, start_time, end_time);

-- 6. TS.BS Nguyễn Thị Tâm
WITH target_doctor AS (
    SELECT d.doctor_id FROM doctors d JOIN users u ON d.user_id = u.user_id WHERE u.username = 'nguyenthitam'
)
INSERT INTO doctor_schedules (doctor_id, work_date, start_time, end_time)
SELECT doctor_id, work_date, CAST(start_time AS TIME), CAST(end_time AS TIME)
FROM target_doctor CROSS JOIN (
    VALUES ('Thứ 3', '08:00', '12:00'), ('Thứ 4', '08:00', '12:00'), ('Thứ 7', '08:00', '12:00'),
           ('Thứ 2', '13:00', '17:00'), ('Thứ 3', '13:00', '17:00'), ('Thứ 5', '13:00', '17:00'), ('Thứ 6', '13:00', '17:00'), ('Thứ 7', '13:00', '17:00')
) AS schedule(work_date, start_time, end_time);

-- 7. Bác sĩ Phạm Thị Quỳnh
WITH target_doctor AS (
    SELECT d.doctor_id FROM doctors d JOIN users u ON d.user_id = u.user_id WHERE u.username = 'phamthiquynh'
)
INSERT INTO doctor_schedules (doctor_id, work_date, start_time, end_time)
SELECT doctor_id, work_date, CAST(start_time AS TIME), CAST(end_time AS TIME)
FROM target_doctor CROSS JOIN (
    VALUES ('Thứ 2', '08:00', '12:00'), ('Thứ 3', '08:00', '12:00'), ('Thứ 5', '08:00', '12:00'), ('Thứ 6', '08:00', '12:00'), ('Thứ 7', '08:00', '12:00'), ('Chủ nhật', '08:00', '12:00'),
           ('Thứ 3', '13:00', '17:00'), ('Thứ 4', '13:00', '17:00'), ('Thứ 6', '13:00', '17:00')
) AS schedule(work_date, start_time, end_time);

-- 8. Bác sĩ Cao Chí Trung
WITH target_doctor AS (
    SELECT d.doctor_id FROM doctors d JOIN users u ON d.user_id = u.user_id WHERE u.username = 'caochitrung'
)
INSERT INTO doctor_schedules (doctor_id, work_date, start_time, end_time)
SELECT doctor_id, work_date, CAST(start_time AS TIME), CAST(end_time AS TIME)
FROM target_doctor CROSS JOIN (
    VALUES ('Thứ 2', '08:00', '12:00'), ('Thứ 5', '08:00', '12:00'), ('Thứ 6', '08:00', '12:00'),
           ('Thứ 2', '13:00', '17:00'), ('Thứ 3', '13:00', '17:00'), ('Thứ 6', '13:00', '17:00'), ('Thứ 7', '13:00', '17:00'), ('Chủ nhật', '13:00', '17:00')
) AS schedule(work_date, start_time, end_time);

-- 9. Bác sĩ Vũ Thái Hà
WITH target_doctor AS (
    SELECT d.doctor_id FROM doctors d JOIN users u ON d.user_id = u.user_id WHERE u.username = 'vuthaiha'
)
INSERT INTO doctor_schedules (doctor_id, work_date, start_time, end_time)
SELECT doctor_id, work_date, CAST(start_time AS TIME), CAST(end_time AS TIME)
FROM target_doctor CROSS JOIN (
    VALUES ('Thứ 3', '08:00', '12:00'), ('Thứ 5', '08:00', '12:00'), ('Thứ 6', '08:00', '12:00'), ('Thứ 7', '08:00', '12:00'), ('Chủ nhật', '08:00', '12:00'),
           ('Thứ 2', '13:00', '17:00'), ('Thứ 3', '13:00', '17:00'), ('Thứ 7', '13:00', '17:00')
) AS schedule(work_date, start_time, end_time);

-- 10. Bác sĩ Nguyễn Tiến Thành
WITH target_doctor AS (
    SELECT d.doctor_id FROM doctors d JOIN users u ON d.user_id = u.user_id WHERE u.username = 'nguyentienthanh'
)
INSERT INTO doctor_schedules (doctor_id, work_date, start_time, end_time)
SELECT doctor_id, work_date, CAST(start_time AS TIME), CAST(end_time AS TIME)
FROM target_doctor CROSS JOIN (
    VALUES ('Thứ 2', '08:00', '12:00'), ('Thứ 4', '08:00', '12:00'), ('Thứ 6', '08:00', '12:00'),
           ('Thứ 3', '13:00', '17:00'), ('Thứ 4', '13:00', '17:00'), ('Thứ 5', '13:00', '17:00'), ('Thứ 6', '13:00', '17:00'), ('Thứ 7', '13:00', '17:00')
) AS schedule(work_date, start_time, end_time);

-- 11. Bác sĩ Nguyễn Thị Lan Hương
WITH target_doctor AS (
    SELECT d.doctor_id FROM doctors d JOIN users u ON d.user_id = u.user_id WHERE u.username = 'nguyenthilanhuong'
)
INSERT INTO doctor_schedules (doctor_id, work_date, start_time, end_time)
SELECT doctor_id, work_date, CAST(start_time AS TIME), CAST(end_time AS TIME)
FROM target_doctor CROSS JOIN (
    VALUES ('Thứ 2', '08:00', '12:00'), ('Thứ 5', '08:00', '12:00'), ('Thứ 6', '08:00', '12:00'),
           ('Thứ 2', '13:00', '17:00'), ('Thứ 3', '13:00', '17:00'), ('Thứ 4', '13:00', '17:00'), ('Thứ 6', '13:00', '17:00'), ('Thứ 7', '13:00', '17:00'), ('Chủ nhật', '13:00', '17:00')
) AS schedule(work_date, start_time, end_time);

-- 12. Bác sĩ Thái Văn Thành
WITH target_doctor AS (
    SELECT d.doctor_id FROM doctors d JOIN users u ON d.user_id = u.user_id WHERE u.username = 'thaivanthanh'
)
INSERT INTO doctor_schedules (doctor_id, work_date, start_time, end_time)
SELECT doctor_id, work_date, CAST(start_time AS TIME), CAST(end_time AS TIME)
FROM target_doctor CROSS JOIN (
    VALUES ('Thứ 3', '08:00', '12:00'), ('Thứ 4', '08:00', '12:00'), ('Thứ 5', '08:00', '12:00'), ('Thứ 6', '08:00', '12:00'), ('Thứ 7', '08:00', '12:00'), ('Chủ nhật', '08:00', '12:00'),
           ('Thứ 2', '13:00', '17:00'), ('Thứ 5', '13:00', '17:00')
) AS schedule(work_date, start_time, end_time);

-- 13. Ths. BSCKII Đặng Bá Hiệp
WITH target_doctor AS (
    SELECT d.doctor_id FROM doctors d JOIN users u ON d.user_id = u.user_id WHERE u.username = 'dangbahiep'
)
INSERT INTO doctor_schedules (doctor_id, work_date, start_time, end_time)
SELECT doctor_id, work_date, CAST(start_time AS TIME), CAST(end_time AS TIME)
FROM target_doctor CROSS JOIN (
    VALUES ('Thứ 4', '08:00', '12:00'), ('Thứ 6', '08:00', '12:00'), ('Chủ nhật', '08:00', '12:00'),
           ('Thứ 2', '13:00', '17:00'), ('Thứ 3', '13:00', '17:00'), ('Thứ 4', '13:00', '17:00'), ('Thứ 5', '13:00', '17:00'), ('Thứ 6', '13:00', '17:00')
) AS schedule(work_date, start_time, end_time);

-- 14. BS. CKI Đặng Văn Giáp
WITH target_doctor AS (
    SELECT d.doctor_id FROM doctors d JOIN users u ON d.user_id = u.user_id WHERE u.username = 'dangvangiap'
)
INSERT INTO doctor_schedules (doctor_id, work_date, start_time, end_time)
SELECT doctor_id, work_date, CAST(start_time AS TIME), CAST(end_time AS TIME)
FROM target_doctor CROSS JOIN (
    VALUES ('Thứ 2', '08:00', '12:00'), ('Thứ 3', '08:00', '12:00'), ('Thứ 4', '08:00', '12:00'), ('Thứ 5', '08:00', '12:00'), ('Thứ 7', '08:00', '12:00'),
           ('Thứ 2', '13:00', '17:00'), ('Thứ 3', '13:00', '17:00'), ('Thứ 6', '13:00', '17:00'), ('Thứ 7', '13:00', '17:00')
) AS schedule(work_date, start_time, end_time);
