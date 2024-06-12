package jp.co.metateam.library.model;

import java.util.Date;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

/**
 * 在庫管理DTO
 */
@Getter
@Setter
public class CalendarDto{

    private String title;

    private String stockId;

    private Long stockNum;

    private Date expectedRentalOn;

    private Object rentalAvailable;

}