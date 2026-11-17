# Auditoria 02 – Incongruências entre documentos e código (foco na interface)

- **Data:** 17/11/2026 · **Base:** commit `df374cd` · **Modo:** somente leitura
- **Documentos comparados:** Projeto de TCC corrigido (Nota 1), Nota 3 – Relatório Técnico-Científico, Apresentação PetAgenda (PDFs da pasta do projeto). A pasta `docs/referencia/` sugerida pelo guia não existe no repositório; os PDFs foram lidos direto da pasta do projeto no claude.ai.
- As incongruências de back-end e diagramas (antichoque "por funcionário", `@Transactional`, Spring Data, classe Pagamento etc.) já estão no guia de auditoria e não foram repetidas aqui.

| # | Afirmação (documento) | O que o código mostra | Tipo | Correção sugerida |
|---|---|---|---|---|
| 1 | Nota 1, Resumo e seção 3.4: front-end "em Bootstrap, para a componentização da interface e a responsividade" | Nenhum arquivo carrega Bootstrap; o CSS é próprio, com `@media` (`index.html:541-547`, `dashboard.html:110`). README: "JavaScript puro (sem framework)" | Contradição | **Texto:** "CSS3 próprio, com consultas de mídia (media queries) para a responsividade" |
| 2 | Nota 1, seção 3.3: protótipo com "paleta de cores acessível" | 10 combinações de cores abaixo do mínimo da WCAG 2.1 AA (ver 05-frontend, 4.1) | Exagero | **Código** (ajustar cores, baixo esforço) **e texto** (citar a medição) |
| 3 | Nota 3, seção 9: responsividade para celular (RNF03) é "melhoria futura" | Já existe e foi testada (CT-13 e prints de 360 px) | Desatualizado | **Texto:** RNF03 implementado parcialmente |
| 4 | Apresentação: "Durante testes de usabilidade, foi notado que o cliente não recebia confirmação explícita" | Não há registro desses testes (roteiro, participantes, data). O toast de confirmação existe (`index.html:815-821`) | Não comprovado | **Texto:** chamar de "testes internos da dupla" ou registrar o teste |
| 5 | Nota 1, seção 3.5: avaliação de usabilidade pela ISO/IEC 25010 | Nenhuma avaliação com usuários. Esta auditoria traz uma **avaliação heurística** (inspeção), que pode ser usada como evidência parcial | Não implementado | **Texto:** declarar o método (avaliação heurística de Nielsen + verificação WCAG) e a limitação (sem usuários reais) |
| 6 | Landing: "98% Satisfação", "0 Conflitos", "24h Online", "Profissional: Ana", status "Confirmado" | Nada medido; sem escolha de profissional; status "Confirmado" não existe | Exagero / fictício | **Código:** hero com exemplo ilustrativo honesto |
| 7 | Landing: "Histórico completo de atendimentos", "Cancelamento com facilidade" | O tutor não tem histórico nem cancelamento | Não implementado | **Código:** retirar da lista (ou marcar como trabalho futuro no TCC) |
| 8 | Nota 1, Resumo: "software compatível com a Lei Geral de Proteção de Dados Pessoais (LGPD)" | Formulário sem aviso de privacidade nem caminho para o titular pedir exclusão | Não implementado | **Código:** aviso + caixa de ciência; **texto:** limitar a afirmação às medidas existentes (login, BCrypt, dados só no painel) |
| 9 | Nota 3, seção 9: futuro com "animações ricas" e "efeitos dinâmicos" | A landing já tem animações infinitas e não respeita `prefers-reduced-motion` | Risco de acessibilidade | **Texto:** acrescentar que animações devem respeitar a preferência de movimento reduzido |

## As 5 que mais arriscam a nota se a banca perceber (interface)

1. Números fictícios do hero (#6) — qualquer avaliador abre a página e vê.
2. "Compatível com a LGPD" sem aviso de privacidade (#8).
3. Bootstrap citado e não usado (#1).
4. "Testes de usabilidade" e "paleta acessível" sem evidência (#2, #4, #5).
5. Promessas da landing que o sistema não cumpre (#7).
