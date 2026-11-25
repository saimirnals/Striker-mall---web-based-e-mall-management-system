package com.strikermall;
import java.sql.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.regex.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.Base64;
public final class Db {
 static final String URL="jdbc:hsqldb:file:data/striker;shutdown=true";
 static {try{Class.forName("org.hsqldb.jdbcDriver");}catch(Exception e){throw new RuntimeException("Database driver missing: lib/hsqldb.jar",e);}}
 static Connection connect()throws Exception{return DriverManager.getConnection(URL,"sa","");}
 static void exec(Connection c,String sql,Object...args)throws Exception{try(PreparedStatement p=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);p.executeUpdate();}}
 static List<Map<String,Object>> rows(Connection c,String sql,Object...args)throws Exception{try(PreparedStatement p=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);try(ResultSet r=p.executeQuery()){List<Map<String,Object>> out=new ArrayList<>();ResultSetMetaData md=r.getMetaData();while(r.next()){Map<String,Object> m=new LinkedHashMap<>();for(int j=1;j<=md.getColumnCount();j++){Object v=r.getObject(j);if(v instanceof java.sql.Timestamp)v=v.toString();m.put(md.getColumnLabel(j).toLowerCase(),v);}out.add(m);}return out;}}}
 static Map<String,Object> row(Connection c,String sql,Object...args)throws Exception{List<Map<String,Object>> r=rows(c,sql,args);return r.isEmpty()?null:r.get(0);}
 static int number(Connection c,String sql,Object...args)throws Exception{Map<String,Object> m=row(c,sql,args);return m==null?0:((Number)m.values().iterator().next()).intValue();}
 static int insert(Connection c,String sql,Object...args)throws Exception{exec(c,sql,args);return number(c,"CALL IDENTITY()");}
 static String str(Object o){return o==null?"":o.toString();}
 static String salt(){byte[] b=new byte[16];new SecureRandom().nextBytes(b);return Base64.getEncoder().encodeToString(b);}
 static String hash(String password,String salt)throws Exception{PBEKeySpec key=new PBEKeySpec(password.toCharArray(),Base64.getDecoder().decode(salt),260000,256);byte[] bytes=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(key).getEncoded();return Base64.getEncoder().encodeToString(bytes);}
 static String now(){return LocalDateTime.now().toString();}
 static void initialize()throws Exception{Files.createDirectories(Path.of("data"));try(Connection c=connect()){
  String[] schema={
   "CREATE TABLE users (id INTEGER IDENTITY PRIMARY KEY,full_name VARCHAR(100) NOT NULL,phone VARCHAR(30) NOT NULL,username VARCHAR(50) NOT NULL,email VARCHAR(150) DEFAULT '',password_salt VARCHAR(100) NOT NULL,password_hash VARCHAR(150) NOT NULL,role VARCHAR(15) NOT NULL,created_at VARCHAR(40) DEFAULT '',UNIQUE(username))",
   "CREATE TABLE shops (id INTEGER IDENTITY PRIMARY KEY,name VARCHAR(100) NOT NULL,category VARCHAR(40) NOT NULL,description VARCHAR(500) DEFAULT '',active INTEGER DEFAULT 1)",
   "CREATE TABLE products (id INTEGER IDENTITY PRIMARY KEY,shop_id INTEGER NOT NULL,section VARCHAR(100) NOT NULL,name VARCHAR(150) NOT NULL,price DECIMAL(14,2) NOT NULL,stock INTEGER NOT NULL,active INTEGER DEFAULT 1,description VARCHAR(600) DEFAULT '',image_url VARCHAR(600) DEFAULT '',FOREIGN KEY(shop_id) REFERENCES shops(id))",
   "CREATE TABLE product_specs (product_id INTEGER NOT NULL,spec_key VARCHAR(100) NOT NULL,spec_value VARCHAR(250) NOT NULL,PRIMARY KEY(product_id,spec_key),FOREIGN KEY(product_id) REFERENCES products(id))",
   "CREATE TABLE orders (id INTEGER IDENTITY PRIMARY KEY,user_id INTEGER NOT NULL,created_at VARCHAR(40) NOT NULL,subtotal DECIMAL(14,2) NOT NULL,gst DECIMAL(14,2) NOT NULL,total DECIMAL(14,2) NOT NULL,status VARCHAR(20) NOT NULL,delivery_name VARCHAR(100) NOT NULL,delivery_phone VARCHAR(30) NOT NULL,delivery_address VARCHAR(500) NOT NULL,notes VARCHAR(500) DEFAULT '',FOREIGN KEY(user_id) REFERENCES users(id))",
   "CREATE TABLE order_items (id INTEGER IDENTITY PRIMARY KEY,order_id INTEGER NOT NULL,product_id INTEGER NOT NULL,name_snapshot VARCHAR(150) NOT NULL,qty INTEGER NOT NULL,unit_price DECIMAL(14,2) NOT NULL,tax_rate DECIMAL(5,2) NOT NULL,FOREIGN KEY(order_id) REFERENCES orders(id),FOREIGN KEY(product_id) REFERENCES products(id))",
   "CREATE TABLE movies (id INTEGER IDENTITY PRIMARY KEY,title VARCHAR(150) NOT NULL,director VARCHAR(100) DEFAULT '',genre VARCHAR(80) DEFAULT '',duration_min INTEGER DEFAULT 120,active INTEGER DEFAULT 1)",
   "CREATE TABLE shows (id INTEGER IDENTITY PRIMARY KEY,movie_id INTEGER NOT NULL,screen_no INTEGER NOT NULL,show_time VARCHAR(50) NOT NULL,total_seats INTEGER NOT NULL,booked_seats INTEGER DEFAULT 0,price DECIMAL(14,2) NOT NULL,active INTEGER DEFAULT 1,FOREIGN KEY(movie_id) REFERENCES movies(id))",
   "CREATE TABLE bookings (id INTEGER IDENTITY PRIMARY KEY,user_id INTEGER NOT NULL,show_id INTEGER NOT NULL,tickets INTEGER NOT NULL,total DECIMAL(14,2) NOT NULL,status VARCHAR(20) NOT NULL,created_at VARCHAR(40) NOT NULL,FOREIGN KEY(user_id) REFERENCES users(id),FOREIGN KEY(show_id) REFERENCES shows(id))",
   "CREATE TABLE settings (key VARCHAR(80) PRIMARY KEY,value VARCHAR(500) NOT NULL)"
  };
  if(row(c,"SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES WHERE TABLE_NAME='USERS'")==null){for(String sql:schema)exec(c,sql);seed(c);}
  exec(c,"CHECKPOINT");
 }}
 static void seed(Connection c)throws Exception{
  for(String[] kv:new String[][]{{"name","Striker Mall"},{"tagline","Every store. One extraordinary place."},{"announcement","A better way to shop, dine and unwind."}})exec(c,"INSERT INTO settings(key,value) VALUES(?,?)",kv[0],kv[1]);
  for(String[] user:new String[][]{{"Mall Administrator","1234567890","1234567890","ADMIN"},{"Demo Customer","demo","demo123","CUSTOMER"}}){String salt=salt();exec(c,"INSERT INTO users(full_name,phone,username,email,password_salt,password_hash,role,created_at) VALUES(?,?,?,?,?,?,?,?)",user[0],"9000000000",user[1],"",salt,hash(user[2],salt),user[3],now());}
  String sql=Files.readString(Path.of("src/main/resources/original-seed.sql"),StandardCharsets.UTF_8);
  int shopId=0;for(String[] r:parseSeed(sql,"shops"))exec(c,"INSERT INTO shops(id,name,category,description,active) VALUES(?,?,?,?,1)",++shopId,r[0],r[1],r[2]);
  int productId=0;for(String[] r:parseSeed(sql,"products"))exec(c,"INSERT INTO products(id,shop_id,section,name,price,stock,active,description,image_url) VALUES(?,?,?,?,?,?,1,'','')",++productId,Integer.parseInt(r[0]),r[1],r[2],new java.math.BigDecimal(r[3]),Integer.parseInt(r[4]));
  for(String[] r:parseSeed(sql,"product_specs"))exec(c,"INSERT INTO product_specs(product_id,spec_key,spec_value) VALUES(?,?,?)",Integer.parseInt(r[0]),r[1],r[2]);
  int movieId=0;for(String[] r:parseSeed(sql,"movies"))exec(c,"INSERT INTO movies(id,title,director,genre,duration_min,active) VALUES(?,?,?,?,?,1)",++movieId,r[0],r[1],r[2],Integer.parseInt(r[3]));
  int[][] sh={{1,1,10,0,120,350},{1,1,18,30,120,400},{2,2,14,0,100,380},{3,2,20,0,100,380},{4,3,11,0,150,250},{5,3,16,0,150,220}};
  for(int i=0;i<sh.length;i++){int[] x=sh[i];LocalDateTime t=LocalDate.now().plusDays(i>=4?2:1).atTime(x[2],x[3]);exec(c,"INSERT INTO shows(movie_id,screen_no,show_time,total_seats,booked_seats,price,active) VALUES(?,?,?,?,0,?,1)",x[0],x[1],t.toString(),x[4],new java.math.BigDecimal(x[5]));}
 }
 static List<String[]> parseSeed(String sql,String table){Matcher block=Pattern.compile("INSERT INTO "+table+"\\([^;]*?\\) VALUES\\s*(.*?);",Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(sql);if(!block.find())return List.of();List<String[]> out=new ArrayList<>();Matcher groups=Pattern.compile("\\(([^()]*)\\)").matcher(block.group(1));Pattern parts=Pattern.compile("'((?:''|[^'])*)'|(-?\\d+(?:\\.\\d+)?)");while(groups.find()){Matcher m=parts.matcher(groups.group(1));List<String> r=new ArrayList<>();while(m.find())r.add(m.group(1)!=null?m.group(1).replace("''","'"):m.group(2));out.add(r.toArray(new String[0]));}return out;}
 static void checkpoint(){try(Connection c=connect()){exec(c,"CHECKPOINT");}catch(Exception ignored){}}
}
