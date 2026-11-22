package dev.adrianlopes.finlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@SpringBootApplication
@Controller
public class FinlabApplication {

    private double balance = 0;
    private String message = "";

    public static void main(String[] args) {
        SpringApplication.run(FinlabApplication.class, args);
    }

    @GetMapping("/")
    @ResponseBody
    public String home() {
        return "<html><body>"
                + "<h1>FinLab</h1>"
                + "<h2>Balance: R$ " + String.format("%.2f", balance) + "</h2>"
                + "<p>" + message + "</p>"
                + "<form method='post'>"
                + "<input type='number' name='amount' step='0.01' required> "
                + "<button formaction='/deposit'>Deposit</button> "
                + "<button formaction='/withdraw'>Withdraw</button>"
                + "</form>"
                + "</body></html>";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam double amount) {
        if (amount <= 0) {
            message = "Amount must be greater than zero.";
        } else {
            balance = balance + amount;
            message = "Deposited R$ " + String.format("%.2f", amount);
        }
        return "redirect:/";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam double amount) {
        if (amount <= 0) {
            message = "Amount must be greater than zero.";
        } else if (amount > balance) {
            message = "Insufficient balance.";
        } else {
            balance = balance - amount;
            message = "Withdrew R$ " + String.format("%.2f", amount);
        }
        return "redirect:/";
    }
}
