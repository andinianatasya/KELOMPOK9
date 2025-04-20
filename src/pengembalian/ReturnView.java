//package pengembalian;
//
//import pengembalian.ReturnController;
//import model.*;
//
//import javax.swing.*;
//import javax.swing.table.DefaultTableModel;
//import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.util.*;
//import java.util.List;
//
//public class ReturnView extends JFrame {
//    private JTextField txtTransactionId;
//    private JButton btnSearch;
//    private JTable tblItems;
//    private DefaultTableModel tableModel;
//    private JTextField txtReason;
//    private JButton btnProcess;
//    private JButton btnCancel;
//
//    private ReturnController controller;
//    private User currentUser;
//    private Map<String, Object> originalTransaction;
//    private List<ReturnController.ReturnItem> itemsToReturn;
//
//    public ReturnView(User currentUser) {
//        this.currentUser = currentUser;
//        this.controller = new ReturnController();
//        this.itemsToReturn = new ArrayList<>();
//
//        initComponents();
//        setupListeners();
//    }
//
//    private void initComponents() {
//        setTitle("Proses Pengembalian Produk");
//        setSize(800, 600);
//        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//        setLocationRelativeTo(null);
//
//        JPanel mainPanel = new JPanel(new BorderLayout());
//
//        // Panel pencarian transaksi
//        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
//        JLabel lblTransactionId = new JLabel("ID Transaksi:");
//        txtTransactionId = new JTextField(20);
//        btnSearch = new JButton("Cari");
//
//        searchPanel.add(lblTransactionId);
//        searchPanel.add(txtTransactionId);
//        searchPanel.add(btnSearch);
//
//        // Tabel untuk produk
//        String[] columns = {"Kode", "Nama Produk", "Harga", "Jumlah Beli", "Jumlah Return", "Subtotal"};
//        tableModel = new DefaultTableModel(columns, 0) {
//            @Override
//            public boolean isCellEditable(int row, int column) {
//                // Hanya allow edit di kolom Jumlah Return (index 4)
//                return column == 4;
//            }
//        };
//
//        tblItems = new JTable(tableModel);
//        JScrollPane scrollPane = new JScrollPane(tblItems);
//
//        // Panel bawah untuk alasan dan tombol
//        JPanel bottomPanel = new JPanel(new BorderLayout());
//
//        JPanel reasonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
//        JLabel lblReason = new JLabel("Alasan Return:");
//        txtReason = new JTextField(40);
//        reasonPanel.add(lblReason);
//        reasonPanel.add(txtReason);
//
//        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//        btnProcess = new JButton("Proses Return");
//        btnCancel = new JButton("Batal");
//        buttonPanel.add(btnProcess);
//        buttonPanel.add(btnCancel);
//
//        bottomPanel.add(reasonPanel, BorderLayout.NORTH);
//        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
//
//        mainPanel.add(searchPanel, BorderLayout.NORTH);
//        mainPanel.add(scrollPane, BorderLayout.CENTER);
//        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
//
//        add(mainPanel);
//    }
//
//    private void setupListeners() {
//        btnSearch.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                searchTransaction();
//            }
//        });
//
//        btnProcess.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                processReturn();
//            }
//        });
//
//        btnCancel.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                dispose();
//            }
//        });
//    }
//
//    private void searchTransaction() {
//        String transactionId = txtTransactionId.getText().trim();
//        if (transactionId.isEmpty()) {
//            JOptionPane.showMessageDialog(this, "Masukkan ID transaksi yang valid", "Error", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//
//        originalTransaction = controller.findTransactionById(transactionId);
//        if (originalTransaction.get("transaction") == null) {
//            JOptionPane.showMessageDialog(this, "Transaksi tidak ditemukan", "Error", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//
//        displayTransactionItems();
//    }
//
//    private void displayTransactionItems() {
//        // Bersihkan tabel
//        tableModel.setRowCount(0);
//
//        @SuppressWarnings("unchecked")
//        List<Map<String, Object>> items = (List<Map<String, Object>>) originalTransaction.get("items");
//
//        for (Map<String, Object> item : items) {
//            String code = (String) item.get("product_code");
//            String name = (String) item.get("product_name");
//            double price = (double) item.get("price");
//            int quantity = (int) item.get("quantity");
//            double subtotal = price * quantity;
//
//            // Tambahkan ke tabel dengan jumlah return awal 0
//            tableModel.addRow(new Object[]{
//                    code, name, price, quantity, 0, subtotal
//            });
//        }
//    }
//
//    private void processReturn() {
//        if (originalTransaction == null || originalTransaction.get("transaction") == null) {
//            JOptionPane.showMessageDialog(this, "Pilih transaksi terlebih dahulu", "Error", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//
//        String reason = txtReason.getText().trim();
//        if (reason.isEmpty()) {
//            JOptionPane.showMessageDialog(this, "Masukkan alasan pengembalian", "Error", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//
//        // Kumpulkan item yang akan di-return
//        itemsToReturn.clear();
//        boolean anyItemSelected = false;
//
//        for (int i = 0; i < tableModel.getRowCount(); i++) {
//            String code = (String) tableModel.getValueAt(i, 0);
//            int originalQty = (int) tableModel.getValueAt(i, 3);
//
//            // Get return quantity (user input)
//            Object returnQtyObj = tableModel.getValueAt(i, 4);
//            int returnQty = 0;
//
//            if (returnQtyObj instanceof Integer) {
//                returnQty = (Integer) returnQtyObj;
//            } else if (returnQtyObj instanceof String) {
//                try {
//                    returnQty = Integer.parseInt((String) returnQtyObj);
//                } catch (NumberFormatException e) {
//                    JOptionPane.showMessageDialog(this,
//                            "Jumlah return untuk produk " + code + " tidak valid",
//                            "Error", JOptionPane.ERROR_MESSAGE);
//                    return;
//                }
//            }
//
//            // Validasi jumlah return
//            if (returnQty < 0) {
//                JOptionPane.showMessageDialog(this,
//                        "Jumlah return untuk produk " + code + " tidak boleh negatif",
//                        "Error", JOptionPane.ERROR_MESSAGE);
//                return;
//            }
//
//            if (returnQty > originalQty) {
//                JOptionPane.showMessageDialog(this,
//                        "Jumlah return untuk produk " + code + " melebihi jumlah pembelian",
//                        "Error", JOptionPane.ERROR_MESSAGE);
//                return;
//            }
//
//            if (returnQty > 0) {
//                itemsToReturn.add(new ReturnController.ReturnItem(code, returnQty, reason));
//                anyItemSelected = true;
//            }
//        }
//
//        if (!anyItemSelected) {
//            JOptionPane.showMessageDialog(this,
//                    "Pilih setidaknya satu produk untuk dikembalikan",
//                    "Error", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//
//        // Konfirmasi
//        int confirm = JOptionPane.showConfirmDialog(this,
//                "Anda yakin akan memproses pengembalian produk ini?",
//                "Konfirmasi", JOptionPane.YES_NO_OPTION);
//
//        if (confirm == JOptionPane.YES_OPTION) {
//            Map<String, Object> transactionData = (Map<String, Object>) originalTransaction.get("transaction");
//            String originalTransactionId = (String) transactionData.get("transaction_id");
//            int userId = currentUser.getId();
//            String username = currentUser.getUsername();
//
//            boolean success = controller.processReturn(
//                    originalTransactionId, userId, username, reason, itemsToReturn
//            );
//
//            if (success) {
//                JOptionPane.showMessageDialog(this,
//                        "Proses pengembalian berhasil",
//                        "Sukses", JOptionPane.INFORMATION_MESSAGE);
//                dispose();
//            } else {
//                JOptionPane.showMessageDialog(this,
//                        "Gagal memproses pengembalian",
//                        "Error", JOptionPane.ERROR_MESSAGE);
//            }
//        }
//    }
//
//    public static void main(String[] args) {
//    }
//}

//masih erorr
