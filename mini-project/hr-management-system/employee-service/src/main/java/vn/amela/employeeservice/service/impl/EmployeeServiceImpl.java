package vn.amela.employeeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import vn.amela.employeeservice.dto.request.CreateEmployeeRequest;
import vn.amela.employeeservice.dto.request.EmployeeFilterRequest;
import vn.amela.employeeservice.dto.request.UpdateContactRequest;
import vn.amela.employeeservice.dto.request.UpdateEmployeeRequest;
import vn.amela.employeeservice.dto.response.EmployeeResponse;
import vn.amela.employeeservice.dto.response.PageResponse;
import vn.amela.employeeservice.entity.Department;
import vn.amela.employeeservice.entity.Employee;
import vn.amela.employeeservice.entity.EmployeeCreatedPayload;
import vn.amela.employeeservice.entity.OutboxEvent;
import vn.amela.employeeservice.entity.enums.EmployeeStatus;
import vn.amela.employeeservice.exception.BusinessException;
import vn.amela.employeeservice.exception.DuplicateResourceException;
import vn.amela.employeeservice.exception.ResourceNotFoundException;
import vn.amela.employeeservice.mapper.DepartmentMapper;
import vn.amela.employeeservice.mapper.EmployeeMapper;
import vn.amela.employeeservice.mapper.OutboxEventMapper;
import vn.amela.employeeservice.service.EmployeeService;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final String EMPLOYEE_CREATED_EVENT = "employee.created";
    private static final String EMPLOYEE_AGGREGATE_TYPE = "Employee";

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final JsonMapper objectMapper;

    @Override
    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        String employeeCode = normalizeEmployeeCode(request.employeeCode());
        String email = normalizeEmail(request.email());
        Department department = requireActiveDepartment(request.departmentId());

        ensureEmployeeCodeAvailable(employeeCode);
        ensureEmailAvailable(email);
        ensureAuthUserAvailable(request.authUserId());

        Employee employee = buildEmployee(request, employeeCode, email);

        employeeMapper.insert(employee);
        saveEmployeeCreatedEvent(employee);

        Employee createdEmployee = loadCreatedEmployee(employee.getId());

        return toResponse(createdEmployee, department.getName());
    }

    @Override
    public EmployeeResponse getById(Long id, Long requesterId, String requesterRole) {
        return null;
    }

    @Override
    public PageResponse<EmployeeResponse> search(EmployeeFilterRequest filter) {
        return null;
    }

    @Override
    public EmployeeResponse updateByHr(Long id, UpdateEmployeeRequest request) {
        return null;
    }

    @Override
    public EmployeeResponse updateContact(Long id, UpdateContactRequest request, Long requesterId) {
        return null;
    }

    @Override
    public void deactivate(Long id) {

    }

    private String normalizeEmployeeCode(String employeeCode) {
        return normalizeRequiredText(employeeCode, "Employee code").toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return normalizeRequiredText(email, "Email").toLowerCase(Locale.ROOT);
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(fieldName + " cannot be empty");
        }
        return value.trim();
    }

    private Department requireActiveDepartment(Long departmentId) {
        if (departmentId == null) {
            throw new ResourceNotFoundException("Department ID is required");
        }

        Department department = departmentMapper.findById(departmentId);
        if (department == null || !Boolean.TRUE.equals(department.getIsActive())) {
            throw new ResourceNotFoundException("Department not found");
        }

        return department;
    }

    private void ensureEmployeeCodeAvailable(String employeeCode) {
        if (employeeMapper.findByEmployeeCode(employeeCode) != null) {
            throw new DuplicateResourceException("Employee code already exists");
        }
    }

    private void ensureEmailAvailable(String email) {
        if (employeeMapper.findByEmail(email) != null) {
            throw new DuplicateResourceException("Email already exists");
        }
    }

    private void ensureAuthUserAvailable(Long authUserId) {
        if (authUserId == null) {
            throw new BusinessException("Auth user ID is required");
        }
        if (employeeMapper.findByAuthUserId(authUserId) != null) {
            throw new DuplicateResourceException("Auth user already assigned to an employee");
        }
    }

    private Employee loadCreatedEmployee(Long employeeId) {
        Employee createdEmployee = employeeMapper.findById(employeeId);
        if (createdEmployee == null) {
            throw new BusinessException("Failed to load created employee");
        }
        return createdEmployee;
    }

    private EmployeeResponse toResponse(Employee employee, String departmentName) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .position(employee.getPosition())
                .departmentId(employee.getDepartmentId())
                .departmentName(departmentName)
                .authUserId(employee.getAuthUserId())
                .salary(employee.getSalary())
                .startDate(employee.getStartDate())
                .status(employee.getStatus())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }

    private void saveEmployeeCreatedEvent(Employee employee) {
        EmployeeCreatedPayload payload = new EmployeeCreatedPayload(
                EMPLOYEE_CREATED_EVENT,
                EMPLOYEE_AGGREGATE_TYPE,
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getDepartmentId(),
                employee.getPosition(),
                Instant.now()
        );

        String payloadJson = objectMapper.writeValueAsString(payload);

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(EMPLOYEE_AGGREGATE_TYPE)
                .aggregateId(employee.getId())
                .eventType(EMPLOYEE_CREATED_EVENT)
                .payload(payloadJson)
                .build();

        outboxEventMapper.insert(event);
    }

    private Employee buildEmployee(CreateEmployeeRequest request, String employeeCode, String email) {
        return Employee.builder()
                .employeeCode(employeeCode)
                .fullName(normalizeRequiredText(request.fullName(), "Full name"))
                .email(email)
                .phone(normalizeRequiredText(request.phone(), "Phone"))
                .position(normalizeRequiredText(request.position(), "Position"))
                .departmentId(request.departmentId())
                .authUserId(request.authUserId())
                .salary(request.salary())
                .startDate(request.startDate())
                .status(EmployeeStatus.ACTIVE)
                .build();
    }
}
