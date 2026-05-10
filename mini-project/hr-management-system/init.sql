-- ============================================================
-- HR Management System — Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS db_auth;
CREATE DATABASE IF NOT EXISTS db_emp;
CREATE DATABASE IF NOT EXISTS db_leave;

-- ============================================================
-- db_auth — Authentication & Authorization
-- ============================================================
USE db_auth;

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)      NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,               -- BCrypt hash
    email       VARCHAR(100)    NOT NULL UNIQUE,
    full_name   VARCHAR(100)    NOT NULL,
    role        ENUM('HR', 'EMPLOYEE') NOT NULL DEFAULT 'EMPLOYEE',
    is_active   TINYINT(1)      NOT NULL DEFAULT 1,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_role     ON users (role);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id     BIGINT          NOT NULL,
    token_hash  VARCHAR(255)    NOT NULL,
    expires_at  DATETIME        NOT NULL,
    revoked_at  DATETIME        NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX uk_refresh_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_user_id         ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_expires_at      ON refresh_tokens (expires_at);

INSERT INTO users (username, password, email, full_name, role) VALUES
('hr_admin',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'hr@company.com',       'HR Admin',      'HR'),
('hr_manager', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'hr2@company.com',      'HR Manager',    'HR'),
('emp_nguyen', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'nguyen@company.com',   'Nguyễn Văn An', 'EMPLOYEE'),
('emp_tran',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'tran@company.com',     'Trần Thị Bình', 'EMPLOYEE'),
('emp_le',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'le@company.com',       'Lê Minh Cường', 'EMPLOYEE'),
('emp_pham',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'pham@company.com',     'Phạm Thị Dung', 'EMPLOYEE');


-- ============================================================
-- db_emp — Employee & Department Management
-- ============================================================
USE db_emp;

CREATE TABLE IF NOT EXISTS departments (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)    NOT NULL UNIQUE,
    description VARCHAR(255),
    manager_id  BIGINT,                                -- employee_id của trưởng phòng (nullable)
    is_active   TINYINT(1)      NOT NULL DEFAULT 1,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS employees (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    employee_code   VARCHAR(20)     NOT NULL UNIQUE,   -- VD: EMP001
    full_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(100)    NOT NULL UNIQUE,
    phone           VARCHAR(20),
    position        VARCHAR(100)    NOT NULL,           -- Chức vụ: Developer, Designer...
    department_id   BIGINT          NOT NULL,
    auth_user_id    BIGINT          NOT NULL UNIQUE,   -- FK logic tới db_auth.users.id (không FK vật lý)
    salary          DECIMAL(15, 2)  NOT NULL DEFAULT 0,
    start_date      DATE            NOT NULL,
    status          ENUM('ACTIVE', 'INACTIVE', 'ON_LEAVE') NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_emp_dept FOREIGN KEY (department_id) REFERENCES departments (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_emp_status        ON employees (status);
CREATE INDEX idx_emp_department    ON employees (department_id);
CREATE INDEX idx_emp_auth_user     ON employees (auth_user_id);
CREATE INDEX idx_emp_code          ON employees (employee_code);

INSERT INTO departments (id, name, description) VALUES
(1, 'Engineering',  'Phòng kỹ thuật phát triển phần mềm'),
(2, 'HR',           'Phòng nhân sự'),
(3, 'Marketing',    'Phòng marketing và truyền thông'),
(4, 'Finance',      'Phòng tài chính kế toán');

INSERT INTO employees (employee_code, full_name, email, phone, position, department_id, auth_user_id, salary, start_date, status) VALUES
                                                                                                                                      ('EMP001', 'Nguyễn Văn An', 'nguyen@company.com', '0901234567', 'Backend Developer',  1, 3, 15000000, '2024-01-15', 'ACTIVE'),
                                                                                                                                      ('EMP002', 'Trần Thị Bình', 'tran@company.com',   '0902345678', 'Frontend Developer', 1, 4, 14000000, '2024-02-01', 'ACTIVE'),
                                                                                                                                      ('EMP003', 'Lê Minh Cường', 'le@company.com',     '0903456789', 'Marketing Specialist',3, 5, 12000000, '2024-03-10', 'ACTIVE'),
                                                                                                                                      ('EMP004', 'Phạm Thị Dung', 'pham@company.com',   '0904567890', 'Accountant',          4, 6, 13000000, '2024-01-20', 'ACTIVE');

UPDATE departments SET manager_id = 1 WHERE id = 1;  -- Nguyễn Văn An làm trưởng Engineering
UPDATE departments SET manager_id = 3 WHERE id = 3;  -- Lê Minh Cường làm trưởng Marketing


-- ============================================================
-- db_leave — Leave Request Management
-- ============================================================
USE db_leave;

CREATE TABLE IF NOT EXISTS leave_requests (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    employee_id     BIGINT          NOT NULL,           -- FK logic tới db_emp.employees.id
    employee_code   VARCHAR(20)     NOT NULL,           -- Denormalize để tránh Feign call khi query
    employee_name   VARCHAR(100)    NOT NULL,           -- Denormalize
    department_name VARCHAR(100)    NOT NULL,           -- Denormalize
    leave_type      ENUM('ANNUAL', 'SICK', 'PERSONAL', 'UNPAID') NOT NULL,
    from_date       DATE            NOT NULL,
    to_date         DATE            NOT NULL,
    CHECK (to_date >= from_date),
    total_days      INT             NOT NULL,           -- Tính sẵn khi tạo đơn
    reason          TEXT            NOT NULL,
    status          ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    reviewed_by     BIGINT,                             -- auth_user_id của HR duyệt (nullable)
    reviewer_note   VARCHAR(500),                       -- Ghi chú khi duyệt/từ chối
    reviewed_at     DATETIME,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_leave_employee_id  ON leave_requests (employee_id);
CREATE INDEX idx_leave_status       ON leave_requests (status);
CREATE INDEX idx_leave_from_date    ON leave_requests (from_date);
CREATE INDEX idx_leave_type         ON leave_requests (leave_type);
CREATE INDEX idx_leave_status_dept  ON leave_requests (status, department_name);

INSERT INTO leave_requests (employee_id, employee_code, employee_name, department_name, leave_type, from_date, to_date, total_days, reason, status) VALUES
(1, 'EMP001', 'Nguyễn Văn An', 'Engineering', 'ANNUAL',   '2026-06-01', '2026-06-03', 3, 'Nghỉ phép năm',              'APPROVED'),
(2, 'EMP002', 'Trần Thị Bình', 'Engineering', 'SICK',     '2026-05-20', '2026-05-21', 2, 'Ốm, có giấy bác sĩ',         'APPROVED'),
(3, 'EMP003', 'Lê Minh Cường', 'Marketing',   'PERSONAL', '2026-06-10', '2026-06-10', 1, 'Việc gia đình',              'PENDING'),
(1, 'EMP001', 'Nguyễn Văn An', 'Engineering', 'ANNUAL',   '2026-07-01', '2026-07-05', 5, 'Đi du lịch cùng gia đình',  'PENDING'),
(4, 'EMP004', 'Phạm Thị Dung', 'Finance',     'SICK',     '2026-05-15', '2026-05-15', 1, 'Khám sức khỏe định kỳ',     'REJECTED');