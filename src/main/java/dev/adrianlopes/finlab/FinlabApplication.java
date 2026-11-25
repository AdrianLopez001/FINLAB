package dev.adrianlopes.finlab;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@SpringBootApplication
@Controller
public class FinlabApplication implements ErrorController {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000.00");
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");

    // Shared by every request. Methods that read or change it are synchronized,
    // so only one request touches the balance at a time.
    private BigDecimal balance = BigDecimal.ZERO;
    private String message = "";

    public static void main(String[] args) {
        SpringApplication.run(FinlabApplication.class, args);
    }

    @GetMapping("/")
    @ResponseBody
    public synchronized String home() {
        return "<html><body>"
                + "<h1>FinLab</h1>"
                + "<h2>Balance: " + formatMoney(balance) + "</h2>"
                + "<p>" + message + "</p>"
                + "<form method='post'>"
                + "<input type='number' name='amount' step='0.01' min='0.01' required> "
                + "<button formaction='/deposit'>Deposit</button> "
                + "<button formaction='/withdraw'>Withdraw</button>"
                + "</form>"
                + "</body></html>";
    }

    @GetMapping({"/deposit", "/withdraw"})
    public String backToHome() {
        return "redirect:/";
    }

    @PostMapping("/deposit")
    public synchronized String deposit(@RequestParam(required = false) String amount) {
        BigDecimal value = toMoney(amount);
        String error = validate(value);
        if (error != null) {
            message = error;
        } else {
            balance = balance.add(value);
            message = "Deposited " + formatMoney(value);
        }
        return "redirect:/";
    }

    @PostMapping("/withdraw")
    public synchronized String withdraw(@RequestParam(required = false) String amount) {
        BigDecimal value = toMoney(amount);
        String error = validate(value);
        if (error != null) {
            message = error;
        } else if (value.compareTo(balance) > 0) {
            message = "Insufficient balance.";
        } else {
            balance = balance.subtract(value);
            message = "Withdrew " + formatMoney(value);
        }
        return "redirect:/";
    }

    @RequestMapping("/error")
    public ResponseEntity<String> error(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = (code instanceof Integer number) ? number : 500;
        String text = (status == 404) ? "Page not found." : "Something went wrong.";
        String page = "<html><body>"
                + "<h1>FinLab</h1>"
                + "<h2>" + text + "</h2>"
                + "<p><a href='/'>Back to your account</a></p>"
                + "</body></html>";
        return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML).body(page);
    }

    private static BigDecimal toMoney(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String validate(BigDecimal value) {
        if (value == null) {
            return "Enter a valid amount, like 10.50.";
        } else if (value.signum() <= 0) {
            return "Amount must be greater than zero.";
        } else if (value.stripTrailingZeros().scale() > 2) {
            return "Amount must have at most 2 decimal places.";
        } else if (value.compareTo(MAX_AMOUNT) > 0) {
            return "Amount must be at most " + formatMoney(MAX_AMOUNT) + ".";
        }
        return null;
    }

    private static String formatMoney(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(BRAZIL).format(value);
    }
}
