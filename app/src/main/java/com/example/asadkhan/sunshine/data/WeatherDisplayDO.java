package com.example.asadkhan.sunshine.data;

/**
 * Kreated by asadkhan on 08 | September |  2017 | at 12:37 PM.
 */

public class WeatherDisplayDO {
    // These are the values that will be collected.


    public WeatherDisplayDO() {
    }

    public WeatherDisplayDO(long dateTime, double pressure, int humidity, double windSpeed, double windDirection, double high, double low, String description, int weatherId) {
        this.dateTime = dateTime;
        this.pressure = pressure;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.high = high;
        this.low = low;
        this.description = description;
        this.weatherId = weatherId;
    }

    private long dateTime;
    private double pressure;
    private int humidity;
    private double windSpeed;
    private double windDirection;

    private double high;
    private double low;

    private String description;

    private int weatherId;


    public int getWeatherId() { return weatherId; }

    public void setWeatherId(int weatherId) { this.weatherId = weatherId; }

    public long getDateTime() { return dateTime; }

    public void setDateTime(long dateTime) { this.dateTime = dateTime; }

    public double getPressure() { return pressure; }

    public void setPressure(double pressure) { this.pressure = pressure; }

    public int getHumidity() { return humidity; }

    public void setHumidity(int humidity) { this.humidity = humidity; }

    public double getWindSpeed() { return windSpeed; }

    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }

    public void setWindDirection(double windDirection) { this.windDirection = windDirection; }

    public double getWindDirection() { return windDirection; }

    public double getHigh() { return high; }

    public void setHigh(double high) { this.high = high; }

    public double getLow() { return low; }

    public void setLow(double low) { this.low = low; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

}
