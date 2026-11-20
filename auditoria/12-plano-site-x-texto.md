# Plano: ajustar o site ao texto e aos diagramas (18/11/2026)

> **Decisão da dupla (18/11):** pacotes A e B aprovados. O item 9 mudou: em vez de mudar o texto, cada serviço passou a ter um **profissional responsável** e o antichoque é verificado por profissional (o tutor vê quem vai atender, não escolhe). Pagamento continua como limitação. O resultado está em `13-diagramas-etapa3.md`.

**Base da comparação:** código na `main` (com o PR #2 incorporado), `banco/petagenda.sql`, Projeto de TCC corrigido (Nota 1), Nota 3, e os diagramas de casos de uso, classes e sequência da pasta do projeto.

**Prazo:** entrega escrita em 27/11. O esforço abaixo é o tempo de programação. Depois de cada pacote, vocês precisam rodar o sistema, testar e tirar prints (mais ou menos 1 hora por pacote).

> Aviso importante: os itens que mudam o banco (A3, A4, A5, B1) exigem rodar de novo o `banco/petagenda.sql` no phpMyAdmin, e **esse script apaga os dados de teste**. Vou deixar o script atualizado e, se der, um script de migração que não apaga nada.

## O que o texto e os diagramas prometem × o que existe

| # | Promessa (onde) | Hoje no código | Decisão sugerida |
|---|---|---|---|
| 1 | "Controle de concorrência em transações", ACID, "verificação transacional" (Nota 1, Introdução, 3.3 e 3.4) | `synchronized`, sem `@Transactional`. Se a gravação do agendamento falhar, o tutor e o pet ficam gravados mesmo assim | **Implementar (A1)** |
| 2 | Métodos `confirmar()`, `cancelar()`, `finalizar()` (diagrama de classes); "a equipe confirma" (landing) | Não existe o status "Confirmado". A API aceita qualquer troca, inclusive reativar um agendamento cancelado sem passar pelo antichoque | **Implementar (A2)** |
| 3 | UC07 Cadastrar serviços e preços; `Servico.cadastrar/editar/desativar` | Só pelo banco | **Implementar (A3)** |
| 4 | UC08 Cadastrar profissionais; Administrador herda de Funcionário; `Administrador.cadastrarFuncionario()` | Só pelo banco. `tipo_perfil` nunca é verificado: qualquer funcionário tem o mesmo acesso do administrador. Sem troca de senha | **Implementar (A4)** |
| 5 | UC10 Bloquear horários na agenda; `Administrador.bloquearHorario()` | Não existe | **Implementar (A5)**: conversa direto com a regra antichoque, que é o centro do TCC |
| 6 | Pet com `idade` (diagrama de classes e de sequência) | A coluna existe no banco, mas o formulário não pede | **Implementar (A6)**, campo opcional |
| 7 | UC03 Cancelar agendamento e UC04 Ver histórico, pelo **cliente** | Não existe. A justificativa da Nota 1 fala em "histórico de atendimentos" | **Opcional (B1)**, ver abaixo |
| 8 | UC04 / `consultarHistorico()` pelo funcionário; UC11 Relatórios e métricas | O painel mostra só um dia; os indicadores existem | **Opcional (B2)**: busca do histórico por telefone do tutor no painel |
| 9 | Conflito "para o mesmo profissional" (Nota 1, Introdução e 3.5; Nota 3) | Capacidade total configurável, sem escolher profissional | **Não implementar. Mudar o texto.** Fazer direito exige refazer o cálculo de horários livres, a escolha na landing e todos os testes do antichoque (cerca de 10 a 12 h). Dá para explicar na banca: "a capacidade simultânea representa quantos profissionais atendem ao mesmo tempo" |
| 10 | Classe `Pagamento` | Não existe | **Não implementar.** Tirar do diagrama ou marcar como trabalho futuro |
| 11 | Agendamento–Serviço `1..*` | Um serviço por agendamento (o pacote "Banho + Tosa" cobre o caso comum) | **Corrigir o diagrama** para `1` |
| 12 | Tutor com `email` e `endereco` | Colunas existem, não são usadas | **Não coletar** (princípio da necessidade da LGPD, art. 6º, III). Tirar do diagrama ou marcar como opcional |
| 13 | Bootstrap; Spring Data (Nota 1, 3.4) | CSS próprio; só JdbcTemplate | **Mudar o texto.** Adicionar Bootstrap agora quebraria o visual que acabou de ser corrigido |
| 14 | Modelo entidade-relacionamento (Nota 1, 3.3) | Não há MER desenhado | **Eu gero o MER** a partir do banco (Mermaid), para vocês redesenharem no draw.io |
| 15 | Diagrama de sequência sem a verificação de conflito e sem reaproveitar tutor e pet | O código busca o tutor pelo telefone e verifica o conflito antes de gravar | **Eu escrevo o diagrama corrigido** (Mermaid) para vocês redesenharem |
| 16 | "Redução do tempo de registro" (Resumo) | Nunca foi medido | **Vocês medem** (teste CT-UI-11, uns 30 min) ou a frase sai do Resumo |

## Pacote A – recomendado (cabe com folga antes de 27/11)

| Item | O que faço | Esforço |
|---|---|---|
| A1 | `@Transactional` no agendamento: grava tutor, pet e agendamento juntos ou nada. Teste novo provando que, se falhar, nada fica gravado | 1 h |
| A2 | Status "Confirmado" e regras de transição: Pendente → Confirmado → Em andamento → Concluído; Cancelado e Concluído são finais. Botão "Confirmar" no painel. Testes das transições proibidas | 2 h |
| A3 | Tela "Serviços" no painel (só administrador): listar, criar, editar preço e duração, desativar. Os cards da landing passam a ser gerados a partir do banco (serviço novo aparece sozinho) | 4 h |
| A4 | Perfis: Administrador × Atendente de verdade. Tela "Funcionários" (só administrador): cadastrar, desativar. "Trocar minha senha" para todos. A API devolve 403 quando um atendente tenta uma ação de administrador | 4 h |
| A5 | Bloqueio de horários (só administrador): bloquear um período, como folga, feriado ou manutenção. Os horários bloqueados somem da landing e a regra antichoque os respeita. Testes | 3 h |
| A6 | Campo opcional "Idade do pet" no formulário | 0,5 h |
| Docs | MER e diagrama de sequência corrigido em Mermaid, roteiro de testes novos (CT-14 em diante) e lista do que mudar em cada diagrama | 1,5 h |

**Total: cerca de 16 h de programação.** Um PR só, com commits separados por item, para vocês poderem testar e citar cada um.

Depois do pacote A, os casos de uso ficariam assim: UC01, UC02, UC05, UC06, UC07, UC08, UC10, UC11 (indicadores) e UC12 implementados; UC03 e UC04 como trabalho futuro (ou o B1); o ator Administrador "herda de" Funcionário de verdade.

## Pacote B – opcional

| Item | O que faço | Esforço | Cuidado |
|---|---|---|---|
| B1 | UC03 e UC04 pelo cliente: tela "Meus agendamentos". O tutor informa o telefone e o **código do agendamento** (mostrado na mensagem de sucesso), vê o histórico e cancela o que ainda está Pendente ou Confirmado | 4 h | Pedir só o telefone deixaria qualquer pessoa ver e cancelar a agenda de outra. Com o código, fica razoável para um MVP |
| B2 | Histórico do tutor no painel: buscar por telefone e ver todos os atendimentos anteriores | 1,5 h | — |

## O que fica só no texto (vocês ajustam, eu ajudo com o rascunho)

Itens 9 a 13 e 16 da tabela. Quando o código estiver pronto, rodo os prompts 7 a 10 do guia para rascunhar a Nota 2 já com o sistema novo.
