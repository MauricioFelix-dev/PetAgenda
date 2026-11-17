# Auditoria 01 – Inventário do que existe de fato (foco na interface)

- **Data:** 17/11/2026 · **Base:** branch `main`, commit `df374cd` · **Modo:** somente leitura
- Escopo desta rodada: interface e usabilidade. Banco (prompt 3) e back-end (prompt 4) não foram auditados em profundidade.

## 1. Arquivos e papel de cada um

| Arquivo | Papel |
|---|---|
| `src/main/resources/static/index.html` (904 linhas) | Landing page pública: apresentação, cards de serviços, "como funciona" e formulário de agendamento (HTML + CSS + JS no mesmo arquivo) |
| `src/main/resources/static/login.html` (162 linhas) | Tela de login dos funcionários |
| `src/main/resources/static/dashboard.html` (311 linhas) | Painel administrativo: indicadores, agenda por data e troca de status |
| `src/main/java/com/petagenda/api/PetAgendaController.java` | Toda a API REST (um único controller) |
| `src/main/java/com/petagenda/api/RegraConflito.java` | Regra antichoque (sobreposição de intervalos) |
| `src/main/java/com/petagenda/api/InicializadorAdmin.java` | Cria o usuário inicial `admin` com senha em BCrypt |
| `src/main/resources/application.properties` | Conexão com MySQL, porta 8080, capacidade simultânea = 1 |
| `banco/petagenda.sql` | Script que cria o banco e os 6 serviços |
| `src/test/.../RegraConflitoTest.java`, `testes/TestesCT.java` | Testes unitários do antichoque e roteiro automático CT-01 a CT-13 |
| `testes/resultados/resultado-2026-11-14_16-54.md` | Último resultado: 13 passaram, 0 falharam, 2 manuais |

## 2. Versões

Java 17 (`pom.xml:30`), Spring Boot 4.0.5 (`pom.xml:8`), `spring-boot-starter-data-jdbc`, `spring-boot-starter-webmvc`, `spring-security-crypto` (BCrypt), `mysql-connector-j`. **Front-end:** HTML, CSS e JavaScript puro, sem framework e **sem Bootstrap**; fontes Playfair Display e DM Sans carregadas do Google Fonts (`index.html:7-9`). [Certo]

## 3. Endpoints usados pelas telas

| Método e rota | Tela que usa | Login? | Arquivo:linha |
|---|---|---|---|
| `GET /api/servicos` | landing (select de serviço) | não | `PetAgendaController.java:93` |
| `GET /api/horarios-disponiveis?data&servico` | landing (select de horário) | não | `:99` |
| `POST /api/agendamento` | landing (envio) – 200, 400 ou 409 | não | `:133` |
| `POST /api/login` / `POST /api/logout` | login / painel | — | `:208` / `:228` |
| `GET /api/agendamentos?data` | painel (cards) | sim (401) | `:238` |
| `PUT /api/agendamento/{id}/status` | painel (Iniciar, Concluir, Cancelar) | sim | `:262` |
| `GET /api/indicadores?data` | painel (indicadores) | sim | `:287` |

## 4. Telas

| Tela | O que faz | Observação de interface |
|---|---|---|
| Landing (`/`) | Mostra serviços e permite pedir horário; só oferece horários livres; mostra toast verde (sucesso) ou vermelho (erro da API) | Hero com números fictícios; menu some no celular (ver 05-frontend) |
| Login (`/login.html`) | Envia login e senha; guarda o nome em `sessionStorage` só para exibir | Mensagem de erro única para qualquer falha |
| Painel (`/dashboard.html`) | 7 indicadores, filtro por data, cards com alerta de saúde e botões de status | Usa `confirm()`/`alert()` |

## 5. Como rodar do zero

Ver `README.md` ("Como rodar"). Para analisar só a interface sem banco, as telas podem ser abertas por um servidor estático com a API simulada — foi o que esta auditoria fez para tirar os prints (ver `05-frontend.md`, Método).

## 6. Requisitos de interface × código

| Requisito | Existe? | Evidência |
|---|---|---|
| RF02 Solicitação eletrônica de horário | Sim | `index.html:706-771`, `:868-902` |
| RF03 Painel com grade diária | Sim (lista de cards por data, não uma grade/calendário visual) | `dashboard.html:123-136`, `:207-221` |
| RNF01 Autenticação | Sim | `login.html`, `PetAgendaController.java:208` |
| RNF03 Usabilidade/responsividade | **Parcial.** Responsivo (sem rolagem horizontal em 360/768/1280 px), mas com falhas de contraste, menu ausente no celular e mensagens sem acessibilidade | `index.html:541-547`, `dashboard.html:110`; ver 05-frontend |
| Aviso LGPD na interface | **Não** | 0 ocorrências de "privacidade"/"LGPD" nos HTML |
