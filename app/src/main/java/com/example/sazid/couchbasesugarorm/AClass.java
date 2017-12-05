package com.example.sazid.couchbasesugarorm;

import com.orm.SugarRecord;

/**
 * Created by Sazid on 12/4/2017.
 */

public class AClass extends SugarRecord {
    private int a;

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }
}
