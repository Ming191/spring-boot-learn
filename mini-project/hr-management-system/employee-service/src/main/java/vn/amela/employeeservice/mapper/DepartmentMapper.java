package vn.amela.employeeservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vn.amela.employeeservice.entity.Department;

import java.util.List;

@Mapper
public interface DepartmentMapper {

    void insert(Department department);

    Department findById(@Param("id") Long id);

    Department findByName(@Param("name") String name);

    List<Department> findAll();

    List<Department> findActive();

    boolean existsByNameExceptId(
            @Param("name") String name,
            @Param("id") Long id
    );

    int update(Department department);

    int deactivate(@Param("id") Long id);

}
