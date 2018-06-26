package io.snabble.sdk.codes;

import java.io.Serializable;

import io.snabble.sdk.SnabbleSdk;

public class ScannableCode implements Serializable {
    private String code;

    public ScannableCode(String code){
        this.code = code;
    }

    public String getCode(){
        return code;
    }

    public String getLookupCode() {
        return "";
    }

    public int getEmbeddedData() {
        return 0;
    }

    public boolean hasAmountData() {
        return false;
    }

    public boolean hasPriceData() {
        return false;
    }

    public boolean hasWeighData() {
        return false;
    }

    public boolean hasEmbeddedData(){
        return false;
    }

    public boolean isEmbeddedDataOk() {
        return true;
    }

    public static ScannableCode parse(SnabbleSdk snabbleSdk, String code){
        if(EAN13.isEan13(code)){
            return new EAN13(code, snabbleSdk);
        } else {
            return new ScannableCode(code);
        }
    }
}
