package model;

import java.util.List;

public class BundleProduct extends Product {
    private List<Product> isiBundle;

    public BundleProduct(String kode, String nama, List<Product> isiBundle) {
        super(kode, nama, 0); // harga akan dihitung dari isi bundle
        this.isiBundle = isiBundle;
        updateHarga();
    }

    public void setIsiBundle(List<Product> isiBundle) {
        this.isiBundle = isiBundle;
        updateHarga(); // Update harga saat isi diganti
    }

    private void updateHarga() {
        double total = 0;
        for (Product p : isiBundle) {
            total += p.getHarga();
        }
        this.harga = total * 0.9; //diskon 10%
    }

    @Override
    public String getTipe() {
        return "Bundle";
    }

    @Override
    public String getDetail() {
        StringBuilder sb = new StringBuilder();
        sb.append("Isi bundle:\n");
        for (Product p : isiBundle) {
            sb.append("- ").append(p.getNama()).append(" (").append(p.getHargaFormatted()).append(")\n");
        }
        return sb.toString();
    }
}
