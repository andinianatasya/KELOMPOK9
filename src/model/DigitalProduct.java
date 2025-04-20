package model;

import java.net.URL;


public class DigitalProduct extends Product {
    private URL url;
    private String vendor;

    public DigitalProduct(String kode, String nama, double harga, URL url, String vendor) {
        super(kode, nama, harga);
        this.url = url;
        this.vendor = vendor;
    }

    @Override
    public String getTipe() {
        return "Digital";
    }

    @Override
    public String getDetail() {
        return "Vendor: " + vendor + ", URL: " + url.toString();
    }

    public String getvendor(){ return vendor;}
    public String getURL(){return url.toString();}
}
