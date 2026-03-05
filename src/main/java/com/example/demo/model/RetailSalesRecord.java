package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA Entity that maps to the database table.
 * This is the OUTPUT object for the Spring Batch Writer.
 *
 * Contains ORIGINAL columns from CSV + COMPUTED/TRANSFORMED columns
 * that demonstrate various arithmetic and logical operations.
 */
@Entity
@Table(name = "retail_sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetailSalesRecord {

    // ─── ORIGINAL COLUMNS (from CSV) ───────────────────────────

    @Id
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "transaction_date")
    private String transactionDate;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "gender")
    private String gender;

    @Column(name = "age")
    private Integer age;

    @Column(name = "product_category")
    private String productCategory;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "price_per_unit")
    private Double pricePerUnit;

    @Column(name = "total_amount")
    private Double totalAmount;

    // ─── TRANSFORMED / COMPUTED COLUMNS ────────────────────────

    /**
     * ADDITION: Price per Unit + 18% GST Tax
     * Formula: pricePerUnit + (pricePerUnit * 0.18)
     */
    @Column(name = "price_with_tax")
    private Double priceWithTax;

    /**
     * SUBTRACTION: Total Amount - 10% Discount
     * Formula: totalAmount - (totalAmount * 0.10)
     */
    @Column(name = "discounted_amount")
    private Double discountedAmount;

    /**
     * MULTIPLICATION: Total Amount * 2 (Double-order scenario)
     * Formula: totalAmount * 2
     */
    @Column(name = "total_amount_doubled")
    private Double totalAmountDoubled;

    /**
     * DIVISION: Average Price per Unit
     * Formula: totalAmount / quantity
     */
    @Column(name = "avg_price_per_unit")
    private Double avgPricePerUnit;

    /**
     * ADDITION: Quantity + 5 bonus items
     * Formula: quantity + 5
     */
    @Column(name = "quantity_with_bonus")
    private Integer quantityWithBonus;

    /**
     * SUBTRACTION: Price per Unit minus flat $10 coupon
     * Formula: max(pricePerUnit - 10, 0)
     */
    @Column(name = "price_after_coupon")
    private Double priceAfterCoupon;

    /**
     * MULTIPLICATION: Total Amount with tax (18% GST on total)
     * Formula: totalAmount * 1.18
     */
    @Column(name = "total_with_tax")
    private Double totalWithTax;

    /**
     * DIVISION: Spending per year of age
     * Formula: totalAmount / age
     */
    @Column(name = "spending_per_age_year")
    private Double spendingPerAgeYear;

    /**
     * CONDITIONAL / LOGICAL: Age group classification
     * 0-25 → "Young", 26-45 → "Middle-Aged", 46+ → "Senior"
     */
    @Column(name = "age_group")
    private String ageGroup;

    /**
     * CONDITIONAL: Spending tier based on Total Amount
     * < 100 → "Low", 100-500 → "Medium", > 500 → "High"
     */
    @Column(name = "spending_tier")
    private String spendingTier;

    /**
     * MODULO: Whether quantity is even or odd
     * quantity % 2 == 0 → "Even", else → "Odd"
     */
    @Column(name = "quantity_parity")
    private String quantityParity;

    /**
     * COMBINED: Profit estimate (30% margin on total)
     * Formula: totalAmount - (totalAmount * 0.70)  i.e. totalAmount * 0.30
     */
    @Column(name = "estimated_profit")
    private Double estimatedProfit;
}
