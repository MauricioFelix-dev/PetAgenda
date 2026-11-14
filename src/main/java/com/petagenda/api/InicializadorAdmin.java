package com.petagenda.api;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Ao iniciar a aplicação, cria um usuário administrador caso a tabela
 * de funcionários esteja vazia. A senha é gravada criptografada (BCrypt).
 *
 * Login inicial: admin / admin123  (troque após o primeiro acesso)
 */
@Component
public class InicializadorAdmin implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public InicializadorAdmin(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM funcionario", Integer.class);
        if (total != null && total == 0) {
            String hash = new BCryptPasswordEncoder().encode("admin123");
            jdbcTemplate.update(
                    "INSERT INTO funcionario (nome, telefone, email, login, senha, tipo_perfil) VALUES (?, ?, ?, ?, ?, ?)",
                    "Administrador", "00000000000", "admin@petagenda.local", "admin", hash, "Administrador");
            System.out.println(">>> Usuário inicial criado: admin / admin123 (troque a senha)");
        }
    }
}
