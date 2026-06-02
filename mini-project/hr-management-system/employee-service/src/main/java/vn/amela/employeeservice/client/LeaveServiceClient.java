package vn.amela.employeeservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.amela.employeeservice.dto.response.PageResponse;

@FeignClient(name = "leave-service", fallback = LeaveServiceClientFallback.class)
public interface LeaveServiceClient {

    @GetMapping("/api/leaves")
    PageResponse<Object> findLeaves(
            @RequestParam("employee_id") Long employeeId,
            @RequestParam("status") String status,
            @RequestParam("page") int page,
            @RequestParam("size") int size
    );

    default boolean hasPendingLeavesByEmployeeId(Long employeeId) {
        PageResponse<Object> response = findLeaves(employeeId, "PENDING", 0, 1);
        if (response == null) {
            return false;
        }
        return response.totalElements() > 0 || response.items() != null && !response.items().isEmpty();
    }
}
