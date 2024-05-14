package jp.co.metateam.library.model;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jp.co.metateam.library.values.RentalStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 貸出管理DTO
 */
@Getter
@Setter
public class RentalManageDto {

    private Long id;

    @NotEmpty(message="在庫管理番号は必須です")
    private String stockId;

    @NotEmpty(message="社員番号は必須です")
    private String employeeId;

    @NotNull(message="貸出ステータスは必須です")
    private Integer status;

    @DateTimeFormat(pattern="yyyy-MM-dd")
    @NotNull(message="貸出予定日は必須です")
    private Date expectedRentalOn;

    @DateTimeFormat(pattern="yyyy-MM-dd")
    @NotNull(message="返却予定日は必須です")
    private Date expectedReturnOn;

    private Timestamp rentaledAt;

    private Timestamp returnedAt;

    private Timestamp canceledAt;

    private Stock stock;

    private Account account;
    

   public Optional<String> validateStatus(Integer previousRentalStatus){

    //ステータスが貸出待ちから返却済みの場合のエラー
    if (previousRentalStatus == RentalStatus.RENT_WAIT.getValue() && previousRentalStatus != this.status) {
        if(this.status == RentalStatus.RETURNED.getValue())//変更先が返却済みだったら
        {
            return Optional.of("貸出待ちの本は返却済みに変更できません。");
        }
    }
    //ステータスが貸出中から返却済み以外はエラー
    else if (previousRentalStatus == RentalStatus.RENTAlING.getValue() && previousRentalStatus != this.status ) {
        if(this.status != RentalStatus.RETURNED.getValue())
        {
        return Optional.of("貸出中の本は"+ RentalStatus.getTextFormValue(this.status) +"に変更できません。");
        }
    }
    // キャンセルのステータスを変更する場合にエラー
    else if (previousRentalStatus == RentalStatus.CANCELED.getValue() && this.status != RentalStatus.CANCELED.getValue()) {
        return Optional.of("キャンセルのステータスを変更することはできません。"); 
    } 
    // 返却済みのステータスを変更する場合にエラー
    else if (previousRentalStatus == RentalStatus.RETURNED.getValue() && this.status != RentalStatus.RETURNED.getValue()) {
        return Optional.of("返却済みのステータスを変更することはできません。");
    }

    return Optional.empty(); // バリデーションが成功した場合
}  
}


