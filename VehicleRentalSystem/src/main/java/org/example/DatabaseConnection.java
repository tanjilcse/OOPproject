package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class for managing database connections
 */
public class DatabaseConnection {
    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USER = "postgres";
    private static final String PASSWORD = "12345678";

    /**
     * Get a connection to the database
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Initialize the database by creating necessary tables if they don't exist
     */
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create vehicles table
            stmt.execute("CREATE TABLE IF NOT EXISTS vehicles (" +
                    "vehicle_id VARCHAR(50) PRIMARY KEY, " +
                    "type VARCHAR(50) NOT NULL, " +
                    "brand VARCHAR(50) NOT NULL, " +
                    "model VARCHAR(50) NOT NULL, " +
                    "base_price_per_day DECIMAL(10, 2) NOT NULL, " +
                    "is_available BOOLEAN DEFAULT TRUE" +
                    ")");

            // Create customers table
            stmt.execute("CREATE TABLE IF NOT EXISTS customers (" +
                    "customer_id VARCHAR(50) PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "password VARCHAR(100) NOT NULL" +
                    ")");

            // Create rentals table
            stmt.execute("CREATE TABLE IF NOT EXISTS rentals (" +
                    "rental_id SERIAL PRIMARY KEY, " +
                    "vehicle_id VARCHAR(50) NOT NULL, " +
                    "customer_id VARCHAR(50) NOT NULL, " +
                    "days INTEGER NOT NULL, " +
                    "FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id), " +
                    "FOREIGN KEY (customer_id) REFERENCES customers(customer_id)" +
                    ")");

            // Create admin table
            stmt.execute("CREATE TABLE IF NOT EXISTS admin (" +
                    "username VARCHAR(50) PRIMARY KEY, " +
                    "password VARCHAR(100) NOT NULL" +
                    ")");

            // Insert default admin if not exists
            stmt.execute("INSERT INTO admin (username, password) " +
                    "VALUES ('admin', 'admin123') " +
                    "ON CONFLICT (username) DO NOTHING");

            // Initialize sample vehicles
            initializeSampleVehicles();

            System.out.println("Database initialized successfully");
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize sample vehicles in the database
     */
    public static void initializeSampleVehicles() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Check if vehicles table is empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM vehicles");
            rs.next();
            int count = rs.getInt(1);

            // Only add sample vehicles if the table is empty
            if (count == 0) {
                // Sample vehicles data
                String[][] vehiclesData = {
                    {"V001", "Car", "Toyota", "Camry", "60.0"},
                    {"V002", "Motorcycle", "Honda", "CBR500R", "45.0"},
                    {"V003", "Truck", "Ford", "F-150", "120.0"},
                    {"V004", "Car", "Mahindra", "Thar", "150.0"}
                };

                // Insert sample vehicles
                for (String[] vehicle : vehiclesData) {
                    String sql = String.format(
                        "INSERT INTO vehicles (vehicle_id, type, brand, model, base_price_per_day, is_available) " +
                        "VALUES ('%s', '%s', '%s', '%s', %s, TRUE) " +
                        "ON CONFLICT (vehicle_id) DO NOTHING",
                        vehicle[0], vehicle[1], vehicle[2], vehicle[3], vehicle[4]
                    );
                    stmt.execute(sql);
                }

                System.out.println("Sample vehicles initialized successfully");
            }
        } catch (SQLException e) {
            System.err.println("Sample vehicles initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Close resources safely
     * @param connection Connection to close
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
}
