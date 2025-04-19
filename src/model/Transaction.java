package model;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class Transaction {
    protected String transactionId;
    protected LocalDateTime date;

    public Transaction() {
        this.transactionId = UUID.randomUUID().toString().substring(0, 8);
        this.date = LocalDateTime.now();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", date=" + date +
                '}';
    }
}
