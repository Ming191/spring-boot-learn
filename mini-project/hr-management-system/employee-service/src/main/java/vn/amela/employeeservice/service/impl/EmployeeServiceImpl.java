package vn.amela.employeeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.amela.employeeservice.dto.request.CreateEmployeeRequest;
import vn.amela.employeeservice.dto.request.EmployeeFilterRequest;
import vn.amela.employeeservice.dto.request.UpdateContactRequest;
import vn.amela.employeeservice.dto.request.UpdateEmployeeRequest;
import vn.amela.employeeservice.dto.response.EmployeeResponse;
import vn.amela.employeeservice.dto.response.PageResponse;
import vn.amela.employeeservice.entity.Department;
import vn.amela.employeeservice.entity.Employee;
import vn.amela.employeeservice.exception.BusinessException;
import vn.amela.employeeservice.exception.ResourceNotFoundException;
import vn.amela.employeeservice.mapper.DepartmentMapper;
import vn.amela.employeeservice.mapper.EmployeeMapper;
import vn.amela.employeeservice.service.EmployeeService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    protected final EmployeeMapper employeeMapper;
    protected final DepartmentMapper departmentMapper;

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
