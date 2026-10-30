package com.reconengine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReconciliationApiTests {

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void reset() throws Exception {
        mvc.perform(post("/api/reconcile/reset")).andExpect(status().isOk());
    }

    @Test
    void seedsFiftyTransactionsWithExpectedStatuses() throws Exception {
        mvc.perform(get("/api/reconcile/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(50)))
                .andExpect(jsonPath("$[0].id").value("pay_RZP100049"))
                .andExpect(jsonPath("$[0].status").value("UNRESOLVED_EXCEPTION"))
                .andExpect(jsonPath("$[1].status").value("EXACT_MATCH"))
                .andExpect(jsonPath("$[3].status").value("PARTIAL_MATCH"));

        mvc.perform(get("/api/reconcile/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(50))
                .andExpect(jsonPath("$.countsByStatus.UNRESOLVED_EXCEPTION").value(10));
    }

    @Test
    void runEngineDiagnosesExceptions() throws Exception {
        mvc.perform(post("/api/reconcile/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rootCause", startsWith("Automated Java Engine")));
    }

    @Test
    void overrideResolvesTransactionAndUnknownIdIs404() throws Exception {
        mvc.perform(post("/api/reconcile/override").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"pay_RZP100049\",\"action\":\"CHARGEBACK_FILED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED_MANUAL"))
                .andExpect(jsonPath("$.rootCause").value("Manually Resolved: Flagged for Chargeback Dispute"));

        mvc.perform(post("/api/reconcile/override").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"nope\",\"action\":\"CHARGEBACK_FILED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void bulkApproveMovesExceptionsToPartialMatch() throws Exception {
        mvc.perform(post("/api/reconcile/bulk-approve-mdr").contentType(MediaType.APPLICATION_JSON)
                        .content("[\"pay_RZP100049\",\"pay_RZP100054\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PARTIAL_MATCH"))
                .andExpect(jsonPath("$[5].status").value("PARTIAL_MATCH"));
    }

    @Test
    void csvUploadClassifiesRows() throws Exception {
        String csv = "Transaction ID,UTR,Gateway Amount,Bank Credit\n"
                + "T1,U1,1000,1000\n"
                + "T2,U2,1000,980\n"
                + "T3,U3,\"1,000.00\",500\n"
                + "T4,U4,abc,1\n";
        MockMultipartFile file = new MockMultipartFile("file", "s.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/reconcile/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].status").value("EXACT_MATCH"))
                .andExpect(jsonPath("$[1].status").value("PARTIAL_MATCH"))
                .andExpect(jsonPath("$[2].status").value("UNRESOLVED_EXCEPTION"));
    }

    @Test
    void registerThenLogin() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"email\":\"t@example.com\",\"password\":\"secret1\"}"))
                .andExpect(jsonPath("$.success").value(true));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"t@example.com\",\"password\":\"secret1\"}"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.name").value("Test"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"t@example.com\",\"password\":\"wrong\"}"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void demoUserCanLogIn() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@reconengine.com\",\"password\":\"admin123\"}"))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void healthIsUp() throws Exception {
        mvc.perform(get("/api/health")).andExpect(jsonPath("$.status").value("UP"));
    }
}
