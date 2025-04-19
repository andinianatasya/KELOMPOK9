package model;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.net.URL;

public class DataProduk {
    private static final List<Product> produkList = new ArrayList<>();

    public static void loadProdukFromDatabase(Connection conn) throws Exception {
        produkList.clear();

        // Map kode ke produk (untuk referensi bundle)
        Map<String, Product> produkMap = new HashMap<>();

        // 1. Ambil semua produk dari tabel 'produk'
        String queryProduk = "SELECT * FROM produk";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(queryProduk)) {
            while (rs.next()) {
                String kode = rs.getString("kode");
                String nama = rs.getString("nama");
                double harga = rs.getDouble("harga");
                String tipe = rs.getString("tipe");

                Product p = null;

                switch (tipe) {
                    case "Non-Perishable":
                        p = new NonPerishableProduct(kode, nama, harga);
                        break;

                    case "Perishable":
                        try (PreparedStatement psPerish = conn.prepareStatement(
                                "SELECT tanggal_kadaluarsa FROM perishable_produk WHERE kode = ?")) {
                            psPerish.setString(1, kode);
                            try (ResultSet rsPerish = psPerish.executeQuery()) {
                                if (rsPerish.next()) {
                                    LocalDate tgl = rsPerish.getDate("tanggal_kadaluarsa").toLocalDate();
                                    p = new PerishableProduct(kode, nama, harga, tgl);
                                }
                            }
                        }
                        break;

                    case "Digital":
                        try (PreparedStatement psDigital = conn.prepareStatement(
                                "SELECT url, vendor FROM digital_produk WHERE kode = ?")) {
                            psDigital.setString(1, kode);
                            try (ResultSet rsDigital = psDigital.executeQuery()) {
                                if (rsDigital.next()) {
                                    URL url = new URL(rsDigital.getString("url"));
                                    String vendor = rsDigital.getString("vendor");
                                    p = new DigitalProduct(kode, nama, harga, url, vendor);
                                }
                            }
                        }
                        break;

                    case "Bundle":
                        p = new BundleProduct(kode, nama, new ArrayList<>());
                        break;
                }

                if (p != null) {
                    produkList.add(p);
                    produkMap.put(kode, p);
                }
            }
        }

        // 2. Isi bundle dari tabel 'bundle_isi'
        for (Product p : produkList) {
            if (p instanceof BundleProduct) {
                BundleProduct bundle = (BundleProduct) p;

                try (PreparedStatement psBundle = conn.prepareStatement(
                        "SELECT kode_produk FROM bundle_isi WHERE kode_bundle = ?")) {
                    psBundle.setString(1, bundle.getKode());
                    try (ResultSet rsBundle = psBundle.executeQuery()) {
                        List<Product> isi = new ArrayList<>();
                        while (rsBundle.next()) {
                            String kodeIsi = rsBundle.getString("kode_produk");
                            if (produkMap.containsKey(kodeIsi)) {
                                isi.add(produkMap.get(kodeIsi));
                            }
                        }
                        bundle.setIsiBundle(isi);
                    }
                }
            }
        }
    }

    public static List<Product> getProdukList() {
        return produkList;
    }


}
