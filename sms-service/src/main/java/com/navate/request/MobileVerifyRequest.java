package com.navate.request;

import lombok.Data;

@Data
public class MobileVerifyRequest {
    private String mobile;
    private String code;
}