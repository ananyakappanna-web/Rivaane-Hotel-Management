package com.rivaane.hotel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@CrossOrigin
public class RivaaneApplication {

    private final List<Room> rooms = new ArrayList<>();
    private final List<Reservation> reservations = new ArrayList<>();
    private final Map<String, Guest> guests = new HashMap<>();
    private final Map<String, String> sessions = new HashMap<>();
    private final List<Staff> staff = new ArrayList<>();
    private final List<Invoice> invoices = new ArrayList<>();
    private int roomCounter = 1;
    private int staffCounter = 1;
    private int invoiceCounter = 1;

    public RivaaneApplication() {
        seedData();
    }

    private final Map<Integer, HousekeepingTask> housekeepingTasks = new HashMap<>();
    private final List<String> housekeepingNotifications = new ArrayList<>();

    public static void main(String[] args) {
        SpringApplication.run(RivaaneApplication.class, args);
    }

    private void seedData() {
        addSeed("Royal Heritage Suite", "Suite", 12000, 2, "A grand Indo-Western suite with palace-inspired interiors.", "royal");
        addSeed("Maharaja Deluxe", "Deluxe", 8500, 2, "Elegant deluxe room combining Indian craftsmanship with modern luxury.", "deluxe");
        addSeed("Regal Executive", "Executive", 6500, 2, "Sophisticated business stay with premium amenities.", "executive");
        addSeed("Ivory Garden Room", "Standard", 4500, 2, "Peaceful ivory-toned room overlooking the hotel gardens.", "garden");
        addSeed("Riwaayat Presidential Suite", "Presidential", 18000, 4, "The signature RIVAANE experience with royal palace aesthetics.", "presidential");
        addSeed("Champagne Family Suite", "Family", 10500, 4, "Spacious family suite designed for elegant group stays.", "family");
        addSeed("Rajputana Courtyard Villa", "Villa", 15000, 4, "A private courtyard villa with handcrafted details and a quiet royal atmosphere.", "royal");

        // Seed staff data
        staff.add(new Staff(staffCounter++, "Rajesh Kumar", "rajesh@rivaane.com", "+91-9876543210", "Front Desk Manager", "Front Office", 45000, "ACTIVE"));
        staff.add(new Staff(staffCounter++, "Priya Sharma", "priya@rivaane.com", "+91-9876543211", "Housekeeping Supervisor", "Housekeeping", 38000, "ACTIVE"));
        staff.add(new Staff(staffCounter++, "Amit Patel", "amit@rivaane.com", "+91-9876543212", "Executive Chef", "Food & Beverage", 55000, "ACTIVE"));
        staff.add(new Staff(staffCounter++, "Sneha Reddy", "sneha@rivaane.com", "+91-9876543213", "Spa Manager", "Wellness", 42000, "ACTIVE"));
        staff.add(new Staff(staffCounter++, "Vikram Singh", "vikram@rivaane.com", "+91-9876543214", "Concierge", "Front Office", 35000, "ACTIVE"));

        seedDemoOperations();
    }

    private void seedDemoOperations() {
        Room demoRoom = rooms.get(1);
        Guest demoGuest = new Guest("Ananya Mehta", "ananya.mehta@example.com", "+91-98765-10001");
        guests.put(demoGuest.email, demoGuest);

        Reservation demoReservation = new Reservation("RIV-DEMO2026", demoRoom.id, demoGuest.name, demoGuest.email, demoGuest.phone, LocalDate.now().minusDays(5), LocalDate.now().minusDays(2), demoRoom.price * 3, "CARD", "CHECKED_OUT");
        reservations.add(demoReservation);
        demoRoom.status = "CLEANING";

        HousekeepingTask task = housekeepingTasks.get(demoRoom.id);
        task.status = "DIRTY";
        task.assignedStaff = "Priya Sharma";
        task.priority = "HIGH";
        task.scheduledDate = LocalDate.now().toString();
        task.notes = "Refresh linens and inspect minibar.";
        task.updatedAt = LocalDateTime.now();

        double tax = demoReservation.total * 0.18;
        invoices.add(new Invoice(invoiceCounter++, "INV-DEMO2026", demoReservation.reservationId, demoGuest.name, demoGuest.email, demoReservation.total, tax, 0, demoReservation.total + tax, "PENDING", LocalDate.now(), LocalDate.now().plusDays(7)));

        Room upcomingRoom = rooms.get(2);
        Guest upcomingGuest = new Guest("Arjun Kapoor", "arjun.kapoor@example.com", "+91-98765-10002");
        guests.put(upcomingGuest.email, upcomingGuest);
        Reservation upcomingReservation = new Reservation("RIV-DEMO2027", upcomingRoom.id, upcomingGuest.name, upcomingGuest.email, upcomingGuest.phone, LocalDate.now().plusDays(2), LocalDate.now().plusDays(5), upcomingRoom.price * 3, "UPI", "CONFIRMED");
        reservations.add(upcomingReservation);
        upcomingRoom.status = "RESERVED";
    }

    private void addSeed(String name, String type, double price, int capacity, String description, String image) {
        Room room = new Room(roomCounter++, name, type, price, capacity, description, image);
        rooms.add(room);
        housekeepingTasks.put(room.id, new HousekeepingTask(room.id));
    }

    @GetMapping("/api/rooms")
    public ResponseEntity<?> getRooms(@RequestParam(required = false) String search, @RequestParam(required = false) String type) {
        String query = search == null ? "" : search.toLowerCase();
        return ResponseEntity.ok(rooms.stream().filter(r -> query.isBlank() || r.name.toLowerCase().contains(query) || r.type.toLowerCase().contains(query))
                .filter(r -> type == null || type.isBlank() || type.equalsIgnoreCase("all") || r.type.equalsIgnoreCase(type)).collect(Collectors.toList()));
    }

    @GetMapping("/api/rooms/available")
    public ResponseEntity<?> availableRooms(@RequestParam String checkIn, @RequestParam String checkOut) {
        try {
            LocalDate start = LocalDate.parse(checkIn), end = LocalDate.parse(checkOut);
            if (!end.isAfter(start)) {
                return error(HttpStatus.BAD_REQUEST, "Check-out date must be after check-in date.");
            }
            return ResponseEntity.ok(rooms.stream().filter(r -> isRoomAvailable(r.id, start, end)).toList());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, "Invalid date format.");
        }
    }

    private boolean isRoomAvailable(int roomId, LocalDate start, LocalDate end) {
        return reservations.stream().filter(r -> r.roomId == roomId).filter(r -> !r.status.equals("CANCELLED"))
                .noneMatch(r -> start.isBefore(r.checkOut) && end.isAfter(r.checkIn));
    }

    @PostMapping("/api/bookings")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request) {
        try {
            if (request == null || request.name == null || request.name.isBlank()) {
                return error(HttpStatus.BAD_REQUEST, "Guest name is required.");
            }
            if (request.email == null || request.email.isBlank() || request.phone == null || request.phone.isBlank()) {
                return error(HttpStatus.BAD_REQUEST, "Guest email and phone are required.");
            }
            LocalDate checkIn = LocalDate.parse(request.checkIn), checkOut = LocalDate.parse(request.checkOut);
            if (!checkOut.isAfter(checkIn)) {
                return error(HttpStatus.BAD_REQUEST, "Check-out must be after check-in.");
            }
            Room room = findRoom(request.roomId);
            if (room == null) {
                return error(HttpStatus.NOT_FOUND, "Room not found.");
            }
            if (!isRoomAvailable(room.id, checkIn, checkOut)) {
                return error(HttpStatus.CONFLICT, "This room is not available for the selected dates.");
            }
            Guest guest = new Guest(request.name, request.email, request.phone);
            guests.put(guest.email, guest);
            Payment payment = "UPI".equalsIgnoreCase(request.paymentMethod) ? new UpiPayment() : new CardPayment();
            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
            double total = nights * room.price;
            payment.process(total);
            Reservation reservation = new Reservation("RIV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), room.id, guest.name, guest.email, guest.phone, checkIn, checkOut, total, payment.getMethod(), "CONFIRMED");
            reservations.add(reservation);
            room.status = "RESERVED";
            return ResponseEntity.ok(Map.of("message", "Booking confirmed successfully.", "reservationId", reservation.reservationId, "reservation", reservation, "room", room, "nights", nights, "total", total));
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, "Unable to create booking.");
        }
    }

    @PostMapping("/api/bookings/{id}/cancel")
    public ResponseEntity<?> cancelBooking(@RequestHeader(value = "X-Admin-Token", required = false) String token, @PathVariable String id) {
        if (!isAdmin(token)) {
            return unauthorized();
        }
        Reservation reservation = findReservation(id);
        if (reservation == null) {
            return error(HttpStatus.NOT_FOUND, "Reservation not found.");
        }
        if (reservation.status.equals("CANCELLED")) {
            return error(HttpStatus.BAD_REQUEST, "Reservation is already cancelled.");
        }
        reservation.status = "CANCELLED";
        Room room = findRoom(reservation.roomId);
        if (room != null && reservations.stream().noneMatch(r -> r.roomId == room.id && !r.status.equals("CANCELLED"))) {
            room.status = "AVAILABLE";
        }
        return ResponseEntity.ok(Map.of("message", "Reservation cancelled successfully."));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if ("admin@rivaane.com".equalsIgnoreCase(request.email) && "rivaane123".equals(request.password)) {
            String token = UUID.randomUUID().toString();
            sessions.put(token, request.email);
            return ResponseEntity.ok(Map.of("success", true, "token", token, "user", "RIVAANE Administrator"));
        }
        return error(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (token != null) {
            sessions.remove(token);
        
        }return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    @GetMapping("/api/dashboard")
    public ResponseEntity<?> dashboard(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!isAdmin(token)) {
            return unauthorized();
        }
        return ResponseEntity.ok(Map.of("totalRooms", rooms.size(), "available", countStatus("AVAILABLE"), "reserved", countStatus("RESERVED"), "occupied", countStatus("OCCUPIED"), "cleaning", countStatus("CLEANING"), "totalGuests", guests.size(), "totalStaff", staff.size(), "activeBookings", reservations.stream().filter(r -> !r.status.equals("CANCELLED")).count(), "revenue", reservations.stream().filter(r -> !r.status.equals("CANCELLED")).mapToDouble(r -> r.total).sum()));
    }

    @GetMapping("/api/admin/rooms")
    public ResponseEntity<?> adminRooms(@RequestHeader(value = "X-Admin-Token", required = false) String token, @RequestParam(required = false) String search) {
        if (!isAdmin(token)) {
            return unauthorized();
        
        }String query = search == null ? "" : search.toLowerCase();
        return ResponseEntity.ok(rooms.stream().filter(r -> query.isBlank() || r.name.toLowerCase().contains(query) || r.type.toLowerCase().contains(query)).toList());
    }

    @PostMapping("/api/admin/rooms")
    public ResponseEntity<?> addRoom(@RequestHeader(value = "X-Admin-Token", required = false) String token, @RequestBody RoomRequest request) {
        if (!isAdmin(token)) {
            return unauthorized();
        }
        String validationMessage = validateRoom(request);
        if (validationMessage != null) {
            return error(HttpStatus.BAD_REQUEST, validationMessage);
        }
        Room room = new Room(roomCounter++, request.name.trim(), request.type.trim(), request.price, request.capacity, request.description, request.image);
        rooms.add(room);
        return ResponseEntity.ok(room);
    }

    @PutMapping("/api/admin/rooms/{id}")
    public ResponseEntity<?> updateRoom(@RequestHeader(value = "X-Admin-Token", required = false) String token, @PathVariable int id, @RequestBody RoomRequest request) {
        if (!isAdmin(token)) {
            return unauthorized();
        }
        String validationMessage = validateRoom(request);
        if (validationMessage != null) {
            return error(HttpStatus.BAD_REQUEST, validationMessage);
        }
        Room room = findRoom(id);
        if (room == null) {
            return error(HttpStatus.NOT_FOUND, "Room not found.");
        
        }room.name = request.name.trim();
        room.type = request.type.trim();
        room.price = request.price;
        room.capacity = request.capacity;
        room.description = request.description;
        room.image = request.image;
        return ResponseEntity.ok(room);
    }

    @DeleteMapping("/api/admin/rooms/{id}")
    public ResponseEntity<?> deleteRoom(@RequestHeader(value = "X-Admin-Token", required = false) String token, @PathVariable int id) {
        if (!isAdmin(token)) {
            return unauthorized();
        
        }Room room = findRoom(id);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        if (reservations.stream().anyMatch(r -> r.roomId == id && !r.status.equals("CANCELLED"))) {
            return error(HttpStatus.CONFLICT, "Cannot delete a room with an active reservation.");
        }
        rooms.remove(room);
        return ResponseEntity.ok(Map.of("message", "Room deleted successfully."));
    }

    @GetMapping("/api/admin/reservations")
    public ResponseEntity<?> getReservations(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        return isAdmin(token) ? ResponseEntity.ok(reservations) : unauthorized();
    }

    @GetMapping("/api/admin/guests")
    public ResponseEntity<?> getGuests(@RequestHeader(value = "X-Admin-Token", required = false) String token, @RequestParam(required = false) String search) {
        if (!isAdmin(token)) {
            return unauthorized();
        }
        String query = search == null ? "" : search.trim().toLowerCase();
        return ResponseEntity.ok(guests.values().stream().filter(g -> query.isBlank() || g.name.toLowerCase().contains(query) || g.email.toLowerCase().contains(query) || g.phone.toLowerCase().contains(query)).toList());
    }

    @GetMapping("/api/admin/staff")
    public ResponseEntity<?> getStaff(@RequestHeader(value = "X-Admin-Token", required = false) String token, @RequestParam(required = false) String search) {
        if (!isAdmin(token)) {
            return unauthorized();
        }
        String query = search == null ? "" : search.trim().toLowerCase();
        return ResponseEntity.ok(staff.stream().filter(s -> query.isBlank() || s.name.toLowerCase().contains(query) || s.email.toLowerCase().contains(query) || s.position.toLowerCase().contains(query) || s.department.toLowerCase().contains(query)).toList());
    }

    @PostMapping("/api/admin/staff")
    public ResponseEntity<?> addStaff(@RequestHeader(value = "X-Admin-Token", required = false) String token, @RequestBody StaffRequest request) {
        if (!isAdmin(token)) {
            return unauthorized();
        }
        String validationMessage = validateStaff(request);
        if (validationMessage != null) {
            return error(HttpStatus.BAD_REQUEST, validationMessage);
        }
        Staff newStaff = new Staff(staffCounter++, request.name.trim(), request.email.trim(), request.phone.trim(), request.position.trim(), request.department.trim(), request.salary, request.status);
        staff.add(newStaff);
        return ResponseEntity.ok(newStaff);
    }

    @PutMapping("/api/admin/staff/{id}")
    public ResponseEntity<?> updateStaff(@RequestHeader(value = "X-Admin-Token", required = false) String token, @PathVariable int id, @RequestBody StaffRequest request) {
        if (!isAdmin(token)) {
            return unauthorized();
        }
        String validationMessage = validateStaff(request);
        if (validationMessage != null) {
            return error(HttpStatus.BAD_REQUEST, validationMessage);
        }
        Staff staffMember = findStaff(id);
        if (staffMember == null) {
            return error(HttpStatus.NOT_FOUND, "Staff member not found.");
        }
        staffMember.name = request.name.trim();
        staffMember.email = request.email.trim();
        staffMember.phone = request.phone.trim();
        staffMember.position = request.position.trim();
        staffMember.department = request.department.trim();
        staffMember.salary = request.salary;
        staffMember.status = request.status.trim();
        return ResponseEntity.ok(staffMember);
    }

    @DeleteMapping("/api/admin/staff/{id}")
    public ResponseEntity<?> deleteStaff(@RequestHeader(value = "X-Admin-Token", required = false) String token, @PathVariable int id) {
        if (!isAdmin(token)) {
            return unauthorized();
        }
        Staff staffMember = findStaff(id);
        if (staffMember == null) {
            return ResponseEntity.notFound().build();
        }
        staff.remove(staffMember);
        return ResponseEntity.ok(Map.of("message", "Staff member deleted successfully."));
    }

    @GetMapping("/api/admin/invoices")
    public ResponseEntity<?> getInvoices(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!isAdmin(token)) {
            return unauthorized();
        }
        return ResponseEntity.ok(invoices);
    }

    @PostMapping("/api/admin/invoices")
    public ResponseEntity<?> createInvoice(@RequestHeader(value = "X-Admin-Token", required = false) String token, @RequestBody InvoiceRequest request) {
        if (!isAdmin(token)) {
            return unauthorized();
        }
        if (request.reservationId == null || request.reservationId.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "Reservation ID is required.");
        }

        Reservation reservation = findReservation(request.reservationId);
        if (reservation == null) {
            return error(HttpStatus.NOT_FOUND, "Reservation not found.");
        }
        if (!reservation.status.equals("CHECKED_OUT")) {
            return error(HttpStatus.BAD_REQUEST, "Invoice can only be generated for checked-out reservations.");
        }

        // Check if invoice already exists for this reservation
        if (invoices.stream().anyMatch(inv -> inv.reservationId.equals(request.reservationId))) {
            return error(HttpStatus.CONFLICT, "Invoice already exists for this reservation.");
        }

        double taxAmount = reservation.total * (request.tax / 100);
        double discountAmount = reservation.total * (request.discount / 100);
        double grandTotal = reservation.total + taxAmount - discountAmount;

        String invoiceId = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(7);

        Invoice invoice = new Invoice(invoiceCounter++, invoiceId, request.reservationId, reservation.guestName, reservation.guestEmail, reservation.total, taxAmount, discountAmount, grandTotal, "PENDING", issueDate, dueDate);
        invoices.add(invoice);

        return ResponseEntity.ok(invoice);
    }

    @PutMapping("/api/admin/invoices/{id}/status")
    public ResponseEntity<?> updateInvoiceStatus(@RequestHeader(value = "X-Admin-Token", required = false) String token, @PathVariable String id, @RequestBody Map<String, String> request) {
        if (!isAdmin(token)) {
            return unauthorized();
        }
        Invoice invoice = invoices.stream().filter(inv -> inv.invoiceId.equals(id)).findFirst().orElse(null);
        if (invoice == null) {
            return error(HttpStatus.NOT_FOUND, "Invoice not found.");
        }

        String newStatus = request.get("status");
        if (newStatus == null || (!Set.of("PENDING", "PAID", "OVERDUE", "CANCELLED").contains(newStatus))) {
            return error(HttpStatus.BAD_REQUEST, "Invalid status. Must be PENDING, PAID, OVERDUE, or CANCELLED.");
        }

        invoice.status = newStatus;
        return ResponseEntity.ok(Map.of("message", "Invoice status updated successfully.", "invoice", invoice));
    }

    @PostMapping("/api/admin/reservations/{id}/checkin")
    public ResponseEntity<?> checkIn(@RequestHeader(value = "X-Admin-Token", required = false) String token, @PathVariable String id) {
        if (!isAdmin(token)) {
            return unauthorized();
        
        }Reservation r = findReservation(id);
        if (r == null) {
            return error(HttpStatus.NOT_FOUND, "Reservation not found.");
        
        }if (!r.status.equals("CONFIRMED")) {
            return error(HttpStatus.BAD_REQUEST, "Only confirmed reservations can be checked in.");
        
        }r.status = "CHECKED_IN";
        Room room = findRoom(r.roomId);
        if (room != null) {
            room.status = "OCCUPIED";
        
        }return ResponseEntity.ok(Map.of("message", "Guest checked in successfully."));
    }

    @PostMapping("/api/admin/reservations/{id}/checkout")
    public ResponseEntity<?> checkOut(@RequestHeader(value = "X-Admin-Token", required = false) String token, @PathVariable String id) {
        if (!isAdmin(token)) {
            return unauthorized();
        
        }Reservation r = findReservation(id);
        if (r == null) {
            return error(HttpStatus.NOT_FOUND, "Reservation not found.");
        
        }if (!r.status.equals("CHECKED_IN")) {
            return error(HttpStatus.BAD_REQUEST, "Only checked-in reservations can be checked out.");
        
        }r.status = "CHECKED_OUT";
        Room room = findRoom(r.roomId);
        if (room != null) {
            room.status = "CLEANING";
            HousekeepingTask task = housekeepingTasks.computeIfAbsent(room.id, HousekeepingTask::new);
            task.status = "DIRTY";
            task.updatedAt = LocalDateTime.now();
        }
        return ResponseEntity.ok(Map.of("message", "Guest checked out. Room sent to cleaning."));
    }

    @PostMapping("/api/admin/rooms/{id}/clean")
    public ResponseEntity<?> cleanRoom(@RequestHeader(value = "X-Admin-Token", required = false) String token, @PathVariable int id) {
        if (!isAdmin(token)) {
            return unauthorized();
        
        }Room room = findRoom(id);
        if (room == null) {
            return error(HttpStatus.NOT_FOUND, "Room not found.");
        
        }room.status = "AVAILABLE";
        HousekeepingTask task = housekeepingTasks.computeIfAbsent(id, HousekeepingTask::new);
        task.status = "CLEAN";
        task.completedAt = LocalDateTime.now();
        task.updatedAt = LocalDateTime.now();
        housekeepingNotifications.add("Room " + id + " is clean and ready for reception.");
        return ResponseEntity.ok(Map.of("message", "Room cleaned and marked available.", "notification", "Reception notified."));
    }

    @GetMapping("/api/admin/housekeeping")
    public ResponseEntity<?> getHousekeeping(@RequestHeader(value = "X-Admin-Token", required = false) String token, @RequestParam(required = false) String status) {
        if (!isAdmin(token)) {
            return unauthorized();
        }
        String requestedStatus = status == null ? "" : status.trim().toUpperCase();
        return ResponseEntity.ok(rooms.stream().map(room -> housekeepingTasks.computeIfAbsent(room.id, HousekeepingTask::new)).filter(task -> requestedStatus.isBlank() || task.status.equals(requestedStatus)).toList());
    }

    @PutMapping("/api/admin/housekeeping/{roomId}")
    public ResponseEntity<?> updateHousekeeping(@RequestHeader(value = "X-Admin-Token", required = false) String token, @PathVariable int roomId, @RequestBody HousekeepingRequest request) {
        if (!isAdmin(token)) {
            return unauthorized();
        }
        Room room = findRoom(roomId);
        if (room == null) {
            return error(HttpStatus.NOT_FOUND, "Room not found.");
        }
        if (request == null || request.status == null || !Set.of("CLEAN", "DIRTY", "IN_PROGRESS", "OUT_OF_SERVICE").contains(request.status.toUpperCase())) {
            return error(HttpStatus.BAD_REQUEST, "Status must be CLEAN, DIRTY, IN_PROGRESS, or OUT_OF_SERVICE.");
        }
        HousekeepingTask task = housekeepingTasks.computeIfAbsent(roomId, HousekeepingTask::new);
        String nextStatus = request.status.toUpperCase();
        task.status = nextStatus;
        task.assignedStaff = request.assignedStaff == null ? "" : request.assignedStaff.trim();
        task.notes = request.notes == null ? "" : request.notes.trim();
        task.priority = request.priority == null || request.priority.isBlank() ? "NORMAL" : request.priority.trim().toUpperCase();
        task.scheduledDate = request.scheduledDate;
        task.updatedAt = LocalDateTime.now();
        if ("IN_PROGRESS".equals(nextStatus)) {
            task.startedAt = task.startedAt == null ? LocalDateTime.now() : task.startedAt;
        }
        if ("CLEAN".equals(nextStatus)) {
            task.completedAt = LocalDateTime.now();
            room.status = "AVAILABLE";
            housekeepingNotifications.add("Room " + roomId + " is clean and ready for reception.");
        }
        if ("DIRTY".equals(nextStatus) || "IN_PROGRESS".equals(nextStatus)) {
            room.status = "CLEANING";
        }
        return ResponseEntity.ok(task);
    }

    @GetMapping("/api/admin/housekeeping/analytics")
    public ResponseEntity<?> housekeepingAnalytics(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!isAdmin(token)) {
            return unauthorized();
        }
        long completed = housekeepingTasks.values().stream().filter(task -> "CLEAN".equals(task.status)).count();
        long inProgress = housekeepingTasks.values().stream().filter(task -> "IN_PROGRESS".equals(task.status)).count();
        long outstanding = housekeepingTasks.values().stream().filter(task -> Set.of("DIRTY", "IN_PROGRESS").contains(task.status)).count();
        double averageMinutes = housekeepingTasks.values().stream().filter(task -> task.startedAt != null && task.completedAt != null).mapToLong(task -> ChronoUnit.MINUTES.between(task.startedAt, task.completedAt)).average().orElse(0);
        return ResponseEntity.ok(Map.of("roomsCleaned", completed, "inProgress", inProgress, "outstanding", outstanding, "averageCleaningMinutes", averageMinutes, "notifications", new ArrayList<>(housekeepingNotifications)));
    }

    @GetMapping("/api/concepts")
    public ResponseEntity<?> concepts() {
        return ResponseEntity.ok(List.of(Map.of("name", "OOP", "description", "Hotel entities are represented using classes and objects."), Map.of("name", "Encapsulation", "description", "Private fields with accessors protect entity state."), Map.of("name", "Inheritance", "description", "Guest and AdminUser inherit common Person properties."), Map.of("name", "Polymorphism", "description", "CardPayment and UpiPayment implement Payment differently."), Map.of("name", "ArrayList", "description", "Rooms and reservations are stored in lists."), Map.of("name", "HashMap", "description", "Guests and admin sessions use maps."), Map.of("name", "Streams", "description", "Streams power filtering, statistics and revenue."), Map.of("name", "LocalDate", "description", "Stay dates use Java LocalDate."), Map.of("name", "REST API", "description", "Spring Boot endpoints are consumed with fetch().")));
    }

    private long countStatus(String status) {
        return rooms.stream().filter(r -> r.status.equals(status)).count();
    }

    private boolean isAdmin(String token) {
        return token != null && sessions.containsKey(token);
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return error(HttpStatus.UNAUTHORIZED, "Admin authentication required.");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    private Room findRoom(int id) {
        return rooms.stream().filter(r -> r.id == id).findFirst().orElse(null);
    }

    private Reservation findReservation(String id) {
        return reservations.stream().filter(r -> r.reservationId.equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    private Staff findStaff(int id) {
        return staff.stream().filter(s -> s.id == id).findFirst().orElse(null);
    }

    private String validateRoom(RoomRequest request) {
        if (request == null || request.name == null || request.name.isBlank()) {
            return "Room name is required.";
        }
        if (request.type == null || request.type.isBlank()) {
            return "Room type is required.";
        }
        if (request.price <= 0) {
            return "Price must be greater than zero.";
        }
        if (request.capacity <= 0) {
            return "Capacity must be greater than zero.";
        }
        return null;
    }

    private String validateStaff(StaffRequest request) {
        if (request == null || request.name == null || request.name.isBlank()) {
            return "Staff name is required.";
        }
        if (request.email == null || request.email.isBlank()) {
            return "Staff email is required.";
        }
        if (request.phone == null || request.phone.isBlank()) {
            return "Staff phone is required.";
        }
        if (request.position == null || request.position.isBlank()) {
            return "Position is required.";
        }
        if (request.department == null || request.department.isBlank()) {
            return "Department is required.";
        }
        if (request.salary <= 0) {
            return "Salary must be greater than zero.";
        }
        if (request.status == null || request.status.isBlank()) {
            return "Status is required.";
        }
        return null;
    }

    interface Payment {

        void process(double amount);

        String getMethod();
    }

    static class CardPayment implements Payment {

        @Override
        public void process(double amount) {
            System.out.println("Card payment processed: " + amount);
        }

        @Override
        public String getMethod() {
            return "CARD";
        }
    }

    static class UpiPayment implements Payment {

        @Override
        public void process(double amount) {
            System.out.println("UPI payment processed: " + amount);
        }

        @Override
        public String getMethod() {
            return "UPI";
        }
    }

    static class Person {

        public String name, email, phone;

        Person(String name, String email, String phone) {
            this.name = name;
            this.email = email;
            this.phone = phone;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getPhone() {
            return phone;
        }
    }

    static class Guest extends Person {

        Guest(String name, String email, String phone) {
            super(name, email, phone);
        }
    }

    static class AdminUser extends Person {

        AdminUser(String name, String email, String phone) {
            super(name, email, phone);
        }
    }

    static class Room {

        public int id, capacity;
        public String name, type, description, image, status = "AVAILABLE";
        public double price;

        Room(int id, String name, String type, double price, int capacity, String description, String image) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.price = price;
            this.capacity = capacity;
            this.description = description;
            this.image = image;
        }
    }

    static class Reservation {

        public String reservationId, guestName, guestEmail, guestPhone, paymentMethod, status;
        public int roomId;
        public LocalDate checkIn, checkOut;
        public double total;

        Reservation(String id, int roomId, String name, String email, String phone, LocalDate in, LocalDate out, double total, String payment, String status) {
            reservationId = id;
            this.roomId = roomId;
            guestName = name;
            guestEmail = email;
            guestPhone = phone;
            checkIn = in;
            checkOut = out;
            this.total = total;
            paymentMethod = payment;
            this.status = status;
        }
    }

    static class HousekeepingTask {

        public int roomId;
        public String status = "CLEAN", assignedStaff = "", notes = "", priority = "NORMAL", scheduledDate;
        public LocalDateTime startedAt, completedAt, updatedAt = LocalDateTime.now();

        HousekeepingTask(int roomId) {
            this.roomId = roomId;
        }
    }

    static class Staff {

        public int id;
        public String name, email, phone, position, department, status;
        public double salary;
        public LocalDateTime createdAt = LocalDateTime.now();

        Staff(int id, String name, String email, String phone, String position, String department, double salary, String status) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.position = position;
            this.department = department;
            this.salary = salary;
            this.status = status;
        }
    }

    static class Invoice {

        public int id;
        public String invoiceId, reservationId, guestName, guestEmail;
        public double total, tax, discount, grandTotal;
        public String status;
        public LocalDate issueDate, dueDate;
        public LocalDateTime createdAt = LocalDateTime.now();

        Invoice(int id, String invoiceId, String reservationId, String guestName, String guestEmail, double total, double tax, double discount, double grandTotal, String status, LocalDate issueDate, LocalDate dueDate) {
            this.id = id;
            this.invoiceId = invoiceId;
            this.reservationId = reservationId;
            this.guestName = guestName;
            this.guestEmail = guestEmail;
            this.total = total;
            this.tax = tax;
            this.discount = discount;
            this.grandTotal = grandTotal;
            this.status = status;
            this.issueDate = issueDate;
            this.dueDate = dueDate;
        }
    }

    public static class BookingRequest {

        public int roomId;
        public String name, email, phone, checkIn, checkOut, paymentMethod;
    }

    public static class LoginRequest {

        public String email, password;
    }

    public static class RoomRequest {

        public String name, type, description, image;
        public double price;
        public int capacity;
    }

    public static class HousekeepingRequest {

        public String status, assignedStaff, notes, priority, scheduledDate;
    }

    public static class StaffRequest {

        public String name, email, phone, position, department, status;
        public double salary;
    }

    public static class InvoiceRequest {

        public String reservationId;
        public double tax;
        public double discount;
    }
}
