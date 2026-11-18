package com.petagenda.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpSession;

/**
 * Quem pode fazer o quê (casos de uso: Funcionário × Administrador).
 *
 * - Funcionário (Atendente): agenda do dia, status, histórico (UC05, UC06, UC12).
 * - Administrador: tudo do funcionário + serviços, funcionários e bloqueios
 *   (UC07, UC08, UC10). É a relação "Administrador herda de Funcionário".
 *
 * A sessão guarda só o id. A cada pedido o perfil é lido de novo no banco:
 * assim, se o administrador desligar alguém ou mudar o perfil, vale na hora.
 */
@Component
public class ControleAcesso {

    static final String SESSAO_ID = "idFuncionario";
    static final String ADMINISTRADOR = "Administrador";
    static final String ATENDENTE = "Atendente";

    private final JdbcTemplate jdbcTemplate;

    public ControleAcesso(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Funcionário logado e ativo, ou null. */
    public Map<String, Object> funcionarioLogado(HttpSession sessao) {
        Object id = sessao.getAttribute(SESSAO_ID);
        if (id == null) {
            return null;
        }
        List<Map<String, Object>> r = jdbcTemplate.queryForList(
                "SELECT id_funcionario AS id, nome, login, tipo_perfil AS perfil FROM funcionario "
                        + "WHERE id_funcionario = ? AND ativo = 1", id);
        return r.isEmpty() ? null : r.get(0);
    }

    /** Garante login. Responde 401 se não houver. */
    public Map<String, Object> exigirLogin(HttpSession sessao) {
        Map<String, Object> f = funcionarioLogado(sessao);
        if (f == null) {
            throw new RegraNegocioException(HttpStatus.UNAUTHORIZED, "Faça login para acessar.");
        }
        return f;
    }

    /** Garante login de administrador. Responde 403 para atendente. */
    public Map<String, Object> exigirAdmin(HttpSession sessao) {
        Map<String, Object> f = exigirLogin(sessao);
        if (!ADMINISTRADOR.equals(f.get("perfil"))) {
            throw new RegraNegocioException(HttpStatus.FORBIDDEN, "Somente o administrador pode fazer isso.");
        }
        return f;
    }
}
