package com.pelea.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Sorts;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;
import com.pelea.ui.BookingInfo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class DatabaseManager {

    private static DatabaseManager instance;
    private MongoDatabase database;
    private MongoCollection<Document> usersCollection;
    private MongoCollection<Document> bookingsCollection;

    private DatabaseManager() {
        try {
            Dotenv dotenv = Dotenv.load();
            String connectionString = dotenv.get("MONGO_URI");
            String dbName = dotenv.get("DB_NAME");

            MongoClient mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase(dbName);
            
            usersCollection = database.getCollection("users");
            bookingsCollection = database.getCollection("bookings");
            
            System.out.println("OK: Connected to MongoDB: " + dbName);
            
            cleanOldBookings();
        } catch (Exception e) {
            System.err.println("ERROR: MongoDB Connection failed: " + e.getMessage());
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    private void cleanOldBookings() {
        try {
            if (bookingsCollection == null) return;

            String cutoffDate = LocalDate.now().minusDays(30).toString();

            var result = bookingsCollection.deleteMany(Filters.lt("booking_date", cutoffDate));

            if (result.getDeletedCount() > 0) {
                System.out.println("DATABASE CLEANUP: Deleted " + result.getDeletedCount() + " bookings older than " + cutoffDate);
            } else {
                System.out.println("DATABASE CLEANUP: No old bookings found to delete.");
            }
            
        } catch (Exception e) {
            System.err.println("Warning: Could not clean old bookings: " + e.getMessage());
        }
    }

    public boolean saveBooking(String deskId, String email, String name, String photo, String date, String interval) {
        try {
            Document booking = new Document()
                    .append("desk_id", deskId)
                    .append("user_email", email)
                    .append("user_name", name)
                    .append("user_photo", photo)
                    .append("booking_date", date)
                    .append("booking_interval", interval)
                    .append("createdAt", new Date());

            bookingsCollection.insertOne(booking);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public BookingInfo getBooking(String deskId, LocalDate date) {
        if (bookingsCollection == null) return null;

        Document query = new Document("desk_id", deskId)
                                .append("booking_date", date.toString());
        
        Document result = bookingsCollection.find(query).first();

        if (result != null) {
            return new BookingInfo(
                result.getString("desk_id"), 
                result.getString("user_email"),
                result.getString("user_name"),
                result.getString("user_photo"),
                result.getString("booking_date"),
                result.getString("booking_interval")
            );
        }
        return null;
    }
    
    public List<BookingInfo> getBookingsForUser(String email) {
        List<BookingInfo> userBookings = new ArrayList<>();
        if (bookingsCollection == null) return userBookings;

        bookingsCollection.find(Filters.eq("user_email", email))
                .sort(Sorts.ascending("booking_date"))
                .forEach(doc -> {
                    userBookings.add(new BookingInfo(
                        doc.getString("desk_id"),
                        doc.getString("user_email"),
                        doc.getString("user_name"),
                        doc.getString("user_photo"),
                        doc.getString("booking_date"),
                        doc.getString("booking_interval")
                    ));
                });
        return userBookings;
    }
    
    public void deleteBooking(String deskId, String date, String userEmail) {
        if (bookingsCollection == null) return;
        bookingsCollection.deleteOne(Filters.and(
            Filters.eq("desk_id", deskId),
            Filters.eq("booking_date", date),
            Filters.eq("user_email", userEmail)
        ));
    }

    public void saveUser(String email, String name) {
        if (usersCollection == null) return;
        Document query = new Document("email", email);
        Document update = new Document("$set", new Document()
                .append("email", email)
                .append("name", name)
                .append("lastLogin", new Date()))
                .append("$setOnInsert", new Document("joinedAt", new Date()));
        usersCollection.updateOne(query, update, new UpdateOptions().upsert(true));
    }
    
    public List<String> getAllBookedNamesForDate(String date) {
        List<String> names = new ArrayList<>();
        if (bookingsCollection == null) return names;

        bookingsCollection.find(Filters.eq("booking_date", date))
                .forEach(doc -> {
                    String name = doc.getString("user_name");
                    if (name != null && !names.contains(name)) {
                        names.add(name);
                    }
                });
        return names;
    }
    
    public java.util.Map<String, BookingInfo> getAllBookingsForDateAsMap(String date) {
        java.util.Map<String, BookingInfo> map = new java.util.HashMap<>();
        if (bookingsCollection == null) return map;

        bookingsCollection.find(Filters.eq("booking_date", date))
                .forEach(doc -> {
                    String deskId = doc.getString("desk_id");
                    BookingInfo info = new BookingInfo(
                        deskId,
                        doc.getString("user_email"),
                        doc.getString("user_name"),
                        doc.getString("user_photo"),
                        doc.getString("booking_date"),
                        doc.getString("booking_interval")
                    );
                    map.put(deskId, info);
                });
        return map;
    }
}