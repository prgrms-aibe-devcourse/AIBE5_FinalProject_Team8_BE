package com.Rootin.test;

import com.Rootin.global.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public ApiResponse<String> test() {
        return ApiResponse.ok("ApiResponse 포맷 확인", "OK");
    }
}
