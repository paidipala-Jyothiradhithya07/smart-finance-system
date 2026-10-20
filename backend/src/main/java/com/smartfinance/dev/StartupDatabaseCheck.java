package com.smartfinance.dev;

import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.smartfinance.entity.Category;
import com.smartfinance.repository.CategoryRepository;

import org.springframework.boot.CommandLineRunner;

@Component
@Profile("dev")
public class StartupDatabaseCheck implements CommandLineRunner {
	private static final Logger log = LoggerFactory.getLogger(StartupDatabaseCheck.class);

    private final CategoryRepository categoryRepository;

    public StartupDatabaseCheck(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        List<Category> defaults = categoryRepository.findByUserIsNullOrderByNameAsc();
        log.info("System categories found: {}", defaults.size());
        defaults.forEach(c -> log.info(" - {} ({})", c.getName(), c.getType()));
    }
}
