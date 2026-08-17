# Event Ticketing System

**ELEC5619 — Object Oriented Application Frameworks**
**Project Group:** ELEC5619_Practical04_Group_1
**Repository:** Lab_Practical4_Group_1

---

## 1. Project Overview

Pop-up businesses often lack an efficient way to sell specific seating locations and instead rely on first-come, first-served arrangements. This limits their ability to price premium seats differently and manage seating layouts effectively.

Traditional ticketing platforms are typically designed for fixed venues and are poorly suited to businesses that frequently relocate or reconfigure their seating. Customising these systems can also be costly and impractical for temporary or mobile operations.

This project proposes a clean, intuitive drag-and-drop platform that allows businesses to create, modify, and rearrange seating layouts easily. Individual seats or sections can then be priced and sold independently, giving businesses greater control over seating configuration, pricing, and venue changes.

### 1.1 Key Requirements

Directly addressing the problems above, the platform must support:

- **Flexible layout creation:** organisers can build, edit, and rearrange a seating floorplan for any temporary or mobile venue through a drag-and-drop interface.
- **Independent seat/section pricing:** individual seats or sections can be priced separately (e.g. VIP vs. standard), rather than one flat ticket price for the whole event.
- **Real-time seat selection:** customers view live seat availability and choose a specific seat.
- **Reusable floorplans:** a layout built once for a venue can be saved and reused across multiple future events at that same pop-up location.
- **Secure payment processing:** ticket purchases are processed securely, without the platform storing raw card data.
- **Ticket delivery & check-in:** confirmed bookings generate a scannable e-ticket/printable ticket.

---

## 2. Used Libraries and Versions

### Backend (Java/Spring Boot)
- Spring Boot: 3.5.4
- Spring Boot Starter Web: (managed by Spring Boot)
- Spring Boot Starter Data JPA: (managed by Spring Boot)
- Spring Security Core: (managed by Spring Boot)
- H2 Database: (latest via Spring Boot dependency management)
- PostgreSQL Driver: (managed by Spring Boot) — for production/staging; H2 for local dev/testing


### Frontend (React)
- React: 19.2.8 *(latest stable)*
- React Router: 8.3.0 *(latest stable)*
- Axios: 1.19.0 *(latest stable)*

---

## 3. Functionalities

### 3.1 User Management

**User Registration (FR-1):**
- Users can create an account using their personal and login information.
- Users can register as customers or event organisers.

**User Authentication and Login (FR-2):**
- Registered users can securely log in and log out.
- Access to functionality is controlled according to the user's role.

### 3.2 Event and Seating Management

**Event and Floorplan Creation (FR-3):**
- Organisers can create and manage events.
- Organisers can create, edit and rearrange seating layouts using an interactive interface.
- Floorplans can be saved and reused for future events at the same venue.

**Seat and Section Pricing (FR-4):**
- Organisers can assign prices to individual seats or seating sections.
- Different categories such as VIP, Premium and Standard seating can have different prices.

### 3.3 Booking and Transactions

**Real-Time Seat Selection and Booking (FR-5):**
- Customers can view seat availability and select a specific available seat.
- Selecting a seat places a temporary hold on it (short timeout) so two customers cannot book the same seat simultaneously.
- Seats that have already been booked cannot be purchased by another customer.

**Payment, Booking Confirmation and E-Ticket (FR-6):**
- Customers can complete payment for selected seats.
- Successful payments generate a confirmed booking and release any expired holds.
- The system generates a digital ticket (QR-code based) containing event and seating information.

**Ticket Validation / Check-In (FR-7):**
- Organisers can scan or look up a ticket at the event to validate entry.
- Validated tickets are flagged as used to prevent duplicate entry.

---

## 4. Team Roles & Responsibilities

| SID (Name) | Role | Responsibilities |
|---|---|---|
| **520442472 (Dmitry Khachumov)** | Backend Developer — Events, Data & Payments | Database entities and relationships, event and venue management APIs, seat/section management, pricing logic, JPA repositories, and payment gateway integration |
| **540938186 (Abdul Samad)** | Full-Stack Developer — Floorplans | Organiser floorplan creation and editing, reusable floorplan functionality, frontend-backend integration for floorplans |
| **550264462 (Maria Hamlili)** | Frontend Developer — Booking | Event browsing and details pages, interactive customer seat-selection interface, booking summary and confirmation pages, responsive frontend development |
| **550358783 (Jose Maria Mosciaro Herrera)** | Backend Developer — Users, Bookings & Concurrency | User registration and authentication, role-based access, seat reservation/holding logic (concurrency handling), booking APIs |
| **520494057 (Thomas Huynh)** | Full-Stack Developer — Tickets, Check-In & QA | Customer booking/ticket interfaces, e-ticket and QR-code functionality, organiser check-in flow, frontend-backend integration for tickets, test infrastructure |

---

## 5. Repository Structure *(to be finalised)*

/frontend      # React application
/backend       # Spring Boot application
/docs          # Design docs, ER diagrams, API specs
/screenshots   # Sprint 0 evidence (GitHub org, sprints board)
