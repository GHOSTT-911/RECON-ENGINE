package com.reconengine.config;

import com.reconengine.repository.TransactionRepository;
import com.reconengine.service.AuthService;
import com.reconengine.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Seeds demo data and a demo user on first start, then prints where to open the app. */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final TransactionRepository repository;
    private final ReconciliationService reconciliationService;
    private final AuthService authService;
    private final ReconProperties properties;
    private final Environment environment;

    public DataSeeder(TransactionRepository repository,
                      ReconciliationService reconciliationService,
                      AuthService authService,
                      ReconProperties properties,
                      Environment environment) {
        this.repository = repository;
        this.reconciliationService = reconciliationService;
        this.authService = authService;
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.isSeedSampleData() && repository.count() == 0) {
            int n = reconciliationService.resetToSampleData().size();
            log.info("Seeded {} sample transactions into the ledger", n);
        }
        authService.ensureDemoUser();

        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8081"));
        log.info("");
        log.info("==============================================================");
        log.info("  ReconEngine is running");
        log.info("  Dashboard : http://localhost:{}/", port);
        log.info("  API       : http://localhost:{}/api/reconcile/transactions", port);
        log.info("  Health    : http://localhost:{}/api/health", port);
        log.info("  Demo login: {} / {}", AuthService.DEMO_EMAIL, AuthService.DEMO_PASSWORD);
        log.info("==============================================================");
    }
}
