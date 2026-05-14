package vn.amela.employeeservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vn.amela.employeeservice.entity.Employee;
import vn.amela.employeeservice.entity.enums.EmployeeStatus;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface EmployeeMapper {
    void insert(Employee employee);

    Employee findById(@Param("id") Long id);

    Employee findByEmployeeCode(@Param("employeeCode") String employeeCode);

    Employee findByEmail(@Param("email") String email);

    Employee findByAuthUserId(@Param("authUserId") Long authUserId);

    boolean existsByEmployeeCodeExceptId(
            @Param("employeeCode") String employeeCode,
            @Param("id") Long id
    );

    boolean existsByEmailExceptId(
            @Param("email") String email,
            @Param("id") Long id
    );

    List<Employee> search(
            @Param("keyword") String keyword,
            @Param("departmentId") Long departmentId,
            @Param("position") String position,
            @Param("status") EmployeeStatus status,
            @Param("startDateFrom") LocalDate startDateFrom,
            @Param("startDateTo") LocalDate startDateTo,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("size") int size
    );

    int countByFilter(
            @Param("keyword") String keyword,
            @Param("departmentId") Long departmentId,
            @Param("position") String position,
            @Param("status") EmployeeStatus status,
            @Param("startDateFrom") LocalDate startDateFrom,
            @Param("startDateTo") LocalDate startDateTo
    );

    int updateByHr(Employee employee);

    int updateContact(
            @Param("id") Long id,
            @Param("email") String email,
            @Param("phone") String phone
    );

    int deactivate(@Param("id") Long id);

    int countActiveByDepartmentId(@Param("departmentId") Long departmentId);

    boolean existsActiveInDepartment(
            @Param("employeeId") Long employeeId,
            @Param("departmentId") Long departmentId
    );
}
