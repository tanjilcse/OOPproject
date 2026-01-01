package org.example;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

// Represents a generic vehicle available for rent
class Vehicle {
    private String vehicleId;
    private String type; // e.g., "Car", "Motorcycle", "Truck"
    private String brand;
    private String model;
    private double basePricePerDay;
    private boolean isAvailable;

    public Vehicle(String vehicleId, String type, String brand, String model, double basePricePerDay) {
        this.vehicleId = vehicleId;
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.basePricePerDay = basePricePerDay;
        this.isAvailable = true; // Initially available
    }

    // Getters for vehicle properties
    public String getVehicleId() {
        return vehicleId;
    }

    public String getType() {
        return type;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public double getBasePricePerDay() {
        return basePricePerDay;
    }

    // Setter for base price (can be updated by admin)
    public void setBasePricePerDay(double basePricePerDay) {
        this.basePricePerDay = basePricePerDay;
    }

    // Calculates the total rental price for a given number of days
    public double calculatePrice(int rentalDays) {
        return basePricePerDay * rentalDays;
    }

    // Checks if the vehicle is currently available
    public boolean isAvailable() {
        return isAvailable;
    }

    // Marks the vehicle as rented
    public void rent() {
        isAvailable = false;
    }

    // Marks the vehicle as returned
    public void returnVehicle() {
        isAvailable = true;
    }

    @Override
    public String toString() {
        return vehicleId + " - " + type + " " + brand + " " + model + " ($" + basePricePerDay + "/day, " + (isAvailable ? "Available" : "Rented") + ")";
    }
}

// Represents a customer in the rental system
class Customer {
    private String customerId;
    private String name;
    private String password;

    public Customer(String customerId, String name, String password) {
        this.customerId = customerId;
        this.name = name;
        this.password = password;
    }

    // Getters for customer properties
    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    // Setter for customer password
    public void setPassword(String newPassword) {
        this.password = newPassword;
    }

    @Override
    public String toString() {
        return "ID: " + customerId + ", Name: " + name;
    }
}

// Represents an administrator of the system
class Admin {
    private String username;
    private String password;

    public Admin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters for admin credentials
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    // Setter for admin password
    public void setPassword(String newPassword) {
        this.password = newPassword;
    }
}

// Represents a rental transaction
class Rental {
    private Vehicle vehicle;
    private Customer customer;
    private int days;

    public Rental(Vehicle vehicle, Customer customer, int days) {
        this.vehicle = vehicle;
        this.customer = customer;
        this.days = days;
    }

    // Getters for rental details
    public Vehicle getVehicle() {
        return vehicle;
    }

    public Customer getCustomer() {
        return customer;
    }

    public int getDays() {
        return days;
    }

    @Override
    public String toString() {
        return "Vehicle: " + vehicle.getVehicleId() + " (" + vehicle.getBrand() + " " + vehicle.getModel() + "), Customer: " + customer.getName() + " (ID: " + customer.getCustomerId() + "), Days: " + days;
    }
}

// The main system class managing vehicles, customers, and rentals using PostgreSQL database
class VehicleRentalSystem {
    // No need to store lists in memory as we'll use the database

    public VehicleRentalSystem() {
        // Initialize database tables
        DatabaseConnection.initializeDatabase();
    }

    // Adds a new vehicle to the system
    public void addVehicle(Vehicle vehicle) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "INSERT INTO vehicles (vehicle_id, type, brand, model, base_price_per_day, is_available) VALUES (?, ?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, vehicle.getVehicleId());
            stmt.setString(2, vehicle.getType());
            stmt.setString(3, vehicle.getBrand());
            stmt.setString(4, vehicle.getModel());
            stmt.setDouble(5, vehicle.getBasePricePerDay());
            stmt.setBoolean(6, true); // New vehicles are available by default

            stmt.executeUpdate();
            System.out.println("Vehicle added successfully to database.");
        } catch (SQLException e) {
            System.err.println("Error adding vehicle: " + e.getMessage());
        } finally {
            closeResources(stmt, conn);
        }
    }

    // Adds a new customer to the system
    public void addCustomer(Customer customer) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "INSERT INTO customers (customer_id, name, password) VALUES (?, ?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, customer.getCustomerId());
            stmt.setString(2, customer.getName());
            stmt.setString(3, customer.getPassword());

            stmt.executeUpdate();
            System.out.println("Customer added successfully to database.");
        } catch (SQLException e) {
            System.err.println("Error adding customer: " + e.getMessage());
        } finally {
            closeResources(stmt, conn);
        }
    }

    // Helper method to close JDBC resources
    private void closeResources(Statement stmt, Connection conn) {
        try {
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            System.err.println("Error closing statement: " + e.getMessage());
        }

        if (conn != null) {
            DatabaseConnection.closeConnection(conn);
        }
    }

    // Handles the rental process for a vehicle
    public void rentVehicle(Vehicle vehicle, Customer customer, int days) {
        Connection conn = null;
        PreparedStatement updateStmt = null;
        PreparedStatement insertStmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // First check if vehicle is available
            String checkSql = "SELECT is_available FROM vehicles WHERE vehicle_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, vehicle.getVehicleId());
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getBoolean("is_available")) {
                // Update vehicle availability
                String updateSql = "UPDATE vehicles SET is_available = false WHERE vehicle_id = ?";
                updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, vehicle.getVehicleId());
                updateStmt.executeUpdate();

                // Create rental record
                String insertSql = "INSERT INTO rentals (vehicle_id, customer_id, days) VALUES (?, ?, ?)";
                insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setString(1, vehicle.getVehicleId());
                insertStmt.setString(2, customer.getCustomerId());
                insertStmt.setInt(3, days);
                insertStmt.executeUpdate();

                conn.commit();
                vehicle.rent(); // Update the in-memory object state
                System.out.println("Vehicle rented successfully.");
            } else {
                System.out.println("Vehicle is not available for rent.");
                conn.rollback();
            }

            rs.close();
            checkStmt.close();
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
            }
            System.err.println("Error renting vehicle: " + e.getMessage());
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
            }
            closeResources(updateStmt, null);
            closeResources(insertStmt, conn);
        }
    }

    // Handles the return process for a vehicle
    public void returnVehicle(Vehicle vehicle) {
        Connection conn = null;
        PreparedStatement updateStmt = null;
        PreparedStatement deleteStmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Update vehicle availability
            String updateSql = "UPDATE vehicles SET is_available = true WHERE vehicle_id = ?";
            updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setString(1, vehicle.getVehicleId());
            int updated = updateStmt.executeUpdate();

            if (updated > 0) {
                // Delete rental record
                String deleteSql = "DELETE FROM rentals WHERE vehicle_id = ?";
                deleteStmt = conn.prepareStatement(deleteSql);
                deleteStmt.setString(1, vehicle.getVehicleId());
                deleteStmt.executeUpdate();

                conn.commit();
                vehicle.returnVehicle(); // Update the in-memory object state
                System.out.println("Vehicle returned successfully.");
            } else {
                System.out.println("Vehicle was not found or not rented.");
                conn.rollback();
            }
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
            }
            System.err.println("Error returning vehicle: " + e.getMessage());
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
            }
            closeResources(updateStmt, null);
            closeResources(deleteStmt, conn);
        }
    }

    // Updates the base price of a vehicle
    public void updateVehicle(String vehicleId, double newPrice) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE vehicles SET base_price_per_day = ? WHERE vehicle_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setDouble(1, newPrice);
            stmt.setString(2, vehicleId);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.println("Vehicle " + vehicleId + " updated successfully.");
            } else {
                System.out.println("Vehicle ID not found.");
            }
        } catch (SQLException e) {
            System.err.println("Error updating vehicle: " + e.getMessage());
        } finally {
            closeResources(stmt, conn);
        }
    }

    // Deletes a vehicle from the system
    public void deleteVehicle(String vehicleId) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseConnection.getConnection();

            // First check if vehicle is available (not rented)
            String checkSql = "SELECT is_available FROM vehicles WHERE vehicle_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, vehicleId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getBoolean("is_available")) {
                // Vehicle exists and is available, proceed with deletion
                String sql = "DELETE FROM vehicles WHERE vehicle_id = ?";
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, vehicleId);

                int deleted = stmt.executeUpdate();
                if (deleted > 0) {
                    System.out.println("Vehicle " + vehicleId + " deleted successfully.");
                } else {
                    System.out.println("Vehicle ID not found.");
                }
            } else {
                System.out.println("Vehicle not found or currently rented.");
            }

            rs.close();
            checkStmt.close();
        } catch (SQLException e) {
            System.err.println("Error deleting vehicle: " + e.getMessage());
        } finally {
            closeResources(stmt, conn);
        }
    }

    // Displays all vehicles in the system
    public void viewVehicles() {
        List<Vehicle> vehicles = getVehicles();
        if (vehicles.isEmpty()) {
            System.out.println("No vehicles available.");
        } else {
            System.out.println("\n== All Vehicles ==\n");
            for (Vehicle vehicle : vehicles) {
                System.out.println(vehicle);
            }
        }
    }

    // Displays all active rental records
    public void viewRentals() {
        List<Rental> rentals = getRentals();
        if (rentals.isEmpty()) {
            System.out.println("No active rentals.");
        } else {
            System.out.println("\n== Active Rentals ==\n");
            for (Rental rental : rentals) {
                System.out.println(rental);
            }
        }
    }

    // Authenticates admin login
    public boolean adminLogin(String username, String password) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean authenticated = false;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM admin WHERE username = ? AND password = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);

            rs = stmt.executeQuery();
            authenticated = rs.next(); // If there's a result, authentication is successful
        } catch (SQLException e) {
            System.err.println("Error authenticating admin: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                System.err.println("Error closing result set: " + e.getMessage());
            }
            closeResources(stmt, conn);
        }

        return authenticated;
    }

    // Allows admin to change their password
    public void changeAdminPassword(String newPassword) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE admin SET password = ? WHERE username = 'admin'";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, newPassword);

            stmt.executeUpdate();
            System.out.println("Admin password updated successfully.");
        } catch (SQLException e) {
            System.err.println("Error updating admin password: " + e.getMessage());
        } finally {
            closeResources(stmt, conn);
        }
    }

    // Updates customer password in the database
    public boolean updateCustomerPassword(String customerId, String newPassword) {
        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE customers SET password = ? WHERE customer_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, newPassword);
            stmt.setString(2, customerId);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.println("Customer password updated successfully.");
                success = true;
            } else {
                System.out.println("Customer ID not found.");
            }
        } catch (SQLException e) {
            System.err.println("Error updating customer password: " + e.getMessage());
        } finally {
            closeResources(stmt, conn);
        }

        return success;
    }

    // Getters for data from database
    public List<Vehicle> getVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM vehicles");

            while (rs.next()) {
                String id = rs.getString("vehicle_id");
                String type = rs.getString("type");
                String brand = rs.getString("brand");
                String model = rs.getString("model");
                double price = rs.getDouble("base_price_per_day");
                boolean isAvailable = rs.getBoolean("is_available");

                Vehicle vehicle = new Vehicle(id, type, brand, model, price);
                if (!isAvailable) {
                    vehicle.rent(); // Set as rented if not available
                }
                vehicles.add(vehicle);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving vehicles: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                System.err.println("Error closing result set: " + e.getMessage());
            }
            closeResources(stmt, conn);
        }

        return vehicles;
    }

    public List<Customer> getCustomers() {
        List<Customer> customers = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM customers");

            while (rs.next()) {
                String id = rs.getString("customer_id");
                String name = rs.getString("name");
                String password = rs.getString("password");

                customers.add(new Customer(id, name, password));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving customers: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                System.err.println("Error closing result set: " + e.getMessage());
            }
            closeResources(stmt, conn);
        }

        return customers;
    }

    public List<Rental> getRentals() {
        List<Rental> rentals = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            String sql = "SELECT r.*, v.type, v.brand, v.model, v.base_price_per_day, " +
                    "c.name, c.password FROM rentals r " +
                    "JOIN vehicles v ON r.vehicle_id = v.vehicle_id " +
                    "JOIN customers c ON r.customer_id = c.customer_id";
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                // Extract vehicle data
                String vehicleId = rs.getString("vehicle_id");
                String type = rs.getString("type");
                String brand = rs.getString("brand");
                String model = rs.getString("model");
                double price = rs.getDouble("base_price_per_day");

                // Extract customer data
                String customerId = rs.getString("customer_id");
                String name = rs.getString("name");
                String password = rs.getString("password");

                // Extract rental data
                int days = rs.getInt("days");

                // Create objects
                Vehicle vehicle = new Vehicle(vehicleId, type, brand, model, price);
                vehicle.rent(); // Set as rented
                Customer customer = new Customer(customerId, name, password);

                rentals.add(new Rental(vehicle, customer, days));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving rentals: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                System.err.println("Error closing result set: " + e.getMessage());
            }
            closeResources(stmt, conn);
        }

        return rentals;
    }

    public Admin getAdmin() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        Admin admin = null;

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM admin WHERE username = 'admin'");

            if (rs.next()) {
                String username = rs.getString("username");
                String password = rs.getString("password");
                admin = new Admin(username, password);
            } else {
                // Default admin if not found in database
                admin = new Admin("admin", "admin123");
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving admin: " + e.getMessage());
            // Default admin if error occurs
            admin = new Admin("admin", "admin123");
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                System.err.println("Error closing result set: " + e.getMessage());
            }
            closeResources(stmt, conn);
        }

        return admin;
    }
}

public class VehicleRentalSystemApp {
    private static VehicleRentalSystem rentalSystem;
    private static JFrame mainFrame;

    public static void main(String[] args) {
        rentalSystem = new VehicleRentalSystem();

        // Create and show the GUI
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }

    private static void createAndShowGUI() {
        mainFrame = new JFrame("Vehicle Rental System");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(800, 600);
        mainFrame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title label
        JLabel titleLabel = new JLabel("Vehicle Rental System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 150, 20, 150));

        JButton customerButton = new JButton("Customer Menu");
        customerButton.setFont(new Font("Arial", Font.PLAIN, 18));
        customerButton.addActionListener(e -> showCustomerMenu());

        JButton adminButton = new JButton("Admin Menu");
        adminButton.setFont(new Font("Arial", Font.PLAIN, 18));
        adminButton.addActionListener(e -> showAdminLogin());

        JButton exitButton = new JButton("Exit");
        exitButton.setFont(new Font("Arial", Font.PLAIN, 18));
        exitButton.addActionListener(e -> mainFrame.dispose());

        buttonPanel.add(customerButton);
        buttonPanel.add(adminButton);
        buttonPanel.add(exitButton);

        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainFrame.add(mainPanel);
        mainFrame.setVisible(true);
    }

    private static void showAdminLogin() {
        JPanel loginPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField();
        JLabel passLabel = new JLabel("Password:");
        JPasswordField passField = new JPasswordField();

        loginPanel.add(userLabel);
        loginPanel.add(userField);
        loginPanel.add(passLabel);
        loginPanel.add(passField);

        int result = JOptionPane.showConfirmDialog(mainFrame, loginPanel, "Admin Login",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String username = userField.getText();
            String password = new String(passField.getPassword());

            if (rentalSystem.adminLogin(username, password)) {
                showAdminMenu();
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Invalid admin credentials.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void showAdminMenu() {
        JFrame adminFrame = new JFrame("Admin Menu");
        adminFrame.setSize(600, 400);
        adminFrame.setLocationRelativeTo(mainFrame);

        JPanel adminPanel = new JPanel(new BorderLayout());
        adminPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Admin Menu", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        adminPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(7, 1, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 100, 20, 100));

        String[] buttonLabels = {
                "Add Vehicle", "View All Vehicles", "Update Vehicle Price",
                "Delete Vehicle", "View Rented Vehicles", "Change Admin Password", "Back"
        };

        for (String label : buttonLabels) {
            JButton button = new JButton(label);
            button.setFont(new Font("Arial", Font.PLAIN, 16));
            button.addActionListener(new AdminMenuListener(adminFrame));
            buttonPanel.add(button);
        }

        adminPanel.add(buttonPanel, BorderLayout.CENTER);
        adminFrame.add(adminPanel);
        adminFrame.setVisible(true);
    }

    private static class AdminMenuListener implements ActionListener {
        private JFrame adminFrame;

        public AdminMenuListener(JFrame adminFrame) {
            this.adminFrame = adminFrame;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();

            switch (command) {
                case "Add Vehicle":
                    showAddVehicleDialog();
                    break;
                case "View All Vehicles":
                    showAllVehicles();
                    break;
                case "Update Vehicle Price":
                    showUpdatePriceDialog();
                    break;
                case "Delete Vehicle":
                    showDeleteVehicleDialog();
                    break;
                case "View Rented Vehicles":
                    showRentedVehicles();
                    break;
                case "Change Admin Password":
                    showChangeAdminPasswordDialog();
                    break;
                case "Back":
                    adminFrame.dispose();
                    break;
            }
        }
    }

    private static void showAddVehicleDialog() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField idField = new JTextField();
        JTextField typeField = new JTextField();
        JTextField brandField = new JTextField();
        JTextField modelField = new JTextField();
        JTextField priceField = new JTextField();

        panel.add(new JLabel("Vehicle ID:"));
        panel.add(idField);
        panel.add(new JLabel("Type:"));
        panel.add(typeField);
        panel.add(new JLabel("Brand:"));
        panel.add(brandField);
        panel.add(new JLabel("Model:"));
        panel.add(modelField);
        panel.add(new JLabel("Base Price per Day:"));
        panel.add(priceField);

        int result = JOptionPane.showConfirmDialog(mainFrame, panel, "Add Vehicle",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String id = idField.getText();
                String type = typeField.getText();
                String brand = brandField.getText();
                String model = modelField.getText();
                double price = Double.parseDouble(priceField.getText());

                Vehicle newVehicle = new Vehicle(id, type, brand, model, price);
                rentalSystem.addVehicle(newVehicle);
                JOptionPane.showMessageDialog(mainFrame, "Vehicle added successfully!");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(mainFrame, "Invalid price format.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void showAllVehicles() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== All Vehicles ===\n\n");

        for (Vehicle vehicle : rentalSystem.getVehicles()) {
            sb.append(vehicle.toString()).append("\n");
        }

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(mainFrame, scrollPane, "All Vehicles", JOptionPane.PLAIN_MESSAGE);
    }

    private static void showUpdatePriceDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField idField = new JTextField();
        JTextField priceField = new JTextField();

        panel.add(new JLabel("Vehicle ID:"));
        panel.add(idField);
        panel.add(new JLabel("New Price:"));
        panel.add(priceField);

        int result = JOptionPane.showConfirmDialog(mainFrame, panel, "Update Vehicle Price",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String id = idField.getText();
                double newPrice = Double.parseDouble(priceField.getText());
                rentalSystem.updateVehicle(id, newPrice);
                JOptionPane.showMessageDialog(mainFrame, "Vehicle price updated successfully!");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(mainFrame, "Invalid price format.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void showDeleteVehicleDialog() {
        String id = JOptionPane.showInputDialog(mainFrame, "Enter Vehicle ID to delete:", "Delete Vehicle", JOptionPane.PLAIN_MESSAGE);
        if (id != null && !id.isEmpty()) {
            rentalSystem.deleteVehicle(id);
            JOptionPane.showMessageDialog(mainFrame, "Vehicle deletion processed.");
        }
    }

    private static void showRentedVehicles() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Rented Vehicles ===\n\n");

        for (Rental rental : rentalSystem.getRentals()) {
            sb.append(rental.toString()).append("\n");
        }

        if (sb.toString().equals("=== Rented Vehicles ===\n\n")) {
            sb.append("No vehicles currently rented.");
        }

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(mainFrame, scrollPane, "Rented Vehicles", JOptionPane.PLAIN_MESSAGE);
    }

    private static void showChangeAdminPasswordDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPasswordField currentPassField = new JPasswordField();
        JPasswordField newPassField = new JPasswordField();

        panel.add(new JLabel("Current Password:"));
        panel.add(currentPassField);
        panel.add(new JLabel("New Password:"));
        panel.add(newPassField);

        int result = JOptionPane.showConfirmDialog(mainFrame, panel, "Change Admin Password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String currentPass = new String(currentPassField.getPassword());
            String newPass = new String(newPassField.getPassword());

            if (rentalSystem.getAdmin().getPassword().equals(currentPass)) {
                rentalSystem.changeAdminPassword(newPass);
                JOptionPane.showMessageDialog(mainFrame, "Password changed successfully!");
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Incorrect current password.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void showCustomerMenu() {
        JFrame customerFrame = new JFrame("Customer Menu");
        customerFrame.setSize(600, 400);
        customerFrame.setLocationRelativeTo(mainFrame);

        JPanel customerPanel = new JPanel(new BorderLayout());
        customerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Customer Menu", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        customerPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 100, 20, 100));

        String[] buttonLabels = {
                "Rent a Vehicle", "Return a Vehicle", "Change Password", "Back"
        };

        for (String label : buttonLabels) {
            JButton button = new JButton(label);
            button.setFont(new Font("Arial", Font.PLAIN, 16));
            button.addActionListener(new CustomerMenuListener(customerFrame));
            buttonPanel.add(button);
        }

        customerPanel.add(buttonPanel, BorderLayout.CENTER);
        customerFrame.add(customerPanel);
        customerFrame.setVisible(true);
    }

    private static class CustomerMenuListener implements ActionListener {
        private JFrame customerFrame;

        public CustomerMenuListener(JFrame customerFrame) {
            this.customerFrame = customerFrame;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();

            switch (command) {
                case "Rent a Vehicle":
                    showRentVehicleDialog();
                    break;
                case "Return a Vehicle":
                    showReturnVehicleDialog();
                    break;
                case "Change Password":
                    showChangeCustomerPasswordDialog();
                    break;
                case "Back":
                    customerFrame.dispose();
                    break;
            }
        }
    }

    private static void showRentVehicleDialog() {
        // Step 1: Customer login/registration
        JPanel loginPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField nameField = new JTextField();
        JPasswordField passField = new JPasswordField();

        loginPanel.add(new JLabel("Name:"));
        loginPanel.add(nameField);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(passField);

        int loginResult = JOptionPane.showConfirmDialog(mainFrame, loginPanel, "Customer Login/Registration",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (loginResult != JOptionPane.OK_OPTION) {
            return;
        }

        String customerName = nameField.getText();
        String password = new String(passField.getPassword());
        Customer currentCustomer = null;

        // Find or create customer
        for (Customer c : rentalSystem.getCustomers()) {
            if (c.getName().equalsIgnoreCase(customerName) && c.getPassword().equals(password)) {
                currentCustomer = c;
                break;
            }
        }

        if (currentCustomer == null) {
            currentCustomer = new Customer("CUS" + (rentalSystem.getCustomers().size() + 1), customerName, password);
            rentalSystem.addCustomer(currentCustomer);
            JOptionPane.showMessageDialog(mainFrame,
                    "New customer registered with ID: " + currentCustomer.getCustomerId());
        } else {
            JOptionPane.showMessageDialog(mainFrame,
                    "Welcome back, " + currentCustomer.getName() + "!");
        }

        // Step 2: Show available vehicles
        List<Vehicle> availableVehicles = new ArrayList<>();
        for (Vehicle v : rentalSystem.getVehicles()) {
            if (v.isAvailable()) {
                availableVehicles.add(v);
            }
        }

        if (availableVehicles.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "No vehicles currently available for rent.",
                    "No Vehicles", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] vehicleOptions = new String[availableVehicles.size()];
        for (int i = 0; i < availableVehicles.size(); i++) {
            vehicleOptions[i] = availableVehicles.get(i).toString();
        }

        JComboBox<String> vehicleCombo = new JComboBox<>(vehicleOptions);
        JTextField daysField = new JTextField();

        JPanel rentPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        rentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        rentPanel.add(new JLabel("Select Vehicle:"));
        rentPanel.add(vehicleCombo);
        rentPanel.add(new JLabel("Rental Days:"));
        rentPanel.add(daysField);

        int rentResult = JOptionPane.showConfirmDialog(mainFrame, rentPanel, "Rent Vehicle",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (rentResult == JOptionPane.OK_OPTION) {
            try {
                int days = Integer.parseInt(daysField.getText());
                if (days <= 0) {
                    JOptionPane.showMessageDialog(mainFrame, "Rental days must be positive.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int selectedIndex = vehicleCombo.getSelectedIndex();
                Vehicle selectedVehicle = availableVehicles.get(selectedIndex);
                double totalPrice = selectedVehicle.calculatePrice(days);

                // Show confirmation
                String message = String.format(
                        "Rental Details:\n\n" +
                                "Customer: %s (%s)\n" +
                                "Vehicle: %s\n" +
                                "Days: %d\n" +
                                "Total Price: $%.2f\n\n" +
                                "Confirm rental?",
                        currentCustomer.getName(), currentCustomer.getCustomerId(),
                        selectedVehicle.toString(), days, totalPrice
                );

                int confirm = JOptionPane.showConfirmDialog(mainFrame, message,
                        "Confirm Rental", JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    rentalSystem.rentVehicle(selectedVehicle, currentCustomer, days);
                    JOptionPane.showMessageDialog(mainFrame, "Vehicle rented successfully!");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(mainFrame, "Invalid number of days.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void showReturnVehicleDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField customerIdField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JTextField vehicleIdField = new JTextField();

        panel.add(new JLabel("Customer ID:"));
        panel.add(customerIdField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);
        panel.add(new JLabel("Vehicle ID:"));
        panel.add(vehicleIdField);

        int result = JOptionPane.showConfirmDialog(mainFrame, panel, "Return Vehicle",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String customerId = customerIdField.getText();
            String password = new String(passField.getPassword());
            String vehicleId = vehicleIdField.getText();

            Customer customer = null;
            for (Customer c : rentalSystem.getCustomers()) {
                if (c.getCustomerId().equals(customerId) && c.getPassword().equals(password)) {
                    customer = c;
                    break;
                }
            }

            if (customer != null) {
                Vehicle vehicleToReturn = null;
                for (Vehicle v : rentalSystem.getVehicles()) {
                    if (v.getVehicleId().equals(vehicleId) && !v.isAvailable()) {
                        // Check if this customer rented this vehicle
                        for (Rental r : rentalSystem.getRentals()) {
                            if (r.getVehicle().getVehicleId().equals(vehicleId) &&
                                    r.getCustomer().getCustomerId().equals(customerId)) {
                                vehicleToReturn = v;
                                break;
                            }
                        }
                    }
                }

                if (vehicleToReturn != null) {
                    rentalSystem.returnVehicle(vehicleToReturn);
                    JOptionPane.showMessageDialog(mainFrame,
                            "Vehicle returned successfully by " + customer.getName());
                } else {
                    JOptionPane.showMessageDialog(mainFrame,
                            "Invalid vehicle ID, vehicle is not rented by you, or vehicle not found.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Invalid customer ID or password.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void showChangeCustomerPasswordDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField customerIdField = new JTextField();
        JPasswordField currentPassField = new JPasswordField();
        JPasswordField newPassField = new JPasswordField();

        panel.add(new JLabel("Customer ID:"));
        panel.add(customerIdField);
        panel.add(new JLabel("Current Password:"));
        panel.add(currentPassField);
        panel.add(new JLabel("New Password:"));
        panel.add(newPassField);

        int result = JOptionPane.showConfirmDialog(mainFrame, panel, "Change Password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String customerId = customerIdField.getText();
            String currentPass = new String(currentPassField.getPassword());
            String newPass = new String(newPassField.getPassword());

            Customer customer = null;
            for (Customer c : rentalSystem.getCustomers()) {
                if (c.getCustomerId().equals(customerId) && c.getPassword().equals(currentPass)) {
                    customer = c;
                    break;
                }
            }

            if (customer != null) {
                customer.setPassword(newPass);

                // Update password in the database
                boolean updated = rentalSystem.updateCustomerPassword(customerId, newPass);

                if (updated) {
                    JOptionPane.showMessageDialog(mainFrame,
                            "Password changed successfully for " + customer.getName());
                } else {
                    JOptionPane.showMessageDialog(mainFrame,
                            "Failed to update password in the database.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Invalid customer ID or password.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
