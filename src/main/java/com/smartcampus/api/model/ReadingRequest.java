package com.smartcampus.api.model;

public class ReadingRequest {
    private double value;

    public ReadingRequest() {
    }

    public ReadingRequest(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
