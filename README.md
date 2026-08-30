# RIVAANE

A small hotel management system built with Java 17, Spring Boot, Maven, HTML5, CSS3, and Vanilla JavaScript.

## Run

```powershell
mvn spring-boot:run
```

Open `http://localhost:8080` for the guest site. The admin console is at `http://localhost:8080/login.html`.

Demo credentials:

- Email: `admin@rivaane.com`
- Password: `rivaane123`

The application intentionally uses Java collections instead of a database. Rooms, guests, reservations, and sessions reset when the process restarts.
