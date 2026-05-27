package vn.amela.leaveservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import vn.amela.leaveservice.dto.response.EmployeeSnapshotResponse;

@FeignClient(name = "employee-service")
public interface EmployeeClient {

    @GetMapping("/api/employees/by-auth-user/{authUserId}")
    EmployeeSnapshotResponse findByAuthUserId(@PathVariable Long authUserId);
}