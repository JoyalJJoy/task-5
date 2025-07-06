import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class DeleteProductForm extends JFrame {
    private JTable productTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JButton closeButton;
    private JTextField searchField;
    
    public DeleteProductForm() {
        setTitle("Delete Products");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initComponents();
        setupLayout();
        addEventListeners();
        loadProducts();
    }
    
    private void initComponents() {
        // Create table model with columns
        String[] columns = {"ID", "Name", "Category", "Price", "Quantity", "Description", "Delete"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only delete button is editable
            }
        };
        
        productTable = new JTable(tableModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.getTableHeader().setReorderingAllowed(false);
        
        // Set column widths
        productTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
        productTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Name
        productTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Category
        productTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Price
        productTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // Quantity
        productTable.getColumnModel().getColumn(5).setPreferredWidth(200); // Description
        productTable.getColumnModel().getColumn(6).setPreferredWidth(80);  // Delete
        
        // Set up delete button renderer and editor
        productTable.getColumn("Delete").setCellRenderer(new ButtonRenderer());
        productTable.getColumn("Delete").setCellEditor(new ButtonEditor(new JCheckBox(), this));
        
        searchField = new JTextField(20);
        searchField.setToolTipText("Search products by name or category");
        
        refreshButton = new JButton("Refresh");
        closeButton = new JButton("Close");
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(refreshButton);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(closeButton);
        
        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(productTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void addEventListeners() {
        refreshButton.addActionListener(e -> loadProducts());
        closeButton.addActionListener(e -> dispose());
        
        // Add search functionality
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterProducts();
            }
        });
    }
    
    private void loadProducts() {
        tableModel.setRowCount(0); // Clear table
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM products ORDER BY name")) {
            
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    String.format("$%.2f", rs.getDouble("price")),
                    rs.getInt("quantity"),
                    rs.getString("description"),
                    "Delete"
                };
                tableModel.addRow(row);
            }
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading products: " + ex.getMessage(), 
                                        "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    private void filterProducts() {
        String searchText = searchField.getText().toLowerCase().trim();
        
        if (searchText.isEmpty()) {
            loadProducts();
            return;
        }
        
        tableModel.setRowCount(0); // Clear table
        
        try (Connection conn = Database.getConnection()) {
            String sql = "SELECT * FROM products WHERE LOWER(name) LIKE ? OR LOWER(category) LIKE ? ORDER BY name";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, "%" + searchText + "%");
            ps.setString(2, "%" + searchText + "%");
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    String.format("$%.2f", rs.getDouble("price")),
                    rs.getInt("quantity"),
                    rs.getString("description"),
                    "Delete"
                };
                tableModel.addRow(row);
            }
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error filtering products: " + ex.getMessage(), 
                                        "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    public void deleteProduct(int productId) {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete this product?\n\nThis action cannot be undone.", 
            "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = Database.getConnection()) {
                String sql = "DELETE FROM products WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, productId);
                
                int result = ps.executeUpdate();
                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "Product deleted successfully!", 
                                                "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadProducts(); // Refresh the table
                } else {
                    JOptionPane.showMessageDialog(this, "Product not found or already deleted.", 
                                                "Error", JOptionPane.ERROR_MESSAGE);
                }
                
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error deleting product: " + ex.getMessage(), 
                                            "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        // Test the form
        SwingUtilities.invokeLater(() -> {
            new DeleteProductForm().setVisible(true);
        });
    }
}

// Button renderer for the delete button
class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
    public ButtonRenderer() {
        setOpaque(true);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                                                 boolean hasFocus, int row, int column) {
        setText((value == null) ? "" : value.toString());
        return this;
    }
}

// Button editor for the delete button
class ButtonEditor extends DefaultCellEditor {
    protected JButton button;
    private int productId;
    private boolean isPushed;
    private DeleteProductForm parentForm;
    
    public ButtonEditor(JCheckBox checkBox, DeleteProductForm parentForm) {
        super(checkBox);
        this.parentForm = parentForm;
        button = new JButton();
        button.setOpaque(true);
        button.addActionListener(e -> fireEditingStopped());
    }
    
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, 
                                               int row, int column) {
        productId = (int) table.getValueAt(row, 0); // Get the product ID from the first column
        button.setText((value == null) ? "" : value.toString());
        isPushed = true;
        return button;
    }
    
    @Override
    public Object getCellEditorValue() {
        if (isPushed) {
            parentForm.deleteProduct(productId);
        }
        isPushed = false;
        return "Delete";
    }
} 
