# Striker Mall — Java website

A professional multi-store shopping website built with **Java (HTTP server), HSQLDB (embedded SQL database), HTML, CSS and JavaScript**. The backend does **not** use Python. The original uploaded K.M.S E-Mall Java Swing project is preserved in `original-java/` for reference.

## Run on Windows (easy)

1. Extract this ZIP and open `striker-mall-java`.
2. Install **Java JDK 17+** if necessary (https://adoptium.net/). Check that both `java -version` and `javac -version` work.
3. Double-click **`start-windows.bat`**. Leave the terminal window open.
4. Open **http://localhost:8081** in a browser.

### Run in VS Code

Open the `striker-mall-java` folder. Open **Terminal → New Terminal** and run:

```powershell
.\start-windows.bat
```

### macOS / Linux

Requires Java JDK 17+.

```bash
bash start-unix.sh
```

If port 8081 is occupied, set the `STRIKER_PORT` environment variable before starting. Example on PowerShell: `$env:STRIKER_PORT='8092'` and then `./start-windows.bat`. Open `http://localhost:8092` instead. (The script's printed link is the default.)

## Accounts

| Role | Username | Password |
|---|---|---|
| Admin | `1234567890` | `1234567890` |
| Sample customer | `demo` | `demo123` |

Or create a customer account using Sign Up. The sample/admin passwords are only for a local class demonstration; replace them before connecting the site to the internet.

## Features

- Responsive mall-themed login and registration screen
- Original 7 stores, 50 products and product specifications
- Customer shopping bag and delivery-address checkout
- SQL-backed quantities updated after every successful order; cancellation restores stock
- Customer order history and admin-controlled delivery status (placed, packing, shipped, delivered)
- Admin-only product editor, store creation/editing, pricing, stock controls and site settings
- Film/show schedule editor, cinema ticket booking and cancellation
- Passwords stored as salted PBKDF2-HMAC-SHA256 hashes; HTTP-only SameSite login session cookies
- Embedded Java database automatically created on first run (files in `data/`)

## Project structure

```
striker-mall-java/
  start-windows.bat           ← run this on Windows
  start-unix.sh
  src/main/java/com/strikermall/
    Server.java               ← Java web server and endpoints
    Db.java                   ← SQL persistence, seed catalog, password hashing
    Json.java                 ← JSON reader/writer
  src/main/resources/original-seed.sql
  web/index.html              ← website HTML
  web/style.css               ← website styles
  web/app.js                  ← browser interactions
  web/assets/mall.svg         ← local mall illustration
  lib/hsqldb.jar              ← embedded Java SQL database driver
  original-java/              ← unmodified source project
  data/                       ← generated on first run
```

## Notes

- The website link `http://localhost:8081` opens on **your own computer**, after starting Java. It is not a public deployment link. Publishing a live URL requires deployment to a Java-compatible host, domain/HTTPS and production security configuration.
- The demo checkout does not charge cards or integrate shipping companies. Delivery statuses are manually maintained by the administrator.
- The bundled SQL database stores user accounts, products, quantities and orders between restarts. Keep a backup of `data/` when you want to retain them.
- **Local demo, not production security**: no payment processing, password reset, anti-brute-force lockout, CSRF tokens, or persistent sessions. Exposing this localhost server directly to the internet is not supported.
- To reset everything to the original sample state, exit the program and delete the generated `data/` directory. Existing orders/accounts will be lost.
