package model;

import java.util.List;
import java.util.stream.Collectors;

public class BundleProduct extends Product {
    private List<Product> isiBundle;

    public BundleProduct(String kode, String nama, List<Product> isiBundle) {
        super(kode, nama, 0); // Harga awal diset 0, akan dihitung ulang
        this.isiBundle = isiBundle;
        calculatePrice();
    }

    private void calculatePrice() {
        double totalHarga = 0;
        for (Product p : isiBundle) {
            totalHarga += p.getHarga();
        }
        // Berikan diskon 10% untuk bundle
        this.setHarga(totalHarga * 0.9);
    }

    public List<Product> getIsiBundle() {
        return isiBundle;
    }

    public void setIsiBundle(List<Product> isiBundle) {
        this.isiBundle = isiBundle;
        calculatePrice();
    }

    public String getTipe() {
        return "Bundle";
    }

    @Override
    public String getDetail() {
        String isiList = isiBundle.stream()
                .map(p -> p.getNama())
                .collect(Collectors.joining(", "));
        return "Bundle berisi: " + isiList;
    }
}