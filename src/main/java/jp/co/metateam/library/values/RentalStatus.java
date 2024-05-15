package jp.co.metateam.library.values;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RentalStatus implements Values {
    RENT_WAIT(0, "貸出待ち")
    , RENTAlING(1, "貸出中")
    , RETURNED(2, "返却済み")
    , CANCELED(3, "キャンセル");

    private final Integer value;
    private final String text; 
    
    public static String getTextFormValue(Integer value){
        for (RentalStatus status : RentalStatus.values()) {
            if (status.getValue().equals(value)) {
                return status.getText();
            }
        }
        return "error";
    }
}
