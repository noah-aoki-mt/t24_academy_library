package jp.co.metateam.library.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jp.co.metateam.library.model.BookMst;
import jp.co.metateam.library.model.CalendarDto;
import jp.co.metateam.library.model.Stock;
import jp.co.metateam.library.model.StockDto;
import jp.co.metateam.library.service.BookMstService;
import jp.co.metateam.library.service.StockService;
import jp.co.metateam.library.values.StockStatus;
import lombok.extern.log4j.Log4j2;

/**
 * 在庫情報関連クラス
 */
@Log4j2
@Controller
public class StockController {

    private final BookMstService bookMstService;
    private final StockService stockService;

    @Autowired
    public StockController(BookMstService bookMstService, StockService stockService) {
        this.bookMstService = bookMstService;
        this.stockService = stockService;
    }

    @GetMapping("/stock/index")
    public String index(Model model) {
        List <Stock> stockList = this.stockService.findAll();

        model.addAttribute("stockList", stockList);

        return "stock/index";
    }

    @GetMapping("/stock/add")
    public String add(Model model) {
        List<BookMst> bookMstList = this.bookMstService.findAll();

        model.addAttribute("bookMstList", bookMstList);
        model.addAttribute("stockStatus", StockStatus.values());

        if (!model.containsAttribute("stockDto")) {
            model.addAttribute("stockDto", new StockDto());
        }

        return "stock/add";
    }

    @PostMapping("/stock/add")
    public String save(@Valid @ModelAttribute StockDto stockDto, BindingResult result, RedirectAttributes ra) {
        try {
            if (result.hasErrors()) {
                throw new Exception("Validation error.");
            }
            // 登録処理
            this.stockService.save(stockDto);

            return "redirect:/stock/index";
        } catch (Exception e) {
            log.error(e.getMessage());

            ra.addFlashAttribute("stockDto", stockDto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.stockDto", result);

            return "redirect:/stock/add";
        }
    }

    @GetMapping("/stock/{id}")
    public String detail(@PathVariable("id") String id, Model model) {
        Stock stock = this.stockService.findById(id);

        model.addAttribute("stock", stock);

        return "stock/detail";
    }

    @GetMapping("/stock/{id}/edit")
    public String edit(@PathVariable("id") String id, Model model) {
        List<BookMst> bookMstList = this.bookMstService.findAll();

        model.addAttribute("bookMstList", bookMstList);
        model.addAttribute("stockStatus", StockStatus.values());

        if (!model.containsAttribute("stockDto")) {
            StockDto stockDto = new StockDto();
            Stock stock = this.stockService.findById(id);
            stockDto.setId(stock.getId());
            stockDto.setPrice(stock.getPrice());
            stockDto.setStatus(stock.getStatus());
            stockDto.setBookMst(stock.getBookMst());

            model.addAttribute("stockDto", stockDto);
        }

        return "stock/edit";
    }

    @PostMapping("/stock/{id}/edit")
    public String update(@PathVariable("id") String id, @Valid @ModelAttribute StockDto stockDto, BindingResult result, RedirectAttributes ra) {
        try {
            if (result.hasErrors()) {
                throw new Exception("Validation error.");
            }
            // 登録処理
            stockService.update(id, stockDto);

            return "stock/index";
        } catch (Exception e) {
            log.error(e.getMessage());

            ra.addFlashAttribute("stockDto", stockDto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.stockDto", result);

            return "redirect:/stock/edit";
        }
    }

    @GetMapping("/stock/calendar")
    public String calendar(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month, Model model) {

        LocalDate today = year == null || month == null ? LocalDate.now() : LocalDate.of(year, month, 1);
        Integer targetYear = year == null ? today.getYear() : year;
        Integer targetMonth = today.getMonthValue();

        LocalDate startDate = LocalDate.of(targetYear, targetMonth, 1);
        Integer daysInMonth = startDate.lengthOfMonth();

        List<Object> daysOfWeek = this.stockService.generateDaysOfWeek(targetYear, targetMonth, startDate, daysInMonth);
        List<List<CalendarDto>> stocks = this.stockService.generateValues(targetYear, targetMonth, daysInMonth);

        model.addAttribute("targetYear", targetYear);
        model.addAttribute("targetMonth", targetMonth);
        model.addAttribute("daysOfWeek", daysOfWeek);
        model.addAttribute("daysInMonth", daysInMonth);

        model.addAttribute("stocks", stocks);

        return "stock/calendar";
    }
}
