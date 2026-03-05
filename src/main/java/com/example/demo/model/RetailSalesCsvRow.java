package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO that maps directly to the CSV file columns.
 * This is the INPUT object for the Spring Batch Reader.
 *
 * CSV Columns:
 *   Transaction ID, Date, Customer ID, Gender, Age,
 *   Product Category, Quantity, Price per Unit, Total Amount
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetailSalesCsvRow {

    private Long transactionId;
    private String date;
    private String customerId;
    private String gender;
    private Integer age;
    private String productCategory;
    private Integer quantity;
    private Double pricePerUnit;
    private Double totalAmount;
}
