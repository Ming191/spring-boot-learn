package vn.amela.leaveservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import vn.amela.leaveservice.dto.response.EmployeeSnapshotResponse;
import vn.amela.leaveservice.dto.response.LeaveResponse;
import vn.amela.leaveservice.dto.response.PageResponse;
import vn.amela.leaveservice.entity.LeaveRequest;
import vn.amela.leaveservice.entity.enums.LeaveStatus;
import vn.amela.leaveservice.entity.enums.LeaveType;
import vn.amela.leaveservice.exception.ForbiddenActionException;
import vn.amela.leaveservice.mapper.LeaveMapper;
import vn.amela.leaveservice.mapper.OutboxEventMapper;
import vn.amela.leaveservice.security.CurrentUser;
import vn.amela.leaveservice.service.impl.LeaveServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceImplTest {

    @Mock
    private LeaveMapper leaveMapper;

    @Mock
    private OutboxEventMapper outboxEventMapper;

    @Mock
    private EmployeeSnapshotService employeeSnapshotService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LeaveServiceImpl leaveService;

    @Test
    @DisplayName("findMyLeaves returns mapped page for employee and uses offset")
    void findMyLeavesReturnsMappedPageForEmployee() {
        EmployeeSnapshotResponse employee = employeeSnapshot();
        LeaveRequest leaveRequest = leaveRequest();
        when(employeeSnapshotService.getEmployeeSnapshotByAuthUserId(2L)).thenReturn(employee);
        when(leaveMapper.findByEmployeeId(10L, 2, 4)).thenReturn(List.of(leaveRequest));
        when(leaveMapper.countByEmployeeId(10L)).thenReturn(5L);

        PageResponse<LeaveResponse> response = leaveService.findMyLeaves(employeeUser(), 2, 2);

        assertThat(response.getItems()).hasSize(1);
        LeaveResponse item = response.getItems().getFirst();
        assertThat(item.id()).isEqualTo(100L);
        assertThat(item.employeeId()).isEqualTo(10L);
        assertThat(item.employeeCode()).isEqualTo("EMP001");
        assertThat(item.employeeName()).isEqualTo("Employee One");
        assertThat(item.departmentName()).isEqualTo("Engineering");
        assertThat(item.leaveType()).isEqualTo(LeaveType.ANNUAL);
        assertThat(item.fromDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(item.toDate()).isEqualTo(LocalDate.of(2026, 6, 12));
        assertThat(item.totalDays()).isEqualTo(3);
        assertThat(item.reason()).isEqualTo("Vacation");
        assertThat(item.status()).isEqualTo(LeaveStatus.PENDING);
        assertThat(item.reviewedBy()).isEqualTo(20L);
        assertThat(item.reviewerNote()).isEqualTo("note");
        assertThat(item.reviewedAt()).isEqualTo(LocalDateTime.of(2026, 6, 1, 9, 30));
        assertThat(item.createdAt()).isEqualTo(LocalDateTime.of(2026, 5, 1, 8, 0));
        assertThat(item.updatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 2, 8, 0));
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(5L);
        assertThat(response.getTotalPages()).isEqualTo(3);
        verify(employeeSnapshotService).getEmployeeSnapshotByAuthUserId(2L);
        verify(leaveMapper).findByEmployeeId(10L, 2, 4);
        verify(leaveMapper).countByEmployeeId(10L);
    }

    @Test
    @DisplayName("findMyLeaves allows HR user")
    void findMyLeavesAllowsHrUser() {
        when(employeeSnapshotService.getEmployeeSnapshotByAuthUserId(1L)).thenReturn(employeeSnapshot());
        when(leaveMapper.findByEmployeeId(10L, 10, 0)).thenReturn(List.of());
        when(leaveMapper.countByEmployeeId(10L)).thenReturn(0L);

        PageResponse<LeaveResponse> response = leaveService.findMyLeaves(hrUser(), 0, 10);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isZero();
        assertThat(response.getTotalPages()).isZero();
        verify(employeeSnapshotService).getEmployeeSnapshotByAuthUserId(1L);
        verify(leaveMapper).findByEmployeeId(10L, 10, 0);
    }

    @Test
    @DisplayName("findMyLeaves normalizes negative page and non positive size")
    void findMyLeavesNormalizesNegativePageAndNonPositiveSize() {
        when(employeeSnapshotService.getEmployeeSnapshotByAuthUserId(2L)).thenReturn(employeeSnapshot());
        when(leaveMapper.findByEmployeeId(10L, 10, 0)).thenReturn(List.of());
        when(leaveMapper.countByEmployeeId(10L)).thenReturn(0L);

        PageResponse<LeaveResponse> response = leaveService.findMyLeaves(employeeUser(), -1, 0);

        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(10);
        verify(leaveMapper).findByEmployeeId(10L, 10, 0);
    }

    @Test
    @DisplayName("findMyLeaves caps size at 100 and offset uses capped size")
    void findMyLeavesCapsSizeAt100AndUsesCappedOffset() {
        when(employeeSnapshotService.getEmployeeSnapshotByAuthUserId(2L)).thenReturn(employeeSnapshot());
        when(leaveMapper.findByEmployeeId(10L, 100, 300)).thenReturn(List.of());
        when(leaveMapper.countByEmployeeId(10L)).thenReturn(250L);

        PageResponse<LeaveResponse> response = leaveService.findMyLeaves(employeeUser(), 3, 500);

        assertThat(response.getPage()).isEqualTo(3);
        assertThat(response.getSize()).isEqualTo(100);
        assertThat(response.getTotalElements()).isEqualTo(250L);
        assertThat(response.getTotalPages()).isEqualTo(3);
        verify(leaveMapper).findByEmployeeId(10L, 100, 300);
    }

    @Test
    @DisplayName("findMyLeaves uses employee id from snapshot instead of auth user id")
    void findMyLeavesUsesEmployeeIdFromSnapshot() {
        EmployeeSnapshotResponse employee = new EmployeeSnapshotResponse(
                99L,
                "EMP099",
                "Employee Ninety Nine",
                "employee.ninetynine@company.com",
                "0123456789",
                "Developer",
                "ACTIVE",
                2L,
                30L,
                "Engineering"
        );
        when(employeeSnapshotService.getEmployeeSnapshotByAuthUserId(2L)).thenReturn(employee);
        when(leaveMapper.findByEmployeeId(99L, 20, 20)).thenReturn(List.of());
        when(leaveMapper.countByEmployeeId(99L)).thenReturn(0L);

        leaveService.findMyLeaves(employeeUser(), 1, 20);

        verify(employeeSnapshotService).getEmployeeSnapshotByAuthUserId(2L);
        verify(leaveMapper).findByEmployeeId(99L, 20, 20);
        verify(leaveMapper).countByEmployeeId(99L);
    }

    @Test
    @DisplayName("findMyLeaves rejects null user and does not call dependencies")
    void findMyLeavesRejectsNullUser() {
        assertThatThrownBy(() -> leaveService.findMyLeaves(null, 0, 10))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("Only HR or Employee can view leave requests");

        verifyNoInteractions(leaveMapper, outboxEventMapper, employeeSnapshotService, objectMapper);
    }

    @Test
    @DisplayName("findMyLeaves rejects unsupported role and does not call dependencies")
    void findMyLeavesRejectsUnsupportedRole() {
        assertThatThrownBy(() -> leaveService.findMyLeaves(new CurrentUser("manager", 3L, "MANAGER"), 0, 10))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("Only HR or Employee can view leave requests");

        verifyNoInteractions(leaveMapper, outboxEventMapper, employeeSnapshotService, objectMapper);
    }

    private CurrentUser hrUser() {
        return new CurrentUser("hr_user", 1L, "HR");
    }

    private CurrentUser employeeUser() {
        return new CurrentUser("employee_user", 2L, "EMPLOYEE");
    }

    private EmployeeSnapshotResponse employeeSnapshot() {
        return new EmployeeSnapshotResponse(
                10L,
                "EMP001",
                "Employee One",
                "employee.one@company.com",
                "0123456789",
                "Developer",
                "ACTIVE",
                2L,
                30L,
                "Engineering"
        );
    }

    private LeaveRequest leaveRequest() {
        return LeaveRequest.builder()
                .id(100L)
                .employeeId(10L)
                .employeeCode("EMP001")
                .employeeName("Employee One")
                .departmentName("Engineering")
                .leaveType(LeaveType.ANNUAL)
                .fromDate(LocalDate.of(2026, 6, 10))
                .toDate(LocalDate.of(2026, 6, 12))
                .totalDays(3)
                .reason("Vacation")
                .status(LeaveStatus.PENDING)
                .reviewedBy(20L)
                .reviewerNote("note")
                .reviewedAt(LocalDateTime.of(2026, 6, 1, 9, 30))
                .createdAt(LocalDateTime.of(2026, 5, 1, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 2, 8, 0))
                .build();
    }
}
