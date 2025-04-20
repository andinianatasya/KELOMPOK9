package model;

import java.time.LocalDate;

public class PerishableProduct extends Product {
    private LocalDate tanggal_kadaluarsa;

    public PerishableProduct(String kode, String nama, double harga, LocalDate tanggal_kadaluarsa) {
        super(kode, nama, harga);
        this.tanggal_kadaluarsa = tanggal_kadaluarsa;
    }

    @Override
    public String getTipe() {
        return "Perishable";
    }

    @Override
    public String getDetail() {
        return "Kadaluarsa: " + tanggal_kadaluarsa.toString();
    }
}
