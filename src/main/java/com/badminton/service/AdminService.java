package com.badminton.service;

import java.util.Map;

public interface AdminService {    Map<String, Object> clearDatabase(Long userId, String confirmCode);
}
