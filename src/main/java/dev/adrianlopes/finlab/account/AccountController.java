package dev.adrianlopes.finlab.account;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * HTTP layer: receives requests, calls the service and chooses what to show.
 * No business rules here.
 */
@Controller
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("balance", Money.format(service.getBalance()));
        model.addAttribute("requestId", UUID.randomUUID().toString());
        return "home";
    }

    @GetMapping({"/deposit", "/withdraw"})
    public String backToHome() {
        return "redirect:/";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam(required = false) String requestId,
                          @RequestParam(required = false) String amount,
                          RedirectAttributes redirect) {
        try {
            BigDecimal deposited = service.deposit(requestId, amount);
            redirect.addFlashAttribute("message", "Deposited " + Money.format(deposited));
        } catch (DuplicateOperationException e) {
            // Same form sent twice: the first copy was applied and its message is on screen.
        } catch (OperationException e) {
            redirect.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam(required = false) String requestId,
                           @RequestParam(required = false) String amount,
                           RedirectAttributes redirect) {
        try {
            BigDecimal withdrawn = service.withdraw(requestId, amount);
            redirect.addFlashAttribute("message", "Withdrew " + Money.format(withdrawn));
        } catch (DuplicateOperationException e) {
            // Same form sent twice: the first copy was applied and its message is on screen.
        } catch (OperationException e) {
            redirect.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/";
    }
}
