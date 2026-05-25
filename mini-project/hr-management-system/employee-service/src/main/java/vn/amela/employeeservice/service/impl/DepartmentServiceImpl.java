package vn.amela.employeeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.amela.employeeservice.dto.request.CreateDepartmentRequest;
import vn.amela.employeeservice.dto.request.UpdateDepartmentRequest;
import vn.amela.employeeservice.dto.response.DepartmentResponse;
import vn.amela.employeeservice.entity.Department;
import vn.amela.employeeservice.exception.BusinessException;
import vn.amela.employeeservice.exception.DuplicateResourceException;
import vn.amela.employeeservice.exception.ResourceNotFoundException;
import vn.amela.employeeservice.mapper.DepartmentMapper;
import vn.amela.employeeservice.mapper.EmployeeMapper;
import vn.amela.employeeservice.service.DepartmentService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private static final String DEPARTMENT_NAME = "Department name";

    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;

    @Override
    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {
        String normalizedDepartmentName = normalizeRequiredText(request.name(), DEPARTMENT_NAME);
        String normalizedDescription = normalizeOptionalText(request.description());

        if (departmentMapper.findByName(normalizedDepartmentName) != null) {
            throw new DuplicateResourceException("Department name already exists");
        }

        if (request.managerId() != null) {
            throw new BusinessException("Manager must be assigned after the department has active employees");
        }

        Department department = Department.builder()
                .name(normalizedDepartmentName)
                .description(normalizedDescription)
                .managerId(null)
                .isActive(true)
                .build();

        departmentMapper.insert(department);

        return toResponse(department);
    }

    @Override
    public List<DepartmentResponse> listAll() {

        return departmentMapper.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<DepartmentResponse> listAllActive() {

        return departmentMapper.findActive().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DepartmentResponse update(Long id, UpdateDepartmentRequest request) {

        Department department = departmentMapper.findById(id);
        if (department == null) {
            throw new ResourceNotFoundException("Department not found");
        }

        if (request.name() != null) {
            String normalizedDepartmentName = normalizeRequiredText(request.name(), DEPARTMENT_NAME);
            if (departmentMapper.existsByNameExceptId(normalizedDepartmentName, id)) {
                throw new DuplicateResourceException("Department name already exists");
            }
            department.setName(normalizedDepartmentName);
        }

        if (request.managerId() != null) {
            if (!employeeMapper.existsActiveInDepartment(request.managerId(), id)) {
                throw new BusinessException("Manager must be an active employee in this department");
            }
            department.setManagerId(request.managerId());
        }

        if (request.description() != null) {
            String normalizedDescription = normalizeOptionalText(request.description());
            department.setDescription(normalizedDescription);
        }

        if (request.isActive() != null) {
            if (!request.isActive() && employeeMapper.countActiveByDepartmentId(id) > 0) {
                throw new BusinessException("Department still has active employees");
            }
            department.setIsActive(request.isActive());
        }

        departmentMapper.update(department);

        return toResponse(department);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Department department = departmentMapper.findById(id);

        if (department == null || !Boolean.TRUE.equals(department.getIsActive())) {
            throw new ResourceNotFoundException("Department not found");
        }

        if (employeeMapper.countActiveByDepartmentId(id) > 0) {
            throw new BusinessException("Department still has active employees");
        }

        departmentMapper.deactivate(id);
    }

    protected String normalizeRequiredText(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(fieldName + " is required");
        }
        return text.trim();
    }

    protected String normalizeOptionalText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    private DepartmentResponse toResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .managerId(department.getManagerId())
                .isActive(department.getIsActive())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
}
