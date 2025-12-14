package com.rentaltech.techrental.contract.controller;

import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.common.dto.AuthErrorResponseDto;
import com.rentaltech.techrental.common.dto.SuccessResponseDto;
import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.contract.model.Contract;
import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.contract.model.dto.*;
import com.rentaltech.techrental.contract.repository.ContractExtensionAnnexRepository;
import com.rentaltech.techrental.contract.repository.ContractRepository;
import com.rentaltech.techrental.contract.service.ContractExtensionAnnexService;
import com.rentaltech.techrental.contract.service.ContractService;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.service.DeviceAllocationQueryService;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/contracts")
@Tag(name = "Quản lý hợp đồng", description = "Nhóm API phục vụ tạo, ký và theo dõi hợp đồng thuê thiết bị")
public class ContractController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private DeviceAllocationQueryService deviceAllocationQueryService;

    @Autowired
    private ContractExtensionAnnexService contractExtensionAnnexService;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractExtensionAnnexRepository contractExtensionAnnexRepository;

    @Autowired
    private com.rentaltech.techrental.rentalorder.repository.RentalOrderExtensionRepository rentalOrderExtensionRepository;

    // ========== HELPER METHODS ==========
    
    /**
     * Lấy accountId từ principal
     */
    private Long getAccountIdFromPrincipal(org.springframework.security.core.userdetails.UserDetails principal) {
        try {
            String username = principal.getUsername();
            return accountService.getByUsername(username).orElseThrow(
                () -> new RuntimeException("Không tìm thấy tài khoản với tên đăng nhập: " + username)
            ).getAccountId();
        } catch (Exception e) {
            throw new RuntimeException("Không thể lấy thông tin tài khoản: " + e.getMessage());
        }
    }

    // ========== TEST UTF-8 ==========
    
    @GetMapping("/test-utf8")
    @Operation(summary = "Kiểm tra hiển thị tiếng Việt", description = "API kiểm tra mã hóa UTF-8 cho tài liệu hợp đồng")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Kiểm tra UTF-8 thành công"),
            @ApiResponse(responseCode = "500", description = "Không kiểm tra được mã hóa UTF-8")
    })
    public ResponseEntity<?> testUTF8() {
        return ResponseUtil.createSuccessResponse(
                "Kiểm tra UTF-8 thành công!",
                "Đã kiểm tra mã hóa tiếng Việt: Điều khoản và điều kiện hợp đồng thuê thiết bị",
                "Tiếng Việt có dấu: àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ",
                HttpStatus.OK
        );
    }

    // ========== CONTRACT MANAGEMENT ==========
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Danh sách hợp đồng", description = "Quản trị viên/Điều phối viên xem toàn bộ hợp đồng hiện có trong hệ thống")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trả về danh sách hợp đồng"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống khi lấy danh sách hợp đồng")
    })
    public ResponseEntity<?> getAllContracts() {
        try {
            List<Contract> contracts = contractService.getAllContracts();
            List<ContractResponseDto> dtoList = contracts.stream()
                    .map(this::mapToResponseDto)
                    .toList();
            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách hợp đồng thành công",
                    "Danh sách tất cả hợp đồng trong hệ thống",
                    dtoList,
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
    @Operation(summary = "Chi tiết hợp đồng", description = "Lấy thông tin chi tiết một hợp đồng theo mã")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trả về thông tin hợp đồng"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy hợp đồng theo mã"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống khi lấy chi tiết hợp đồng")
    })
    public ResponseEntity<?> getContractById(@PathVariable Long contractId) {
        try {
            Optional<Contract> contract = contractService.getContractById(contractId);
            if (contract.isPresent()) {
                return ResponseUtil.createSuccessResponse(
                        "Lấy thông tin hợp đồng thành công",
                        "Thông tin chi tiết hợp đồng",
                        mapToResponseDto(contract.get()),
                        HttpStatus.OK
                );
            } else {
                return ResponseUtil.createErrorResponse(
                        "CONTRACT_NOT_FOUND",
                        "Không tìm thấy hợp đồng",
                        "Hợp đồng với mã " + contractId + " không tồn tại",
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


    @PostMapping("/from-order/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Tạo hợp đồng từ đơn thuê", description = "Tự động sinh hợp đồng dựa trên thông tin một đơn thuê đã tồn tại")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tạo hợp đồng từ đơn thuê thành công",
                    content = @Content(schema = @Schema(implementation = SuccessResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Đơn thuê không tồn tại hoặc dữ liệu không hợp lệ",
                    content = @Content(schema = @Schema(implementation = AuthErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ResponseEntity<?> createContractFromOrder(
            @Parameter(description = "Mã của đơn thuê cần tạo hợp đồng", required = true)
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            Long createdBy = getAccountIdFromPrincipal(principal);
            Contract contract = contractService.createContractFromOrder(orderId, createdBy);
            
            return ResponseUtil.createSuccessResponse(
                    "Tạo hợp đồng từ đơn thuê thành công!",
                    "Hợp đồng đã được tạo tự động từ thông tin đơn thuê",
                    mapToResponseDto(contract),
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
    @Operation(summary = "Cập nhật hợp đồng", description = "Quản trị viên/Điều phối viên chỉnh sửa nội dung hợp đồng đã tạo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cập nhật hợp đồng thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy hợp đồng cần cập nhật")
    })
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
                    mapToResponseDto(contract),
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
    @Operation(summary = "Gửi hợp đồng để ký", description = "Đưa hợp đồng sang trạng thái chờ quản trị viên ký và thông báo quy trình ký điện tử")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đã chuyển hợp đồng sang trạng thái chờ ký"),
            @ApiResponse(responseCode = "400", description = "Không thể gửi hợp đồng do dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy hợp đồng cần gửi ký")
    })
    public ResponseEntity<?> sendForSignature(
            @PathVariable Long contractId,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            Long sentBy = getAccountIdFromPrincipal(principal);
            Contract contract = contractService.sendForSignature(contractId, sentBy);
            
            return ResponseUtil.createSuccessResponse(
                    "Gửi hợp đồng để ký thành công!",
                    "Hợp đồng đã được chuyển sang trạng thái chờ quản trị viên ký",
                    mapToResponseDto(contract),
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
    @Operation(summary = "Đổi trạng thái hợp đồng", description = "Cập nhật trạng thái bất kỳ của hợp đồng")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái hợp đồng thành công"),
            @ApiResponse(responseCode = "400", description = "Trạng thái yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy hợp đồng cần cập nhật trạng thái")
    })
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
                    mapToResponseDto(contract),
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
    @Operation(summary = "Gửi PIN qua SMS", description = "Gửi mã PIN ký hợp đồng cho khách hàng bằng SMS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đã gửi mã PIN qua SMS"),
            @ApiResponse(responseCode = "400", description = "Số điện thoại hoặc trạng thái hợp đồng không hợp lệ")
    })
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
    @Operation(summary = "Gửi PIN qua email", description = "Gửi mã PIN ký hợp đồng cho khách hàng bằng email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đã gửi mã PIN qua email"),
            @ApiResponse(responseCode = "400", description = "Email hoặc trạng thái hợp đồng không hợp lệ")
    })
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
    
    /**
     * Customer ký hợp đồng
     * POST /api/contracts/{contractId}/sign
     */
    @PostMapping("/{contractId}/sign")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Khách hàng ký hợp đồng", description = "Khách hàng xác nhận chữ ký điện tử sau khi đã nhận được mã PIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ký hợp đồng thành công"),
            @ApiResponse(responseCode = "400", description = "Hợp đồng chưa sẵn sàng hoặc dữ liệu chữ ký không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Không thể xử lý yêu cầu ký hợp đồng")
    })
    public ResponseEntity<?> signContract(
            @PathVariable Long contractId,
            @RequestBody @Valid DigitalSignatureRequestDto request) {
        try {
            // Validate contract exists and is ready for signature
            if (!contractService.validateContractForSignature(contractId)) {
                return ResponseUtil.createErrorResponse(
                        "CONTRACT_NOT_READY",
                        "Hợp đồng chưa sẵn sàng để ký",
                        "Hợp đồng phải ở trạng thái 'Chờ khách hàng ký' và quản trị viên đã ký trước",
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

    /**
     * Admin ký hợp đồng trước
     * POST /api/contracts/{contractId}/admin/sign
     */
    @PostMapping("/{contractId}/admin/sign")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Quản trị viên ký hợp đồng", description = "Quản trị viên/Điều phối viên ký và chuyển hợp đồng sang trạng thái chờ khách hàng ký")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quản trị viên đã ký hợp đồng thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu chữ ký không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy hợp đồng để ký")
    })
    public ResponseEntity<?> signContractByAdmin(
            @PathVariable Long contractId,
            @RequestBody @Valid DigitalSignatureRequestDto request,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            Long adminId = getAccountIdFromPrincipal(principal);
            
            // Validate signature data
            if (!contractService.validateSignatureData(request)) {
                return ResponseUtil.createErrorResponse(
                        "INVALID_SIGNATURE_DATA",
                        "Dữ liệu chữ ký không hợp lệ",
                        "Vui lòng kiểm tra lại thông tin chữ ký",
                        HttpStatus.BAD_REQUEST
                );
            }
            
            DigitalSignatureResponseDto signatureResponse = contractService.signContractByAdmin(contractId, adminId, request);
            
            return ResponseUtil.createSuccessResponse(
                    "Quản trị viên ký hợp đồng thành công!",
                    "Hợp đồng đã được quản trị viên ký và chờ khách hàng ký",
                    signatureResponse,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "ADMIN_SIGN_CONTRACT_FAILED",
                    "Quản trị viên ký hợp đồng thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("/{contractId}/signature")
    @Operation(summary = "Thông tin chữ ký", description = "Lấy lịch sử ký điện tử của hợp đồng")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trả về thông tin chữ ký điện tử"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy hợp đồng hoặc bản ghi chữ ký"),
            @ApiResponse(responseCode = "500", description = "Không thể lấy thông tin chữ ký")
    })
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

    // ========== CONTRACT EXTENSION ANNEX ==========

    @PostMapping("/{contractId}/annexes/from-extension/{extensionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Tạo phụ lục từ gia hạn", description = "Khởi tạo phụ lục gia hạn dựa trên bản ghi RentalOrderExtension")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tạo phụ lục thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc phụ lục đã tồn tại"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy hợp đồng hoặc bản ghi gia hạn")
    })
    public ResponseEntity<?> createAnnexFromExtension(@PathVariable Long contractId,
                                                      @PathVariable Long extensionId,
                                                      @AuthenticationPrincipal UserDetails principal) {
        try {
            Long createdBy = getAccountIdFromPrincipal(principal);

            var contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng: " + contractId));

            var extension = rentalOrderExtensionRepository.findById(extensionId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi gia hạn: " + extensionId));

            // Tránh tạo trùng
            boolean exists = contractExtensionAnnexRepository
                    .findFirstByRentalOrderExtension_ExtensionId(extensionId)
                    .isPresent();
            if (exists) {
                return ResponseUtil.createErrorResponse(
                        "ANNEX_ALREADY_EXISTS",
                        "Phụ lục cho bản gia hạn đã tồn tại",
                        "Đã có phụ lục gắn với extensionId=" + extensionId,
                        HttpStatus.BAD_REQUEST
                );
            }

            // Kiểm tra ràng buộc hợp đồng - đơn gốc
            if (contract.getOrderId() != null && extension.getRentalOrder() != null) {
                Long contractOrderId = contract.getOrderId();
                Long extensionOrderId = extension.getRentalOrder().getOrderId();
                if (!contractOrderId.equals(extensionOrderId)) {
                    return ResponseUtil.createErrorResponse(
                            "CONTRACT_ORDER_MISMATCH",
                            "Phụ lục không thuộc hợp đồng này",
                            "Contract.orderId=" + contractOrderId + ", extension.orderId=" + extensionOrderId,
                            HttpStatus.BAD_REQUEST
                    );
                }
            }

            var annex = contractExtensionAnnexService.createAnnexForExtension(contract, extension, createdBy);
            ContractExtensionAnnexResponseDto dto = ContractExtensionAnnexResponseDto.from(annex);
            return ResponseUtil.createSuccessResponse(
                    "Tạo phụ lục thành công",
                    "Phụ lục gia hạn đã được khởi tạo",
                    dto,
                    HttpStatus.CREATED
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "CREATE_ANNEX_FAILED",
                    "Không thể tạo phụ lục từ bản gia hạn",
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("/{contractId}/annexes")
    @Operation(summary = "Danh sách phụ lục", description = "Liệt kê toàn bộ phụ lục gia hạn gắn với một hợp đồng")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trả về danh sách phụ lục gia hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy hợp đồng tương ứng"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn dữ liệu phụ lục")
    })
    public ResponseEntity<?> getContractAnnexes(@PathVariable Long contractId) {
        try {
            List<ContractExtensionAnnexResponseDto> annexes = contractExtensionAnnexService.getAnnexesForContract(contractId);
            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách phụ lục gia hạn thành công",
                    "Các phụ lục gia hạn của hợp đồng",
                    annexes,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_CONTRACT_ANNEXES_FAILED",
                    "Không thể lấy danh sách phụ lục",
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("/{contractId}/annexes/{annexId}")
    @Operation(summary = "Chi tiết phụ lục", description = "Xem thông tin chi tiết một phụ lục gia hạn")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trả về thông tin phụ lục gia hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phụ lục theo mã yêu cầu"),
            @ApiResponse(responseCode = "400", description = "Không thể đọc dữ liệu phụ lục")
    })
    public ResponseEntity<?> getContractAnnexDetail(@PathVariable Long contractId,
                                                    @PathVariable Long annexId) {
        try {
            ContractExtensionAnnexResponseDto annex = contractExtensionAnnexService.getAnnexDetail(contractId, annexId);
            return ResponseUtil.createSuccessResponse(
                    "Lấy phụ lục gia hạn thành công",
                    "Thông tin chi tiết phụ lục",
                    annex,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_CONTRACT_ANNEX_FAILED",
                    "Không thể lấy thông tin phụ lục",
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/{contractId}/annexes/{annexId}/sign/admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Quản trị viên ký phụ lục", description = "Quản trị viên/Điều phối viên ký phụ lục gia hạn trước khi gửi khách hàng")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quản trị viên đã ký phụ lục thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu chữ ký phụ lục không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phụ lục cần ký")
    })
    public ResponseEntity<?> signAnnexAsAdmin(@PathVariable Long contractId,
                                              @PathVariable Long annexId,
                                              @Valid @RequestBody ContractExtensionAnnexSignRequestDto request,
                                              @AuthenticationPrincipal UserDetails principal) {
        try {
            Long adminId = getAccountIdFromPrincipal(principal);
            ContractExtensionAnnexResponseDto signedAnnex = contractExtensionAnnexService.signAsAdmin(contractId, annexId, adminId, request);
            return ResponseUtil.createSuccessResponse(
                    "Quản trị viên đã ký phụ lục thành công",
                    "Phụ lục đang chờ khách hàng ký",
                    signedAnnex,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "ADMIN_SIGN_ANNEX_FAILED",
                    "Ký phụ lục thất bại",
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/{contractId}/annexes/{annexId}/sign/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Khách hàng ký phụ lục", description = "Khách hàng ký phụ lục gia hạn sau khi nhận mã PIN và chữ ký của quản trị viên")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Khách hàng ký phụ lục thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu ký phụ lục không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phụ lục để ký")
    })
    public ResponseEntity<?> signAnnexAsCustomer(@PathVariable Long contractId,
                                                 @PathVariable Long annexId,
                                                 @Valid @RequestBody ContractExtensionAnnexSignRequestDto request,
                                                 @AuthenticationPrincipal UserDetails principal) {
        try {
            Long customerAccountId = getAccountIdFromPrincipal(principal);
            ContractExtensionAnnexResponseDto signedAnnex = contractExtensionAnnexService.signAsCustomer(contractId, annexId, customerAccountId, request);
            return ResponseUtil.createSuccessResponse(
                    "Khách hàng đã ký phụ lục thành công",
                    "Đã tạo hóa đơn thanh toán cho phụ lục",
                    signedAnnex,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "CUSTOMER_SIGN_ANNEX_FAILED",
                    "Ký phụ lục thất bại",
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/{contractId}/annexes/{annexId}/send-pin/email")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    @Operation(summary = "Gửi PIN ký phụ lục", description = "Gửi mã PIN qua email để khách hàng ký phụ lục gia hạn")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đã gửi mã PIN ký phụ lục"),
            @ApiResponse(responseCode = "400", description = "Email hoặc thông tin phụ lục không hợp lệ")
    })
    public ResponseEntity<?> sendAnnexPinByEmail(@PathVariable Long contractId,
                                                 @PathVariable Long annexId,
                                                 @RequestBody @Valid EmailPinRequestDto request) {
        try {
            contractExtensionAnnexService.sendSignaturePinByEmail(contractId, annexId, request.getEmail());
            return ResponseUtil.createSuccessResponse(
                    "Đã gửi mã PIN phụ lục",
                    "Mã PIN ký phụ lục đã được gửi tới email người nhận",
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "SEND_ANNEX_PIN_FAILED",
                    "Không thể gửi mã PIN cho phụ lục",
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/{contractId}/verify")
    @Operation(summary = "Xác thực chữ ký", description = "Kiểm tra chữ ký điện tử của hợp đồng có hợp lệ hay không")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chữ ký hợp đồng hợp lệ"),
            @ApiResponse(responseCode = "400", description = "Chữ ký không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Không thể xác thực chữ ký")
    })
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
    @Operation(summary = "Hủy hợp đồng", description = "Quản trị viên/Điều phối viên hủy hợp đồng kèm lý do")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hủy hợp đồng thành công"),
            @ApiResponse(responseCode = "400", description = "Không thể hủy hợp đồng do trạng thái không phù hợp"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy hợp đồng cần hủy")
    })
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
                    mapToResponseDto(contract),
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
    @Operation(summary = "Hợp đồng theo khách hàng", description = "Lấy toàn bộ hợp đồng của một khách hàng cụ thể (dành cho nhân viên)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trả về danh sách hợp đồng của khách hàng"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khách hàng hoặc hợp đồng tương ứng"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn danh sách hợp đồng")
    })
    public ResponseEntity<?> getCustomerContracts(@PathVariable Long customerId) {
        try {
            List<Contract> contracts = contractService.getContractsByCustomerId(customerId);
            List<ContractResponseDto> dtoList = contracts.stream()
                    .map(this::mapToResponseDto)
                    .toList();
            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách hợp đồng khách hàng thành công",
                    "Danh sách hợp đồng của khách hàng",
                    dtoList,
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
    @Operation(summary = "Danh sách hợp đồng của tôi", description = "Khách hàng xem tất cả hợp đồng thuộc tài khoản của mình")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trả về danh sách hợp đồng của khách hàng đang đăng nhập"),
            @ApiResponse(responseCode = "500", description = "Không thể lấy danh sách hợp đồng cá nhân")
    })
    public ResponseEntity<?> getMyContracts(@AuthenticationPrincipal UserDetails principal) {
        try {
            // Lấy customerId từ authenticated user
            Customer customer = getCustomerFromPrincipal(principal);
            List<Contract> contracts = contractService.getContractsByCustomerId(customer.getCustomerId());
            List<ContractResponseDto> dtoList = contracts.stream()
                    .map(this::mapToResponseDto)
                    .toList();
            
            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách hợp đồng thành công",
                    "Danh sách hợp đồng của bạn",
                    dtoList,
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
    @Operation(summary = "Hợp đồng đang chờ ký", description = "Khách hàng xem danh sách hợp đồng đang chờ mình ký")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trả về danh sách hợp đồng chờ ký"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn danh sách hợp đồng chờ ký")
    })
    public ResponseEntity<?> getPendingSignatureContracts(@AuthenticationPrincipal UserDetails principal) {
        try {
            Customer customer = getCustomerFromPrincipal(principal);
            List<Contract> contracts = contractService.getContractsByCustomerIdAndStatus(
                    customer.getCustomerId(), 
                    ContractStatus.PENDING_SIGNATURE
            );
            List<ContractResponseDto> dtoList = contracts.stream()
                    .map(this::mapToResponseDto)
                    .toList();
            
            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách hợp đồng chờ ký thành công",
                    "Các hợp đồng chờ bạn ký",
                    dtoList,
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

    private ContractResponseDto mapToResponseDto(Contract contract) {
        if (contract == null) {
            return null;
        }
        List<Device> allocatedDevices = deviceAllocationQueryService.getAllocatedDevicesForOrder(contract.getOrderId());
        return ContractResponseDto.from(contract, allocatedDevices);
    }

    /**
     * Helper method: Lấy Customer từ authentication
     */
    private Customer getCustomerFromPrincipal(UserDetails principal) {
        try {
            String username = principal.getUsername();
            return customerService.getCustomerByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng cho tên đăng nhập: " + username));
        } catch (Exception e) {
            throw new RuntimeException("Không thể lấy thông tin khách hàng: " + e.getMessage());
        }
    }
}
