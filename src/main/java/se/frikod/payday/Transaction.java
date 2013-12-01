package se.frikod.payday;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;

public class Transaction{
    public String date_string;
    public BigDecimal amount;
    public String currency;
    public String description;
    public DateTime date;

    public Transaction(String date, BigDecimal amount, String currency, String description){
        this.date_string = date;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");
        this.date = format.parseDateTime(date_string);
    }
}
