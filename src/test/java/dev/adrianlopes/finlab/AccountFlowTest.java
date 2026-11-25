package dev.adrianlopes.finlab;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box tests: they use the app like a browser does (HTTP + cookies),
 * so they keep working no matter how the code is organized inside.
 * A real PostgreSQL runs in a throwaway Docker container (Testcontainers).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    /** Non-breaking space: Java puts it between "R$" and the number in pt-BR. */
    private static final String NBSP = "\u00a0";

    @LocalServerPort
    private int port;

    // Keeps cookies like a browser and does NOT follow redirects automatically.
    private final HttpClient browser = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Test
    void depositAddsToBalance() throws Exception {
        BigDecimal before = balance(home());

        String page = submit("/deposit", newRequestId(), "150.50");

        assertThat(balance(page)).isEqualByComparingTo(before.add(new BigDecimal("150.50")));
        assertThat(message(page)).contains("Deposited");
    }

    @Test
    void withdrawalSubtractsFromBalance() throws Exception {
        submit("/deposit", newRequestId(), "100.00");
        BigDecimal before = balance(home());

        String page = submit("/withdraw", newRequestId(), "30.25");

        assertThat(balance(page)).isEqualByComparingTo(before.subtract(new BigDecimal("30.25")));
        assertThat(message(page)).contains("Withdrew");
    }

    @Test
    void withdrawalAboveBalanceIsRejected() throws Exception {
        BigDecimal before = balance(home());

        String page = submit("/withdraw", newRequestId(), before.add(BigDecimal.ONE).toPlainString());

        assertThat(balance(page)).isEqualByComparingTo(before);
        assertThat(message(page)).isEqualTo("Insufficient balance.");
    }

    @Test
    void moneyIsExact() throws Exception {
        BigDecimal before = balance(home());

        submit("/deposit", newRequestId(), "0.30");
        submit("/withdraw", newRequestId(), "0.10");
        submit("/withdraw", newRequestId(), "0.10");
        String page = submit("/withdraw", newRequestId(), "0.10");

        assertThat(balance(page)).isEqualByComparingTo(before);
        assertThat(message(page)).contains("Withdrew");
    }

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "abc", "", "0", "-5", "10.005", "1e308", "1000000.01"})
    void invalidAmountsAreRejected(String amount) throws Exception {
        BigDecimal before = balance(home());

        String page = submit("/deposit", newRequestId(), amount);

        assertThat(balance(page)).isEqualByComparingTo(before);
        assertThat(message(page)).isNotEmpty().doesNotContain("Deposited");
    }

    @Test
    void sameFormSentTwiceIsAppliedOnce() throws Exception {
        BigDecimal before = balance(home());
        String requestId = token(home());

        submit("/deposit", requestId, "5.00");
        String page = submit("/deposit", requestId, "5.00");

        assertThat(balance(page)).isEqualByComparingTo(before.add(new BigDecimal("5.00")));
        assertThat(message(page)).as("the copy is ignored silently").isEmpty();
    }

    @Test
    void doubleClickAtTheSameInstantIsAppliedOnce() throws Exception {
        BigDecimal before = balance(home());
        String requestId = token(home());

        runAtTheSameTime(2, () -> post("/deposit", requestId, "10.00"));

        String page = home();
        assertThat(balance(page)).isEqualByComparingTo(before.add(new BigDecimal("10.00")));
        assertThat(message(page)).isEqualTo("Deposited R$" + NBSP + "10,00");
        assertThat(message(home())).as("no leftover message on the next page").isEmpty();
    }

    @Test
    void requestWithoutRequestIdIsRejected() throws Exception {
        BigDecimal before = balance(home());

        String page = submit("/deposit", null, "1.00");

        assertThat(balance(page)).isEqualByComparingTo(before);
        assertThat(message(page)).isEqualTo("Invalid request. Please try again.");
    }

    @Test
    void simultaneousDepositsAreNeverLost() throws Exception {
        BigDecimal before = balance(home());

        runAtTheSameTime(100, () -> post("/deposit", newRequestId(), "1.00"));

        assertThat(balance(home())).isEqualByComparingTo(before.add(new BigDecimal("100.00")));
    }

    @Test
    void openingDepositInTheAddressBarRedirectsHome() throws Exception {
        HttpResponse<String> response = send(browserRequest(url("/deposit")).GET().build());

        assertThat(response.statusCode()).isEqualTo(302);
    }

    @Test
    void unknownPageShowsFinLabNotFoundPage() throws Exception {
        HttpResponse<String> response = send(browserRequest(url("/unknown")).GET().build());

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("FinLab").contains("Page not found.").doesNotContain("Whitelabel");
    }

    @Test
    void wrongMethodShowsFinLabErrorPage() throws Exception {
        HttpResponse<String> response = send(browserRequest(url("/"))
                .PUT(HttpRequest.BodyPublishers.noBody()).build());

        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(response.body()).contains("FinLab").doesNotContain("Whitelabel");
    }

    @Test
    void everyOperationIsRecordedWithDateTimeAndBalance() throws Exception {
        String page = submit("/deposit", newRequestId(), "12.34");

        assertThat(page).contains("Recent transactions");
        String firstRow = find(page, "<tr>\\s*<td>(.*?)</tr>");
        assertThat(stripTags(firstRow))
                .matches("(?s).*\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}.*")
                .contains("Deposit")
                .contains("+ R$" + NBSP + "12,34");
    }

    @Test
    void onlyAppliedOperationsAreRecorded() throws Exception {
        int before = statementRows();
        String requestId = newRequestId();

        submit("/deposit", requestId, "1.00");
        submit("/deposit", requestId, "1.00");          // same form again
        submit("/deposit", newRequestId(), "abc");      // invalid amount
        submit("/withdraw", newRequestId(), "99999999"); // above the limit

        assertThat(statementRows()).isEqualTo(before + 1);
    }

    @Test
    void simultaneousWithdrawalsNeverOverdraw() throws Exception {
        BigDecimal current = balance(home());
        if (current.signum() > 0) {
            submit("/withdraw", newRequestId(), current.toPlainString());
        }
        submit("/deposit", newRequestId(), "100.00");
        int before = statementRows();

        runAtTheSameTime(10, () -> post("/withdraw", newRequestId(), "80.00"));

        assertThat(balance(home())).isEqualByComparingTo("20.00");
        assertThat(statementRows()).isEqualTo(before + 1);
    }

    // ---------- helpers ----------

    private int statementRows() throws Exception {
        String page = send(browserRequest(url("/statement")).GET().build()).body();
        return Math.max(0, page.split("<tr>").length - 2); // minus the text before the table and the header row
    }

    private String home() throws Exception {
        return send(browserRequest(url("/")).GET().build()).body();
    }

    /** Sends the form, then opens the page the redirect points to, like a browser. */
    private String submit(String path, String requestId, String amount) throws Exception {
        post(path, requestId, amount);
        return home();
    }

    private void post(String path, String requestId, String amount) throws Exception {
        StringBuilder form = new StringBuilder("amount=" + encode(amount));
        if (requestId != null) {
            form.append("&requestId=").append(encode(requestId));
        }
        send(browserRequest(url(path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                .build());
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return browser.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** Browsers always ask for HTML; Spring answers errors in JSON to clients that don't. */
    private static HttpRequest.Builder browserRequest(URI uri) {
        return HttpRequest.newBuilder(uri).header("Accept", "text/html");
    }

    private URI url(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String newRequestId() {
        return UUID.randomUUID().toString();
    }

    private static BigDecimal balance(String html) {
        String text = stripTags(find(html, "<h2[^>]*>(.*?)</h2>"));
        String digits = text.replace("Balance:", "").replace("R$", "")
                .replace(NBSP, "").replace(" ", "")
                .replace(".", "").replace(",", ".");
        return new BigDecimal(digits);
    }

    private static String message(String html) {
        return stripTags(find(html, "<p[^>]*>(.*?)</p>")).trim();
    }

    private static String token(String html) {
        return find(html, "name=[\"']requestId[\"'][^>]*value=[\"']([^\"']+)");
    }

    private static String find(String html, String regex) {
        Matcher matcher = Pattern.compile(regex, Pattern.DOTALL).matcher(html);
        assertThat(matcher.find()).as("pattern %s in page", regex).isTrue();
        return matcher.group(1);
    }

    private static String stripTags(String html) {
        return html.replaceAll("<[^>]+>", "");
    }

    private interface Action {
        void run() throws Exception;
    }

    /** Starts n threads that wait for each other and then fire at the same instant. */
    private static void runAtTheSameTime(int n, Action action) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(n);
        List<Thread> threads = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Thread thread = new Thread(() -> {
                try {
                    barrier.await();
                    action.run();
                } catch (Exception e) {
                    synchronized (errors) {
                        errors.add(e);
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertThat(errors).isEmpty();
    }
}
