import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class AddProductForm extends JFrame {
    private JTextField nameField;
    private JTextField categoryField;
    private JTextField priceField;
    private JTextField quantityField;
    private JTextArea descriptionArea;
    private JButton saveButton;
    private JButton clearButton;
    private JButton cancelButton;
    
    public AddProductForm() {
        setTitle("Add New Product");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
        initComponents();
        setupLayout();
        addEventListeners();
    }
    
    private void initComponents() {
        nameField = new JTextField(20);
        categoryField = new JTextField(20);
        priceField = new JTextField(20);
        quantityField = new JTextField(20);
        descriptionArea = new JTextArea(4, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        
        saveButton = new JButton("Save Product");
        clearButton = new JButton("Clear");
        cancelButton = new JButton("Cancel");
        
        // Set tooltips
        nameField.setToolTipText("Enter product name");
        categoryField.setToolTipText("Enter product category");
        priceField.setToolTipText("Enter product price (e.g., 29.99)");
        quantityField.setToolTipText("Enter quantity in stock");
        descriptionArea.setToolTipText("Enter product description");
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Main form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Add form fields
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Product Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        formPanel.add(categoryField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Price ($):"), gbc);
        gbc.gridx = 1;
        formPanel.add(priceField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        formPanel.add(quantityField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        formPanel.add(new JScrollPane(descriptionArea), gbc);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(saveButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(cancelButton);
        
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void addEventListeners() {
        saveButton.addActionListener(e -> saveProduct());
        clearButton.addActionListener(e -> clearForm());
        cancelButton.addActionListener(e -> dispose());
        
        // Add Enter key listener to save button
        getRootPane().setDefaultButton(saveButton);
    }
    
    private void saveProduct() {
        // Validate input
        String name = nameField.getText().trim();
        String category = categoryField.getText().trim();
        String priceText = priceField.getText().trim();
        String quantityText = quantityField.getText().trim();
        String description = descriptionArea.getText().trim();
        
        // Validation
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Product name is required!", "Validation Error", JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return;
        }
        
        if (category.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Category is required!", "Validation Error", JOptionPane.ERROR_MESSAGE);
            categoryField.requestFocus();
            return;
        }
        
        double price;
        try {
            price = Double.parseDouble(priceText);
            if (price < 0) {
                JOptionPane.showMessageDialog(this, "Price must be positive!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                priceField.requestFocus();
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid price!", "Validation Error", JOptionPane.ERROR_MESSAGE);
            priceField.requestFocus();
            return;
        }
        
        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
            if (quantity < 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be positive!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                quantityField.requestFocus();
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid quantity!", "Validation Error", JOptionPane.ERROR_MESSAGE);
            quantityField.requestFocus();
            return;
        }
        
        // Save to database
        try (Connection conn = Database.getConnection()) {
            String sql = "INSERT INTO products (name, category, price, quantity, description) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setDouble(3, price);
            ps.setInt(4, quantity);
            ps.setString(5, description);
            
            int result = ps.executeUpdate();
            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Product added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add product!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    private void clearForm() {
        nameField.setText("");
        categoryField.setText("");
        priceField.setText("");
        quantityField.setText("");
        descriptionArea.setText("");
        nameField.requestFocus();
    }
    
    public static void main(String[] args) {
        // Test the form
        SwingUtilities.invokeLater(() -> {
            new AddProductForm().setVisible(true);
        });
    }
} 
