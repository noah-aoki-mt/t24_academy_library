package jp.co.metateam.library.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jp.co.metateam.library.model.Account;
import jp.co.metateam.library.model.RentalManage;
import jp.co.metateam.library.model.RentalManageDto;
import jp.co.metateam.library.model.Stock;
import jp.co.metateam.library.service.AccountService;
import jp.co.metateam.library.service.RentalManageService;
import jp.co.metateam.library.service.StockService;
import jp.co.metateam.library.values.RentalStatus;
import jp.co.metateam.library.values.StockStatus;
import lombok.extern.log4j.Log4j2;

/**
 * 貸出管理関連クラスß
 */
@Log4j2
@Controller
public class RentalManageController {

    private final AccountService accountService;
    private final RentalManageService rentalManageService;
    private final StockService stockService;

    @Autowired
    public RentalManageController(
            AccountService accountService,
            RentalManageService rentalManageService,
            StockService stockService) {
        this.accountService = accountService;
        this.rentalManageService = rentalManageService;
        this.stockService = stockService;
    }

    /**
     * 貸出一覧画面初期表示
     * 
     * @param model
     * @return
     */
    @GetMapping("/rental/index")
    public String index(Model model) {
        // 貸出管理テーブルから全件取得
        List<RentalManage> rentalManageList = this.rentalManageService.findAll();
        // 貸出一覧画面に渡すデータをmodelに追加
        model.addAttribute("rentalManageList", rentalManageList);
        // 貸出一覧画面に遷移
        return "rental/index";
    }

    @GetMapping("/rental/add")
    public String add(Model model) {

        List<Account> accountList = this.accountService.findAll();
        List<Stock> stockList = this.stockService.findStockAvailableAll();

        model.addAttribute("accounts", accountList);
        model.addAttribute("stockList", stockList);
        model.addAttribute("rentalStatus", RentalStatus.values());

        if (!model.containsAttribute("rentalManageDto")) {
            model.addAttribute("rentalManageDto", new RentalManageDto());
        }

        return "rental/add";
    }

    @PostMapping("/rental/add")
    public String save(@Valid @ModelAttribute RentalManageDto rentalManageDto, BindingResult result,
            RedirectAttributes ra, Long id, Model model) {
        try {
            if (result.hasErrors()) {
                throw new Exception("Validation error.");
            }

            Boolean rentalCheckErrors = rentalAddCheck(id, rentalManageDto, result);
            if (rentalCheckErrors) {
                if (rentalCheckErrors) {
                    throw new Exception("Rental edit check failed");
                }
            }

            // 登録処理
            this.rentalManageService.save(rentalManageDto);

            return "redirect:/rental/index";
        } catch (Exception e) {
            log.error(e.getMessage());
            ra.addFlashAttribute("RentalManageDto", rentalManageDto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.RentalManageDto", result);

            List<Account> accountList = this.accountService.findAll();
            List<Stock> stockList = this.stockService.findStockAvailableAll();

            model.addAttribute("accounts", accountList);
            model.addAttribute("stockList", stockList);
            model.addAttribute("rentalStatus", RentalStatus.values());

            return "rental/add";
        }
    }

    // //貸出編集画面
    @GetMapping("/rental/{id}/edit")
    public String edit(@PathVariable("id") String id, Model model) {
        List<Account> accountList = this.accountService.findAll();
        List<Stock> stockList = this.stockService.findStockAvailableAll();
        model.addAttribute("accounts", accountList);
        model.addAttribute("stockList", stockList);
        model.addAttribute("rentalStatus", RentalStatus.values());

        if (!model.containsAttribute("rentalManageDto")) {
            RentalManageDto rentalManageDto = new RentalManageDto();
            Long idLong = Long.parseLong(id);
            RentalManage rentalManage = this.rentalManageService.findById(idLong);
            rentalManageDto.setId(rentalManage.getId());
            rentalManageDto.setStockId(rentalManage.getStock().getId());
            rentalManageDto.setEmployeeId(rentalManage.getAccount().getEmployeeId());
            rentalManageDto.setStatus(rentalManage.getStatus());
            rentalManageDto.setExpectedRentalOn(rentalManage.getExpectedRentalOn());
            rentalManageDto.setExpectedReturnOn(rentalManage.getExpectedReturnOn());

            model.addAttribute("rentalManageDto", rentalManageDto);
        }
        return "rental/edit";
    }

    @PostMapping("/rental/{id}/edit")
    public String update(@PathVariable("id") Long id, @Valid @ModelAttribute RentalManageDto rentalManageDto,
            BindingResult result, RedirectAttributes ra, Model model) {
        try {
            if (result.hasErrors()) {
                throw new Exception("Validation error.");
            }
            // 日付妥当性チェック
            Optional<String> dateValidationError = rentalManageDto.validateDate();
            if (dateValidationError.isPresent()) {
                result.addError(new FieldError("rentalManageDto", "expectedReturnOn", dateValidationError.get()));
                throw new Exception(dateValidationError.get());
            }
            RentalManage rentalManage = this.rentalManageService.findById(id);
            Integer previousRentalStatus = rentalManage.getStatus();
            Optional<String> errMessage = rentalManageDto.validateStatus(previousRentalStatus);
            if (errMessage.isPresent()) {
                result.addError(new FieldError("rentalManageDto", "status", errMessage.get()));
                throw new Exception(errMessage.get());
            }

            // 貸出可否チェック
            Boolean rentalCheckErrors = rentalEditCheck(id, rentalManageDto, result);
            if (rentalCheckErrors) {
                throw new Exception("Rental edit check failed");
            }

            // 登録処理
            rentalManageService.update(id, rentalManageDto);
            return "redirect:/rental/index";
        } catch (Exception e) {
            log.error(e.getMessage());
            ra.addFlashAttribute("rentalManageDto", rentalManageDto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.rentalManageDto", result);

            return "redirect:/rental/" + id + "/edit";
        }
    }

    // 貸出登録貸出可否チェック
    public boolean rentalAddCheck(Long id, RentalManageDto rentalManageDto, BindingResult result) {
        Stock stock = this.stockService.findById(rentalManageDto.getStockId());
        // Stock Statusが利用可能の場合(現在の仕様上リストに利用不可のものが出てこないため利用不可ステータスの判定はない)
        if (stock.getStatus() == StockStatus.RENT_AVAILABLE.getValue()) {
            List<RentalManage> rentalManageList = this.rentalManageService.findAllByStatusIn(
                    Arrays.asList(RentalStatus.RENT_WAIT.getValue(), RentalStatus.RENTAlING.getValue()));
            if (!rentalManageList.isEmpty()) {
                for (RentalManage rentalManage : rentalManageList) {
                    // 貸出予定日と返却予定日のチェック
                    if (!(rentalManageDto.getExpectedReturnOn().before(rentalManage.getExpectedRentalOn()) ||
                            rentalManageDto.getExpectedReturnOn().after(rentalManageDto.getExpectedReturnOn()))) {
                        if (!result.hasFieldErrors("expectedReturnOn")) {
                            result.addError(
                                    new FieldError("rentalManageDto", "expectedReturnOn", "入力されている返却予定日ではこの本を貸し出せません"));
                        }
                    }
                    if (!(rentalManageDto.getExpectedRentalOn().before(rentalManage.getExpectedRentalOn()) ||
                            rentalManageDto.getExpectedRentalOn().after(rentalManage.getExpectedReturnOn()))) {
                        if (!result.hasFieldErrors("expectedRentalOn")) {
                            result.addError(
                                    new FieldError("rentalManageDto", "expectedRentalOn", "入力されている貸出予定日ではこの本を貸し出せません"));

                        }
                    }

                }
            }
        }
        return result.hasFieldErrors("expectedReturnOn") || result.hasFieldErrors("expectedRentalOn");
    }

    public boolean rentalEditCheck(Long id, RentalManageDto rentalManageDto, BindingResult result) {
        Stock stock = this.stockService.findById(rentalManageDto.getStockId());
        // Stock Statusが利用可能の場合(現在の仕様上リストに利用不可のものが出てこないため利用不可ステータスの判定はない)
        if (stock.getStatus() == StockStatus.RENT_AVAILABLE.getValue()) {
            List<RentalManage> rentalManageList = this.rentalManageService.findAllByStatusIn(
                    Arrays.asList(RentalStatus.RENT_WAIT.getValue(), RentalStatus.RENTAlING.getValue()));
            if (!rentalManageList.isEmpty()) {
                for (RentalManage rentalManage : rentalManageList) {
                    if (!rentalManage.getId().equals(rentalManageDto.getId())) {
                        // 貸出予定日と返却予定日のチェック
                        if (!(rentalManageDto.getExpectedReturnOn().before(rentalManage.getExpectedRentalOn()) ||
                                rentalManageDto.getExpectedReturnOn().after(rentalManageDto.getExpectedReturnOn()))) {
                            if (!result.hasFieldErrors("expectedReturnOn")) {
                                result.addError(new FieldError("rentalManageDto", "expectedReturnOn",
                                        "入力されている返却予定日ではこの本を貸し出せません"));
                            }
                        }
                        if (!(rentalManageDto.getExpectedRentalOn().before(rentalManage.getExpectedRentalOn()) ||
                                rentalManageDto.getExpectedRentalOn().after(rentalManage.getExpectedReturnOn()))) {
                            if (!result.hasFieldErrors("expectedRentalOn")) {
                                result.addError(new FieldError("rentalManageDto", "expectedRentalOn",
                                        "入力されている貸出予定日ではこの本を貸し出せません"));

                            }
                        }

                    }
                }
            }
        }
        return result.hasFieldErrors("expectedReturnOn") || result.hasFieldErrors("expectedRentalOn");
    }
}
