package model;

import java.time.LocalDateTime;
import java.util.UUID;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class Transaction {
    protected String transactionId;
    protected LocalDateTime date;
    protected double amountPaid;
    protected double change;

    public Transaction() {
        this.transactionId = UUID.randomUUID().toString().substring(0, 8);
        this.date = LocalDateTime.now();
        this.amountPaid = 0.0;
        this.change = 0.0;
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

    public double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public double getChange() {
        return change;
    }

    public void setChange(double change) {
        this.change = change;
    }

    public void calculateChange(double total) {
        this.change = this.amountPaid - total;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", date=" + date +
                ", amountPaid=" + amountPaid +
                ", change=" + change +
                '}';
    }

    protected Connection getConnection() throws SQLException {
        String url = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres";
        String user = "postgres.jnmxqxmrgwmmupkozavo";
        String password = "kelompok9";
        return DriverManager.getConnection(url, user, password);
    }
}
