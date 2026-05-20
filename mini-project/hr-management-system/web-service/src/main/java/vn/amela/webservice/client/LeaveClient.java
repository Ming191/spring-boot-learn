package vn.amela.webservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import vn.amela.webservice.dto.request.CreateLeaveForm;
import vn.amela.webservice.dto.request.LeaveFilterRequest;
import vn.amela.webservice.dto.request.RejectLeaveForm;
import vn.amela.webservice.dto.request.ReviewLeaveForm;
import vn.amela.webservice.dto.response.LeaveResponse;
import vn.amela.webservice.dto.response.PageResponse;

@FeignClient(name = "leave-service", contextId = "leaveClient", path = "/api/leaves")
public interface LeaveClient {

    String USER_ID_HEADER = "X-User-Id";
    String USER_ROLE_HEADER = "X-User-Role";

    @PostMapping
    LeaveResponse create(
        @RequestBody CreateLeaveForm request,
        @RequestHeader(USER_ID_HEADER) Long userId,
        @RequestHeader(USER_ROLE_HEADER) String userRole
    );

    @GetMapping
    PageResponse<LeaveResponse> search(
        @SpringQueryMap LeaveFilterRequest filter,
        @RequestHeader(USER_ID_HEADER) Long userId,
        @RequestHeader(USER_ROLE_HEADER) String userRole
    );

    @GetMapping("/my")
    PageResponse<LeaveResponse> getMyLeaves(
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestHeader(USER_ID_HEADER) Long userId,
        @RequestHeader(USER_ROLE_HEADER) String userRole
    );

    @GetMapping("/{id}")
    LeaveResponse getById(
        @PathVariable("id") Long id,
        @RequestHeader(USER_ID_HEADER) Long userId,
        @RequestHeader(USER_ROLE_HEADER) String userRole
    );

    @PostMapping("/{id}/approve")
    LeaveResponse approve(
        @PathVariable("id") Long id,
        @RequestBody ReviewLeaveForm request,
        @RequestHeader(USER_ID_HEADER) Long userId,
        @RequestHeader(USER_ROLE_HEADER) String userRole
    );

    @PostMapping("/{id}/reject")
    LeaveResponse reject(
        @PathVariable("id") Long id,
        @RequestBody RejectLeaveForm request,
        @RequestHeader(USER_ID_HEADER) Long userId,
        @RequestHeader(USER_ROLE_HEADER) String userRole
    );

    @PostMapping("/{id}/cancel")
    LeaveResponse cancel(
        @PathVariable("id") Long id,
        @RequestHeader(USER_ID_HEADER) Long userId,
        @RequestHeader(USER_ROLE_HEADER) String userRole
    );

    @GetMapping("/pending-count")
    int countPendingByEmployeeId(@RequestParam("employeeId") Long employeeId);
}
