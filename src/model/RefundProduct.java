package model;

public class RefundProduct extends Product {
    public RefundProduct(String kode, String nama, double harga) {
        super(kode, nama, harga);
    }

    @Override
    public String getTipe() {
        return "Refund";
    }


    @Override
    public String getDetail() {
        return "Produk refund: " + nama;
    }
}
