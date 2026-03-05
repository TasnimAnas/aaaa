package com.example.demo.processor;

import com.example.demo.model.RetailSalesCsvRow;
import com.example.demo.model.RetailSalesRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Spring Batch ItemProcessor that transforms raw CSV data into enriched records.
 *
 * Demonstrates the following operations:
 *   1. ADDITION        → priceWithTax, quantityWithBonus
 *   2. SUBTRACTION     → discountedAmount, priceAfterCoupon
 *   3. MULTIPLICATION  → totalAmountDoubled, totalWithTax
 *   4. DIVISION        → avgPricePerUnit, spendingPerAgeYear
 *   5. MODULO          → quantityParity (even/odd check)
 *   6. CONDITIONAL     → ageGroup, spendingTier
 *   7. COMBINED        → estimatedProfit (multi-step calculation)
 */
@Slf4j
@Component
public class RetailSalesProcessor implements ItemProcessor<RetailSalesCsvRow, RetailSalesRecord> {

    @Override
    public RetailSalesRecord process(RetailSalesCsvRow csv) throws Exception {

        log.info("Processing Transaction ID: {}", csv.getTransactionId());

        RetailSalesRecord record = new RetailSalesRecord();

        // ─── 1. Copy original fields ──────────────────────────
        record.setTransactionId(csv.getTransactionId());
        record.setTransactionDate(csv.getDate());
        record.setCustomerId(csv.getCustomerId());
        record.setGender(csv.getGender());
        record.setAge(csv.getAge());
        record.setProductCategory(csv.getProductCategory());
        record.setQuantity(csv.getQuantity());
        record.setPricePerUnit(csv.getPricePerUnit());
        record.setTotalAmount(csv.getTotalAmount());

        // ─── 2. ADDITION Operations ───────────────────────────

        // Price + 18% GST
        double tax = csv.getPricePerUnit() * 0.18;
        record.setPriceWithTax(roundToTwo(csv.getPricePerUnit() + tax));

        // Quantity + 5 bonus items
        record.setQuantityWithBonus(csv.getQuantity() + 5);

        // ─── 3. SUBTRACTION Operations ────────────────────────

        // Total Amount - 10% discount
        double discount = csv.getTotalAmount() * 0.10;
        record.setDiscountedAmount(roundToTwo(csv.getTotalAmount() - discount));

        // Price per unit - flat $10 coupon (minimum 0)
        record.setPriceAfterCoupon(roundToTwo(Math.max(csv.getPricePerUnit() - 10.0, 0.0)));

        // ─── 4. MULTIPLICATION Operations ─────────────────────

        // Total Amount * 2 (simulate double-order)
        record.setTotalAmountDoubled(roundToTwo(csv.getTotalAmount() * 2.0));

        // Total Amount * 1.18 (total with GST)
        record.setTotalWithTax(roundToTwo(csv.getTotalAmount() * 1.18));

        // ─── 5. DIVISION Operations ───────────────────────────

        // Average price = Total Amount / Quantity
        if (csv.getQuantity() != null && csv.getQuantity() > 0) {
            record.setAvgPricePerUnit(roundToTwo(csv.getTotalAmount() / csv.getQuantity()));
        } else {
            record.setAvgPricePerUnit(0.0);
        }

        // Spending per year of age = Total Amount / Age
        if (csv.getAge() != null && csv.getAge() > 0) {
            record.setSpendingPerAgeYear(roundToTwo(csv.getTotalAmount() / csv.getAge()));
        } else {
            record.setSpendingPerAgeYear(0.0);
        }

        // ─── 6. MODULO Operation ──────────────────────────────

        // Check if quantity is even or odd
        record.setQuantityParity(csv.getQuantity() % 2 == 0 ? "Even" : "Odd");

        // ─── 7. CONDITIONAL / LOGICAL Operations ──────────────

        // Age Group classification
        record.setAgeGroup(classifyAgeGroup(csv.getAge()));

        // Spending Tier classification
        record.setSpendingTier(classifySpendingTier(csv.getTotalAmount()));

        // ─── 8. COMBINED Operation ────────────────────────────

        // Estimated Profit = 30% margin on total amount
        record.setEstimatedProfit(roundToTwo(csv.getTotalAmount() * 0.30));

        log.info("Transformed → TxnID={}, priceWithTax={}, discounted={}, doubled={}, avgPrice={}, ageGroup={}, tier={}",
                record.getTransactionId(),
                record.getPriceWithTax(),
                record.getDiscountedAmount(),
                record.getTotalAmountDoubled(),
                record.getAvgPricePerUnit(),
                record.getAgeGroup(),
                record.getSpendingTier());

        return record;
    }

    // ─── Helper Methods ───────────────────────────────────────

    private String classifyAgeGroup(Integer age) {
        if (age == null) return "Unknown";
        if (age <= 25) return "Young";
        if (age <= 45) return "Middle-Aged";
        return "Senior";
    }

    private String classifySpendingTier(Double totalAmount) {
        if (totalAmount == null) return "Unknown";
        if (totalAmount < 100) return "Low";
        if (totalAmount <= 500) return "Medium";
        return "High";
    }

    private double roundToTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
