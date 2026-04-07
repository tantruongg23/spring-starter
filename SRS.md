# Software Requirements Specification (SRS): E-Commerce Order Management System

## 1. Introduction
### 1.1 Purpose
This document provides a detailed software requirements specification for the **E-Commerce Order Management System (eShop)**. The purpose of this project is to serve as a comprehensive practical learning exercise for mastering **Spring Boot**, covering everything from basic REST APIs to complex relational mappings, security, and transaction management.

### 1.2 Domain Choice: E-Commerce
**Why E-Commerce?** 
E-Commerce is the perfect domain to learn Spring because it challenges you with:
- **Relational Databases (JPA/Hibernate):** Handling 1-to-N (User to Orders), M-to-N (Products to Categories), and 1-to-1 relationships.
- **Transactions (`@Transactional`):** Ensuring that when an order is placed, inventory is deducted, and the order is saved successfully, or everything rolls back.
- **Security (Spring Security):** Restricting actions based on user roles (Admin vs. Customer).
- **Data Validation & DTOs:** Validating incoming payloads and mapping them using the `controller -> converter -> dto` pattern.
- **Pagination & Sorting:** Displaying product catalogs efficiently.

---

## 2. Overall Description
### 2.1 User Roles
1. **Customer:** Can browse products, view product details, manage their shopping cart, and place orders. They can only view their own orders.
2. **Admin:** Can manage (CRUD) the product catalog, categories, and view all system orders.

### 2.2 System Modules
1. **User Management Module:** Registration, authentication, and profile management.
2. **Catalog Module:** Product and category management.
3. **Order Management Module:** Cart processing, order creation, order tracking, and stock deduction.

---

## 3. Specific Requirements
### 3.1 Functional Requirements
#### 3.1.1 Catalog Management
- **FR1:** The system shall allow Admins to create, read, update, and delete Products and Categories.
- **FR2:** The system shall allow Customers to retrieve all Products, with pagination and search filters (by name, category).

#### 3.1.2 Order Management
- **FR3:** The system shall allow Customers to create a new Order by selecting Products and quantities.
- **FR4:** When an Order is placed, the system MUST deduct the purchased quantity from the Product's stock. If stock is insufficient, the order must be aborted (Transaction Management).
- **FR5:** The system shall allow Customers to view their past orders and order statuses (`PENDING`, `SHIPPED`, `DELIVERED`).
- **FR6:** The system shall allow Admins to update order statuses.

#### 3.1.3 User Management
- **FR7:** The system shall require users to authenticate using JWT tokens to access private endpoints.
- **FR8:** The system shall securely hash passwords before storing them in the database.

---

## 4. Technical Architecture
Based on the packages you have perfectly created, the project will follow a strict Layered Architecture:
- **`controller`:** Exposes RESTful endpoints (`@RestController`). No business logic here.
- **`service`:** Contains core business logic (`@Service`) and transaction boundaries.
- **`repository`:** Interfaces extending `JpaRepository` for database access.
- **`domain.entity`:** JPA Entities mapped to database tables.
- **`domain.dto`:** Data Transfer Objects to avoid exposing internal Entities directly to the client.
- **`converter`:** Classes/Mappers (like MapStruct or manual mappers) to convert between Entity and DTO.
- **`domain.enumerate`:** Enums for static values like OrderStatus or Role.
- **`util`:** Helper classes (e.g., SecurityUtils, Date formatting).

---

## 5. Development Milestones (Learning Path)
- **Phase 1: Domain & Repositories:** Create JPA entities, repositories, and test DB mappings.
- **Phase 2: Core Services:** Implement business logic and `@Transactional` methods (Product and Order services).
- **Phase 3: Web Layer:** Create REST controllers, DTOs, and global exception handlers (`@ControllerAdvice`).
- **Phase 4: Security:** Integrate Spring Security and JWT.
- **Phase 5: Testing:** Write JUnit tests and Spring Boot Integration Tests.
