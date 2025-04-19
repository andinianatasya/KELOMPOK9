package model;

public abstract class Product {
    protected String kode;
    protected String nama;
    protected double harga;

    public Product(String kode, String nama, double harga) {
        this.kode = kode;
        this.nama = nama;
        this.harga = harga;
    }

    public String getKode() { return kode; }
    public String getNama() { return nama; }
    public double getHarga() { return harga; }

    public void setHarga(double harga) {
        this.harga = harga;
    }

    public String getHargaFormatted() {
        return String.format("Rp%,.2f", harga).replace('.', ',');
    }

    public abstract String getTipe();
    public abstract String getDetail();
}
