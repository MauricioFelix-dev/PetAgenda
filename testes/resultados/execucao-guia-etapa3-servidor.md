# Execução do guia de teste da Etapa 3 no servidor (18/11/2026, madrugada)

Quem rodou: Claude, num servidor Linux com MariaDB, a pedido da dupla. O guia é o `testes/guia-teste-etapa3.md`. Os passos de tela foram feitos com um navegador automático (Playwright/Chromium), clicando nos mesmos botões que uma pessoa clicaria.

- Banco recriado com `banco/petagenda.sql`.
- Roteiro automático `testes/TestesCT.java`: **19 passaram, 0 falharam, 2 manuais** (`resultado-2026-11-18_02-29.md`).
- Passos de tela: **26 de 26 passaram**. Nenhum erro de JavaScript no console.
- Datas usadas: agendamentos em 2026-11-20, bloqueio de feriado em 2026-11-21, folga da Ana em 2026-11-22.

| Passo | Resultado | Detalhe |
|---|---|---|
| Cards com responsavel | PASSOU |  |
| Quem vai atender: Ana | PASSOU | Quem vai atender: Ana |
| CT-01 confirmacao com codigo | PASSOU | codigo AB7TZC |
| CT-15 consulta 08:00 livre com banho da Ana | PASSOU | 08:00,08:30,09:00 |
| Consulta 08:00 gravada com Dra. Helena | PASSOU | X7J26R |
| Banho 08:00 sumiu da lista | PASSOU | 08:30,09:00,09:30 |
| CT-18 codigo errado | PASSOU | Não encontramos agendamento com esse telefone e código. Confira e tente de novo. |
| CT-18 lista com os 2 agendamentos | PASSOU |  |
| CT-18 cancelar | PASSOU | Agendamento cancelado. O horário foi liberado. |
| CT-04 08:00 voltou | PASSOU |  |
| Painel admin 5 abas | PASSOU | Olá, Dra. Helena (Administrador) / 5 |
| CT-11/14 Confirmar>Iniciar>Concluir, finais sem botoes | PASSOU | botoes concluido 0, cancelado 0 |
| Historico do tutor | PASSOU | Maurício Teste · 2 atendimento(s)DataPetServiçoResponsávelSt |
| CT-19 servico cadastrado | PASSOU | Serviço cadastrado. |
| CT-19 card novo na pagina | PASSOU |  |
| CT-19 desativado some | PASSOU |  |
| CT-16 bloqueio cadastrado | PASSOU | Horário bloqueado. |
| CT-16 lista comeca 12:00 | PASSOU | 12:00 |
| CT-16 manha voltou | PASSOU |  |
| CT-16 folga da Ana | PASSOU | banho 0, consulta 20 |
| Funcionarios sem senha | PASSOU |  |
| CT-17 Ana 2 abas | PASSOU | Olá, Ana (Atendente) / 2 |
| CT-17 API admin 403 | PASSOU | 403 {"erro":"Somente o administrador pode fazer isso."} |
| Senha atual errada | PASSOU | A senha atual está incorreta. |
| Troca de senha e login com a nova | PASSOU | voltou para ana12345: 200 |
| CT-13 celular sem rolagem lateral, agenda antes dos indicadores | PASSOU | sobra 0/0 |

Prints em `testes/resultados/prints-etapa3-servidor/` (01 a 20, mesmos nomes do guia). O print 20 mostra o resultado da consulta SQL na tabela `funcionario`, e não a tela do phpMyAdmin.

**Observação para o texto:** estes testes foram feitos por automação, não por pessoas. Eles mostram que as funções funcionam, mas não substituem um teste com usuários reais (limitação já declarada). O campo de data aparece no formato mês/dia/ano nos prints porque o navegador automático estava em inglês; no navegador em português aparece dia/mês/ano.
