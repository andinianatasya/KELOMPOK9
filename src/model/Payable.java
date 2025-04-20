package model;

public interface Payable {
    double calculateTotal();
    void processTransaction();
    boolean serializeTransaction();
}
