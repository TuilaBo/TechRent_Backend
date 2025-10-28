//package com.rentaltech.techrental.config;
//
//import com.rentaltech.techrental.staff.model.StaffRole;
//import org.springframework.core.convert.converter.Converter;
//import org.springframework.stereotype.Component;
//
//@Component
//public class StaffRoleConverter implements Converter<String, StaffRole> {
//
//    @Override
//    public StaffRole convert(String source) {
//        try {
//            // Convert PascalCase to UPPER_CASE mapping
//            switch (source) {
//                case "Admin":
//                    return StaffRole.ADMIN;
//                case "Technician":
//                    return StaffRole.TECHNICIAN;
//                case "Operator":
//                    return StaffRole.OPERATOR;
//                case "CustomerSupportStaff":
//                    return StaffRole.CUSTOMER_SUPPORT_STAFF;
//                default:
//                    // Try direct enum valueOf for UPPER_CASE input
//                    return StaffRole.valueOf(source.toUpperCase());
//            }
//        } catch (IllegalArgumentException e) {
//            throw new IllegalArgumentException("Invalid StaffRole: " + source +
//                ". Valid values are: Admin, Technician, Operator, CustomerSupportStaff");
//        }
//    }
//}
