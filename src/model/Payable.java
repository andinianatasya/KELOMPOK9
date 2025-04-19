package model;

public interface Payable {
    double calculateTotal();
    void processTransaction();
    void serializeTransaction();
}
