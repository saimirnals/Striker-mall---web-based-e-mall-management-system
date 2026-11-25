# K.M.S. E-Mall Management System (Java)

A Java rewrite of the CBSE Class XII project *"E-Mall Management"* (Python + MySQL console app).
Same idea - one mall, many shops - rebuilt as a desktop application (Swing) on top of a properly
normalised MySQL database, using JDBC.

## What the mall offers

| Original (Python)                    | This project                                                    |
|--------------------------------------|-----------------------------------------------------------------|
| Tech showroom (mobiles, laptops)     | **Tech Showroom** - brands as sections, full spec sheet per item |
| Veg / Non-veg / Ice cream / Cafe     | **4 restaurants**, menu sections, prices                         |
| Super market (rice, masala, juice)   | **Fresh Mart** with aisles                                       |
| Bodybuilding supplements             | **Muscle Fuel**                                                  |
| IMAX ticket booking / cancelling     | **IMAX Theatre** - movies, timed shows, seat counts, booking, cancelling |
| Sign-in / sign-up, admin login       | Salted-hash login; role (customer/admin) decided by the database |
| `bill()` / `cart()`                  | Cart -> checkout -> printed bill; **My Orders** history          |
| Admin edits device specs / movies    | Admin console: products, stock, specs, movies, shows, sales report |

## Improvements over the original

* **One table per entity, not per brand/user.** The Python code created a new SQL table for every brand,
  restaurant and *every new customer* and built queries by string concatenation (SQL-injection risk).
  Now: `products`, `product_specs`, `orders`, `order_items` ... and only `PreparedStatement`s.
* **Passwords are stored as salted SHA-256**, not plain text.
* **Stock control** - checkout decrements stock inside a transaction (`SELECT ... FOR UPDATE`), so two customers
  can't buy the last item; cancelling an order restores stock.
* **Seat control** - bookings lock the show row; max 10 tickets; no booking/cancelling after a show starts.
* **GST on the bill** (5% food and grocery, 18% electronics and supplements), stored with each order.
* Quantities, search across all shops, order cancellation, sales-by-shop report, input validation.
* Admins are limited to the shops assigned to them in `shop_admins` (as the original `admin_info` table intended);
  the mall owner (assigned to all shops) additionally manages the theatre and sees sales.

## Database (MySQL 8+)

```
users ──< orders ──< order_items >── products >── shops
  │                                     │
  │                                product_specs
  ├──< ticket_bookings >── shows >── movies
  └──< shop_admins >── shops
```
`database/schema.sql` creates everything (plus the view `v_sales_by_shop`); `database/seed.sql` loads
4 users, 7 shops, 50 products (with 63 spec rows), 5 movies and 6 shows scheduled for tomorrow/day after.

## Setup

1. Install **JDK 17+** and **MySQL 8+**.
2. Create the database:  `scripts/setup_db.sh` (or run `schema.sql` then `seed.sql` in MySQL Workbench).
3. Edit `db.properties` with your MySQL user/password.
4. Either
   * **Maven:** `mvn package` then `java -jar target/emall-management-1.0.0.jar` (run from the project folder), or
   * **No Maven:** download `mysql-connector-j-8.x.jar` into `lib/` and run `scripts/run.sh` (`scripts\run.bat` on Windows).

Optional: `scripts/selfcheck.sh` runs the logic tests that don't need a database.

## Demo logins

| Role               | User ID     | Password   |
|--------------------|-------------|------------|
| Mall owner (admin) | `admin`     | `admin123` |
| Tech shop admin    | `techadmin` | `tech123`  |
| Customer           | `demo`      | `demo123`  |

New customers can register from the *Sign up* tab. Change the admin passwords before any real use.

## Project layout

```
database/   schema.sql, seed.sql
src/main/java/com/emall/
   Main.java
   model/    records: User, Shop, Product, Show, Booking, OrderSummary ...
   dao/      UserDao, ShopDao, ProductDao, OrderDao, TheatreDao   (all JDBC lives here)
   service/  Cart, Tax, Receipt
   util/     Db, PasswordUtil, Validator, AppException
   ui/       LoginFrame, CustomerFrame (Shop/Cart/Orders/Theatre panels), AdminFrame (+ panels)
src/test/java/com/emall/SelfCheck.java
```

## Known limitations / ideas

* Payment is simulated (a confirmation dialog); there is no real gateway.
* Database calls run on the Swing UI thread - fine for a local DB, use `SwingWorker` for a remote one.
* Possible next steps: seat-map selection, per-customer wishlist, product images, PDF bills, REST/web front end.
