package com.rocki.web.vo;

import java.io.Serializable;


public class SignupVo implements Serializable {
    private static final long serialVersionUID = 7545079302381881777L;

    private String date;
    private String count;
    private String increase;
    private String rate;

    public SignupVo(String date, String count, String increase, String rate) {
        this.date = date;
        this.count = count;
        this.increase = increase;
        this.rate = rate;
    }

    public String getDate() {
        return date;
    }

    public String getCount() {
        return count;
    }

    public String getIncrease() {
        return increase;
    }

    public String getRate() {
        return rate;
    }

    @Override
    public String toString() {
        return "SignupVo{" +
                "date='" + date + '\'' +
                ", count='" + count + '\'' +
                ", increase='" + increase + '\'' +
                ", rate='" + rate + '\'' +
                '}';
    }
}
