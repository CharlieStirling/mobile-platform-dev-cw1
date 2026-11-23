package com.example.stirling_charlie_s2530655;

import androidx.annotation.NonNull;

import com.google.android.material.timepicker.TimeFormat;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.time.*;
import java.util.Objects;

public class TrafficEvent {
    // identification
    private String id;
    private String situationType;
    private String source;

    // classification
    private String impactType;
    private String lanesRestricted;
    private String lanesOperational;
    private String generalPublicComment;
    private String compliance;

    // where
    private String direction;
    private double fromLat;
    private double fromLon;
    private ArrayList<String> fromDescriptor = new ArrayList<>();
    private double toLat;
    private double toLon;
    private ArrayList<String> toDescriptor = new ArrayList<>();

    // when
    private OffsetDateTime start;
    private OffsetDateTime end;

    //
    // roadworks specific tags
    //
    private String delaysType;

    private boolean urgentRoadworks;


    //
    // constructor
    //
    public TrafficEvent() {
    }

    //
    // getters and setters
    //
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSituationType() {
        return situationType;
    }

    public void setSituationType(String situationType) {
        this.situationType = situationType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setImpactType(String impactType) {
        this.impactType = impactType;
    }

    public String getCompliance() {
        return compliance;
    }

    public void setCompliance(String compliance) {
        this.compliance = compliance;
    }

    public String getLanesRestricted() {
        return lanesRestricted;
    }

    public void setLanesRestricted(String lanesRestricted) {
        this.lanesRestricted = lanesRestricted;
    }

    public String getLanesOperational() {
        return lanesOperational;
    }


    public void setLanesOperational(String lanesOperational) {
        this.lanesOperational = lanesOperational;
    }

    public String getGeneralPublicComment() {
        return generalPublicComment;
    }

    public void setGeneralPublicComment(String generalPublicComment) {
        this.generalPublicComment = generalPublicComment;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public double getFromLat() {
        return fromLat;
    }

    public void setFromLat(String fromLat) {
        this.fromLat = parseToDouble(fromLat);
    }

    public double getFromLon() {
        return fromLon;
    }

    public void setFromLon(String fromLon) {
        this.fromLon = parseToDouble(fromLon);
    }

    public double getToLat() {
        return toLat;
    }

    public void setToLat(String toLat) {
        this.toLat = parseToDouble(toLat);
    }

    public double getToLon() {
        return toLon;
    }

    public void setToLon(String toLon) {
        this.toLon = parseToDouble(toLon);
    }

    public OffsetDateTime getStart() {
        return start;
    }


    public void setStart(String start) {
        this.start = parseTimecode(start);
    }

    public ArrayList<String> getFromDescriptor() {
        return fromDescriptor;
    }

    public void setFromDescriptor(String fromDescriptor) {
        this.fromDescriptor.add(fromDescriptor);
    }

    public ArrayList<String> getToDescriptor() {
        return toDescriptor;
    }

    public void setToDescriptor(String toDescriptor) {
        this.toDescriptor.add(toDescriptor);
    }

    public void setStart(OffsetDateTime start) {
        this.start = start;
    }

    public OffsetDateTime getEnd() {
        return end;
    }

    public void setEnd(String s) {
        this.end = parseTimecode(s);
    }

    public String getDelaysType() {
        return delaysType;
    }

    public void setDelaysType(String delaysType) {
        this.delaysType = delaysType;
    }

    public boolean isUrgentRoadworks() {
        return urgentRoadworks;
    }

    public void setUrgentRoadworks(boolean urgentRoadworks) {
        this.urgentRoadworks = urgentRoadworks;
    }

    //
    // getters for displaying
    //

    // get better looking text for impact type display
    public String getImpactType() {
        if ("roadclosed".equalsIgnoreCase(impactType)) {
            return "Road Closure";
        } else if ("queuingtraffic".equalsIgnoreCase(impactType)) {
            return "Queuing Traffic";
        } else if ("roadworks".equalsIgnoreCase(impactType)) {
            return "Road Works";
        } else {
            return "Information";
        }
    }

    // get text for the background of events
    public String getBgText() {
        if ("roadclosed".equalsIgnoreCase(impactType)) {
            return "Closure";
        } else if ("queuingtraffic".equalsIgnoreCase(impactType)) {
            return "Traffic";
        } else if ("roadworks".equalsIgnoreCase(impactType)) {
            return "Works";
        } else {
            return "Info";
        }
    }

    // returns date & time in the user's timezone as a string
    public String getLocalisedDateTime(OffsetDateTime dt) {
        return DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .format(dt.atZoneSameInstant(ZoneId.systemDefault()));
    }

    // chooses lanes restricted or operational depending on which is not null
    public String getLanes() {
        if (lanesOperational != null) {
            return "Lanes operational: " + lanesOperational;
        } else if (lanesRestricted != null) {
            return "Lanes restricted: " + lanesRestricted;
        } else {
            return "All lanes operational";
        }
    }

    //
    // helper functions
    //

    private double parseToDouble(String s) {
        return Double.parseDouble(s);
    }

    private OffsetDateTime parseTimecode(String s) {
        return OffsetDateTime.parse(s);
    }

    //
    // to string
    //

    @Override
    public String toString() {
        return "TrafficEvent{" +
                "id='" + id + '\'' +
                ", situationType='" + situationType + '\'' +
                ", source='" + source + '\'' +
                ", impactType='" + impactType + '\'' +
                ", lanesRestricted='" + lanesRestricted + '\'' +
                ", lanesOperational='" + lanesOperational + '\'' +
                ", generalPublicComment='" + generalPublicComment + '\'' +
                ", compliance='" + compliance + '\'' +
                ", direction='" + direction + '\'' +
                ", fromLat=" + fromLat +
                ", fromLon=" + fromLon +
                ", fromDescriptor=" + fromDescriptor +
                ", toLat=" + toLat +
                ", toLon=" + toLon +
                ", toDescriptor=" + toDescriptor +
                ", start='" + start + '\'' +
                '}';
    }
}
