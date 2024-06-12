package jp.co.metateam.library.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Optional;
import java.util.Date;
import java.util.Calendar;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.co.metateam.library.constants.Constants;
import jp.co.metateam.library.model.BookMst;
import jp.co.metateam.library.model.Stock;
import jp.co.metateam.library.model.StockDto;
import jp.co.metateam.library.model.RentalManage;
import jp.co.metateam.library.repository.BookMstRepository;
import jp.co.metateam.library.repository.RentalManageRepository;
import jp.co.metateam.library.repository.StockRepository;
import jp.co.metateam.library.values.StockStatus;
import jp.co.metateam.library.values.RentalStatus;
import jp.co.metateam.library.model.CalendarDto;

@Service
public class StockService {
    private final BookMstRepository bookMstRepository;
    private final StockRepository stockRepository;
    private final RentalManageRepository rentalManageRepository;

    @Autowired
    public StockService(BookMstRepository bookMstRepository, StockRepository stockRepository,
            RentalManageRepository rentalManageRepository) {
        this.bookMstRepository = bookMstRepository;
        this.stockRepository = stockRepository;
        this.rentalManageRepository = rentalManageRepository;
    }

    @Transactional
    public List<Stock> findAll() {
        List<Stock> stocks = this.stockRepository.findByDeletedAtIsNull();

        return stocks;
    }

    @Transactional
    public List<Stock> findStockAvailableAll() {
        List<Stock> stocks = this.stockRepository.findByDeletedAtIsNullAndStatus(Constants.STOCK_AVAILABLE);

        return stocks;
    }

    @Transactional
    public Stock findById(String id) {
        return this.stockRepository.findById(id).orElse(null);
    }

    @Transactional
    public void save(StockDto stockDto) throws Exception {
        try {
            Stock stock = new Stock();
            BookMst bookMst = this.bookMstRepository.findById(stockDto.getBookId()).orElse(null);
            if (bookMst == null) {
                throw new Exception("BookMst record not found.");
            }

            stock.setBookMst(bookMst);
            stock.setId(stockDto.getId());
            stock.setStatus(stockDto.getStatus());
            stock.setPrice(stockDto.getPrice());

            // データベースへの保存
            this.stockRepository.save(stock);
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public void update(String id, StockDto stockDto) throws Exception {
        try {
            Stock stock = findById(id);
            if (stock == null) {
                throw new Exception("Stock record not found.");
            }

            BookMst bookMst = stock.getBookMst();
            if (bookMst == null) {
                throw new Exception("BookMst record not found.");
            }

            stock.setId(stockDto.getId());
            stock.setBookMst(bookMst);
            stock.setStatus(stockDto.getStatus());
            stock.setPrice(stockDto.getPrice());

            // データベースへの保存
            this.stockRepository.save(stock);
        } catch (Exception e) {
            throw e;
        }
    }

    public List<Object> generateDaysOfWeek(int year, int month, LocalDate startDate, int daysInMonth) {
        List<Object> daysOfWeek = new ArrayList<>();
        for (int dayOfMonth = 1; dayOfMonth <= daysInMonth; dayOfMonth++) {
            LocalDate date = LocalDate.of(year, month, dayOfMonth);
            DateTimeFormatter formmater = DateTimeFormatter.ofPattern("dd(E)", Locale.JAPANESE);
            daysOfWeek.add(date.format(formmater));
        }

        return daysOfWeek;
    }

    public List<List<CalendarDto>> generateValues(Integer year, Integer month, Integer daysInMonth) {
        // FIXME ここで各書籍毎の日々の在庫を生成する処理を実装する
        // リストを詰めるリストの作成
        List<Object[]> titleAndStockLists = stockRepository.findAllTitleAndAvailableStockCount();
        List<List<CalendarDto>> stockCalendarList = new ArrayList<>();

        LocalDate currentDate = LocalDate.of(year, month, 1);
        daysInMonth = currentDate.lengthOfMonth();

        for (Object[] data : titleAndStockLists) {
            List<CalendarDto> stockList = new ArrayList<>();
            String title = (String) data[0];
            Long stockNum = (Long) data[1];

            for (int i = 1; i <= daysInMonth; i++) {
            CalendarDto calendarDto = new CalendarDto();
            calendarDto.setTitle(title);
            calendarDto.setStockNum(stockNum);

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR,year);
                calendar.set(Calendar.MONTH,month  - 1);
                calendar.set(Calendar.DAY_OF_MONTH, i);

                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                Date day = calendar.getTime();

                calendarDto.setExpectedRentalOn(day);
                List<String> availableStockId = rentalManageRepository.findByAvailableStockId(day, title);
                if(!availableStockId.isEmpty()){
                    calendarDto.setStockId(availableStockId.get(0));
                }
                Long rentalAnAvailable = rentalManageRepository.findByUnAvailableDayCount(day, title);
                Long rentalAvailable = stockNum - rentalAnAvailable;
                if (rentalAvailable <= 0) {
                    String noRentalAvailable = "×";
                    calendarDto.setRentalAvailable(noRentalAvailable);
                } else {
                    calendarDto.setRentalAvailable(rentalAvailable);
                }
            stockList.add(calendarDto);
            }
            // 書籍ごとの在庫数を最終的なリストに追加
            stockCalendarList.add(stockList);
        }
        return stockCalendarList;
    }
}