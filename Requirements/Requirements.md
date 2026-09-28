# BookWorm E-Store — Backend Requirements

> **Source material:** Architecture diagram (`Architecture.png`) + UI wireframes: E-store Home, Catalogue, Shopping Cart, Payment & Purchase (×3).
> **Application name:** Book Worm
> **Currency context:** Indian Rupee (₹) — pricing, tax, and delivery charges observed in wireframes.
> **Architecture intent:** Multi-tenant — multiple independent stores (tenants) are a first-class design concern.

---

## Table of Contents

1. [Functional Requirements](#1-functional-requirements)
2. [Non-Functional Requirements](#2-non-functional-requirements)
3. [User Roles](#3-user-roles)
4. [User Stories](#4-user-stories)
5. [Business Capabilities](#5-business-capabilities)
6. [Domain Boundaries](#6-domain-boundaries)
7. [Backend Modules](#7-backend-modules)
8. [API Domains](#8-api-domains)
9. [Data Ownership Matrix](#9-data-ownership-matrix)
10. [Assumptions](#10-assumptions)
11. [Out of Scope Items](#11-out-of-scope-items)

---

## 1. Functional Requirements

### 1.1 Identity & Access Management

| ID | Requirement |
|----|-------------|
| FR-IAM-01 | The system shall support **Registered User** and **Guest User** access modes. |
| FR-IAM-02 | Registered users shall authenticate using a **phone number or e-mail address** combined with a password. |
| FR-IAM-03 | The system shall provide a **Forgot Password** recovery flow. |
| FR-IAM-04 | Unauthenticated visitors shall be able to **Continue as Guest** without creating an account. |
| FR-IAM-05 | New users shall be able to **Sign Up** from the login modal. |
| FR-IAM-06 | The system shall enforce **role-based entitlement** — what a user can see and buy depends on their assigned role. |
| FR-IAM-07 | The system shall support **logout** from any authenticated session. |

### 1.2 Store Management & Multi-Tenancy

| ID | Requirement |
|----|-------------|
| FR-STR-01 | The platform shall support **multiple independent tenants (stores)**, each fully isolated in terms of catalogue, pricing, policies, users, and order data. |
| FR-STR-02 | A **Platform Administrator** shall be able to provision a new tenant store, including assigning a unique store identifier, subdomain/slug, and initial configuration. |
| FR-STR-03 | Each tenant shall have its own **Catalogues** that are not visible to or shared with other tenants unless explicitly configured. |
| FR-STR-04 | Each tenant shall be able to define its own **Store Policies** (return window, tax rules, shipping rules, delivery terms). |
| FR-STR-05 | A store shall be associatable with one or more catalogues, and each catalogue shall belong to exactly one tenant. |
| FR-STR-06 | Users (registered and guest) shall be scoped to a **specific tenant context** — authentication, order history, wishlist, and cart are tenant-scoped. |
| FR-STR-07 | Tenant administrators shall be able to **configure branding and store metadata** (name, logo, description) independently. |
| FR-STR-08 | The platform shall enforce **complete data isolation** between tenants — no tenant shall be able to read or affect another tenant's data. |

### 1.3 Catalogue & Product Browsing

| ID | Requirement |
|----|-------------|
| FR-CAT-01 | The system shall present products organised by **genre/category** (Romance, Mystery, Science Fiction, Fantasy, Historical, Biography, Self-help, Memoir, Travel, Cooking, Children's, Young Adult, Comics & Graphic Novels, Poetry, Drama, Science, Philosophy, Religion, Language Learning). |
| FR-CAT-02 | Users shall be able to **filter** the catalogue by Language, Format (Paperback, eBook, etc.), and Price Range. |
| FR-CAT-03 | Users shall be able to **sort** results by Relevance and other criteria. |
| FR-CAT-04 | The catalogue shall surface **Recommended for You** products based on the user's order history. |
| FR-CAT-05 | The catalogue shall feature **Bestsellers this Month** and **New Launches** product sections. |
| FR-CAT-06 | The system shall display **upsell and cross-sell** products at relevant touchpoints. |
| FR-CAT-07 | Catalogue browsing shall respect the user's **entitlement** — not all products are visible to all roles. |
| FR-CAT-08 | Each product listing shall display: title, author, genre tags, format, price, and estimated delivery date. |
| FR-CAT-09 | A product detail page shall show: cover image, description, publisher, author bio, language, star rating, sales count, format, price, estimated delivery, and user reviews. |
| FR-CAT-10 | The product detail page shall show **Related Reads** (cross-sell recommendations). |
| FR-CAT-11 | Users shall be able to submit a **star rating and text review** for a product. |
| FR-CAT-12 | Users shall be able to **Add to Wishlist** from the product detail page. |
| FR-CAT-13 | Users shall be able to navigate the catalogue via a **breadcrumb trail** (e.g., Home / Non-Fiction / Self Help). |
| FR-CAT-14 | The system shall support full-text **search** across the catalogue. |

### 1.4 Shopping Cart & Order Creation

| ID | Requirement |
|----|-------------|
| FR-CART-01 | Authenticated and guest users shall be able to **Add to Cart** from product listings or detail pages. |
| FR-CART-02 | The cart shall allow users to **adjust item quantity** (increment / decrement) before checkout. |
| FR-CART-03 | The cart shall display a **Grand Total summary**: item subtotal, tax, delivery charges, coupon discount, and final amount payable. |
| FR-CART-04 | Users shall be able to **apply a coupon code** and receive an immediate discount calculation. |
| FR-CART-05 | The cart shall display **estimated delivery dates** per item. |
| FR-CART-06 | The cart shall present a **Related Reads** sidebar to encourage additional purchases. |

### 1.5 Checkout & Address Management

| ID | Requirement |
|----|-------------|
| FR-CHK-01 | Users shall be able to enter a **delivery address** manually (First Name, Last Name, Address Line, City, PIN/Postcode, State, Country, e-mail, phone number). |
| FR-CHK-02 | Registered users shall be able to **Use Saved Address** to pre-fill the delivery form. |
| FR-CHK-03 | The checkout page shall present the **order summary** alongside the address form. |
| FR-CHK-04 | Users shall be able to select from multiple **payment methods**: Credit Card, Debit Card, UPI, Wallet. |
| FR-CHK-05 | For card payments the system shall accept: card number, name on card, CVV, and expiry date. |
| FR-CHK-06 | Users shall be able to **redeem gift points or apply coupons** at checkout. |
| FR-CHK-07 | On successful payment the system shall display a **purchase confirmation** screen listing all purchased items with delivery details. |

### 1.6 Order Management

| ID | Requirement |
|----|-------------|
| FR-ORD-01 | The system shall **create an order** upon successful payment. |
| FR-ORD-02 | Users shall be able to **view their order history** (My Orders). |
| FR-ORD-03 | Users shall be able to **cancel an order** within a defined cancellation window. |
| FR-ORD-04 | Users shall be able to **return an order** and initiate a return shipment. |
| FR-ORD-05 | The system shall support **order confirmation** notifications. |
| FR-ORD-06 | The system shall support a **Buy Again** feature that replicates a past order into the cart. |

### 1.7 Payment Processing

| ID | Requirement |
|----|-------------|
| FR-PAY-01 | The system shall process payments through a **payment gateway** (credit card, debit card, UPI, wallet). |
| FR-PAY-02 | The system shall handle **refund processing** for cancelled or returned orders. |
| FR-PAY-03 | The system shall send **payment confirmation** to the user after a successful transaction. |
| FR-PAY-04 | The system shall support a **Gift & Wallet** balance mechanism that users can apply at checkout. |
| FR-PAY-05 | Tax calculation shall be performed server-side and reflected in the order total before payment is initiated. |

### 1.8 Shipping & Delivery

| ID | Requirement |
|----|-------------|
| FR-SHP-01 | The system shall calculate **shipping rates** based on destination and order characteristics. |
| FR-SHP-02 | The system shall estimate and display **approximate delivery dates** at product listing and checkout. |
| FR-SHP-03 | The system shall support **return shipment** initiation when a user raises a return request. |
| FR-SHP-04 | Free delivery shall be available under configurable conditions (as seen in the checkout wireframe). |

### 1.9 Wishlist & Writer Following

| ID | Requirement |
|----|-------------|
| FR-WL-01 | Registered users shall be able to **add products to a Wishlist**. |
| FR-WL-02 | The navigation bar shall expose **My Wishlist** as a persistent link. |
| FR-WL-03 | Users shall be able to follow authors via **My Writers**. |

### 1.10 Recommendations Engine

| ID | Requirement |
|----|-------------|
| FR-REC-01 | The system shall generate **personalised product recommendations** based on the user's purchase history. |
| FR-REC-02 | The system shall surface **cross-sell** products on the product detail page and cart page. |
| FR-REC-03 | The system shall serve **upsell** products at relevant browsing touchpoints. |

---

## 2. Non-Functional Requirements

### 2.1 Performance

| ID | Requirement |
|----|-------------|
| NFR-PER-01 | Catalogue search and browse pages shall respond within **≤ 300 ms** (p95) under normal load. |
| NFR-PER-02 | The checkout and payment initiation flow shall complete within **≤ 500 ms** (p95) excluding external gateway latency. |
| NFR-PER-03 | Product recommendation results shall be served within **≤ 200 ms** using pre-computed or cached results. |

### 2.2 Scalability

| ID | Requirement |
|----|-------------|
| NFR-SCA-01 | The backend shall support **horizontal scaling** of all stateless services without data inconsistency. |
| NFR-SCA-02 | The catalogue and recommendation services shall be capable of handling **peak concurrent sessions** without degradation. |
| NFR-SCA-03 | The system shall support **independent scaling per tenant** — a traffic spike on one tenant must not degrade performance for others. |

### 2.3 Security

| ID | Requirement |
|----|-------------|
| NFR-SEC-01 | All API communication shall use **HTTPS / TLS 1.2+**. |
| NFR-SEC-02 | Passwords shall be stored using a **strong one-way hashing algorithm** (e.g., bcrypt / Argon2). |
| NFR-SEC-03 | Payment card data shall **never be stored** on BookWorm servers; all card handling must be delegated to a PCI-DSS-compliant gateway. |
| NFR-SEC-04 | Authentication tokens shall be short-lived with **refresh token rotation**. |
| NFR-SEC-05 | Role-based access control (RBAC) shall enforce entitlement at every API boundary. |
| NFR-SEC-06 | All personally identifiable information (PII) at rest shall be **encrypted**. |
| NFR-SEC-07 | Every API request shall carry a **tenant context identifier**; the backend shall reject requests where the resolved tenant does not match the resource being accessed. |
| NFR-SEC-08 | Tenant data shall be **logically isolated** at the database/storage layer using a **tenant-discriminator column** on every tenant-scoped table. All queries must include the tenant ID as a mandatory predicate; the ORM/data-access layer shall enforce this automatically to prevent accidental cross-tenant data leakage. |

### 2.4 Availability & Reliability

| ID | Requirement |
|----|-------------|
| NFR-AVL-01 | The platform shall target **≥ 99.9% uptime** for core transactional paths (browse, cart, checkout). |
| NFR-AVL-02 | The system shall implement **graceful degradation** — recommendation failures must not block checkout. |
| NFR-AVL-03 | Payment processing shall have a **retry and idempotency** mechanism to prevent duplicate charges. |

### 2.5 Maintainability & Observability

| ID | Requirement |
|----|-------------|
| NFR-MNT-01 | All backend modules shall emit **structured logs** with correlation IDs traceable across service boundaries. |
| NFR-MNT-02 | The system shall expose **health-check endpoints** for each backend module. |
| NFR-MNT-03 | Critical business events (order placed, payment confirmed, order cancelled) shall be **auditable** with timestamps and actor IDs. |

### 2.6 Data Integrity

| ID | Requirement |
|----|-------------|
| NFR-DAT-01 | Order state transitions shall be **atomic** — partial state changes must not persist. |
| NFR-DAT-02 | Inventory and pricing data shall be **consistent** between catalogue display and checkout confirmation. |

### 2.7 Compliance

| ID | Requirement |
|----|-------------|
| NFR-CMP-01 | The platform shall comply with applicable **Indian data protection regulations** regarding user PII. |
| NFR-CMP-02 | Tax calculations shall conform to **Indian GST rules** applicable to book sales. |

---

## 3. User Roles

| Role | Scope | Description | Key Privileges |
|------|-------|-------------|----------------|
| **Guest User** | Tenant | An unauthenticated visitor scoped to a specific tenant store. | Browse that tenant's catalogue, add items to a session-scoped cart, proceed to checkout, view product detail pages. Cannot access order history or wishlist. |
| **Registered User** | Tenant | An authenticated customer with a personal account on a specific tenant store. | All Guest capabilities plus: view order history, maintain a wishlist, follow authors, use saved addresses, redeem gift points & wallet balance, access personalised recommendations — all scoped to that tenant. |
| **Tenant Administrator** | Tenant | Owns and manages a single tenant store. | Create/modify store branding and metadata, manage catalogues, define store policies, manage product listings, view tenant-scoped reports. |
| **Catalogue Manager** | Tenant | An operator responsible for product data within a tenant. | Create and update product entries, assign categories, set pricing and formats, manage promotions — within their tenant only. |
| **Fulfilment / Shipping Operator** | Tenant | Back-office role managing physical dispatch within a tenant. | View confirmed orders, update shipment status, process return shipments — tenant-scoped. |
| **Finance Operator** | Tenant | Back-office role managing payment reconciliation within a tenant. | View payment records, process refunds, manage gift card and wallet issuance — tenant-scoped. |
| **Platform Administrator** | Platform | Superuser with cross-tenant authority. Operates at the platform level. | Provision/deactivate tenants, manage platform-wide configuration, access all tenant data for support purposes. |

---

## 4. User Stories

### Authentication & Registration

| ID | As a… | I want to… | So that… |
|----|-------|-----------|----------|
| US-001 | Guest User | Sign in using my phone number or e-mail and password | I can access my personal account and order history |
| US-002 | Guest User | Continue browsing without registering | I can shop quickly without creating an account |
| US-003 | New Visitor | Sign up for an account | I can track orders, save addresses, and get personalised recommendations |
| US-004 | Registered User | Reset my forgotten password | I can regain access to my account |
| US-005 | Registered User | Log out securely | My session is terminated and my account remains secure |

### Browsing & Discovery

| ID | As a… | I want to… | So that… |
|----|-------|-----------|----------|
| US-006 | Registered User | See personalised book recommendations on the home page | I discover books relevant to my taste without searching |
| US-007 | Any User | Browse books by genre category | I can narrow my exploration to topics I enjoy |
| US-008 | Any User | Filter books by language, format, and price range | I find books that match my reading preferences and budget |
| US-009 | Any User | Sort search results by relevance, price, or popularity | I can find the most suitable books efficiently |
| US-010 | Any User | View a product detail page with full book information | I can make an informed purchase decision |
| US-011 | Any User | See related books on a product detail page | I discover additional books I might enjoy |
| US-012 | Any User | See bestsellers and new launches on the home page | I stay informed about popular and recent releases |
| US-013 | Any User | Follow a breadcrumb trail during browsing | I can navigate back to previous category levels easily |

### Wishlist & Writers

| ID | As a… | I want to… | So that… |
|----|-------|-----------|----------|
| US-014 | Registered User | Add a book to my Wishlist | I can save it for a future purchase |
| US-015 | Registered User | Follow an author via My Writers | I am notified when new books by that author become available |

### Cart & Checkout

| ID | As a… | I want to… | So that… |
|----|-------|-----------|----------|
| US-016 | Any User | Add one or more books to my shopping cart | I can purchase multiple books in a single transaction |
| US-017 | Any User | Adjust the quantity of an item in the cart | I can buy multiple copies or reduce the number before paying |
| US-018 | Any User | Apply a coupon code at checkout | I receive an advertised discount on my order |
| US-019 | Registered User | Use a saved delivery address at checkout | I save time by not re-entering my address for every order |
| US-020 | Any User | Enter a new delivery address at checkout | My order is shipped to the correct location |
| US-021 | Any User | See a real-time order total with tax and delivery charges | I know exactly how much I will pay before confirming |
| US-022 | Registered User | Redeem my gift points / wallet balance at checkout | I reduce the amount I pay with my stored credit |

### Payment

| ID | As a… | I want to… | So that… |
|----|-------|-----------|----------|
| US-023 | Any User | Pay by Credit Card, Debit Card, UPI, or Wallet | I can use my preferred payment method |
| US-024 | Any User | Receive a purchase confirmation screen after payment | I have immediate proof that my order was placed |
| US-025 | Registered User | Receive a refund when I cancel or return an order | My money is returned to my original payment method or wallet |

### Order Management

| ID | As a… | I want to… | So that… |
|----|-------|-----------|----------|
| US-026 | Registered User | View my full order history via My Orders | I can track all past and current purchases |
| US-027 | Registered User | Cancel an order within the allowed window | I can change my mind before dispatch |
| US-028 | Registered User | Return a delivered order | I can get a refund for books I no longer want |
| US-029 | Registered User | Use "Buy Again" on a past order | I can quickly re-purchase books I enjoyed |

### Reviews

| ID | As a… | I want to… | So that… |
|----|-------|-----------|----------|
| US-030 | Any User | Read book reviews and ratings | I can gauge the quality of a book before buying |
| US-031 | Registered User | Submit a star rating and written review for a book | I share my feedback with the community |

### Administration

| ID | As a… | I want to… | So that… |
|----|-------|-----------|----------|
| US-032 | Platform Administrator | Provision a new tenant store with a unique identifier and initial configuration | A new retailer can begin operating on the platform independently |
| US-033 | Platform Administrator | Deactivate or suspend a tenant store | Non-compliant or inactive tenants are removed from the platform without affecting other tenants |
| US-034 | Tenant Administrator | Configure my store's branding, name, and metadata | My storefront has a distinct identity for my customers |
| US-035 | Tenant Administrator | Define and update store policies (returns, tax, shipping) | My customers are governed by rules specific to my store |
| US-036 | Tenant Administrator | Create and manage catalogues scoped to my store | My product catalogue is independent from other stores on the platform |
| US-037 | Catalogue Manager | Add and manage product listings within my tenant | The catalogue remains current and accurate for my store's customers |
| US-038 | Finance Operator | Process a refund for a returned order within my tenant | The customer's funds are returned promptly |
| US-039 | Fulfilment Operator | Update a shipment's status within my tenant | Customers receive accurate delivery tracking information for their store |
| US-040 | Registered User | Know that my account, orders, and wishlist on one store are not visible to another store | My data privacy is protected across the platform |

---

## 5. Business Capabilities

| # | Capability | Description |
|---|-----------|-------------|
| BC-01 | **Identity & Entitlement** | Manage user identities, authentication, session lifecycle, and role-based access entitlements. |
| BC-02 | **Store Configuration** | Create and manage virtual storefronts, associate catalogues, and enforce store-level policies. |
| BC-03 | **Catalogue & Product Management** | Maintain the product catalogue including metadata, pricing, formats, categories, and media. |
| BC-04 | **Search & Discovery** | Full-text search, faceted filtering, sorting, and personalised product discovery across the catalogue. |
| BC-05 | **Personalisation & Recommendations** | Generate and serve contextual product recommendations based on purchase history and browsing behaviour. |
| BC-06 | **Cart & Basket Management** | Create, modify, and persist shopping carts; apply promotions; compute order totals. |
| BC-07 | **Promotions & Loyalty** | Manage coupon codes, gift points, and wallet balances; evaluate eligibility and apply discounts. |
| BC-08 | **Checkout & Address Management** | Guide users through the checkout flow; manage saved and ad-hoc delivery addresses. |
| BC-09 | **Order Lifecycle Management** | Create, confirm, modify, cancel, and return orders; maintain order history. |
| BC-10 | **Payment Processing** | Orchestrate payment gateway interactions, capture payments, handle failures, and process refunds. |
| BC-11 | **Shipping & Fulfilment** | Calculate shipping rates, estimate delivery dates, manage dispatch, and process return shipments. |
| BC-12 | **Reviews & Ratings** | Accept, moderate, and publish user-submitted reviews and star ratings for products. |
| BC-13 | **Wishlist & Author Following** | Allow users to save products for later and track preferred authors. |
| BC-14 | **Notification & Confirmation** | Deliver transactional communications (order confirmed, payment received, shipment dispatched). |

---

## 6. Domain Boundaries

The BookWorm platform decomposes into the following bounded contexts, each with clear ownership over its data and a well-defined integration contract with neighbouring domains.

```
┌─────────────────────────────────────────────────────────────────┐
│                        BookWorm Platform                        │
│                                                                 │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────┐    │
│  │   Identity   │   │    Store     │   │    Catalogue     │    │
│  │   Domain     │◄──│    Domain    │──►│    Domain        │    │
│  └──────┬───────┘   └──────────────┘   └────────┬─────────┘    │
│         │                                        │              │
│         │           ┌──────────────┐             │              │
│         └──────────►│    Cart &    │◄────────────┘              │
│                     │  Promotions  │                            │
│                     │    Domain    │                            │
│                     └──────┬───────┘                           │
│                            │                                    │
│         ┌──────────────────┼──────────────────┐                │
│         ▼                  ▼                  ▼                │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐        │
│  │    Order     │   │   Payment    │   │   Shipping   │        │
│  │    Domain    │──►│    Domain    │   │    Domain    │        │
│  └──────────────┘   └──────────────┘   └──────────────┘        │
│                                                                 │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐        │
│  │  Wishlist &  │   │  Reviews &   │   │Notification  │        │
│  │   Writers    │   │   Ratings    │   │   Domain     │        │
│  └──────────────┘   └──────────────┘   └──────────────┘        │
└─────────────────────────────────────────────────────────────────┘
```

### Domain Descriptions

| Domain | Core Responsibility | Key Integration Points |
|--------|-------------------|----------------------|
| **Identity** | User registration, authentication, roles, and entitlement | All domains query Identity for auth tokens and role checks |
| **Store** | Store lifecycle, catalogue association, policy management | Catalogue domain reads store assignments; Identity enforces store-level entitlement |
| **Catalogue** | Product data, categories, pricing, formats, search index | Cart reads current price; Recommendation engine reads product metadata |
| **Cart & Promotions** | Session/persistent cart, coupon/gift-point evaluation, total computation | Reads Catalogue for price & availability; feeds Order on checkout |
| **Order** | Order record, state machine (created → confirmed → dispatched → delivered → returned/cancelled) | Receives cart snapshot; triggers Payment and Shipping |
| **Payment** | Gateway orchestration, payment capture, refund processing, wallet/gift balance | Triggered by Order; updates Order on outcome; triggers Notification |
| **Shipping** | Rate calculation, delivery-date estimation, dispatch events, return logistics | Triggered by Order confirmation; provides tracking to Notification |
| **Recommendation** | Personalised recommendations, cross-sell, upsell | Reads Order history and Catalogue metadata; serves Catalogue and Cart |
| **Wishlist & Writers** | User wishlists, author follow relationships | Reads Catalogue; reads Identity for user context |
| **Reviews & Ratings** | Product reviews and star ratings | Writes to Catalogue aggregate read-model; requires Identity for authorship |
| **Notification** | Transactional messages (email/SMS) | Listens to events from Order, Payment, and Shipping domains |

---

## 7. Backend Modules

Each module maps to a deployable unit of the backend, aligned with a domain boundary.

### M-01 Identity Module
- User registration and profile storage
- Authentication (credential validation, token issuance)
- Password reset and credential management
- Role and entitlement management
- Session / token lifecycle (issue, refresh, revoke)

### M-02 Store & Tenant Module
- **Tenant provisioning** — create, configure, suspend, and deactivate tenants
- Tenant metadata management (name, slug/subdomain, branding)
- Store CRUD operations within a tenant
- Catalogue-to-store association (tenant-scoped)
- Store policy management (return windows, tax rules, shipping rules)
- Entitlement filtering rules per store
- **Tenant context resolution** — resolve incoming requests to the correct tenant via subdomain, header, or token claim
- Cross-tenant isolation enforcement (reject cross-tenant resource access)

### M-03 Catalogue Module
- Product CRUD (title, author, publisher, ISBN, description, cover image) — **all records tagged with tenant ID**
- Category / genre taxonomy management (global taxonomy with per-tenant overrides)
- Format management (Paperback, Hardcover, eBook)
- Pricing and tax-rate management (per-tenant pricing rules)
- Search index maintenance (full-text, faceted) — **index partitioned by tenant**
- Product availability / stock tracking
- Breadcrumb / navigation-tree management

### M-04 Recommendation Module
- User preference profile computation (based on order history)
- Personalised recommendation generation ("Recommended for You")
- Cross-sell rule engine ("Related Reads")
- Upsell rule engine ("Bestsellers this Month", "New Launches")
- Recommendation cache management

### M-05 Cart Module
- Cart creation and session binding (guest + registered)
- Add / remove / update cart line items
- Cart persistence (merge guest cart on login)
- Coupon code validation and application
- Gift-point and wallet-balance deduction preview
- Real-time subtotal, tax, and delivery charge computation
- Cart-to-order snapshot preparation

### M-06 Order Module
- Order record creation from cart snapshot
- Order state machine management:
  `PENDING → CONFIRMED → PROCESSING → DISPATCHED → DELIVERED → (CANCELLED | RETURN_REQUESTED | RETURNED)`
- Order history retrieval with pagination
- Order cancellation (within policy window)
- Return/refund request initiation
- Buy Again — cart repopulation from historical order

### M-07 Payment Module
- Payment session creation
- Gateway integration abstraction (supports Credit Card, Debit Card, UPI, Wallet)
- Payment capture and failure handling
- Idempotent payment processing (prevents duplicate charges)
- Refund orchestration
- Gift card and wallet balance ledger
- Payment event publishing (success, failure, refund)

### M-08 Shipping Module
- Shipping rate calculation (by destination, weight, order value)
- Estimated delivery date calculation
- Free-delivery eligibility evaluation
- Dispatch record creation on order confirmation
- Return shipment initiation and tracking
- Shipment status event publishing

### M-09 Wishlist & Writers Module
- Wishlist item add / remove / list
- Author follow / unfollow
- My Writers feed (followed authors)

### M-10 Reviews & Ratings Module
- Review submission (text + star rating)
- Review moderation queue
- Aggregate rating computation per product
- Review retrieval (paginated, most recent / most helpful)

### M-11 Notification Module
- Transactional notification dispatch (email and/or SMS)
- Notification templates (order confirmed, payment received, shipment dispatched, refund processed)
- Delivery status tracking
- Event subscription to Order, Payment, and Shipping modules

---

## 8. API Domains

Each API domain corresponds to a backend module and exposes a coherent contract to frontend consumers. API domains are categorised by primary concern — no endpoint specifications are included here.

| # | API Domain | Primary Consumer(s) | Core Responsibility |
|---|-----------|-------------------|-------------------|
| 1 | **Identity API** | Web/Mobile client, all other API domains (token validation) | Authentication, registration, user profile, role/entitlement — **tenant-scoped** |
| 2 | **Store / Tenant API** | Platform Admin portal, Tenant Admin portal, Catalogue API | Tenant provisioning, store configuration, policy retrieval, catalogue associations |
| 3 | **Catalogue API** | Web/Mobile client, Cart API, Recommendation API | Product search, browse, filter, sort, product detail |
| 4 | **Recommendation API** | Web/Mobile client, Catalogue API, Cart API | Personalised recommendations, cross-sell, upsell signals |
| 5 | **Cart API** | Web/Mobile client | Cart lifecycle, coupon/gift-point application, total computation |
| 6 | **Order API** | Web/Mobile client, Payment API, Shipping API | Order CRUD, status tracking, cancellation, returns, history |
| 7 | **Payment API** | Web/Mobile client (checkout), Order API | Payment initiation, method selection, refunds, wallet balance |
| 8 | **Shipping API** | Web/Mobile client (checkout, order detail), Order API | Rate lookup, delivery date estimation, dispatch status, returns |
| 9 | **Wishlist & Writers API** | Web/Mobile client | Wishlist management, author follow/unfollow |
| 10 | **Reviews & Ratings API** | Web/Mobile client, Catalogue API (aggregate read) | Review submission, moderation, retrieval, rating aggregates |
| 11 | **Notification API** (internal) | Order, Payment, Shipping modules | Trigger and manage transactional notifications |

---

## 9. Data Ownership Matrix

Each domain owns its primary entities exclusively. Cross-domain data is accessed via published events or read-model projections — never by direct database access across domain boundaries.

| Entity | Owning Domain / Module | Shared As | Consumers |
|--------|----------------------|-----------|-----------|
| Tenant | Store & Tenant | Tenant ID in all cross-domain references | All modules (tenant discriminator in every query) |
| User Profile | Identity | JWT claims / user ID + tenant ID reference | All modules (for auth context) |
| Role & Entitlement | Identity | Token claims (includes tenant scope) | Store, Catalogue, Order |
| Store | Store & Tenant | Store ID + tenant ID reference | Catalogue, Identity |
| Store Policy | Store & Tenant | Policy read-model (tenant-scoped) | Cart, Order |
| Product | Catalogue | Product snapshot (price, title, format) | Cart, Order (snapshot), Recommendation |
| Category / Genre Taxonomy | Catalogue | Read-only reference | Recommendation |
| Product Availability | Catalogue | Availability event | Cart, Order |
| Price & Tax Rate | Catalogue | Price snapshot at cart-add time | Cart, Order |
| Cart | Cart & Promotions | Cart snapshot on checkout | Order |
| Coupon / Promotion Rule | Cart & Promotions | Validation result | Cart, Order |
| Gift Points / Wallet Balance | Payment | Balance query result | Cart (display), Order (deduction) |
| Order | Order | Order ID + status event | Payment, Shipping, Notification |
| Order Line Item | Order | Line-item read-model | Payment (amount), Shipping (dispatch list) |
| Payment Record | Payment | Payment status event | Order, Notification |
| Refund Record | Payment | Refund event | Order, Notification |
| Shipment Record | Shipping | Shipment status event | Order, Notification |
| Return Shipment | Shipping | Return status event | Order, Payment |
| Wishlist | Wishlist & Writers | Not shared | — |
| Author Follow | Wishlist & Writers | Author ID list | Notification (new book alerts) |
| Review & Rating | Reviews & Ratings | Aggregate rating read-model | Catalogue (display), Recommendation |
| Notification Log | Notification | — | — |

---

## 10. Assumptions

| # | Assumption |
|---|-----------|
| A-01 | The platform is **multi-tenant by design** — multiple independent tenant stores are a first-class requirement, not a future upgrade. Each tenant is fully isolated in data, catalogue, users, and policies. |
| A-02 | The primary market is **India**; currency is INR (₹), tax is Indian GST, and phone numbers use the +91 prefix. |
| A-03 | **eBooks** are a supported product format. Digital delivery (download/DRM) is assumed to be handled by a third-party DRM system and is outside the initial backend scope. |
| A-04 | Payment gateway integration will use a **third-party PCI-DSS-compliant gateway** (specific provider TBD). BookWorm will never store raw card data. |
| A-05 | **Inventory management** for physical books (stock levels, warehouse location) is considered in scope for the Catalogue module at the level of availability flags; deep warehouse management is out of scope. |
| A-06 | Guest cart items are stored in a **session-scoped** structure. On login, guest cart items are **merged** with the authenticated user's persistent cart. |
| A-07 | The **Recommendation engine** is initially rule-based (order history signals) and does not require a dedicated ML pipeline in phase 1; personalisation can be upgraded later. |
| A-08 | Delivery date estimation is provided by integrating with a **shipping carrier API**; the specific carrier(s) are TBD. |
| A-09 | **Notifications** will be delivered via email and SMS; push notifications are deferred. |
| A-10 | The **"My Writers"** feature tracks user follow relationships; new-book notifications based on followed authors will be triggered by new product additions in the Catalogue. |
| A-11 | Review moderation is **manual** in the first release (a back-office queue); automated moderation may be added later. |
| A-12 | **Coupon codes** are single-use per order; stacking of multiple coupons on a single order is not supported in phase 1. |
| A-13 | Tax calculations are computed **server-side** at checkout time and stored in the order snapshot; they are not re-evaluated post-order creation. |
| A-14 | **Return eligibility** is governed by a store policy (e.g., within N days of delivery); the specific policy values are configurable by the Store Administrator. |

---

## 11. Out of Scope Items

| # | Item | Notes |
|---|------|-------|
| OOS-01 | **eBook DRM & Digital Delivery** | Serving, downloading, or licensing digital book content is out of scope. A third-party DRM provider is assumed. |
| OOS-02 | **Mobile Native Applications** | Only the web e-store frontend is referenced in wireframes. Native iOS/Android apps are not in scope. |
| OOS-03 | **Seller / Publisher Self-Service Portal** | Publishers or authors uploading their own books and managing listings independently. |
| OOS-04 | **Warehouse & Inventory Management** | Deep stock management, warehouse picking/packing workflows, multi-location inventory. |
| OOS-05 | **Real-Time Order Tracking (GPS / Carrier Live Feed)** | Live parcel tracking on a map; estimated delivery dates are in scope but live carrier updates are not. |
| OOS-06 | **ML-Based Personalisation Pipeline** | Advanced machine-learning recommendation models, collaborative filtering infrastructure, and A/B testing for recommendations. |
| OOS-07 | **Push Notifications** | Browser push and mobile push notifications. Email and SMS are in scope. |
| OOS-08 | **Subscription / Book Club Features** | Monthly subscription boxes, curated reading clubs, or recurring billing. |
| OOS-09 | **Multi-Currency Support** | All transactions are in INR. Foreign currency conversion is out of scope. |
| OOS-10 | **Social Sharing & Community Features** | Reading lists shared publicly, social feeds, comments on author pages. |
| OOS-11 | **Affiliate / Referral Programme** | Referral tracking, affiliate link management, and commission calculations. |
| OOS-12 | **B2B / Bulk Order Management** | Corporate or institutional bulk purchase workflows with special pricing. |
| OOS-13 | **Book Preview / Sample Reader** | In-browser reading of book previews or sample chapters. |
| OOS-14 | **Fraud Detection Engine** | An internal ML-based fraud scoring system. Basic gateway-level fraud controls are assumed via the payment provider. |
| OOS-15 | **Analytics & Business Intelligence** | Reporting dashboards, sales analytics, funnel metrics — a separate BI tooling concern. |

---

*Document version: 1.2 | Derived from: BookWorm wireframes and architecture diagram | Changes: v1.1 — Multi-tenancy as first-class design; v1.2 — Tenant isolation strategy confirmed as discriminator-column (shared schema, mandatory tenant-ID predicate on all queries) | Status: Draft for review*
