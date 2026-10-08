package com.example.studentlifeos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One internship listing, as stored in assets/jobs.json. Plain data holder. */
public class Job {

    public String id;
    public String title;
    public String company;
    /** Coarse role bucket: Backend, Frontend, Full Stack, Web, Data & AI, Mobile, ... */
    public String role;
    public boolean remote;
    /** "Work from home" for remote listings, otherwise a city name. */
    public String location;
    public boolean partTime;
    /** Canonical skill names (already normalised). Never empty for listings in the asset. */
    public List<String> skills = new ArrayList<>();
    public String description;
    /** Link to the original listing. */
    public String url;

    /** Original text such as "₹ 15,000-20,000 /month", with spaces tidied. */
    public String stipendText;
    /** Monthly rupee range, or null when the stipend isn't a monthly rupee amount. */
    public Integer stipendMin;
    public Integer stipendMax;
    /** "month", "week", "lump", "unpaid" or null. */
    public String stipendPeriod;
    public boolean incentives;

    /** Short stipend for list rows, e.g. "₹10,000/mo" or "₹15,000–20,000/mo". */
    public String stipendShort() {
        if ("unpaid".equals(stipendPeriod)) return "Unpaid";
        if (stipendMin != null && stipendMax != null) {
            String range = stipendMin.equals(stipendMax)
                    ? String.format(Locale.US, "₹%,d", stipendMin)
                    : String.format(Locale.US, "₹%,d–%,d", stipendMin, stipendMax);
            return range + "/mo";
        }
        return stipendText != null ? stipendText : "";
    }

    /** True when the monthly stipend is known and at least {@code minRupees} at its upper end. */
    public boolean paysAtLeast(int minRupees) {
        return stipendMax != null && stipendMax >= minRupees;
    }

    /** "Remote", "Remote · part time", or the city. */
    public String locationLabel() {
        if (remote) return partTime ? "Remote · part time" : "Remote";
        return location != null ? location : "Not specified";
    }
}