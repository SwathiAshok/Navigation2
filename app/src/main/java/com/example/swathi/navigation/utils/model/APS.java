package com.example.swathi.navigation.utils.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class APS
{
    @Expose
    public String yco;
    @Expose
    public String xco;

    public String getXco() {
        return xco;
    }

    public String getYco() {
        return yco;
    }
}
