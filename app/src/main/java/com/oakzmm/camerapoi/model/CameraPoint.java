package com.oakzmm.camerapoi.model;

import android.graphics.Point;

import com.amap.api.services.core.LatLonPoint;

/**
 * DevApplication
 * Created by acer_april
 * on 2015/12/8
 * Description: TODO
 */
public class CameraPoint {
    private String title;
    private LatLonPoint latLonPoint;
    private double distance;
    private int angle;
    private String url;
    private Point point;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LatLonPoint getLatLonPoint() {
        return latLonPoint;
    }

    public void setLatLonPoint(LatLonPoint latLonPoint) {
        this.latLonPoint = latLonPoint;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    @Override
    public String toString() {
        return "CameraPoint{" +
                "title='" + title + '\'' +
                ", latLonPoint=" + latLonPoint +
                ", distance=" + distance +
                ", angle=" + angle +
                ", url='" + url + '\'' +
                ", point=" + point +
                '}';
    }
}
