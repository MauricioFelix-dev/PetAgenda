# Revisão geral do código (21/11/2026)

Revisão de ponta a ponta da `main` (commit `512c35e`, depois do PR #5), com foco em bugs,
segurança, qualidade e prontidão para a demonstração. Os testes foram rodados num servidor
Linux com MariaDB 10.11 e o mesmo `banco/petagenda.sql`.

## Resultado antes das correções

| Verificação | Resultado |
|---|---|
| Testes de unidade (`mvn test`, com banco ligado) | 26 de 26 passaram (8 conflito + 8 status + 9 serviço H2 + 1 contexto) |
| Testes de unidade sem MySQL ligado | 25 passam; `ApiApplicationTests.contextLoads` falha porque sobe o sistema inteiro e precisa do banco (esperado; agora explicado no README) |
| Roteiro `testes/TestesCT.java` | 21 passaram, 0 falharam, 2 manuais (CT-09 e CT-13) |
| XSS no front-end | Nenhum dado do banco entra por `innerHTML`: todas as telas usam `textContent` (só textos fixos usam `innerHTML`) |
| SQL injection | Todas as consultas usam parâmetros `?` (confirmado linha a linha e no CT-20) |

## O que foi encontrado e corrigido (correções pequenas e seguras)

| # | Achado | Como foi provado | Correção |
|---|---|---|---|
| 1 | **Fixação de sessão:** o identificador da sessão (`JSESSIONID`) era o mesmo antes e depois do login | `curl`: o cookie anterior ao login continuava valendo depois dele | `fazerLogin` chama `changeSessionId()` ao entrar (`PetAgendaController`) |
| 2 | Cookie de sessão sem `SameSite` | Cabeçalho `Set-Cookie` sem o atributo | `server.servlet.session.cookie.same-site=lax` e `http-only=true` em `application.properties` |
| 3 | Nenhum cabeçalho de segurança HTTP (o painel podia ser aberto dentro de outro site) | `curl -I` sem `X-Frame-Options` | Novo filtro `CabecalhosSeguranca.java`: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: same-origin` |
| 4 | Restrições de saúde e observações sem limite no servidor: um texto com 70 mil caracteres enviado direto à API virava erro 500 | `curl` com JSON grande → "Falha ao gravar no banco" | Limites de 500 e 1000 caracteres, iguais ao `maxlength` da página; responde 400 com mensagem clara. Coberto no CT-10 |
| 5 | Bloqueio para um profissional inexistente virava erro 500 | `curl` com `idFuncionario: 999` | `criarBloqueio` confere se o profissional existe e está ativo (400) |
| 6 | JSON malformado, parâmetro faltando ou com tipo errado (`servico=abc`) respondiam no formato padrão do Spring, sem o campo `erro`; a tela mostrava só "Erro 400" | `curl` | `TratadorDeErros` responde `{"erro": "Dados do pedido inválidos ou incompletos."}` |
| 7 | No painel, apagar a data da agenda fazia a API devolver **todos** os agendamentos e o título virava "Invalid Date" | Leitura do código (`/api/agendamentos?data=` sem filtro) | `atualizarTela()` volta para hoje quando a data fica vazia (testado no navegador) |
| 8 | Mensagem de texto longo não citava o limite da espécie | Leitura do código | Mensagem completa |

Nenhuma regra de negócio mudou. As telas continuam iguais.

## Resultado depois das correções

| Verificação | Resultado |
|---|---|
| Testes de unidade | 26 de 26 passaram |
| Roteiro CT-01 a CT-21 | 21 passaram, 0 falharam, 2 manuais (CT-10 agora inclui a observação com 1001 caracteres) |
| Navegador (Playwright) | Landing com 6 cards, login, agenda do dia, data apagada volta para hoje, agenda da semana com 7 colunas, nenhum erro no console |

## O que ficou como recomendação (não mexi)

| Ponto | Por que não mexi agora |
|---|---|
| Não há limite de tentativas de login | Exigiria uma regra nova (bloqueio temporário) e mudaria o texto do TCC. Para o MVP, basta trocar as senhas de exemplo antes de qualquer uso real; está nas limitações do README |
| Sem HTTPS | Depende da hospedagem; já declarado como limitação |
| A trava `synchronized` só vale para um servidor | Já declarado como limitação no TCC e no README |
| Mudar a duração ou o responsável de um serviço não revalida os agendamentos futuros já marcados | Caso raro no MVP; pode entrar como trabalho futuro |
| Desligar um funcionário não reatribui os agendamentos futuros dele | O sistema já impede desligar quem é responsável por serviço ativo; agendamentos antigos ficam no histórico |
| `listarBloqueios` usa `NOW()` do banco, e o resto usa o fuso configurado na aplicação | Igual no XAMPP local (mesma máquina); só importaria com banco em outro fuso |

## Checklist para a demonstração (pitch de 12/12)

1. Rodar `banco/petagenda.sql` de novo na véspera (agenda limpa) e criar 4 ou 5 agendamentos de exemplo no dia da apresentação.
2. Conferir que o Tomcat do XAMPP está **parado** (porta 8080).
3. Abrir antes, em abas: landing, Meus agendamentos, login. Deixar o painel logado como admin numa janela e como Atendente (ana) em outra janela anônima.
4. Roteiro sugerido (5 min): agendar pela landing → mostrar o código → tentar o mesmo horário (409) → no painel, Confirmar → Iniciar → Concluir (faturamento sobe) → Agenda da semana → Meus agendamentos e cancelar → bloquear uma manhã e mostrar que os horários somem.
5. Ter os prints de `auditoria/prints/bootstrap/` como plano B se o notebook ou a rede falharem.
