// =====================================================================
// PetAgenda - Roteiro automatico dos casos de teste CT-01 a CT-13
//
// O QUE FAZ: conversa com o sistema JA RODANDO (http://localhost:8080)
// do mesmo jeito que a landing page e o painel fazem, e confere se cada
// resposta e a esperada. No final grava um relatorio em
// testes/resultados/resultado-AAAA-MM-DD_HH-mm.md
//
// COMO RODAR (com o sistema ja ligado em outro terminal):
//     testes\rodar-testes.bat          (Windows)
//     java testes/TestesCT.java        (qualquer sistema, Java 17+)
//
// Nao precisa de Maven nem de biblioteca extra: usa so o Java padrao.
// Este arquivo esta propositalmente SEM acentos, porque no Windows o
// Java le o codigo-fonte em outro padrao de caracteres e os acentos
// quebravam a compilacao.
//
// Os testes nao apagam nada: eles escolhem automaticamente um dia futuro
// com a agenda vazia e criam agendamentos ficticios nesse dia.
// =====================================================================

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestesCT {

    static String BASE = "http://localhost:8080";
    static String LOGIN = "admin";
    static String SENHA = "admin123";

    // Valor de status com acento, escrito com codigo unicode para o arquivo ficar sem acentos
    static final String CONCLUIDO = "Conclu\u00eddo";

    static HttpClient cliente;            // com cookie (sessao do funcionario)
    static final List<String[]> linhas = new ArrayList<>();  // caso, resultado, detalhe
    static int passou = 0, falhou = 0, manual = 0;

    public static void main(String[] args) throws Exception {
        if (args.length > 0) BASE = args[0];
        if (args.length > 2) { LOGIN = args[1]; SENHA = args[2]; }

        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

        System.out.println("PetAgenda - testes automaticos em " + BASE);
        try {
            get("/api/servicos");
        } catch (IOException e) {
            System.out.println();
            System.out.println("ERRO: o sistema nao respondeu em " + BASE + ".");
            System.out.println("Ligue o sistema primeiro (mvnw.cmd spring-boot:run) e rode de novo.");
            System.exit(2);
        }

        LocalDate dia = escolherDiaVazio();
        System.out.println("Dia escolhido para os testes (agenda vazia): " + dia);
        System.out.println();

        // ---------------------------------------------------------------
        // CT-01 Agendamento valido
        Resp r = agendar("Ana Teste", "(99) 98888-7777", "Rex", 1, dia, "08:00", "", "");
        registrar("CT-01 Agendamento valido", r.status == 200,
                "Banho 08:00 -> HTTP " + r.status + " (esperado 200)");

        // ---------------------------------------------------------------
        // CT-02 Antichoque mesmo horario
        r = agendar("Bia Teste", "99977776666", "Mel", 1, dia, "08:00", "", "");
        registrar("CT-02 Antichoque (mesmo horario)", r.status == 409,
                "2o pedido Banho 08:00 -> HTTP " + r.status + " (esperado 409)");

        // CT-02b concorrencia: 5 pedidos ao MESMO tempo no mesmo horario
        int[] cont = concorrencia(dia, "09:00", 5);
        registrar("CT-02b Pedidos simultaneos", cont[0] == 1 && cont[1] == 4,
                "5 pedidos simultaneos as 09:00 -> " + cont[0] + " gravado(s) (200) e " + cont[1] + " recusado(s) (409)");

        // ---------------------------------------------------------------
        // CT-03 Antichoque pela duracao (Banho + Tosa = 75 min)
        Resp bt = agendar("Caio Teste", "99966665555", "Thor", 4, dia, "10:00", "", "");
        Resp b1030 = agendar("Dani Teste", "99955554444", "Lua", 1, dia, "10:30", "", "");
        Resp b1100 = agendar("Dani Teste", "99955554444", "Lua", 1, dia, "11:00", "", "");
        String livres = get("/api/horarios-disponiveis?data=" + dia + "&servico=1").corpo;
        boolean listaOk = !livres.contains("\"10:00\"") && !livres.contains("\"10:30\"")
                && !livres.contains("\"11:00\"") && livres.contains("\"11:30\"");
        registrar("CT-03 Antichoque (duracao)",
                bt.status == 200 && b1030.status == 409 && b1100.status == 409 && listaOk,
                "Banho+Tosa 10:00 -> " + bt.status + "; Banho 10:30 -> " + b1030.status
                        + "; Banho 11:00 -> " + b1100.status + "; lista de livres tem 11:30 e nao tem 10:00-11:00: "
                        + (listaOk ? "sim" : "NAO"));

        // ---------------------------------------------------------------
        // CT-07 Acesso sem login (antes de logar)
        int s1 = get("/api/agendamentos").status;
        int s2 = get("/api/indicadores").status;
        int s3 = put("/api/agendamento/1/status", "{\"status\":\"Cancelado\"}").status;
        registrar("CT-07 Acesso sem login", s1 == 401 && s2 == 401 && s3 == 401,
                "agendamentos -> " + s1 + ", indicadores -> " + s2 + ", alterar status -> " + s3 + " (esperado 401)");

        // ---------------------------------------------------------------
        // CT-08 Senha errada
        Resp lErr = post("/api/login", "{\"login\":\"" + LOGIN + "\",\"senha\":\"senha-errada-123\"}");
        int depois = get("/api/agendamentos").status;
        registrar("CT-08 Senha errada", lErr.status == 401 && depois == 401,
                "login com senha errada -> " + lErr.status + "; painel depois -> " + depois);

        // Login correto (necessario para os proximos casos)
        Resp lOk = post("/api/login", "{\"login\":\"" + LOGIN + "\",\"senha\":\"" + SENHA + "\"}");
        if (lOk.status != 200) {
            registrar("LOGIN admin", false, "Nao consegui entrar com " + LOGIN + " -> " + lOk.status
                    + ". Rode o resto passando login e senha: java testes/TestesCT.java " + BASE + " LOGIN SENHA");
            terminar(dia);
            return;
        }

        // ---------------------------------------------------------------
        // CT-04 Cancelado libera o horario
        String lista = get("/api/agendamentos?data=" + dia).corpo;
        String idBT = idDoAgendamento(lista, "10:00");
        Resp canc = put("/api/agendamento/" + idBT + "/status", "{\"status\":\"Cancelado\"}");
        String livres2 = get("/api/horarios-disponiveis?data=" + dia + "&servico=1").corpo;
        Resp b1030b = agendar("Dani Teste", "99955554444", "Lua", 1, dia, "10:30", "", "");
        registrar("CT-04 Cancelado libera horario",
                canc.status == 200 && livres2.contains("\"10:30\"") && b1030b.status == 200,
                "cancelar Banho+Tosa -> " + canc.status + "; 10:30 voltou a lista: "
                        + (livres2.contains("\"10:30\"") ? "sim" : "NAO") + "; agendar 10:30 -> " + b1030b.status);

        // ---------------------------------------------------------------
        // CT-05 Telefone em formatos diferentes nao duplica tutor
        Resp t1 = agendar("Ana Teste", "99988887777", "Rex", 2, dia, "14:00", "", "");
        Resp t2 = agendar("Ana Teste", "+55 (99) 98888-7777", "Rex", 1, dia, "15:00", "", "");
        lista = get("/api/agendamentos?data=" + dia).corpo;
        int qtdMesmoTel = contar(lista, "\"telefone\":\"99988887777\"");
        registrar("CT-05 Tutor sem duplicar", t1.status == 200 && t2.status == 200 && qtdMesmoTel == 3,
                "3 formatos do mesmo telefone -> " + t1.status + "/" + t2.status
                        + "; os 3 agendamentos ficaram com o telefone 99988887777: " + (qtdMesmoTel == 3 ? "sim" : "NAO (" + qtdMesmoTel + ")")
                        + ". Confirmar no phpMyAdmin: SELECT * FROM tutor WHERE telefone='99988887777' (1 linha)");

        // ---------------------------------------------------------------
        // CT-06 Restricao de saude
        Resp rs = agendar("Eva Teste", "99944443333", "Nina", 1, dia, "16:00", "alergia a shampoo perfumado", "");
        lista = get("/api/agendamentos?data=" + dia).corpo;
        registrar("CT-06 Restricao de saude (dado)", rs.status == 200 && lista.contains("alergia a shampoo perfumado"),
                "restricao gravada e devolvida ao painel. A FAIXA VERMELHA precisa ser conferida no navegador (print)");

        // ---------------------------------------------------------------
        // CT-09 Senha criptografada (so da para ver no banco)
        registrarManual("CT-09 Senha criptografada",
                "phpMyAdmin > tabela funcionario > coluna senha deve comecar com $2a$ (60 caracteres)");

        // ---------------------------------------------------------------
        // CT-10 Dados invalidos
        LocalDate ontem = LocalDate.now().minusDays(1);
        int[] inval = {
                agendar("Fabi", "12345", "Bob", 1, dia, "17:00", "", "").status,          // telefone curto
                agendar("Fabi", "99933332222", "Bob", 1, ontem, "17:00", "", "").status,  // data passada
                agendar("Fabi", "99933332222", "Bob", 1, dia, "07:30", "", "").status,    // antes de abrir
                agendar("Fabi", "99933332222", "Bob", 4, dia, "17:00", "", "").status,    // passa das 18:00
                agendar("", "99933332222", "Bob", 1, dia, "17:00", "", "").status,        // sem nome
                agendar("Fabi", "99933332222", "Bob", 99, dia, "17:00", "", "").status,   // servico inexistente
        };
        boolean todos400 = true;
        StringBuilder sb = new StringBuilder();
        for (int s : inval) { todos400 &= (s == 400); sb.append(s).append(' '); }
        registrar("CT-10 Dados invalidos", todos400,
                "telefone curto, data passada, 07:30, Banho+Tosa 17:00, nome vazio, servico 99 -> " + sb.toString().trim() + " (esperado 400 em todos)");

        // ---------------------------------------------------------------
        // CT-11 Fluxo de status e faturamento
        lista = get("/api/agendamentos?data=" + dia).corpo;
        String id08 = idDoAgendamento(lista, "08:00");
        String fatAntes = campo(get("/api/indicadores?data=" + dia).corpo, "faturamentoDia");
        int e1 = put("/api/agendamento/" + id08 + "/status", "{\"status\":\"Em andamento\"}").status;
        int e2 = put("/api/agendamento/" + id08 + "/status", "{\"status\":\"" + CONCLUIDO + "\"}").status;
        String fatDepois = campo(get("/api/indicadores?data=" + dia).corpo, "faturamentoDia");
        boolean subiu = paraNumero(fatDepois) > paraNumero(fatAntes);
        registrar("CT-11 Fluxo de status", e1 == 200 && e2 == 200 && subiu,
                "Iniciar -> " + e1 + ", Concluir -> " + e2 + "; faturamento do dia " + fatAntes + " -> " + fatDepois);

        // ---------------------------------------------------------------
        // CT-12 Texto malicioso (XSS)
        Resp x = agendar("Gil Teste", "99922221111", "Zeca", 1, dia, "17:00", "", "<img src=x onerror=alert(1)>");
        lista = get("/api/agendamentos?data=" + dia).corpo;
        registrar("CT-12 XSS (dado)", x.status == 200 && lista.contains("<img src=x onerror=alert(1)>"),
                "texto guardado como texto. No PAINEL deve aparecer escrito e NENHUM alerta pode abrir (print)");

        // ---------------------------------------------------------------
        // CT-13 Responsividade
        registrarManual("CT-13 Responsividade",
                "abrir landing e painel no celular (http://IP-DO-PC:8080) ou F12 > modo celular; tirar print");

        // Logout e confirmacao
        post("/api/logout", "{}");
        int aposLogout = get("/api/agendamentos").status;
        registrar("Extra: logout", aposLogout == 401, "depois de sair, painel -> " + aposLogout);

        terminar(dia);
    }

    // =================================================================
    // Relatorio
    // =================================================================

    static void terminar(LocalDate dia) throws IOException {
        LocalDateTime agora = LocalDateTime.now();
        StringBuilder md = new StringBuilder();
        md.append("# PetAgenda - Resultado dos testes automaticos\n\n");
        md.append("- Data/hora da execucao: ").append(agora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
        md.append("- Sistema testado: ").append(BASE).append("\n");
        md.append("- Dia usado na agenda: ").append(dia).append("\n");
        md.append("- Java: ").append(System.getProperty("java.version"))
          .append(" | Sistema operacional: ").append(System.getProperty("os.name")).append("\n");
        md.append("- Resumo: ").append(passou).append(" passou, ").append(falhou).append(" falhou, ")
          .append(manual).append(" verificar manualmente\n\n");
        md.append("| Caso | Resultado | Detalhe |\n|---|---|---|\n");
        for (String[] l : linhas) {
            md.append("| ").append(l[0]).append(" | ").append(l[1]).append(" | ")
              .append(l[2].replace("|", "/").replace("<", "&lt;").replace(">", "&gt;")).append(" |\n");
        }
        md.append("\nObs.: os casos CT-06, CT-12 e CT-13 tambem precisam de print do navegador; ")
          .append("CT-09 e CT-05 podem ser confirmados no phpMyAdmin.\n");

        Path pasta = Path.of("testes", "resultados");
        Files.createDirectories(pasta);
        Path arq = pasta.resolve("resultado-" + agora.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + ".md");
        Files.writeString(arq, md.toString(), StandardCharsets.UTF_8);

        System.out.println();
        System.out.println("RESUMO: " + passou + " passou | " + falhou + " falhou | " + manual + " verificar manualmente");
        System.out.println("Relatorio salvo em: " + arq.toAbsolutePath());
        if (falhou > 0) System.exit(1);
    }

    static void registrar(String caso, boolean ok, String detalhe) {
        if (ok) passou++; else falhou++;
        linhas.add(new String[]{caso, ok ? "PASSOU" : "FALHOU", detalhe});
        System.out.printf("[%s] %s%n         %s%n", ok ? "PASSOU" : "FALHOU", caso, detalhe);
    }

    static void registrarManual(String caso, String detalhe) {
        manual++;
        linhas.add(new String[]{caso, "MANUAL", detalhe});
        System.out.printf("[MANUAL] %s%n         %s%n", caso, detalhe);
    }

    // =================================================================
    // Auxiliares
    // =================================================================

    /** Procura, a partir de daqui a 7 dias, um dia util com todos os 20 horarios de Banho livres. */
    static LocalDate escolherDiaVazio() throws Exception {
        LocalDate d = LocalDate.now().plusDays(7);
        for (int i = 0; i < 365; i++, d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            String livres = get("/api/horarios-disponiveis?data=" + d + "&servico=1").corpo;
            if (contar(livres, "\"") / 2 == 20) return d;
        }
        throw new IllegalStateException("Nao achei dia com agenda vazia no proximo ano.");
    }

    static Resp agendar(String tutor, String tel, String pet, int servico, LocalDate data,
                        String hora, String restricoes, String obs) throws Exception {
        String json = "{\"tutor\":\"" + esc(tutor) + "\",\"telefone\":\"" + esc(tel) + "\",\"pet\":\"" + esc(pet)
                + "\",\"especie\":\"Cao\",\"raca\":\"SRD\",\"servico\":\"" + servico + "\",\"data\":\"" + data
                + "\",\"horario\":\"" + hora + "\",\"restricoes\":\"" + esc(restricoes) + "\",\"obs\":\"" + esc(obs) + "\"}";
        return post("/api/agendamento", json);
    }

    /** Dispara n pedidos ao mesmo tempo. Devolve {quantos 200, quantos 409}. */
    static int[] concorrencia(LocalDate dia, String hora, int n) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        List<Future<Resp>> fs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            final int k = i;
            fs.add(pool.submit(() -> agendar("Simultaneo " + k, "9991111000" + k, "P" + k, 1, dia, hora, "", "")));
        }
        int ok = 0, conflito = 0;
        for (Future<Resp> f : fs) {
            int s = f.get().status;
            if (s == 200) ok++; else if (s == 409) conflito++;
        }
        pool.shutdown();
        return new int[]{ok, conflito};
    }

    /** Acha o id do agendamento NAO cancelado que comeca no horario indicado. */
    static String idDoAgendamento(String listaJson, String hora) {
        Matcher m = Pattern.compile("\\{[^{}]*\\}").matcher(listaJson);
        while (m.find()) {
            String obj = m.group();
            if (obj.contains("T" + hora + "\"") && !obj.contains("\"Cancelado\"")) {
                return campo(obj, "id");
            }
        }
        return "0";
    }

    /** Le um campo simples de um JSON (numero ou texto), sem biblioteca. */
    static String campo(String json, String nome) {
        Matcher m = Pattern.compile("\"" + nome + "\":\"?([^,\"}]*)").matcher(json);
        return m.find() ? m.group(1) : "";
    }

    static double paraNumero(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return -1; }
    }

    static int contar(String texto, String trecho) {
        int n = 0, i = 0;
        while ((i = texto.indexOf(trecho, i)) >= 0) { n++; i += trecho.length(); }
        return n;
    }

    static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    record Resp(int status, String corpo) { }

    static Resp get(String caminho) throws IOException, InterruptedException {
        return enviar(HttpRequest.newBuilder(URI.create(BASE + caminho)).GET());
    }

    static Resp post(String caminho, String json) throws IOException, InterruptedException {
        return enviar(HttpRequest.newBuilder(URI.create(BASE + caminho))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)));
    }

    static Resp put(String caminho, String json) throws IOException, InterruptedException {
        return enviar(HttpRequest.newBuilder(URI.create(BASE + caminho))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)));
    }

    static Resp enviar(HttpRequest.Builder b) throws IOException, InterruptedException {
        HttpResponse<String> r = cliente.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Resp(r.statusCode(), r.body());
    }
}
