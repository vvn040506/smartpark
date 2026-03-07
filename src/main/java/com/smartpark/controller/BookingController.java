package com.smartpark.controller;

import com.smartpark.model.Booking;
import com.smartpark.service.BookingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@Controller
public class BookingController {

    private final BookingService service;

    @Value("${app.bank.account}") private String bankAccount;
    @Value("${app.bank.owner}")   private String bankOwner;
    @Value("${app.bank.name}")    private String bankName;

    public BookingController(BookingService service) {
        this.service = service;
    }

    // Trang đặt vé
    @GetMapping("/")
    public String index() {
        return "index";
    }

    // Xử lý form đặt vé
    @PostMapping("/dat-ve")
    public String datVe(
            @RequestParam String customerName,
            @RequestParam String licensePlate,
            @RequestParam String vehicleType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkOut,
            Model model) {

        Booking b = service.createBooking(customerName, licensePlate, vehicleType, checkIn, checkOut);
        return "redirect:/ve/" + b.getId();
    }

    // Trang chi tiết vé + QR thanh toán
    @GetMapping("/ve/{id}")
    public String chiTiet(@PathVariable Long id, Model model) {
        Booking b = service.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé"));

        model.addAttribute("booking", b);
        model.addAttribute("bankAccount", bankAccount);
        model.addAttribute("bankOwner",   bankOwner);
        model.addAttribute("bankName",    bankName);

        // URL QR VietQR – quét bằng app ngân hàng bất kỳ
        String qrUrl = String.format(
            "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
            bankName, bankAccount,
            b.getAmountDue(),
            b.getPaymentCode(),
            bankOwner.replace(" ", "%20")
        );
        model.addAttribute("qrUrl", qrUrl);

        return "chi-tiet";
    }

    // Kiểm tra trạng thái (dùng cho auto-refresh JS)
    @GetMapping("/api/status/{id}")
    @ResponseBody
    public String checkStatus(@PathVariable Long id) {
        return service.findById(id)
                .map(Booking::getStatus)
                .orElse("NOT_FOUND");
    }
}
