package vn.amela.leaveservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import vn.amela.leaveservice.dto.request.LeaveFilterRequest;
import vn.amela.leaveservice.dto.response.EmployeeSnapshotResponse;
import vn.amela.leaveservice.dto.response.LeaveResponse;
import vn.amela.leaveservice.dto.response.PageResponse;
import vn.amela.leaveservice.entity.LeaveRequest;
import vn.amela.leaveservice.entity.enums.LeaveStatus;
import vn.amela.leaveservice.entity.enums.LeaveType;
import vn.amela.leaveservice.exception.BusinessException;
import vn.amela.leaveservice.exception.ForbiddenActionException;
import vn.amela.leaveservice.exception.ResourceNotFoundException;
import vn.amela.leaveservice.mapper.LeaveMapper;
import vn.amela.leaveservice.mapper.OutboxEventMapper;
import vn.amela.leaveservice.security.CurrentUser;
import vn.amela.leaveservice.service.impl.LeaveServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceImplTest {

    @Mock private LeaveMapper leaveMapper;
    @Mock private OutboxEventMapper outboxEventMapper;
    @Mock private EmployeeSnapshotService employeeSnapshotService;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private LeaveServiceImpl leaveService;

    @Test
    @DisplayName("getById allows HR to view any leave")
    void getByIdAllowsHrToViewAnyLeave() {
        LeaveRequest leaveRequest = leaveRequest(99L);
        when(leaveMapper.findById(100L)).thenReturn(Optional.of(leaveRequest));

        LeaveResponse response = leaveService.getById(100L, hrUser());

        assertMappedLeave(response, 99L);
        verify(leaveMapper).findById(100L);
        verifyNoInteractions(employeeSnapshotService, outboxEventMapper, objectMapper);
    }

    @Test
    @DisplayName("getById allows employee to view own leave")
    void getByIdAllowsEmployeeToViewOwnLeave() {
        when(leaveMapper.findById(100L)).thenReturn(Optional.of(leaveRequest(10L)));
        when(employeeSnapshotService.getEmployeeSnapshotByAuthUserId(2L)).thenReturn(employeeSnapshot());

        LeaveResponse response = leaveService.getById(100L, employeeUser());

        assertMappedLeave(response, 10L);
        verify(leaveMapper).findById(100L);
        verify(employeeSnapshotService).getEmployeeSnapshotByAuthUserId(2L);
        verifyNoInteractions(outboxEventMapper, objectMapper);
    }

    @Test
    @DisplayName("getById rejects employee viewing another employee leave")
    void getByIdRejectsEmployeeViewingOtherLeave() {
        when(leaveMapper.findById(100L)).thenReturn(Optional.of(leaveRequest(99L)));
        when(employeeSnapshotService.getEmployeeSnapshotByAuthUserId(2L)).thenReturn(employeeSnapshot());

        assertThatThrownBy(() -> leaveService.getById(100L, employeeUser()))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("You can only view your own leave requests");

        verify(leaveMapper).findById(100L);
        verify(employeeSnapshotService).getEmployeeSnapshotByAuthUserId(2L);
        verifyNoInteractions(outboxEventMapper, objectMapper);
    }

    @Test
    @DisplayName("getById rejects null user and does not call dependencies")
    void getByIdRejectsNullUser() {
        assertThatThrownBy(() -> leaveService.getById(100L, null))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("Only HR or Employee can view leave requests");

        verifyNoInteractions(leaveMapper, employeeSnapshotService, outboxEventMapper, objectMapper);
    }

    @Test
    @DisplayName("getById rejects unsupported role and does not call dependencies")
    void getByIdRejectsUnsupportedRole() {
        assertThatThrownBy(() -> leaveService.getById(100L, new CurrentUser("manager", 3L, "MANAGER")))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("Only HR or Employee can view leave requests");

        verifyNoInteractions(leaveMapper, employeeSnapshotService, outboxEventMapper, objectMapper);
    }

    @Test
    @DisplayName("getById rejects missing leave")
    void getByIdRejectsMissingLeave() {
        when(leaveMapper.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveService.getById(100L, employeeUser()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Leave request not found");

        verify(leaveMapper).findById(100L);
        verifyNoInteractions(employeeSnapshotService, outboxEventMapper, objectMapper);
    }

    @Test
    @DisplayName("search returns mapped page for HR user")
    void searchReturnsMappedPageForHr() {
        LeaveFilterRequest filter = LeaveFilterRequest.builder()
                .employeeId(10L).status(LeaveStatus.PENDING).leaveType(LeaveType.ANNUAL)
                .fromDate(LocalDate.of(2026, 6, 10)).toDate(LocalDate.of(2026, 6, 12))
                .departmentName("Engineering").page(1).size(2).sortBy("fromDate").sortDirection("asc")
                .build();
        when(leaveMapper.search(any(LeaveFilterRequest.class))).thenReturn(List.of(leaveRequest()));
        when(leaveMapper.countByFilter(any(LeaveFilterRequest.class))).thenReturn(3L);

        PageResponse<LeaveResponse> response = leaveService.search(filter, hrUser());

        assertMappedLeave(response.getItems().getFirst());
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(3L);
        assertThat(response.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("search normalizes null filter defaults")
    void searchNormalizesNullFilterDefaults() {
        when(leaveMapper.search(any(LeaveFilterRequest.class))).thenReturn(List.of());
        when(leaveMapper.countByFilter(any(LeaveFilterRequest.class))).thenReturn(0L);

        PageResponse<LeaveResponse> response = leaveService.search(null, hrUser());

        ArgumentCaptor<LeaveFilterRequest> filterCaptor = ArgumentCaptor.forClass(LeaveFilterRequest.class);
        verify(leaveMapper).search(filterCaptor.capture());
        LeaveFilterRequest normalized = filterCaptor.getValue();
        assertThat(normalized.page()).isZero();
        assertThat(normalized.size()).isEqualTo(10);
        assertThat(normalized.sortBy()).isEqualTo("createdAt");
        assertThat(normalized.sortDirection()).isEqualTo("desc");
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("search trims department and normalizes ASC direction")
    void searchTrimsDepartmentAndNormalizesAsc() {
        LeaveFilterRequest filter = LeaveFilterRequest.builder()
                .departmentName("  HR  ").sortBy("employeeName").sortDirection(" ASC ").build();
        when(leaveMapper.search(any(LeaveFilterRequest.class))).thenReturn(List.of());
        when(leaveMapper.countByFilter(any(LeaveFilterRequest.class))).thenReturn(0L);

        leaveService.search(filter, hrUser());

        ArgumentCaptor<LeaveFilterRequest> filterCaptor = ArgumentCaptor.forClass(LeaveFilterRequest.class);
        verify(leaveMapper).search(filterCaptor.capture());
        assertThat(filterCaptor.getValue().departmentName()).isEqualTo("HR");
        assertThat(filterCaptor.getValue().sortDirection()).isEqualTo("asc");
    }

    @Test
    @DisplayName("search defaults invalid sort")
    void searchDefaultsInvalidSort() {
        LeaveFilterRequest filter = LeaveFilterRequest.builder().sortBy("employeeName;drop table").sortDirection("up").build();
        when(leaveMapper.search(any(LeaveFilterRequest.class))).thenReturn(List.of());
        when(leaveMapper.countByFilter(any(LeaveFilterRequest.class))).thenReturn(0L);

        leaveService.search(filter, hrUser());

        ArgumentCaptor<LeaveFilterRequest> filterCaptor = ArgumentCaptor.forClass(LeaveFilterRequest.class);
        verify(leaveMapper).search(filterCaptor.capture());
        assertThat(filterCaptor.getValue().sortBy()).isEqualTo("createdAt");
        assertThat(filterCaptor.getValue().sortDirection()).isEqualTo("desc");
    }

    @Test
    @DisplayName("search rejects non HR user and does not call mappers")
    void searchRejectsNonHrUser() {
        assertThatThrownBy(() -> leaveService.search(LeaveFilterRequest.builder().build(), employeeUser()))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("Only HR can search leave requests");

        verifyNoInteractions(leaveMapper, outboxEventMapper, employeeSnapshotService, objectMapper);
    }

    @Test
    @DisplayName("search rejects invalid date range and does not call mappers")
    void searchRejectsInvalidDateRange() {
        LeaveFilterRequest filter = LeaveFilterRequest.builder()
                .fromDate(LocalDate.of(2026, 6, 12)).toDate(LocalDate.of(2026, 6, 10)).build();

        assertThatThrownBy(() -> leaveService.search(filter, hrUser()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("To date must be greater than or equal to from date");

        verify(leaveMapper, never()).search(any(LeaveFilterRequest.class));
        verify(leaveMapper, never()).countByFilter(any(LeaveFilterRequest.class));
        verifyNoInteractions(outboxEventMapper, employeeSnapshotService, objectMapper);
    }

    @Test
    @DisplayName("search caps size at 100")
    void searchCapsSizeAt100() {
        when(leaveMapper.search(any(LeaveFilterRequest.class))).thenReturn(List.of());
        when(leaveMapper.countByFilter(any(LeaveFilterRequest.class))).thenReturn(250L);

        PageResponse<LeaveResponse> response = leaveService.search(LeaveFilterRequest.builder().size(500).build(), hrUser());

        ArgumentCaptor<LeaveFilterRequest> filterCaptor = ArgumentCaptor.forClass(LeaveFilterRequest.class);
        verify(leaveMapper).search(filterCaptor.capture());
        assertThat(filterCaptor.getValue().size()).isEqualTo(100);
        assertThat(response.getSize()).isEqualTo(100);
        assertThat(response.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("findMyLeaves returns mapped page for employee and uses offset")
    void findMyLeavesReturnsMappedPageForEmployee() {
        when(employeeSnapshotService.getEmployeeSnapshotByAuthUserId(2L)).thenReturn(employeeSnapshot());
        when(leaveMapper.findByEmployeeId(10L, 2, 4)).thenReturn(List.of(leaveRequest()));
        when(leaveMapper.countByEmployeeId(10L)).thenReturn(5L);

        PageResponse<LeaveResponse> response = leaveService.findMyLeaves(employeeUser(), 2, 2);

        assertMappedLeave(response.getItems().getFirst());
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
                99L, "EMP099", "Employee Ninety Nine", "employee.ninetynine@company.com",
                "0123456789", "Developer", "ACTIVE", 2L, 30L, "Engineering");
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

    private void assertMappedLeave(LeaveResponse item) {
        assertMappedLeave(item, 10L);
    }

    private void assertMappedLeave(LeaveResponse item, Long employeeId) {
        assertThat(item.id()).isEqualTo(100L);
        assertThat(item.employeeId()).isEqualTo(employeeId);
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
    }

    private CurrentUser hrUser() {
        return new CurrentUser("hr_user", 1L, "HR");
    }

    private CurrentUser employeeUser() {
        return new CurrentUser("employee_user", 2L, "EMPLOYEE");
    }

    private EmployeeSnapshotResponse employeeSnapshot() {
        return new EmployeeSnapshotResponse(
                10L, "EMP001", "Employee One", "employee.one@company.com",
                "0123456789", "Developer", "ACTIVE", 2L, 30L, "Engineering");
    }

    private LeaveRequest leaveRequest() {
        return leaveRequest(10L);
    }

    private LeaveRequest leaveRequest(Long employeeId) {
        return LeaveRequest.builder()
                .id(100L)
                .employeeId(employeeId)
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
