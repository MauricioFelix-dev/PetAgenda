# PetAgenda — MVP (Etapa 3)

Sistema web de agendamento para pet shops de pequeno porte.
Landing page pública com autoatendimento, página "Meus agendamentos" para o tutor
e painel com login e perfis (Administrador e Atendente).

## Tecnologias
- Java 17+ · Spring Boot 4 · Spring JDBC (JdbcTemplate)
- MySQL / MariaDB (XAMPP)
- HTML5, CSS3, JavaScript e Bootstrap 5.3 (com Bootstrap Icons)
  - Bootstrap, ícones e fontes ficam em `src/main/resources/static/vendor/`: o site funciona sem internet
  - Tema com a paleta do PetAgenda em `static/css/petagenda.css`; funções comuns em `static/js/petagenda.js`
- BCrypt (spring-security-crypto) para senhas

## Como rodar (Windows + XAMPP)

1. **Banco:** abra o XAMPP, inicie o **MySQL**, entre no phpMyAdmin (http://localhost/phpmyadmin),
   aba **SQL**, cole o conteúdo de `banco/petagenda.sql` e clique em **Executar**.
   > Atenção: o script apaga e recria as tabelas.
   > Para atualizar um banco da Etapa 2 **sem perder os dados**, rode `banco/migracao-etapa3.sql` em vez dele.
2. **Aplicação:** na pasta do projeto, abra o terminal e rode:
   ```
   mvnw.cmd spring-boot:run        (Windows)
   ./mvnw spring-boot:run          (Linux/Mac)
   ```
   Espere aparecer `Started ApiApplication`. Na primeira execução aparece também
   `Usuário inicial criado: admin / admin123`.
3. **Acessar:**
   - Landing page: http://localhost:8080/
   - Meus agendamentos (tutor): http://localhost:8080/meus-agendamentos.html
   - Login: http://localhost:8080/login.html
   - Painel: http://localhost:8080/dashboard.html

   Usuários de teste criados pelo script:
   | Login | Senha | Perfil | Responsável por |
   |---|---|---|---|
   | admin | admin123 | Administrador (Dra. Helena, veterinária e dona) | Consulta Veterinária |
   | ana | ana12345 | Atendente | Banho, Tosa, Banho + Tosa, Hidratação, Escovação |

Se a senha do MySQL não for vazia, ajuste `spring.datasource.password` em
`src/main/resources/application.properties`.

### Problemas comuns no Windows (encontrados no teste de 14/11/2026)
| Mensagem | Causa | Solução |
|---|---|---|
| `'mvnw.cmd' não é reconhecido` | o terminal (PowerShell) não procura programas na pasta atual | use `.\mvnw.cmd spring-boot:run` |
| `JAVA_HOME environment variable is not defined correctly` | o Java está instalado, mas a variável JAVA_HOME não existe | crie a variável de usuário JAVA_HOME com a pasta do JDK (ex.: `C:\Program Files\Java\jdk-...`), coloque `%JAVA_HOME%\bin` no PATH e reabra o terminal/VS Code |
| `Port 8080 was already in use` | o **Tomcat do XAMPP** está ligado e usa a mesma porta | no painel do XAMPP, clique em **Stop** no Tomcat (só o MySQL precisa ficar ligado) |

## Testes automatizados
Testes de unidade (não precisam do MySQL; o `AgendamentoServiceTest` usa um banco H2 em memória):
```
mvnw.cmd test -Dtest=RegraStatusTest,RegraConflitoTest,AgendamentoServiceTest
```
O `AgendamentoServiceTest` prova a transação: força uma falha na última gravação e confere
que tutor e pet não ficaram gravados.

### Roteiro automático dos casos CT-01 a CT-19 (precisa do sistema ligado)
Com o sistema rodando em outro terminal (`mvnw.cmd spring-boot:run`), dê dois cliques em
`testes\rodar-testes.bat` ou rode na pasta do projeto:
```
java testes/TestesCT.java
```
O roteiro escolhe sozinho um dia futuro com a agenda vazia, cria agendamentos fictícios
e confere cada resposta (409, 401, 400 etc.). O resultado aparece na tela e é salvo em
`testes/resultados/resultado-AAAA-MM-DD_HH-mm.md`. CT-09 e CT-13 continuam manuais, e
CT-06 e CT-12 também precisam de print do painel no navegador.

## Regras de negócio implementadas
| Regra | Onde |
|---|---|
| **Antichoque por profissional:** um agendamento não pode se sobrepor a outro ativo **do mesmo profissional**, considerando a **duração** de cada serviço. Profissionais diferentes atendem no mesmo horário | `RegraConflito.java` + `AgendamentoService.estaLivre` (responde **HTTP 409**) |
| Cada serviço tem um profissional responsável; o tutor vê quem vai atender (não escolhe) | `servico.id_responsavel`, copiado para `agendamento.id_funcionario` |
| **Transação:** tutor, pet e agendamento são gravados juntos ou nada é gravado | `@Transactional` em `AgendamentoService.criar` |
| Capacidade de atendimentos simultâneos por profissional (padrão 1) | `petagenda.capacidade-simultanea` em `application.properties` |
| Bloqueio de horários (feriado, folga de um profissional) | tabela `bloqueio`, aba Bloqueios do painel |
| Regra de status: Pendente → Confirmado → Em andamento → Concluído; Cancelado e Concluído são finais | `RegraStatus.java` (mudança proibida responde **HTTP 409**) |
| Landing só oferece horários livres (de 30 em 30 min, 08:00–18:00) | `GET /api/horarios-disponiveis` |
| Tutor consulta e cancela com telefone + código de 6 caracteres | `meus-agendamentos.html`, `POST /api/meus-agendamentos` |
| Perfis: Atendente vê agenda e histórico; só o Administrador cadastra serviços, funcionários e bloqueios | `ControleAcesso.java` (responde **HTTP 403**) |
| Tutor não é duplicado (telefone normalizado só com dígitos + UNIQUE no banco) | `buscarOuCriarTutor` |
| Restrições de saúde do pet ficam destacadas em vermelho no painel | tabela `pet.restricoes_saude` + `dashboard.html` |
| Painel e dados pessoais só são acessíveis com login (sessão no servidor) | rotas restritas do controller |
| Senhas guardadas com hash BCrypt; troca de senha pelo painel | `InicializadorAdmin`, `fazerLogin`, `PUT /api/minha-senha` |
| Indicadores: atendimentos, ocupação da agenda, faturamento, serviço mais procurado | `GET /api/indicadores` |

## Roteiro de testes manuais (simulação)
Registre data, resultado obtido e print de cada caso.

| Caso | Passos | Resultado esperado |
|---|---|---|
| CT-01 Agendamento válido | Na landing, preencher tudo e escolher um horário livre | Toast verde; aparece no painel na data escolhida |
| CT-02 Antichoque (mesmo horário) | Abrir a landing em **duas abas**, escolher o mesmo serviço, data e horário nas duas; enviar a 1ª e depois a 2ª | 2ª aba mostra toast vermelho "Este horário já está ocupado" (HTTP 409) |
| CT-03 Antichoque (duração) | Agendar Banho + Tosa (75 min) às 10:00; depois tentar Banho às 10:30 e às 11:00 | Esses horários não aparecem mais na lista; 11:30 aparece |
| CT-04 Cancelado libera horário | No painel, cancelar o agendamento do CT-03; voltar à landing | O horário volta a ficar disponível |
| CT-05 Tutor sem duplicar | Fazer dois agendamentos com o mesmo telefone escrito de jeitos diferentes, ex. `(99) 98888-7777` e `99988887777` | No phpMyAdmin, a tabela `tutor` tem um único registro |
| CT-06 Restrição de saúde | Agendar informando "alergia a shampoo perfumado" | Card no painel com faixa vermelha de alerta |
| CT-07 Acesso sem login | Em janela anônima, abrir http://localhost:8080/api/agendamentos | Resposta 401 "Faça login para acessar" |
| CT-08 Senha errada | Login com senha incorreta | Mensagem de erro; não entra |
| CT-09 Senha criptografada | No phpMyAdmin, ver `funcionario.senha` | Texto começando com `$2a$` (hash), não "admin123" |
| CT-10 Data passada / dados inválidos | Tentar telefone com 5 dígitos | Toast vermelho com a mensagem de validação |
| CT-11 Fluxo de status | No painel: Iniciar → Concluir | Status muda; faturamento do dia aumenta |
| CT-12 Texto malicioso (XSS) | Nas observações, digitar `<img src=x onerror=alert(1)>` | No painel o texto aparece literalmente; nenhum alerta é executado |
| CT-13 Responsividade | Abrir a landing e o painel no celular (mesma rede, IP do PC) ou no modo responsivo do navegador (F12) | Layout se adapta sem quebrar |
| CT-14 Status proibido | No painel, tentar mudar um agendamento Concluído ou Cancelado | Não há botão; pela API responde 409 |
| CT-15 Profissionais diferentes | Agendar Banho às 08:00 e Consulta às 08:00 no mesmo dia | Os dois são aceitos (Ana e Dra. Helena) |
| CT-16 Bloqueio | Aba Bloqueios: bloquear a manhã de um dia; ver os horários na landing; remover | A manhã some da landing e volta depois de remover |
| CT-17 Perfis | Entrar com ana / ana12345 | Não aparecem as abas Serviços, Funcionários e Bloqueios |
| CT-18 Meus agendamentos | Agendar, guardar o código, abrir Meus agendamentos e cancelar | O agendamento aparece e muda para Cancelado |
| CT-19 Serviços e senha | Aba Serviços: cadastrar e desativar um serviço; "Trocar minha senha" | O serviço aparece e some da landing; a senha muda |

## Limitações conhecidas (declarar no relatório)
- Funciona com **um único servidor**: a proteção contra duas reservas simultâneas usa `synchronized` em memória.
- O tutor não escolhe o profissional: cada serviço tem um responsável fixo, que o administrador pode trocar.
- Um agendamento tem um único serviço (o pacote "Banho + Tosa" cobre o caso comum).
- O tutor não tem login; ele usa telefone + código do agendamento.
- O aceite da política de privacidade não é gravado no banco.
- Não há pagamento, notificação por WhatsApp ou calendário visual.
- A sessão usa cookie padrão do servidor, sem HTTPS (adequado para teste local, não para produção).
