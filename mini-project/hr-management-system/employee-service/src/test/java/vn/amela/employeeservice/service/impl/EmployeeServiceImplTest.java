package vn.amela.employeeservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.amela.employeeservice.dto.request.EmployeeFilterRequest;
import vn.amela.employeeservice.dto.response.EmployeeResponse;
import vn.amela.employeeservice.dto.response.PageResponse;
import vn.amela.employeeservice.entity.Department;
import vn.amela.employeeservice.entity.Employee;
import vn.amela.employeeservice.entity.enums.EmployeeStatus;
import vn.amela.employeeservice.exception.BusinessException;
import vn.amela.employeeservice.mapper.DepartmentMapper;
import vn.amela.employeeservice.mapper.EmployeeMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    void search_delegatesToMapperAndBuildsPageResponse() {
        EmployeeFilterRequest filter = new EmployeeFilterRequest();
        filter.setLikeName("  An  ");
        filter.setDepartmentId(1L);
        filter.setPosition(" Backend Developer ");
        filter.setStatus(EmployeeStatus.ACTIVE);
        filter.setStartDateFrom(LocalDate.of(2024, 1, 1));
        filter.setStartDateTo(LocalDate.of(2024, 12, 31));
        filter.setPage(1);
        filter.setSize(2);
        filter.setSortBy("employeeCode");
        filter.setSortDirection("ASC");

        Employee employee = Employee.builder()
                .id(10L)
                .employeeCode("EMP010")
                .fullName("Nguyen Van An")
                .email("an@company.com")
                .position("Backend Developer")
                .departmentId(1L)
                .authUserId(20L)
                .status(EmployeeStatus.ACTIVE)
                .startDate(LocalDate.of(2024, 1, 15))
                .build();
        Department department = new Department();
        department.setId(1L);
        department.setName("Engineering");

        when(employeeMapper.search(
                "An",
                1L,
                "Backend Developer",
                EmployeeStatus.ACTIVE,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                "employeeCode",
                "asc",
                2,
                2
        )).thenReturn(List.of(employee));
        when(employeeMapper.countByFilter(
                "An",
                1L,
                "Backend Developer",
                EmployeeStatus.ACTIVE,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31)
        )).thenReturn(5);
        when(departmentMapper.findById(1L)).thenReturn(department);

        PageResponse<EmployeeResponse> response = employeeService.search(filter);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.items())
                .extracting(EmployeeResponse::departmentName)
                .containsExactly("Engineering");
        verify(departmentMapper).findById(1L);
    }

    @Test
    void search_nullFilterUsesSrsDefaults() {
        when(employeeMapper.search(null, null, null, null, null, null, "createdAt", "desc", 0, 10))
                .thenReturn(List.of());
        when(employeeMapper.countByFilter(null, null, null, null, null, null)).thenReturn(0);

        PageResponse<EmployeeResponse> response = employeeService.search(null);

        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.items()).isEmpty();
        verifyNoInteractions(departmentMapper);
    }

    @Test
    void search_negativePageThrows() {
        EmployeeFilterRequest filter = new EmployeeFilterRequest();
        filter.setPage(-1);

        assertThrows(BusinessException.class, () -> employeeService.search(filter));
        verifyNoInteractions(employeeMapper);
    }

    @Test
    void search_invalidDateRangeThrows() {
        EmployeeFilterRequest filter = new EmployeeFilterRequest();
        filter.setStartDateFrom(LocalDate.of(2024, 12, 31));
        filter.setStartDateTo(LocalDate.of(2024, 1, 1));

        assertThrows(BusinessException.class, () -> employeeService.search(filter));
        verifyNoInteractions(employeeMapper);
    }
}
