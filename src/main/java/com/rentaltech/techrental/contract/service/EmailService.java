package com.rentaltech.techrental.contract.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Gửi Email OTP ký hợp đồng (HTML)
     */
    public boolean sendOTP(String email, String otp) {
        return sendHtmlEmail(
                email,
                "TechRental - Mã PIN ký hợp đồng",
                buildOTPEmailContent(otp)
        );
    }

    /**
     * Gửi Email OTP ký biên bản bàn giao nhận thiết bị (checkout)
     */
    public boolean sendHandoverCheckoutOTP(String email, String otp) {
        return sendHtmlEmail(
                email,
                "TechRental - Mã PIN ký biên bản bàn giao nhận thiết bị",
                buildGenericOtpContent(
                        "Bạn đang ký biên bản bàn giao NHẬN thiết bị trên hệ thống TechRental.",
                        otp)
        );
    }

    /**
     * Gửi Email OTP ký biên bản bàn giao trả thiết bị (checkin)
     */
    public boolean sendHandoverCheckinOTP(String email, String otp) {
        return sendHtmlEmail(
                email,
                "TechRental - Mã PIN ký biên bản bàn giao TRẢ thiết bị",
                buildGenericOtpContent(
                        "Bạn đang ký biên bản bàn giao TRẢ thiết bị trên hệ thống TechRental.",
                        otp)
        );
    }

    /**
     * Gửi Email OTP ký biên bản ĐỔI thiết bị (device replacement)
     */
    public boolean sendDeviceReplacementOTP(String email, String otp) {
        return sendHtmlEmail(
                email,
                "TechRental - Mã PIN ký biên bản ĐỔI thiết bị",
                buildGenericOtpContent(
                        "Bạn đang ký biên bản ĐỔI thiết bị trên hệ thống TechRental.",
                        otp)
        );
    }

    /**
     * Tạo nội dung email OTP
     */
    private String buildOTPEmailContent(String otp) {
        return """
            <html>
              <body style="font-family: Arial, sans-serif; background:#f5f5f5; padding:16px;">
                <table align="center" width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;">
                  <tr>
                    <td style="background:#ffffff;border-radius:8px;padding:24px;">
                      <h2 style="color:#111827;margin-top:0;">TechRental</h2>
                      <p style="color:#374151;">Bạn đã yêu cầu ký <b>hợp đồng</b> trên hệ thống TechRental.</p>
                      <p style="color:#374151;">Mã PIN của bạn là:</p>
                      <div style="background:#111827;color:#ffffff;font-size:24px;font-weight:bold;
                                  letter-spacing:4px;text-align:center;padding:12px 16px;border-radius:6px;">
                        %s
                      </div>
                      <p style="color:#6b7280;font-size:13px;margin-top:16px;">
                        Mã PIN có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.
                      </p>
                      <p style="color:#9ca3af;font-size:12px;margin-top:24px;">
                        Nếu bạn không yêu cầu ký hợp đồng, vui lòng bỏ qua email này.
                      </p>
                      <p style="color:#374151;margin-top:16px;">Trân trọng,<br/>Đội ngũ TechRental</p>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
            """.formatted(otp);
    }

    /**
     * Tạo nội dung email OTP dùng chung cho các nghiệp vụ khác (bàn giao, đổi thiết bị, phụ lục...)
     */
    private String buildGenericOtpContent(String actionLine, String otp) {
        return """
            <html>
              <body style="font-family: Arial, sans-serif; background:#f5f5f5; padding:16px;">
                <table align="center" width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;">
                  <tr>
                    <td style="background:#ffffff;border-radius:8px;padding:24px;">
                      <h2 style="color:#111827;margin-top:0;">TechRental</h2>
                      <p style="color:#374151;">%s</p>
                      <p style="color:#374151;">Mã PIN của bạn là:</p>
                      <div style="background:#111827;color:#ffffff;font-size:24px;font-weight:bold;
                                  letter-spacing:4px;text-align:center;padding:12px 16px;border-radius:6px;">
                        %s
                      </div>
                      <p style="color:#6b7280;font-size:13px;margin-top:16px;">
                        Mã PIN có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.
                      </p>
                      <p style="color:#9ca3af;font-size:12px;margin-top:24px;">
                        Nếu bạn không thực hiện thao tác này, vui lòng bỏ qua email này.
                      </p>
                      <p style="color:#374151;margin-top:16px;">Trân trọng,<br/>Đội ngũ TechRental</p>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
            """.formatted(actionLine, otp);
    }

    /**
     * Helper gửi email HTML dùng MimeMessage
     */
    private boolean sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper =
                    new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML

            mailSender.send(mimeMessage);
            return true;
        } catch (Exception e) {
            System.err.println("Lỗi gửi email HTML: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gửi email thông báo hợp đồng đã ký
     */
    public boolean sendContractSignedNotification(String email, String contractNumber) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("TechRental - Hợp đồng đã được ký thành công");
            message.setText(buildContractSignedContent(contractNumber));
            
            mailSender.send(message);
            return true;
            
        } catch (Exception e) {
            System.err.println("Lỗi gửi email thông báo: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gửi email nội dung tự do (sử dụng cho các nhắc nhở/notification)
     */
    public boolean sendGeneric(String email, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            System.err.println("Lỗi gửi email: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tạo nội dung email thông báo hợp đồng đã ký
     */
    private String buildContractSignedContent(String contractNumber) {
        return String.format(
            "Xin chào,\n\n" +
            "Hợp đồng số %s đã được ký thành công.\n\n" +
            "Hợp đồng hiện đã có hiệu lực pháp lý.\n" +
            "Bạn có thể tải xuống bản sao hợp đồng từ hệ thống.\n\n" +
            "Cảm ơn bạn đã tin tưởng TechRental.\n\n" +
            "Trân trọng,\n" +
            "Đội ngũ TechRental",
            contractNumber
        );
    }
}

