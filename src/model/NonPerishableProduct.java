package model;

public class NonPerishableProduct extends Product {
    public NonPerishableProduct(String kode, String nama, double harga) {
        super(kode, nama, harga);
    }

    @Override
    public String getTipe() {
        return "Non-Perishable";
    }

    @Override
    public String getDetail() {
        return "Produk tahan lama";
    }
}
