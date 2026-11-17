# Auditoria de interface e usabilidade do PetAgenda (17/11/2026)

Relatórios gerados a partir do "Guia de Prompts – Auditoria do PetAgenda", com foco em **interface e usabilidade**. Os relatórios 01 a 06 foram feitos em modo somente leitura sobre o commit `df374cd`. As correções aprovadas depois estão em `11-correcoes.md`. Cada achado traz arquivo:linha e a marcação [Certo], [Provável] ou [Suposição].

| Arquivo | Prompt do guia | Conteúdo |
|---|---|---|
| `01-inventario.md` | 1 | Arquivos, versões, endpoints usados pelas telas, requisitos de interface × código |
| `02-incongruencias.md` | 2 | O que os documentos (Nota 1, Nota 3, Apresentação) dizem da interface × o que o código faz |
| `05-frontend.md` | 5 | **Principal:** bugs de interface, conteúdo fictício, avaliação heurística (Nielsen), contraste e acessibilidade (WCAG 2.1 AA), responsividade testada, LGPD, 10 melhorias priorizadas |
| `05b-referencial-usabilidade.md` | — | Rascunho da subseção de referencial teórico (usabilidade, acessibilidade, responsividade, LGPD na interface) com referências ABNT |
| `06-testes-interface.md` | 6 (parte de front-end) | Plano de testes de interface CT-UI-01 a CT-UI-11 e mapeamento para a ISO/IEC 25010 |
| `11-correcoes.md` | Correção | As 7 melhorias aplicadas (aprovadas pela dupla), resultado dos testes antes × depois e limitações |
| `prints/` | — | Prints `antes-*` e `depois-*` das telas em 360 px (celular) e 1280 px (computador), com API simulada |

Não foram feitos nesta rodada: prompt 3 (banco), prompt 4 (back-end) e prompts 7 a 10 (escrita da Nota 2).
