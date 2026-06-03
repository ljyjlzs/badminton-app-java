package com.badminton.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.badminton.entity.Registration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {    @Select("SELECT COUNT(*) FROM registrations " +
            "WHERE activity_id = #{activityId} " +
            "AND (cancel_status IS NULL OR cancel_status != 'approved')")
    int countValidRegistrations(@Param("activityId") Long activityId);    @Update("UPDATE registrations SET " +
            "cancel_status = NULL, " +
            "cancel_reason = NULL, " +
            "cancel_requested_at = NULL, " +
            "cancel_processed_at = NULL, " +
            "cancel_processed_by = NULL " +
            "WHERE id = #{id}")
    void resetCancelStatus(@Param("id") Long id);
}
