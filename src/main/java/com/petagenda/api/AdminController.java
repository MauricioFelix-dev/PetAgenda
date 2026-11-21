package com.petagenda.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

/**
 * Telas do administrador e troca de senha.
 *
 *   /api/admin/servicos      UC07 Cadastrar serviços e preços (com profissional responsável)
 *   /api/admin/funcionarios  UC08 Cadastrar profissionais (perfil Administrador ou Atendente)
 *   /api/admin/bloqueios     UC10 Bloquear horários na agenda
 *   /api/minha-senha         qualquer funcionário troca a própria senha
 *
 * Todas as rotas /api/admin exigem perfil Administrador (403 para atendente).
 */
@RestController
@RequestMapping("/api")
public class AdminController {

    private static final int SENHA_MINIMA = 6;

    private final JdbcTemplate jdbcTemplate;
    private final ControleAcesso acesso;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminController(JdbcTemplate jdbcTemplate, ControleAcesso acesso) {
        this.jdbcTemplate = jdbcTemplate;
        this.acesso = acesso;
    }

    // =====================================================================
    // UC07 – SERVIÇOS
    // =====================================================================

    @GetMapping("/admin/servicos")
    public List<Map<String, Object>> listarServicos(HttpSession sessao) {
        acesso.exigirAdmin(sessao);
        return jdbcTemplate.queryForList(
                "SELECT s.id_servico AS id, s.descricao, s.detalhes, s.icone, s.preco, s.duracaoMinutos, s.ativo, "
                        + "s.id_responsavel AS idResponsavel, f.nome AS responsavel FROM servico s "
                        + "LEFT JOIN funcionario f ON f.id_funcionario = s.id_responsavel ORDER BY s.id_servico");
    }

    @PostMapping("/admin/servicos")
    public Map<String, Object> criarServico(@RequestBody Map<String, Object> dados, HttpSession sessao) {
        acesso.exigirAdmin(sessao);
        ServicoValidado s = validarServico(dados);
        jdbcTemplate.update(
                "INSERT INTO servico (descricao, detalhes, icone, preco, duracaoMinutos, ativo, id_responsavel) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                s.descricao, s.detalhes, s.icone, s.preco, s.duracao, s.ativo, s.idResponsavel);
        return Map.of("sucesso", true, "mensagem", "Serviço cadastrado.");
    }

    /**
     * Editar ou desativar. Não existe "apagar": um serviço que já foi agendado
     * precisa continuar no histórico, então ele é só desativado (some da página).
     */
    @PutMapping("/admin/servicos/{id}")
    public Map<String, Object> editarServico(@PathVariable int id, @RequestBody Map<String, Object> dados,
                                             HttpSession sessao) {
        acesso.exigirAdmin(sessao);
        ServicoValidado s = validarServico(dados);
        int linhas = jdbcTemplate.update(
                "UPDATE servico SET descricao = ?, detalhes = ?, icone = ?, preco = ?, duracaoMinutos = ?, "
                        + "ativo = ?, id_responsavel = ? WHERE id_servico = ?",
                s.descricao, s.detalhes, s.icone, s.preco, s.duracao, s.ativo, s.idResponsavel, id);
        if (linhas == 0) {
            throw new RegraNegocioException(HttpStatus.NOT_FOUND, "Serviço não encontrado.");
        }
        return Map.of("sucesso", true, "mensagem", "Serviço atualizado.");
    }

    private record ServicoValidado(String descricao, String detalhes, String icone, BigDecimal preco,
                                   int duracao, int ativo, Integer idResponsavel) { }

    private ServicoValidado validarServico(Map<String, Object> d) {
        String descricao = texto(d.get("descricao"));
        if (descricao.isEmpty() || descricao.length() > 100) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Informe o nome do serviço (até 100 caracteres).");
        }
        String detalhes = texto(d.get("detalhes"));
        if (detalhes.length() > 255) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "A descrição do card pode ter até 255 caracteres.");
        }
        String icone = texto(d.get("icone"));
        if (icone.length() > 16) {
            icone = icone.substring(0, 16);
        }
        BigDecimal preco;
        int duracao;
        try {
            preco = new BigDecimal(texto(d.get("preco")).replace(",", "."));
            duracao = Integer.parseInt(texto(d.get("duracaoMinutos")));
        } catch (NumberFormatException e) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Preço ou duração inválidos.");
        }
        if (preco.signum() < 0 || preco.compareTo(new BigDecimal("99999")) > 0) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Preço inválido.");
        }
        // A duração precisa caber no expediente (08:00 às 18:00 = 600 minutos)
        if (duracao < 5 || duracao > 600) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Duração deve ficar entre 5 e 600 minutos.");
        }
        Integer idResponsavel = inteiroOuNulo(d.get("idResponsavel"));
        if (idResponsavel != null) {
            Integer ok = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM funcionario WHERE id_funcionario = ? AND ativo = 1", Integer.class, idResponsavel);
            if (ok == null || ok == 0) {
                throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Profissional responsável inválido ou desligado.");
            }
        }
        int ativo = Boolean.FALSE.equals(d.get("ativo")) || "false".equals(String.valueOf(d.get("ativo"))) ? 0 : 1;
        return new ServicoValidado(descricao, detalhes.isEmpty() ? null : detalhes,
                icone.isEmpty() ? null : icone, preco, duracao, ativo, idResponsavel);
    }

    // =====================================================================
    // UC08 – FUNCIONÁRIOS
    // =====================================================================

    @GetMapping("/admin/funcionarios")
    public List<Map<String, Object>> listarFuncionarios(HttpSession sessao) {
        acesso.exigirAdmin(sessao);
        // A senha (mesmo criptografada) nunca sai do servidor
        return jdbcTemplate.queryForList(
                "SELECT id_funcionario AS id, nome, telefone, email, login, tipo_perfil AS perfil, ativo "
                        + "FROM funcionario ORDER BY ativo DESC, nome");
    }

    @PostMapping("/admin/funcionarios")
    public Map<String, Object> criarFuncionario(@RequestBody Map<String, Object> dados, HttpSession sessao) {
        acesso.exigirAdmin(sessao);
        FuncionarioValidado f = validarFuncionario(dados);
        String senha = texto(dados.get("senha"));
        validarSenhaNova(senha);
        try {
            jdbcTemplate.update(
                    "INSERT INTO funcionario (nome, telefone, email, login, senha, tipo_perfil, ativo) "
                            + "VALUES (?, ?, ?, ?, ?, ?, 1)",
                    f.nome, f.telefone, f.email, f.login, encoder.encode(senha), f.perfil);
        } catch (DuplicateKeyException e) {
            throw new RegraNegocioException(HttpStatus.CONFLICT, "Já existe um funcionário com esse login.");
        }
        return Map.of("sucesso", true, "mensagem", "Funcionário cadastrado.");
    }

    /**
     * Editar dados, perfil, ativo/desligado e, se vier "novaSenha", redefinir a senha.
     * Proteções: ninguém desliga ou rebaixa a si mesmo, e sempre sobra pelo menos
     * um administrador ativo (senão ninguém conseguiria mais administrar o sistema).
     */
    @PutMapping("/admin/funcionarios/{id}")
    public Map<String, Object> editarFuncionario(@PathVariable int id, @RequestBody Map<String, Object> dados,
                                                 HttpSession sessao) {
        Map<String, Object> eu = acesso.exigirAdmin(sessao);
        FuncionarioValidado f = validarFuncionario(dados);
        boolean ativo = !(Boolean.FALSE.equals(dados.get("ativo")) || "false".equals(String.valueOf(dados.get("ativo"))));
        boolean souEu = ((Number) eu.get("id")).intValue() == id;

        if (souEu && (!ativo || !ControleAcesso.ADMINISTRADOR.equals(f.perfil))) {
            throw new RegraNegocioException(HttpStatus.CONFLICT,
                    "Você não pode desligar nem tirar o perfil de administrador da sua própria conta.");
        }
        if (!ativo || !ControleAcesso.ADMINISTRADOR.equals(f.perfil)) {
            Integer outrosAdmins = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM funcionario WHERE tipo_perfil = 'Administrador' AND ativo = 1 AND id_funcionario <> ?",
                    Integer.class, id);
            if (outrosAdmins == null || outrosAdmins == 0) {
                throw new RegraNegocioException(HttpStatus.CONFLICT, "O sistema precisa de pelo menos um administrador ativo.");
            }
        }
        if (!ativo) {
            Integer servicos = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM servico WHERE id_responsavel = ? AND ativo = 1", Integer.class, id);
            if (servicos != null && servicos > 0) {
                throw new RegraNegocioException(HttpStatus.CONFLICT,
                        "Este funcionário é responsável por serviços ativos. Troque o responsável desses serviços antes de desligá-lo.");
            }
        }
        try {
            int linhas = jdbcTemplate.update(
                    "UPDATE funcionario SET nome = ?, telefone = ?, email = ?, login = ?, tipo_perfil = ?, ativo = ? "
                            + "WHERE id_funcionario = ?",
                    f.nome, f.telefone, f.email, f.login, f.perfil, ativo ? 1 : 0, id);
            if (linhas == 0) {
                throw new RegraNegocioException(HttpStatus.NOT_FOUND, "Funcionário não encontrado.");
            }
        } catch (DuplicateKeyException e) {
            throw new RegraNegocioException(HttpStatus.CONFLICT, "Já existe um funcionário com esse login.");
        }
        String novaSenha = texto(dados.get("novaSenha"));
        if (!novaSenha.isEmpty()) {
            validarSenhaNova(novaSenha);
            jdbcTemplate.update("UPDATE funcionario SET senha = ? WHERE id_funcionario = ?", encoder.encode(novaSenha), id);
        }
        return Map.of("sucesso", true, "mensagem", "Funcionário atualizado.");
    }

    private record FuncionarioValidado(String nome, String telefone, String email, String login, String perfil) { }

    private FuncionarioValidado validarFuncionario(Map<String, Object> d) {
        String nome = texto(d.get("nome"));
        String telefone = AgendamentoService.somenteDigitos(texto(d.get("telefone")));
        String email = texto(d.get("email"));
        String login = texto(d.get("login")).toLowerCase();
        String perfil = texto(d.get("perfil"));
        if (nome.isEmpty() || nome.length() > 60) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Informe o nome (até 60 caracteres).");
        }
        if (telefone.length() < 10 || telefone.length() > 11) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Telefone inválido. Use DDD + número.");
        }
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") || email.length() > 90) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "E-mail inválido.");
        }
        if (!login.matches("[a-z0-9._-]{3,50}")) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST,
                    "Login deve ter de 3 a 50 letras minúsculas, números, ponto, hífen ou sublinhado.");
        }
        if (!ControleAcesso.ADMINISTRADOR.equals(perfil) && !ControleAcesso.ATENDENTE.equals(perfil)) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Perfil deve ser Administrador ou Atendente.");
        }
        return new FuncionarioValidado(nome, telefone, email, login, perfil);
    }

    // =====================================================================
    // TROCA DA PRÓPRIA SENHA (qualquer funcionário)
    // =====================================================================

    @PutMapping("/minha-senha")
    public Map<String, Object> trocarMinhaSenha(@RequestBody Map<String, Object> dados, HttpSession sessao) {
        Map<String, Object> eu = acesso.exigirLogin(sessao);
        String atual = texto(dados.get("senhaAtual"));
        String nova = texto(dados.get("novaSenha"));
        String hash = jdbcTemplate.queryForObject(
                "SELECT senha FROM funcionario WHERE id_funcionario = ?", String.class, eu.get("id"));
        if (!encoder.matches(atual, hash)) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "A senha atual está incorreta.");
        }
        validarSenhaNova(nova);
        if (nova.equals(atual)) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "A nova senha precisa ser diferente da atual.");
        }
        jdbcTemplate.update("UPDATE funcionario SET senha = ? WHERE id_funcionario = ?", encoder.encode(nova), eu.get("id"));
        return Map.of("sucesso", true, "mensagem", "Senha alterada.");
    }

    private static void validarSenhaNova(String senha) {
        if (senha.length() < SENHA_MINIMA || senha.length() > 72) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST,
                    "A senha precisa ter pelo menos " + SENHA_MINIMA + " caracteres.");
        }
    }

    // =====================================================================
    // UC10 – BLOQUEIOS DE AGENDA
    // =====================================================================

    /** Bloqueios que ainda não terminaram (do mais próximo para o mais distante). */
    @GetMapping("/admin/bloqueios")
    public List<Map<String, Object>> listarBloqueios(HttpSession sessao) {
        acesso.exigirAdmin(sessao);
        return jdbcTemplate.queryForList(
                "SELECT b.id_bloqueio AS id, DATE_FORMAT(b.inicio, '%Y-%m-%dT%H:%i') AS inicio, "
                        + "DATE_FORMAT(b.fim, '%Y-%m-%dT%H:%i') AS fim, b.motivo, b.id_funcionario AS idFuncionario, "
                        + "f.nome AS profissional FROM bloqueio b "
                        + "LEFT JOIN funcionario f ON f.id_funcionario = b.id_funcionario "
                        + "WHERE b.fim > NOW() ORDER BY b.inicio");
    }

    /**
     * Cria um bloqueio. Recebe data, horaInicio e horaFim (ou diaInteiro=true),
     * motivo e, opcionalmente, o profissional. Não deixa bloquear por cima de
     * atendimentos já marcados: avisa quantos existem para o administrador
     * remarcar ou cancelar antes.
     */
    @PostMapping("/admin/bloqueios")
    public Map<String, Object> criarBloqueio(@RequestBody Map<String, Object> dados, HttpSession sessao) {
        acesso.exigirAdmin(sessao);
        String motivo = texto(dados.get("motivo"));
        if (motivo.isEmpty() || motivo.length() > 100) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Informe o motivo (até 100 caracteres).");
        }
        LocalDateTime inicio;
        LocalDateTime fim;
        try {
            LocalDate dia = LocalDate.parse(texto(dados.get("data")));
            boolean diaInteiro = Boolean.TRUE.equals(dados.get("diaInteiro")) || "true".equals(String.valueOf(dados.get("diaInteiro")));
            if (diaInteiro) {
                inicio = dia.atTime(AgendamentoService.ABERTURA);
                fim = dia.atTime(AgendamentoService.FECHAMENTO);
            } else {
                inicio = dia.atTime(LocalTime.parse(texto(dados.get("horaInicio"))));
                fim = dia.atTime(LocalTime.parse(texto(dados.get("horaFim"))));
            }
        } catch (DateTimeParseException e) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Data ou horário inválidos.");
        }
        if (!fim.isAfter(inicio)) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "O fim precisa ser depois do início.");
        }
        Integer idFuncionario = inteiroOuNulo(dados.get("idFuncionario"));
        // Revisão de 21/11/2026: um profissional inexistente virava erro 500 do banco
        if (idFuncionario != null) {
            Integer existe = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM funcionario WHERE id_funcionario = ? AND ativo = 1", Integer.class, idFuncionario);
            if (existe == null || existe == 0) {
                throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Profissional inválido ou desligado.");
            }
        }

        // Atendimentos ativos que cairiam dentro do bloqueio (do profissional, ou de todos)
        List<Map<String, Object>> afetados = jdbcTemplate.queryForList(
                "SELECT a.dataHora, s.duracaoMinutos, a.id_funcionario AS idFuncionario FROM agendamento a "
                        + "JOIN servico s ON s.id_servico = a.id_servico "
                        + "WHERE a.status NOT IN ('Cancelado', 'Concluído') AND a.dataHora < ? AND a.dataHora >= ?",
                fim, inicio.minusDays(1));
        long conflitos = afetados.stream()
                .filter(a -> idFuncionario == null || (a.get("idFuncionario") != null
                        && idFuncionario == ((Number) a.get("idFuncionario")).intValue()))
                .filter(a -> RegraConflito.haSobreposicao(inicio, (int) java.time.Duration.between(inicio, fim).toMinutes(),
                        AgendamentoService.paraLocalDateTime(a.get("dataHora")), ((Number) a.get("duracaoMinutos")).intValue()))
                .count();
        if (conflitos > 0) {
            throw new RegraNegocioException(HttpStatus.CONFLICT, "Já existem " + conflitos
                    + " atendimento(s) marcado(s) nesse período. Remarque ou cancele antes de bloquear.");
        }
        jdbcTemplate.update("INSERT INTO bloqueio (inicio, fim, motivo, id_funcionario) VALUES (?, ?, ?, ?)",
                inicio, fim, motivo, idFuncionario);
        return Map.of("sucesso", true, "mensagem", "Horário bloqueado.");
    }

    @DeleteMapping("/admin/bloqueios/{id}")
    public Map<String, Object> removerBloqueio(@PathVariable int id, HttpSession sessao) {
        acesso.exigirAdmin(sessao);
        int linhas = jdbcTemplate.update("DELETE FROM bloqueio WHERE id_bloqueio = ?", id);
        if (linhas == 0) {
            throw new RegraNegocioException(HttpStatus.NOT_FOUND, "Bloqueio não encontrado.");
        }
        return Map.of("sucesso", true, "mensagem", "Bloqueio removido. Os horários voltaram a ficar livres.");
    }

    // =====================================================================

    private static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor).trim();
    }

    private static Integer inteiroOuNulo(Object valor) {
        String t = texto(valor);
        if (t.isEmpty() || "null".equals(t)) {
            return null;
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Profissional inválido.");
        }
    }
}
