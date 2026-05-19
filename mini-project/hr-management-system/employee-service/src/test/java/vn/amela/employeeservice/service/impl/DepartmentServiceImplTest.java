package vn.amela.employeeservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.amela.employeeservice.dto.request.CreateDepartmentRequest;
import vn.amela.employeeservice.dto.request.UpdateDepartmentRequest;
import vn.amela.employeeservice.dto.response.DepartmentResponse;
import vn.amela.employeeservice.entity.Department;
import vn.amela.employeeservice.exception.BusinessException;
import vn.amela.employeeservice.exception.DuplicateResourceException;
import vn.amela.employeeservice.exception.ResourceNotFoundException;
import vn.amela.employeeservice.mapper.DepartmentMapper;
import vn.amela.employeeservice.mapper.EmployeeMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    @Test
    void createDepartment_success() {
        CreateDepartmentRequest request = new CreateDepartmentRequest(" Engineering ", " Product team ", null);
        when(departmentMapper.findByName("Engineering")).thenReturn(null);

        DepartmentResponse response = departmentService.create(request);

        ArgumentCaptor<Department> departmentCaptor = ArgumentCaptor.forClass(Department.class);
        verify(departmentMapper).insert(departmentCaptor.capture());
        Department department = departmentCaptor.getValue();
        assertThat(department.getName()).isEqualTo("Engineering");
        assertThat(department.getDescription()).isEqualTo("Product team");
        assertThat(department.getManagerId()).isNull();
        assertThat(department.getIsActive()).isTrue();
        assertThat(response.name()).isEqualTo("Engineering");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void createDepartment_duplicateName_throws() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("Engineering", null, null);
        when(departmentMapper.findByName("Engineering")).thenReturn(Department.builder().id(1L).build());

        assertThrows(DuplicateResourceException.class, () -> departmentService.create(request));
        verify(departmentMapper, never()).insert(any());
    }

    @Test
    void createDepartment_withManager_throws() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("Engineering", null, 1L);
        when(departmentMapper.findByName("Engineering")).thenReturn(null);

        assertThrows(BusinessException.class, () -> departmentService.create(request));
        verify(departmentMapper, never()).insert(any());
    }

    @Test
    void updateDepartment_success() {
        Department department = Department.builder()
                .id(1L)
                .name("Engineering")
                .description("Old")
                .isActive(true)
                .build();
        UpdateDepartmentRequest request = UpdateDepartmentRequest.builder()
                .name("Platform")
                .description(" Platform team ")
                .managerId(10L)
                .isActive(true)
                .build();

        when(departmentMapper.findById(1L)).thenReturn(department);
        when(departmentMapper.existsByNameExceptId("Platform", 1L)).thenReturn(false);
        when(employeeMapper.existsActiveInDepartment(10L, 1L)).thenReturn(true);

        DepartmentResponse response = departmentService.update(1L, request);

        verify(departmentMapper).update(department);
        assertThat(department.getName()).isEqualTo("Platform");
        assertThat(department.getDescription()).isEqualTo("Platform team");
        assertThat(department.getManagerId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Platform");
        assertThat(response.managerId()).isEqualTo(10L);
    }

    @Test
    void updateDepartment_notFound_throws() {
        when(departmentMapper.findById(1L)).thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> departmentService.update(1L, UpdateDepartmentRequest.builder().name("Engineering").build())
        );
        verify(departmentMapper, never()).update(any());
    }

    @Test
    void updateDepartment_invalidManager_throws() {
        Department department = Department.builder()
                .id(1L)
                .name("Engineering")
                .isActive(true)
                .build();
        UpdateDepartmentRequest request = UpdateDepartmentRequest.builder()
                .name("Engineering")
                .managerId(10L)
                .build();

        when(departmentMapper.findById(1L)).thenReturn(department);
        when(employeeMapper.existsActiveInDepartment(10L, 1L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> departmentService.update(1L, request));
        verify(departmentMapper, never()).update(any());
    }

    @Test
    void deleteDepartment_hasActiveEmployee_throws() {
        Department department = Department.builder()
                .id(1L)
                .name("Engineering")
                .isActive(true)
                .build();
        when(departmentMapper.findById(1L)).thenReturn(department);
        when(employeeMapper.countActiveByDepartmentId(1L)).thenReturn(1);

        assertThrows(BusinessException.class, () -> departmentService.delete(1L));
        verify(departmentMapper, never()).deactivate(anyLong());
    }

    @Test
    void deleteDepartment_success() {
        Department department = Department.builder()
                .id(1L)
                .name("Engineering")
                .isActive(true)
                .build();
        when(departmentMapper.findById(1L)).thenReturn(department);
        when(employeeMapper.countActiveByDepartmentId(1L)).thenReturn(0);

        departmentService.delete(1L);

        verify(departmentMapper).deactivate(1L);
    }
}
