# 17. Teste de SQL injection (CT-20) e Agenda da semana (CT-21) — 21/11/2026

## O que foi feito

### CT-20: teste automático de SQL injection
Antes, a proteção contra SQL injection só tinha sido conferida **lendo o código** (todas as consultas
usam `?`, os parâmetros do `JdbcTemplate`). Agora há um caso de teste automático no roteiro
`testes/TestesCT.java` que tenta atacar o sistema por seis caminhos:

| Tentativa | Resposta esperada | Resultado |
|---|---|---|
| Login `admin' OR '1'='1` com senha `x' OR '1'='1` | 401 (não entra) | 401 |
| Login `admin' -- ` (tentar "comentar" a checagem da senha) | 401 | 401 |
| Painel depois dessas tentativas | 401 (continua sem login) | 401 |
| Meus agendamentos com `' OR '1'='1` no telefone e no código | não mostrar nada (400 ou 404) | 404 |
| `servico=1 OR 1=1` na lista de horários | 400 (não é número) | 400 |
| `data=AAAA-MM-DD' OR '1'='1` na agenda do painel | 400 (data inválida) | 400 |
| Histórico com telefone `' OR '1'='1` | lista vazia | `[]` |
| Agendar com o nome `Robert'); DROP TABLE tutor;--` e observação `' OR 1=1; DELETE FROM agendamento;--` | 200, gravado como texto, tabelas intactas | 200, gravado como texto, os outros agendamentos continuam lá |

**Por que funciona (para explicar na banca):** com `?`, o comando SQL vai para o banco já pronto e
o texto digitado vai separado, como um valor. O banco nunca lê o texto como parte do comando.
Por isso o nome "Robert'); DROP TABLE tutor;--" aparece no painel exatamente assim, como nome.

### CT-21 e a nova aba "Agenda da semana" (RF03)
O documento pede um **calendário interativo**; até aqui o painel só mostrava um dia por vez.
Agora o menu do painel tem a aba **Agenda da semana**:

- mostra de segunda a domingo, com os atendimentos de cada dia em ordem de horário;
- cada atendimento mostra hora, pet, serviço, responsável e status (cor da borda igual à da agenda do dia);
- triângulo vermelho quando o pet tem restrição de saúde; cancelado aparece riscado;
- resumo no topo: atendimentos da semana, quantos têm alerta de saúde e quantos foram cancelados;
- botões Anterior, Esta semana e Próxima; o dia de hoje fica destacado;
- clicar em um dia ou em um atendimento abre a **Agenda do dia** naquela data (onde se muda o status);
- no celular, os dias ficam um embaixo do outro (sem rolagem lateral);
- Atendente e Administrador veem a aba (é tela de atendimento, não de administração).

No servidor, a mudança é pequena: a rota que já existia `GET /api/agendamentos` aceita agora
`?semana=AAAA-MM-DD`. O servidor calcula a segunda-feira daquela semana e faz **uma consulta só**
(de segunda 00:00 até a segunda seguinte), com os mesmos `?` de sempre. Exige login (401 sem login)
e recusa data inválida (400).

Arquivos alterados: `PetAgendaController.java` (parâmetro `semana`), `dashboard.html` (aba e
função `carregarSemana`), `petagenda.css` (estilos `.semana-*`), `TestesCT.java` (CT-20 e CT-21),
`rodar-testes.bat` e `README.md`.

Correção de acessibilidade aproveitada: as linhas de serviço **desativado** na aba Serviços tinham
contraste baixo (o axe-core apontou quando havia serviço desativado na lista). A cor foi escurecida.

## Testes (21/11/2026, servidor Linux na nuvem, MariaDB 10.11)
| Teste | Resultado |
|---|---|
| Testes de unidade (JUnit) | 26 passaram, 0 falharam (sem mudança) |
| Roteiro `testes/TestesCT.java` (CT-01 a CT-21) | **21 passaram**, 0 falharam, 2 manuais |
| Fluxo de tela automatizado | 18 de 18 passos (o Atendente agora vê 3 abas: dia, semana e histórico) |
| Agenda da semana na tela | 7 colunas, clique abre o dia certo, navegação de semanas, sem erro de JavaScript |
| axe-core (WCAG 2.0/2.1 A e AA) | 0 problemas em todas as páginas e abas, incluindo a nova |
| Celular (390 px) | sem rolagem lateral |

Relatório do roteiro: `testes/resultados/resultado-2026-11-21_01-15.md`.

## O que muda no texto do TCC
(Somar à lista do `16-bootstrap-nova-interface.md`.)

| Onde | O que diz hoje | O que passa a dizer |
|---|---|---|
| Seção de testes / quadro dos casos de teste | CT-01 a CT-19 | CT-01 a **CT-21**: acrescentar as linhas CT-20 (SQL injection) e CT-21 (Agenda da semana), com o texto da tabela do README |
| Resultado do roteiro automático | "19 passaram, 0 falharam" | "**21 passaram**, 0 falharam, 2 verificados manualmente" |
| Segurança / OWASP (injeção) | Proteção contra SQL injection verificada por inspeção do código (consultas parametrizadas) | "verificada por inspeção do código **e pelo caso de teste CT-20**, que tenta seis formas de injeção sem sucesso" |
| RF03 / quadro "Previsto × implementado" | Calendário: parcial (agenda por dia) | "Agenda do dia e **agenda da semana** em forma de calendário, com navegação entre semanas" (atendido; só não há visão de mês) |
| UC05 Visualizar agenda (descrição do caso de uso) | O funcionário escolhe o dia | Acrescentar: "ou vê a semana inteira e clica em um dia para abri-lo" |
| Limitações | "não há calendário visual" (se estiver no texto) | Trocar por "não há visão mensal" |
| Diagrama de classes / API | `listarAgendamentos(data, telefone)` | `listarAgendamentos(data, semana, telefone)` (só se o diagrama mostrar os parâmetros) |
| Testes de unidade | 26 | continua 26 |

Na avaliação do site (Claude Doc), o item 5 (CT-20) e o item 6 (visão semanal) da lista de
prioridades ficam **feitos**, e a promessa RF03 passa de "parcial" para "atendida".

## Prints
| Figura | Arquivo | Situação |
|---|---|---|
| E.5 Painel do administrador | `13-painel-agenda.png` | **Refeito** (o menu agora tem "Agenda da semana") |
| E.7 Painel do Atendente | `24-painel-atendente.png` | **Refeito** (3 abas) |
| Nova: Agenda da semana | `25-agenda-semana.png` | **Novo**: sugerimos como figura E.8 |
| Nova (opcional): Agenda da semana no celular | `26-celular-agenda-semana.png` | Novo |
| Diálogo de status, painel no celular e menu do celular | `14-…`, `21-…`, `22-…` | Refeitos pelo mesmo motivo |

Os prints 15 a 18 (histórico, serviços, funcionários, bloqueios) foram tirados antes da nova aba:
o menu lateral deles tem um item a menos. Não estão no Apêndice E; se forem usados, vale refazer.

Os prints refeitos usam uma semana de exemplo (11 a 17/01/2027) com nomes fictícios, para não
aparecerem os dados dos testes automáticos.
