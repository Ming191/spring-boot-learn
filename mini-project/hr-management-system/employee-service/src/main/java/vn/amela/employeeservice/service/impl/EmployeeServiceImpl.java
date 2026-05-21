package vn.amela.employeeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
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
import vn.amela.employeeservice.exception.BusinessException;
import vn.amela.employeeservice.exception.DuplicateResourceException;
import vn.amela.employeeservice.exception.ResourceNotFoundException;
import vn.amela.employeeservice.mapper.DepartmentMapper;
import vn.amela.employeeservice.mapper.EmployeeMapper;
import vn.amela.employeeservice.mapper.OutboxEventMapper;
import vn.amela.employeeservice.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Objects;

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
            try {
                OutboxEvent event = OutboxEvent.builder()
                        .aggregateType("EMPLOYEE")
                        .aggregateId(currentEmployee.getId())
                        .eventType("employee.status.changed")
                        .payload(objectMapper.writeValueAsString(currentEmployee))
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
