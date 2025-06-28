package com.example.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reimbursement {
    private String uuid;
    private String description;
    private BigDecimal amount;
    private String transactionDefinition;
}
