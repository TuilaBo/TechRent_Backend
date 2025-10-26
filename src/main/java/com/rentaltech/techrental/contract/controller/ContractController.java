package com.rentaltech.techrental.contract.controller;

import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.contract.model.Contract;
import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.contract.model.dto.*;
import com.rentaltech.techrental.contract.service.ContractService;
import com.rentaltech.techrental.webapi.customer.service.CustomerService;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.rentaltech.techrental.common.dto.AuthErrorResponseDto;
import com.rentaltech.techrental.common.dto.SuccessResponseDto;
import com.rentaltech.techrental.common.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/contracts")
@Tag(name = "Contract Management", description = "APIs để quản lý hợp đồng ")
public class ContractController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private CustomerService customerService;

    // ========== HELPER METHODS ==========
    
    /**
     * Lấy accountId từ principal
     */
    private Long getAccountIdFromPrincipal(org.springframework.security.core.userdetails.UserDetails principal) {
        try {
            String username = principal.getUsername();
            return accountService.getByUsername(username).orElseThrow(
                () -> new RuntimeException("Không tìm thấy tài khoản với username: " + username)
            ).getAccountId();
        } catch (Exception e) {
            throw new RuntimeException("Không thể lấy thông tin tài khoản: " + e.getMessage());
        }
    }

    // ========== TEST UTF-8 ==========
    
    @GetMapping("/test-utf8")
    public ResponseEntity<?> testUTF8() {
        return ResponseUtil.createSuccessResponse(
                "Test UTF-8 thành công!",
                "Kiểm tra encoding tiếng Việt: Điều khoản và điều kiện hợp đồng thuê thiết bị",
                "Tiếng Việt có dấu: àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ",
                HttpStatus.OK
        );
    }

    // ========== CONTRACT MANAGEMENT ==========
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<?> getAllContracts() {
        try {
            List<Contract> contracts = contractService.getAllContracts();
            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách hợp đồng thành công",
                    "Danh sách tất cả hợp đồng trong hệ thống",
                    contracts,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_CONTRACTS_FAILED",
                    "Lấy danh sách hợp đồng thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/{contractId}")
    public ResponseEntity<?> getContractById(@PathVariable Long contractId) {
        try {
            Optional<Contract> contract = contractService.getContractById(contractId);
            if (contract.isPresent()) {
                return ResponseUtil.createSuccessResponse(
                        "Lấy thông tin hợp đồng thành công",
                        "Thông tin chi tiết hợp đồng",
                        contract.get(),
                        HttpStatus.OK
                );
            } else {
                return ResponseUtil.createErrorResponse(
                        "CONTRACT_NOT_FOUND",
                        "Không tìm thấy hợp đồng",
                        "Hợp đồng với ID " + contractId + " không tồn tại",
                        HttpStatus.NOT_FOUND
                );
            }
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_CONTRACT_FAILED",
                    "Lấy thông tin hợp đồng thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation( description = "Tạo một hợp đồng mới trong hệ thống")
    @Deprecated
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tạo hợp đồng thành công",
                    content = @Content(schema = @Schema(implementation = SuccessResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                    content = @Content(schema = @Schema(implementation = AuthErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ResponseEntity<?> createContract(
            @Parameter(description = "Thông tin hợp đồng cần tạo", required = true)
            @RequestBody @Valid ContractCreateRequestDto request,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            // Lấy accountId từ username thông qua AccountService
            Long createdBy = getAccountIdFromPrincipal(principal);
            Contract contract = contractService.createContract(request, createdBy);
            
            return ResponseUtil.createSuccessResponse(
                    "Tạo hợp đồng thành công!",
                    "Hợp đồng đã được tạo và lưu vào hệ thống",
                    contract,
                    HttpStatus.CREATED
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "CREATE_CONTRACT_FAILED",
                    "Tạo hợp đồng thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/from-order/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(description = "Tạo hợp đồng tự động từ đơn thuê")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tạo hợp đồng từ đơn thuê thành công",
                    content = @Content(schema = @Schema(implementation = SuccessResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Đơn thuê không tồn tại hoặc dữ liệu không hợp lệ",
                    content = @Content(schema = @Schema(implementation = AuthErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ResponseEntity<?> createContractFromOrder(
            @Parameter(description = "ID của đơn thuê cần tạo hợp đồng", required = true)
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            Long createdBy = getAccountIdFromPrincipal(principal);
            Contract contract = contractService.createContractFromOrder(orderId, createdBy);
            
            return ResponseUtil.createSuccessResponse(
                    "Tạo hợp đồng từ đơn thuê thành công!",
                    "Hợp đồng đã được tạo tự động từ thông tin đơn thuê",
                    contract,
                    HttpStatus.CREATED
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "CREATE_CONTRACT_FROM_ORDER_FAILED",
                    "Tạo hợp đồng từ đơn thuê thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @PutMapping("/{contractId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<?> updateContract(
            @PathVariable Long contractId,
            @RequestBody @Valid ContractCreateRequestDto request,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            Long updatedBy = getAccountIdFromPrincipal(principal);
            Contract contract = contractService.updateContract(contractId, request, updatedBy);
            
            return ResponseUtil.createSuccessResponse(
                    "Cập nhật hợp đồng thành công!",
                    "Thông tin hợp đồng đã được cập nhật",
                    contract,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "UPDATE_CONTRACT_FAILED",
                    "Cập nhật hợp đồng thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    // ========== CONTRACT WORKFLOW ==========
    
    /**
     * Gửi hợp đồng để khách hàng ký
     */
    @PostMapping("/{contractId}/send-for-signature")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<?> sendForSignature(
            @PathVariable Long contractId,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            Long sentBy = getAccountIdFromPrincipal(principal);
            Contract contract = contractService.sendForSignature(contractId, sentBy);
            
            return ResponseUtil.createSuccessResponse(
                    "Gửi hợp đồng để ký thành công!",
                    "Hợp đồng đã được gửi cho khách hàng để ký",
                    contract,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "SEND_FOR_SIGNATURE_FAILED",
                    "Gửi hợp đồng để ký thất bại",
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
    
    /**
     * Cập nhật trạng thái hợp đồng
     */
    @PutMapping("/{contractId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<?> updateContractStatus(
            @PathVariable Long contractId,
            @RequestBody Map<String, String> request) {
        try {
            String statusStr = request.get("status");
            ContractStatus status = ContractStatus.valueOf(statusStr);
            
            Contract contract = contractService.updateContractStatus(contractId, status);
            
            return ResponseUtil.createSuccessResponse(
                    "Cập nhật trạng thái hợp đồng thành công!",
                    "Trạng thái hợp đồng đã được cập nhật thành: " + status,
                    contract,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "UPDATE_STATUS_FAILED",
                    "Cập nhật trạng thái hợp đồng thất bại",
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    // ========== PIN CODE MANAGEMENT ==========
    
    /**
     * Gửi PIN code qua SMS
     */
    @PostMapping("/{contractId}/send-pin/sms")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> sendSMSPIN(
            @PathVariable Long contractId,
            @RequestBody @Valid SmsPinRequestDto request) {
        
        String phoneNumber = request.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return ResponseUtil.createErrorResponse(
                    "PHONE_REQUIRED",
                    "Số điện thoại không được để trống",
                    "Vui lòng cung cấp số điện thoại để nhận mã PIN",
                    HttpStatus.BAD_REQUEST
            );
        }
        
        return contractService.sendSMSPIN(contractId, phoneNumber);
    }
    
    /**
     * Gửi PIN code qua Email
     */
    @PostMapping("/{contractId}/send-pin/email")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<?> sendEmailPIN(
            @PathVariable Long contractId,
            @RequestBody @Valid EmailPinRequestDto request) {
        
        String email = request.getEmail();
        if (email == null || email.trim().isEmpty()) {
            return ResponseUtil.createErrorResponse(
                    "EMAIL_REQUIRED",
                    "Email không được để trống",
                    "Vui lòng cung cấp email để nhận mã PIN",
                    HttpStatus.BAD_REQUEST
            );
        }
        
        return contractService.sendEmailPIN(contractId, email);
    }

    // ========== DIGITAL SIGNATURE ==========
    
    @PostMapping("/{contractId}/sign")
    public ResponseEntity<?> signContract(
            @PathVariable Long contractId,
            @RequestBody @Valid DigitalSignatureRequestDto request) {
        try {
            // Validate contract exists and is ready for signature
            if (!contractService.validateContractForSignature(contractId)) {
                return ResponseUtil.createErrorResponse(
                        "CONTRACT_NOT_READY",
                        "Hợp đồng chưa sẵn sàng để ký",
                        "Hợp đồng phải ở trạng thái 'Chờ ký' để có thể ký",
                        HttpStatus.BAD_REQUEST
                );
            }
            
            // Validate signature data
            if (!contractService.validateSignatureData(request)) {
                return ResponseUtil.createErrorResponse(
                        "INVALID_SIGNATURE_DATA",
                        "Dữ liệu chữ ký không hợp lệ",
                        "Vui lòng kiểm tra lại thông tin chữ ký",
                        HttpStatus.BAD_REQUEST
                );
            }
            
            DigitalSignatureResponseDto signatureResponse = contractService.signContract(request);
            
            return ResponseUtil.createSuccessResponse(
                    "Ký hợp đồng thành công!",
                    "Hợp đồng đã được ký điện tử và có hiệu lực",
                    signatureResponse,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "SIGN_CONTRACT_FAILED",
                    "Ký hợp đồng thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("/{contractId}/signature")
    public ResponseEntity<?> getSignatureInfo(@PathVariable Long contractId) {
        try {
            DigitalSignatureResponseDto signatureInfo = contractService.getSignatureInfo(contractId);
            return ResponseUtil.createSuccessResponse(
                    "Lấy thông tin chữ ký thành công",
                    "Thông tin chữ ký điện tử của hợp đồng",
                    signatureInfo,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_SIGNATURE_FAILED",
                    "Lấy thông tin chữ ký thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/{contractId}/verify")
    public ResponseEntity<?> verifySignature(@PathVariable Long contractId) {
        try {
            boolean isValid = contractService.verifySignature(contractId);
            if (isValid) {
                return ResponseUtil.createSuccessResponse(
                        "Xác thực chữ ký thành công",
                        "Chữ ký điện tử hợp lệ",
                        HttpStatus.OK
                );
            } else {
                return ResponseUtil.createErrorResponse(
                        "INVALID_SIGNATURE",
                        "Chữ ký không hợp lệ",
                        "Chữ ký điện tử không hợp lệ hoặc đã bị thay đổi",
                        HttpStatus.BAD_REQUEST
                );
            }
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "VERIFY_SIGNATURE_FAILED",
                    "Xác thực chữ ký thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // ========== CONTRACT STATUS MANAGEMENT ==========

    @PostMapping("/{contractId}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<?> cancelContract(
            @PathVariable Long contractId,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            Long cancelledBy = getAccountIdFromPrincipal(principal);
            Contract contract = contractService.cancelContract(contractId, reason, cancelledBy);
            
            return ResponseUtil.createSuccessResponse(
                    "Hủy hợp đồng thành công!",
                    "Hợp đồng đã được hủy bỏ",
                    contract,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "CANCEL_CONTRACT_FAILED",
                    "Hủy hợp đồng thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    // ========== CUSTOMER CONTRACTS ==========
    
    /**
     * Lấy danh sách hợp đồng của customer (theo customerId)
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getCustomerContracts(@PathVariable Long customerId) {
        try {
            List<Contract> contracts = contractService.getContractsByCustomerId(customerId);
            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách hợp đồng khách hàng thành công",
                    "Danh sách hợp đồng của khách hàng",
                    contracts,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_CUSTOMER_CONTRACTS_FAILED",
                    "Lấy danh sách hợp đồng khách hàng thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Customer xem danh sách hợp đồng của chính mình
     */
    @GetMapping("/my-contracts")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMyContracts(@AuthenticationPrincipal UserDetails principal) {
        try {
            // Lấy customerId từ authenticated user
            Customer customer = getCustomerFromPrincipal(principal);
            List<Contract> contracts = contractService.getContractsByCustomerId(customer.getCustomerId());
            
            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách hợp đồng thành công",
                    "Danh sách hợp đồng của bạn",
                    contracts,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_MY_CONTRACTS_FAILED",
                    "Lấy danh sách hợp đồng thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Customer xem hợp đồng chờ ký của mình
     */
    @GetMapping("/pending-signature")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getPendingSignatureContracts(@AuthenticationPrincipal UserDetails principal) {
        try {
            Customer customer = getCustomerFromPrincipal(principal);
            List<Contract> contracts = contractService.getContractsByCustomerIdAndStatus(
                    customer.getCustomerId(), 
                    ContractStatus.PENDING_SIGNATURE
            );
            
            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách hợp đồng chờ ký thành công",
                    "Các hợp đồng chờ bạn ký",
                    contracts,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_PENDING_SIGNATURE_FAILED",
                    "Lấy danh sách hợp đồng chờ ký thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Helper method: Lấy Customer từ authentication
     */
    private Customer getCustomerFromPrincipal(UserDetails principal) {
        try {
            String username = principal.getUsername();
            return customerService.getCustomerByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin customer cho username: " + username));
        } catch (Exception e) {
            throw new RuntimeException("Không thể lấy thông tin customer: " + e.getMessage());
        }
    }
}
