# Rascunho – Referencial teórico de usabilidade, acessibilidade e interface

- **Data:** 17/11/2026
- **Para que serve:** acrescentar ao Capítulo 2 (Referencial Teórico) da Nota 1 uma subseção sobre **usabilidade e interface**, que hoje falta. A Nota 1 promete avaliar a usabilidade pela ISO/IEC 25010 (seção 3.5), mas não cita nenhum autor da área. Sem isso, a avaliação heurística feita em `05-frontend.md` fica sem base teórica.
- **Como usar:** é um **rascunho**. Reescreva com a voz da dupla, confira cada referência no livro/site antes de citar e só mantenha na lista final as obras que forem citadas no texto.

Legenda das marcações:
- **[CONFERIR NA FICHA CATALOGRÁFICA]** = a obra existe, mas edição, ano ou editora precisam ser conferidos no exemplar que vocês usarem.
- **[VERIFICAR]** = afirmação que eu não confirmei na fonte original.
- Os números de artigos da LGPD foram conferidos com o texto da lei que a Nota 1 já cita (planalto.gov.br). Mesmo assim, abra o link e confira antes de entregar.

---

## Sugestão de texto (subseção 2.4)

### 2.4 Usabilidade, acessibilidade e projeto de interfaces web

A usabilidade é definida pela norma ABNT NBR ISO 9241-11 como a medida em que um sistema pode ser usado por usuários específicos para alcançar objetivos específicos com eficácia, eficiência e satisfação em um contexto de uso específico (ABNT, 2021). [VERIFICAR a redação exata da definição na norma.] A definição é útil ao PetAgenda porque separa três dimensões que podem ser observadas no sistema: se o tutor consegue concluir o agendamento (eficácia), quanto esforço e tempo isso exige (eficiência) e como ele percebe a experiência (satisfação).

Nielsen (1993) trata a usabilidade como um atributo de qualidade composto por facilidade de aprendizado, eficiência de uso, facilidade de memorização, baixa taxa de erros e satisfação. Para avaliar interfaces sem a presença de usuários, Nielsen e Molich (1990) propuseram a **avaliação heurística**, um método de inspeção no qual avaliadores examinam a interface e julgam sua conformidade com princípios reconhecidos de usabilidade. Esses princípios foram consolidados por Nielsen (1994) em dez heurísticas: visibilidade do status do sistema; correspondência entre o sistema e o mundo real; controle e liberdade do usuário; consistência e padrões; prevenção de erros; reconhecimento em vez de memorização; flexibilidade e eficiência de uso; estética e design minimalista; ajuda aos usuários para reconhecer, diagnosticar e corrigir erros; e ajuda e documentação. Cada problema encontrado recebe uma nota de severidade de 0 (não é problema) a 4 (catástrofe de usabilidade) (NIELSEN, 1994). Nielsen (1992) observou ainda que avaliadores isolados encontram apenas parte dos problemas e recomenda de três a cinco avaliadores, o que explica por que o método é tratado como complementar, e não substituto, do teste com usuários. [VERIFICAR os números no artigo de 1992.]

No contexto brasileiro, Barbosa e Silva (2010) e Cybis, Betiol e Faust (2015) apresentam a avaliação heurística como um dos métodos de avaliação de interfaces mais acessíveis a equipes pequenas, por exigir pouco tempo e nenhum laboratório. Krug (2014) reforça, em linguagem voltada a desenvolvedores, que uma página web deve ser evidente por si mesma, de modo que o usuário não precise pensar para entender o que fazer, o que se aplica diretamente a um formulário de agendamento usado por tutores sem treinamento.

A **acessibilidade** amplia a usabilidade para pessoas com deficiência e com limitações temporárias ou situacionais. A principal referência técnica é a *Web Content Accessibility Guidelines* (WCAG) 2.1, recomendação do World Wide Web Consortium (W3C, 2018), organizada em quatro princípios: perceptível, operável, compreensível e robusto. Cada princípio possui critérios de sucesso verificáveis, em três níveis de conformidade (A, AA e AAA). Entre os critérios de nível AA estão o contraste mínimo de 4,5:1 entre texto e fundo (critério 1.4.3), o contraste de 3:1 para componentes de interface (1.4.11) e o anúncio de mensagens de status por tecnologias assistivas (4.1.3). No Brasil, a Lei Brasileira de Inclusão (BRASIL, 2015, art. 63) torna obrigatória a acessibilidade nos sítios da internet mantidos por empresas com sede ou representação comercial no país, e o Modelo de Acessibilidade em Governo Eletrônico (eMAG) adapta essas recomendações à realidade nacional (BRASIL, 2014).

O **design responsivo**, proposto por Marcotte (2010), combina grades fluidas, imagens flexíveis e consultas de mídia (*media queries*) para que uma mesma página se adapte a telas de tamanhos diferentes. Wroblewski (2011) acrescenta a estratégia *mobile first*, em que a interface é pensada primeiro para o celular, cujas restrições obrigam a priorizar o conteúdo essencial. Como os tutores de um pet shop de bairro tendem a acessar a página pelo celular [VERIFICAR — se quiserem afirmar isso, citem uma pesquisa, como a TIC Domicílios do Cetic.br], a responsividade deixa de ser estética e passa a ser condição para o uso do sistema.

Por fim, a interface também é o ponto em que o sistema cumpre os princípios da LGPD perante o titular. A lei estabelece, entre outros, os princípios da finalidade, da necessidade e da transparência (BRASIL, 2018, art. 6º, I, III e VI) e garante ao titular o acesso facilitado a informações claras sobre o tratamento de seus dados (art. 9º). Assim, um formulário que coleta nome e telefone deve informar, no próprio ponto de coleta, para que esses dados serão usados e como o titular pode exercer seus direitos (art. 18).

Na norma ISO/IEC 25010 (ISO; IEC, 2011), já citada neste trabalho, a usabilidade é uma das características de qualidade do produto e se divide em reconhecimento da adequação, apreensibilidade, operabilidade, proteção contra erros do usuário, estética da interface e acessibilidade. As heurísticas de Nielsen e os critérios da WCAG funcionam, portanto, como instrumentos práticos para verificar essas subcaracterísticas no PetAgenda. Cabe registrar que a revisão de 2023 da norma renomeou essa característica para *interaction capability* (capacidade de interação) e reorganizou suas subcaracterísticas (ISO; IEC, 2023). [VERIFICAR na norma e decidir com a orientadora qual versão citar.]

---

## Quadro – conceito × autor × onde aparece no PetAgenda

| Conceito | Autor / norma | Onde é usado no projeto |
|---|---|---|
| Usabilidade (eficácia, eficiência, satisfação) | ABNT (2021); Nielsen (1993) | Objetivo de RNF03; CT-UI-11 (tempo de tarefa) |
| Avaliação heurística e escala de severidade | Nielsen e Molich (1990); Nielsen (1994) | `05-frontend.md`, seção 3 |
| Número de avaliadores | Nielsen (1992) | Limitação do método (Considerações Finais) |
| Usabilidade na web | Krug (2014); Barbosa e Silva (2010); Cybis, Betiol e Faust (2015) | Formulário, mensagens, menu |
| Acessibilidade (contraste, foco, mensagens de status) | W3C (2018); Brasil (2015); Brasil (2014) | `05-frontend.md`, seção 4 |
| Design responsivo e *mobile first* | Marcotte (2010); Wroblewski (2011) | `05-frontend.md`, seção 5; CT-13 |
| Transparência e direitos do titular | Brasil (2018) | Aviso de privacidade (`05-frontend.md`, seção 6) |
| Qualidade de produto – usabilidade | ISO; IEC (2011, 2023) | `06-testes-interface.md`, seção 3 |

---

## Referências novas (ABNT NBR 6023)

ASSOCIAÇÃO BRASILEIRA DE NORMAS TÉCNICAS. **ABNT NBR ISO 9241-11**: ergonomia da interação humano-sistema – Parte 11: usabilidade: definições e conceitos. Rio de Janeiro: ABNT, 2021. [CONFERIR na norma: a versão brasileira de 2021 existe (normas.com.br lista edição de 07/2021); confirme o título exato.]

BARBOSA, Simone Diniz Junqueira; SILVA, Bruno Santana da. **Interação humano-computador**. Rio de Janeiro: Elsevier, 2010. [CONFERIR NA FICHA CATALOGRÁFICA]

BRASIL. Lei nº 13.146, de 6 de julho de 2015. Institui a Lei Brasileira de Inclusão da Pessoa com Deficiência (Estatuto da Pessoa com Deficiência). Brasília, DF: Presidência da República, 2015. Disponível em: https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2015/lei/l13146.htm. Acesso em: [data do acesso].

BRASIL. Ministério do Planejamento, Orçamento e Gestão. **eMAG**: Modelo de Acessibilidade em Governo Eletrônico. Versão 3.1. Brasília, DF: MP, 2014. Disponível em: https://emag.governoeletronico.gov.br/. Acesso em: [data do acesso]. [VERIFICAR o endereço atual e o órgão responsável na capa do documento]

CYBIS, Walter; BETIOL, Adriana Holtz; FAUST, Richard. **Ergonomia e usabilidade**: conhecimentos, métodos e aplicações. 3. ed. São Paulo: Novatec, 2015. [CONFERIR NA FICHA CATALOGRÁFICA]

INTERNATIONAL ORGANIZATION FOR STANDARDIZATION; INTERNATIONAL ELECTROTECHNICAL COMMISSION. **ISO/IEC 25010:2023**: systems and software engineering – Systems and software Quality Requirements and Evaluation (SQuaRE) – Product quality model. Geneva: ISO, 2023. [VERIFICAR o título exato; só incluir se a dupla decidir citar a revisão]

KRUG, Steve. **Não me faça pensar**: atualizado: uma abordagem de bom senso à usabilidade web e mobile. Rio de Janeiro: Alta Books, 2014. [CONFERIR NA FICHA CATALOGRÁFICA: a edição brasileira de 2014 corresponde à 3ª edição em inglês; confira subtítulo e número da edição]

MARCOTTE, Ethan. Responsive web design. **A List Apart**, n. 306, 25 maio 2010. Disponível em: https://alistapart.com/article/responsive-web-design/. Acesso em: [data do acesso].

NIELSEN, Jakob. **Usability engineering**. Boston: Academic Press, 1993. [CONFERIR NA FICHA CATALOGRÁFICA: algumas fichas trazem San Diego ou Morgan Kaufmann]

NIELSEN, Jakob. Finding usability problems through heuristic evaluation. *In*: CONFERENCE ON HUMAN FACTORS IN COMPUTING SYSTEMS (CHI '92), 1992, Monterey. **Proceedings** [...]. New York: ACM, 1992. p. 373-380. [VERIFICAR páginas]

NIELSEN, Jakob. **10 usability heuristics for user interface design**. [*S. l.*]: Nielsen Norman Group, 1994. Atualizado em 2024. Disponível em: https://www.nngroup.com/articles/ten-usability-heuristics/. Acesso em: [data do acesso]. [VERIFICAR a data de atualização na página]

NIELSEN, Jakob. **Severity ratings for usability problems**. [*S. l.*]: Nielsen Norman Group, 1994. Disponível em: https://www.nngroup.com/articles/how-to-rate-the-severity-of-usability-problems/. Acesso em: [data do acesso].

NIELSEN, Jakob; MOLICH, Rolf. Heuristic evaluation of user interfaces. *In*: CONFERENCE ON HUMAN FACTORS IN COMPUTING SYSTEMS (CHI '90), 1990, Seattle. **Proceedings** [...]. New York: ACM, 1990. p. 249-256.

WORLD WIDE WEB CONSORTIUM. **Web Content Accessibility Guidelines (WCAG) 2.1**. W3C Recommendation, 5 jun. 2018. Disponível em: https://www.w3.org/TR/WCAG21/. Acesso em: [data do acesso].

WROBLEWSKI, Luke. **Mobile first**. New York: A Book Apart, 2011.

> Opcional, se quiserem um livro-texto clássico de Interação Humano-Computador: ROGERS, Yvonne; SHARP, Helen; PREECE, Jennifer. **Design de interação**: além da interação humano-computador. 3. ed. Porto Alegre: Bookman, 2013. [CONFERIR NA FICHA CATALOGRÁFICA — a 1ª edição brasileira é de 2005, com Preece como primeira autora.]

**Obras que já estão na Nota 1 e continuam valendo aqui:** BRASIL (2018) – LGPD; ISO/IEC 25010:2011; Koscianski e Soares (2007); Sommerville (2018); Pressman e Maxim (2016). Lembrete do guia: as referências quebradas de Dornelas e Sommerville e o ano de Osterwalder e Pigneur ainda precisam ser corrigidos.

---

## Como isso entra em Resultados e Discussão (ideia de parágrafo)

"A avaliação heurística, conduzida pelos autores com base nas dez heurísticas de Nielsen (1994), identificou X problemas, dos quais Y de severidade 3 (graves), concentrados nas heurísticas de visibilidade do status do sistema e de recuperação de erros. A verificação de contraste segundo a WCAG 2.1 (W3C, 2018) apontou 10 combinações de cores abaixo do mínimo do nível AA. Após as correções (Apêndice X), [resultado depois das correções]. Por ter sido feita sem usuários reais e com dois avaliadores, abaixo dos três a cinco recomendados por Nielsen (1992), a avaliação é uma evidência parcial da característica de usabilidade da ISO/IEC 25010 (ISO; IEC, 2011) e deve ser complementada por testes com tutores e funcionários em trabalhos futuros."
