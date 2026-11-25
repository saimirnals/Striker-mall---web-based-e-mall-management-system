-- E-Mall Management System : schema (MySQL 8+)
DROP DATABASE IF EXISTS emall;
CREATE DATABASE emall CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE emall;

CREATE TABLE users (
  user_id       INT AUTO_INCREMENT PRIMARY KEY,
  full_name     VARCHAR(80)  NOT NULL,
  phone         VARCHAR(15)  NOT NULL,
  username      VARCHAR(40)  NOT NULL UNIQUE,
  password_hash CHAR(64)     NOT NULL,
  salt          CHAR(32)     NOT NULL,
  role          ENUM('CUSTOMER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE shops (
  shop_id     INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(80) NOT NULL,
  category    ENUM('TECH','RESTAURANT','GROCERY','SUPPLEMENT') NOT NULL,
  description VARCHAR(200)
) ENGINE=InnoDB;

-- which admin manages which shop (replaces admin_info of the Python project)
CREATE TABLE shop_admins (
  user_id INT NOT NULL,
  shop_id INT NOT NULL,
  PRIMARY KEY (user_id, shop_id),
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  FOREIGN KEY (shop_id) REFERENCES shops(shop_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- one table for every sellable item (replaces one-table-per-brand / per-restaurant)
CREATE TABLE products (
  product_id  INT AUTO_INCREMENT PRIMARY KEY,
  shop_id     INT NOT NULL,
  section     VARCHAR(60) NOT NULL,          -- brand / menu section / aisle
  name        VARCHAR(100) NOT NULL,
  price       DECIMAL(10,2) NOT NULL CHECK (price >= 0),
  stock       INT NOT NULL DEFAULT 0 CHECK (stock >= 0),
  active      BOOLEAN NOT NULL DEFAULT TRUE,
  FOREIGN KEY (shop_id) REFERENCES shops(shop_id) ON DELETE CASCADE,
  INDEX idx_prod_shop (shop_id, section)
) ENGINE=InnoDB;

-- flexible specs for tech items (display, os, ram ...)
CREATE TABLE product_specs (
  product_id INT NOT NULL,
  spec_key   VARCHAR(40) NOT NULL,
  spec_value VARCHAR(100) NOT NULL,
  PRIMARY KEY (product_id, spec_key),
  FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE movies (
  movie_id     INT AUTO_INCREMENT PRIMARY KEY,
  title        VARCHAR(100) NOT NULL,
  director     VARCHAR(80),
  genre        VARCHAR(40),
  duration_min INT
) ENGINE=InnoDB;

CREATE TABLE shows (
  show_id      INT AUTO_INCREMENT PRIMARY KEY,
  movie_id     INT NOT NULL,
  screen_no    INT NOT NULL,
  show_time    DATETIME NOT NULL,
  total_seats  INT NOT NULL,
  booked_seats INT NOT NULL DEFAULT 0,
  price        DECIMAL(8,2) NOT NULL,
  FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE CASCADE,
  CHECK (total_seats > 0),
  CHECK (booked_seats >= 0 AND booked_seats <= total_seats)
) ENGINE=InnoDB;

CREATE TABLE orders (
  order_id   INT AUTO_INCREMENT PRIMARY KEY,
  user_id    INT NOT NULL,
  order_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  subtotal   DECIMAL(12,2) NOT NULL,
  gst        DECIMAL(12,2) NOT NULL,
  total      DECIMAL(12,2) NOT NULL,
  status     ENUM('PAID','CANCELLED') NOT NULL DEFAULT 'PAID',
  FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

CREATE TABLE order_items (
  item_id    INT AUTO_INCREMENT PRIMARY KEY,
  order_id   INT NOT NULL,
  product_id INT NOT NULL,
  qty        INT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (order_id)   REFERENCES orders(order_id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products(product_id),
  CHECK (qty > 0)
) ENGINE=InnoDB;

CREATE TABLE ticket_bookings (
  booking_id  INT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT NOT NULL,
  show_id     INT NOT NULL,
  num_tickets INT NOT NULL,
  total       DECIMAL(10,2) NOT NULL,
  status      ENUM('BOOKED','CANCELLED') NOT NULL DEFAULT 'BOOKED',
  booked_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(user_id),
  FOREIGN KEY (show_id) REFERENCES shows(show_id),
  CHECK (num_tickets BETWEEN 1 AND 10)
) ENGINE=InnoDB;

CREATE VIEW v_sales_by_shop AS
SELECT s.shop_id, s.name AS shop, SUM(oi.qty) AS units, SUM(oi.qty*oi.unit_price) AS revenue
FROM order_items oi
JOIN orders o   ON o.order_id = oi.order_id AND o.status='PAID'
JOIN products p ON p.product_id = oi.product_id
JOIN shops s    ON s.shop_id = p.shop_id
GROUP BY s.shop_id, s.name;
