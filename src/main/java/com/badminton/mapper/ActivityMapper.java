package com.badminton.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.badminton.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {    @Select("SELECT a.*, u.nickname as organizer_name " +
            "FROM activities a " +
            "LEFT JOIN users u ON a.organizer_id = u.id " +
            "WHERE a.status = 'registering' " +
            "ORDER BY a.time ASC")
    List<Activity> findAvailableActivities();    @Select("SELECT a.*, u.nickname as organizer_name " +
            "FROM activities a " +
            "LEFT JOIN users u ON a.organizer_id = u.id " +
            "WHERE a.status = 'registering' " +
            "AND (a.name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR a.location LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY a.time ASC")
    List<Activity> searchActivities(@Param("keyword") String keyword);
}
