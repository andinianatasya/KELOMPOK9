package model;

import java.time.LocalDate;

public class PerishableProduct extends Product {
    private LocalDate expiryDate;

    public PerishableProduct(String kode, String nama, double harga, LocalDate expiryDate) {
        super(kode, nama, harga);
        this.expiryDate = expiryDate;
    }

    @Override
    public String getTipe() {
        return "Perishable";
    }

    @Override
    public String getDetail() {
        return "Kadaluarsa: " + expiryDate.toString();
    }
}
