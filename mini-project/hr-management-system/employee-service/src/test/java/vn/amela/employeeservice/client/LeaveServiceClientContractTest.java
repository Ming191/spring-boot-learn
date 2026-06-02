package vn.amela.employeeservice.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.amela.employeeservice.dto.response.PageResponse;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeaveServiceClientContractTest {

    @Test
    void findLeaves_usesLeaveListEndpointFromSrs() throws Exception {
        Method method = LeaveServiceClient.class.getMethod(
                "findLeaves",
                Long.class,
                String.class,
                int.class,
                int.class
        );

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping);
        assertArrayEquals(new String[]{"/api/leaves"}, getMapping.value());

        RequestParam[] requestParams = method.getParameters()[0].getAnnotationsByType(RequestParam.class);
        assertEquals("employee_id", requestParams[0].value());
        assertEquals("status", method.getParameters()[1].getAnnotation(RequestParam.class).value());
        assertEquals("page", method.getParameters()[2].getAnnotation(RequestParam.class).value());
        assertEquals("size", method.getParameters()[3].getAnnotation(RequestParam.class).value());
    }

    @Test
    void hasPendingLeavesByEmployeeId_queriesPendingLeavesWithSingleResultPage() {
        CapturingLeaveServiceClient client = new CapturingLeaveServiceClient(
                PageResponse.builder()
                        .items(List.of(new Object()))
                        .page(0)
                        .size(1)
                        .totalElements(1)
                        .totalPages(1)
                        .build()
        );

        assertTrue(client.hasPendingLeavesByEmployeeId(10L));
        assertEquals(10L, client.employeeId);
        assertEquals("PENDING", client.status);
        assertEquals(0, client.page);
        assertEquals(1, client.size);
    }

    private static class CapturingLeaveServiceClient implements LeaveServiceClient {
        private final PageResponse<Object> response;
        private Long employeeId;
        private String status;
        private int page;
        private int size;

        private CapturingLeaveServiceClient(PageResponse<Object> response) {
            this.response = response;
        }

        @Override
        public PageResponse<Object> findLeaves(Long employeeId, String status, int page, int size) {
            this.employeeId = employeeId;
            this.status = status;
            this.page = page;
            this.size = size;
            return response;
        }
    }
}
