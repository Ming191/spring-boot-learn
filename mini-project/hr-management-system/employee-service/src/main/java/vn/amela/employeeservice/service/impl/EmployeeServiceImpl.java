package vn.amela.employeeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.amela.employeeservice.client.LeaveServiceClient;
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
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final String EMPLOYEE_CREATED_EVENT = "employee.created";
    private static final String EMPLOYEE_AGGREGATE_TYPE = "Employee";
    private static final String EMPLOYEE_STATUS_CHANGED_EVENT = "employee.status.changed";
    private static final String EMPLOYEE_UPDATE_AGGREGATE_TYPE = "EMPLOYEE";

    protected final EmployeeMapper employeeMapper;
    protected final DepartmentMapper departmentMapper;
    protected final LeaveServiceClient leaveServiceClient;
    protected final OutboxEventMapper outboxEventMapper;
    protected final ObjectMapper objectMapper;

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

        try {
            employeeMapper.insert(employee);
            saveEmployeeCreatedEvent(employee);
        } catch (DuplicateKeyException e) {
            throw new DuplicateResourceException("Employee with the same code, email, or auth user already exists");
        }

        Employee createdEmployee = loadCreatedEmployee(employee.getId());

        return toResponse(createdEmployee, department.getName());
    }

    @Override
    public EmployeeResponse getById(Long id, Long requesterId, String requesterRole) {
        return null;
    }

    @Override
    public PageResponse<EmployeeResponse> search(EmployeeFilterRequest filter) {
        EmployeeFilterRequest normalizedFilter = filter == null ? defaultFilter() : filter;
        int page = normalizedFilter.page();
        int size = normalizedFilter.size();

        LocalDate startDateFrom = normalizedFilter.startDateFrom();
        LocalDate startDateTo = normalizedFilter.startDateTo();
        if (startDateFrom != null && startDateTo != null && startDateFrom.isAfter(startDateTo)) {
            throw new BusinessException("Start date from cannot be after start date to");
        }

        String keyword = normalizeOptionalText(normalizedFilter.likeName());
        String position = normalizeOptionalText(normalizedFilter.position());
        String sortBy = normalizeSortBy(normalizedFilter.sortBy());
        String sortDirection = normalizeSortDirection(normalizedFilter.sortDirection());
        int offset = page * size;

        List<Employee> employees = employeeMapper.search(
                keyword,
                normalizedFilter.departmentId(),
                position,
                normalizedFilter.status(),
                startDateFrom,
                startDateTo,
                sortBy,
                sortDirection,
                offset,
                size
        );
        int totalElements = employeeMapper.countByFilter(
                keyword,
                normalizedFilter.departmentId(),
                position,
                normalizedFilter.status(),
                startDateFrom,
                startDateTo
        );
        int totalPages = (int) Math.ceil((double) totalElements / size);

        Map<Long, String> departmentNames = new HashMap<>();
        List<EmployeeResponse> items = employees.stream()
                .map(employee -> toResponse(
                        employee,
                        departmentNames.computeIfAbsent(employee.getDepartmentId(), this::findDepartmentName)
                ))
                .toList();

        return PageResponse.<EmployeeResponse>builder()
                .items(items)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    private EmployeeFilterRequest defaultFilter() {
        return new EmployeeFilterRequest(null, null, null, null, null, null, 0, 10, null, null);
    }

    @Override
    @Transactional
    public EmployeeResponse updateByHr(Long id, UpdateEmployeeRequest request) {
        String fullName = normalizeRequiredText(request.fullName(), "Full name");
        String email = normalizeEmail(request.email());
        String phone = normalizeRequiredText(request.phone(), "Phone");
        String position = normalizeRequiredText(request.position(), "Position");

        Employee currentEmployee = employeeMapper.findById(id);
        if (currentEmployee == null) {
            throw new ResourceNotFoundException("Employee not found");
        }

        Department department = requireActiveDepartment(request.departmentId());

        if (request.salary() == null) {
            throw new BusinessException("Salary is required");
        }

        if (request.startDate() == null) {
            throw new BusinessException("Start date is required");
        }

        boolean isDepartmentChanged = currentEmployee.getDepartmentId() == null ||
                !currentEmployee.getDepartmentId().equals(request.departmentId());
        boolean isSalaryChanged = currentEmployee.getSalary() == null ||
                currentEmployee.getSalary().compareTo(request.salary()) != 0;

        currentEmployee.setFullName(fullName);
        currentEmployee.setEmail(email);
        currentEmployee.setPhone(phone);
        currentEmployee.setPosition(position);
        currentEmployee.setDepartmentId(request.departmentId());
        currentEmployee.setSalary(request.salary());
        currentEmployee.setStartDate(request.startDate());

        try {
            employeeMapper.updateByHr(currentEmployee);
        } catch (DuplicateKeyException e) {
            throw new DuplicateResourceException("Email already exists: " + email);
        }

        if (isDepartmentChanged || isSalaryChanged) {
            saveEmployeeStatusChangedEvent(currentEmployee);
        }

        return toResponse(currentEmployee, department.getName());
    }

    @Override
    @Transactional
    public EmployeeResponse updateContact(Long id, UpdateContactRequest request, Long requesterId) {
        Employee currentEmployee = employeeMapper.findById(id);
        if (currentEmployee == null) {
            throw new ResourceNotFoundException("Employee not found");
        }

        if (!Objects.equals(requesterId, currentEmployee.getAuthUserId())) {
            throw new BusinessException("You are not authorized to update this employee's contact");
        }

        String email = normalizeEmail(request.email());
        String phone = normalizeRequiredText(request.phone(), "Phone");

        try {
            employeeMapper.updateContact(id, email, phone);
        } catch (DuplicateKeyException e) {
            throw new DuplicateResourceException("Email already exists: " + email);
        }

        currentEmployee.setEmail(email);
        currentEmployee.setPhone(phone);

        Department department = currentEmployee.getDepartmentId() != null
                ? departmentMapper.findById(currentEmployee.getDepartmentId())
                : null;
        String departmentName = department != null ? department.getName() : null;

        return toResponse(currentEmployee, departmentName);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Employee currentEmployee = employeeMapper.findById(id);
        if (currentEmployee == null) {
            throw new ResourceNotFoundException("Employee not found");
        }

        if (!EmployeeStatus.ACTIVE.equals(currentEmployee.getStatus())) {
            throw new BusinessException("Employee is not active");
        }

        if (leaveServiceClient.hasPendingLeavesByEmployeeId(id)) {
            throw new BusinessException("Cannot deactivate employee with pending leaves");
        }

        int updatedRows = employeeMapper.deactivate(id);
        if (updatedRows == 0) {
            throw new BusinessException("Employee could not be deactivated");
        }

        currentEmployee.setStatus(EmployeeStatus.INACTIVE);

        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Employee")
                    .aggregateId(id)
                    .eventType("employee.deactivated")
                    .payload(objectMapper.writeValueAsString(currentEmployee))
                    .build();
            outboxEventMapper.insert(event);
        } catch (Exception e) {
            throw new BusinessException("Failed to serialize outbox event payload");
        }
    }

    private String normalizeEmployeeCode(String employeeCode) {
        return normalizeRequiredText(employeeCode, "Employee code").toUpperCase(Locale.ROOT);
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

        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(EMPLOYEE_AGGREGATE_TYPE)
                    .aggregateId(employee.getId())
                    .eventType(EMPLOYEE_CREATED_EVENT)
                    .payload(objectMapper.writeValueAsString(payload))
                    .build();

            outboxEventMapper.insert(event);
        } catch (Exception e) {
            throw new BusinessException("Failed to serialize outbox event payload");
        }
    }

    private void saveEmployeeStatusChangedEvent(Employee employee) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(EMPLOYEE_UPDATE_AGGREGATE_TYPE)
                    .aggregateId(employee.getId())
                    .eventType(EMPLOYEE_STATUS_CHANGED_EVENT)
                    .payload(objectMapper.writeValueAsString(employee))
                    .build();

            outboxEventMapper.insert(event);
        } catch (Exception e) {
            throw new BusinessException("Failed to serialize outbox event payload");
        }
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

    protected String normalizeEmail(String email) {
        return normalizeRequiredText(email, "Email").toLowerCase(Locale.ROOT);
    }

    protected String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(fieldName + " cannot be empty");
        }
        return value.trim();
    }

    protected String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    protected String normalizeSortBy(String sortBy) {
        String normalizedSortBy = normalizeOptionalText(sortBy);
        return normalizedSortBy == null ? "createdAt" : normalizedSortBy;
    }

    protected String normalizeSortDirection(String sortDirection) {
        String normalizedSortDirection = normalizeOptionalText(sortDirection);
        if (normalizedSortDirection == null) {
            return "desc";
        }
        return "asc".equalsIgnoreCase(normalizedSortDirection) ? "asc" : "desc";
    }

    protected String findDepartmentName(Long departmentId) {
        if (departmentId == null) {
            return null;
        }

        Department department = departmentMapper.findById(departmentId);
        return department == null ? null : department.getName();
    }

    protected Department requireActiveDepartment(Long departmentId) {
        if (departmentId == null) {
            throw new ResourceNotFoundException("Department ID is required");
        }

        Department department = departmentMapper.findById(departmentId);
        if (department == null || !Boolean.TRUE.equals(department.getIsActive())) {
            throw new ResourceNotFoundException("Department not found");
        }

        return department;
    }

    protected EmployeeResponse toResponse(Employee employee, String departmentName) {
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
}
