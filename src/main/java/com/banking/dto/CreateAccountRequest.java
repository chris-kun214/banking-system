package com.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "账户名不能为空")
    @Size(max = 20, message = "账户名不能超过20个字符")
    private String accountName;

    @NotBlank(message = "账号不能为空")
    @Size(max = 30, message = "账号不能超过30个字符")
    private String accountNumber;

    @NotNull(message = "初始余额不能为空")
    @DecimalMin(value = "0.0", message = "初始余额不能为负数")
    private BigDecimal initialBalance;
}

