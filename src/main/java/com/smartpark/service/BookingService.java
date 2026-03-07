package com.smartpark.service;

import com.smartpark.model.Booking;
import com.smartpark.repository.BookingRepository;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class BookingService {

    private static final long RATE_XE_MAY = 5_000L;
    private static final long RATE_O_TO   = 20_000L;

    private final BookingRepository repo;

    public BookingService(BookingRepository repo) {
        this.repo = repo;
    }

    // Khách đặt vé → tự động CONFIRMED + sinh mã thanh toán luôn
    public Booking createBooking(String customerName, String plate,
                                 String vehicleType,
                                 LocalDateTime checkIn, LocalDateTime checkOut) {
        Booking b = new Booking();
        b.setCustomerName(customerName);
        b.setLicensePlate(plate.toUpperCase().trim());
        b.setVehicleType(vehicleType);
        b.setCheckIn(checkIn);
        b.setCheckOut(checkOut);
        b.setAmountDue(calcAmount(vehicleType, checkIn, checkOut));
        b.setPaymentCode(genCode());
        b.setStatus("CONFIRMED");
        return repo.save(b);
    }

    // SePay webhook gọi về
    public boolean processPayment(String content, long amount, String bankRef) {
        if (content == null) return false;
        String upper = content.toUpperCase();

        Optional<Booking> found = repo.findAll().stream()
                .filter(b -> b.getPaymentCode() != null
                          && upper.contains(b.getPaymentCode())
                          && !"PAID".equals(b.getStatus()))
                .findFirst();

        if (found.isEmpty()) return false;

        Booking b = found.get();
        if (amount < b.getAmountDue()) return false;

        b.setStatus("PAID");
        b.setPaidAt(LocalDateTime.now());
        b.setBankRef(bankRef);
        repo.save(b);

        System.out.printf("[PAID] ✅ %s | %s | %,d đ%n",
                b.getPaymentCode(), b.getLicensePlate(), amount);
        return true;
    }

    public List<Booking> getAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Booking> findById(Long id) {
        return repo.findById(id);
    }

    // ── Helpers ──────────────────────────────────────
    private long calcAmount(String type, LocalDateTime in, LocalDateTime out) {
        double hours = Math.max(Duration.between(in, out).toMinutes() / 60.0, 1.0);
        long rate = "o_to".equalsIgnoreCase(type) ? RATE_O_TO : RATE_XE_MAY;
        return Math.round(hours * rate);
    }

    private String genCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("SP");
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }
}
