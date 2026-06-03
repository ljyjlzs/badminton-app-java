package com.badminton.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.badminton.entity.Score;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;@Mapper
public interface ScoreMapper extends BaseMapper<Score> {    @Select("SELECT user_id, SUM(score_change) as total_score " +
            "FROM scores " +
            "WHERE activity_id = #{activityId} " +
            "GROUP BY user_id " +
            "ORDER BY total_score DESC")
    List<Map<String, Object>> getUserScores(@Param("activityId") Long activityId);
}
