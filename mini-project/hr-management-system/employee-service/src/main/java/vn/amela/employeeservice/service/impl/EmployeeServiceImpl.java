package vn.amela.employeeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.amela.employeeservice.dto.request.CreateEmployeeRequest;
import vn.amela.employeeservice.dto.request.EmployeeFilterRequest;
import vn.amela.employeeservice.dto.request.UpdateContactRequest;
import vn.amela.employeeservice.dto.request.UpdateEmployeeRequest;
import vn.amela.employeeservice.dto.response.EmployeeResponse;
import vn.amela.employeeservice.dto.response.PageResponse;
import vn.amela.employeeservice.entity.Department;
import vn.amela.employeeservice.entity.Employee;
import vn.amela.employeeservice.entity.OutboxEvent;
import vn.amela.employeeservice.entity.enums.OutboxEventStatus;
import vn.amela.employeeservice.exception.BusinessException;
import vn.amela.employeeservice.exception.DuplicateResourceException;
import vn.amela.employeeservice.exception.ResourceNotFoundException;
import vn.amela.employeeservice.mapper.DepartmentMapper;
import vn.amela.employeeservice.mapper.EmployeeMapper;
import vn.amela.employeeservice.mapper.OutboxEventMapper;
import vn.amela.employeeservice.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    protected final EmployeeMapper employeeMapper;
    protected final DepartmentMapper departmentMapper;
    protected final OutboxEventMapper outboxEventMapper;
    protected final ObjectMapper objectMapper;

    @Override
    public EmployeeResponse create(CreateEmployeeRequest request) {
        return null;
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
    @Transactional
    public EmployeeResponse updateByHr(Long id, UpdateEmployeeRequest request) {
        Employee currentEmployee = employeeMapper.findById(id);
        if (currentEmployee == null) {
            throw new ResourceNotFoundException("Employee not found");
        }

        String email = request.email() != null ? normalizeEmail(request.email()) : null;
        if (email != null) {
            Employee existing = employeeMapper.findByEmail(email);
            if (existing != null && !existing.getId().equals(id)) {
                throw new DuplicateResourceException("Email is already in use");
            }
        }
        Department department = requireActiveDepartment(request.departmentId());

        boolean isDepartmentChanged = currentEmployee.getDepartmentId() == null ||
                !currentEmployee.getDepartmentId().equals(request.departmentId());
        boolean isSalaryChanged = currentEmployee.getSalary() == null ||
                currentEmployee.getSalary().compareTo(request.salary()) != 0;

        currentEmployee.setFullName(request.fullName());
        currentEmployee.setEmail(email);
        currentEmployee.setPhone(request.phone());
        currentEmployee.setPosition(request.position());
        currentEmployee.setDepartmentId(request.departmentId());
        currentEmployee.setSalary(request.salary());
        currentEmployee.setStartDate(request.startDate());

        employeeMapper.updateByHr(currentEmployee);

        if (isDepartmentChanged || isSalaryChanged) {
            try {
                OutboxEvent event = OutboxEvent.builder()
                        .aggregateType("EMPLOYEE")
                        .aggregateId(currentEmployee.getId())
                        .eventType("employee.status.changed")
                        .payload(objectMapper.writeValueAsString(currentEmployee))
                        .status(OutboxEventStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();
                outboxEventMapper.insert(event);
            } catch (Exception e) {
                throw new BusinessException("Failed to serialize outbox event payload");
            }
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

        if (!requesterId.equals(currentEmployee.getAuthUserId())) {
            throw new BusinessException("You are not authorized to update this employee's contact");
        }

        String email = normalizeEmail(request.email());
        Employee existing = employeeMapper.findByEmail(email);
        if (existing != null && !existing.getId().equals(id)) {
            throw new DuplicateResourceException("Email is already in use");
        }

        employeeMapper.updateContact(id, email, request.phone());

        currentEmployee.setEmail(email);
        currentEmployee.setPhone(request.phone());

        Department department = currentEmployee.getDepartmentId() != null 
                ? departmentMapper.findById(currentEmployee.getDepartmentId()) 
                : null;
        String departmentName = department != null ? department.getName() : null;

        return toResponse(currentEmployee, departmentName);
    }

    @Override
    public void deactivate(Long id) {
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
