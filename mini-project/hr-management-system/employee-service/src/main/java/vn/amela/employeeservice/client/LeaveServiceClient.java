package vn.amela.employeeservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "leave-service", fallback = LeaveServiceClientFallback.class)
public interface LeaveServiceClient {

    @GetMapping("/api/leaves/pending-count")
    int countPendingLeavesByEmployeeId(@RequestParam("employeeId") Long employeeId);

    default boolean hasPendingLeavesByEmployeeId(Long employeeId) {
        return countPendingLeavesByEmployeeId(employeeId) > 0;
    }
}
