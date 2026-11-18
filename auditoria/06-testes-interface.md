# Auditoria 06 – Testes de interface e usabilidade (parte do front-end do prompt 6)

- **Data:** 17/11/2026 · **Base:** commit `df374cd` · **Modo:** somente leitura
- Os casos CT-01 a CT-13 já testam a API (resultado de 14/11/2026: 13 passaram). Faltam casos que testem **a experiência na tela**. Este plano complementa o roteiro, sem substituir.

## 1. O que os testes atuais não cobrem na interface

| Lacuna | Por que importa |
|---|---|
| Nenhum teste verifica se o toast aparece e é legível | É a resposta do sistema ao tutor (heurística H1) |
| Nenhum teste de navegação só com teclado | Acessibilidade (WCAG 2.1.1 e 2.4.7) |
| Nenhum teste de contraste | O texto do TCC promete "paleta acessível" |
| CT-13 (responsividade) é só "abrir e ver" | Precisa de critérios objetivos (sem rolagem horizontal, menu acessível) |
| Nenhum teste de tempo de tarefa | O Resumo da Nota 1 promete "redução do tempo de registro" |

## 2. Plano complementar (preencher "Resultado obtido" ao executar)

| ID | Requisito / critério | Pré-condição | Passos | Dados | Resultado esperado | Resultado obtido |
|---|---|---|---|---|---|---|
| CT-UI-01 | RF02 / H1 | Sistema ligado | Preencher o formulário e enviar | Dados válidos | Botão fica desabilitado durante o envio; toast verde aparece e fica visível por pelo menos 6 s | |
| CT-UI-02 | H9 / WCAG 3.3.1 | Sistema ligado | Enviar com telefone de 5 dígitos | `99999` | Mensagem diz o que está errado e como corrigir | |
| CT-UI-03 | H9 | **Servidor desligado** | Tentar entrar no login | admin / admin123 | Mensagem "servidor indisponível" (e não "senha incorreta") | |
| CT-UI-04 | RNF03 / WCAG 1.4.10 | Navegador em 360 px (F12, modo celular) | Abrir landing e painel; rolar até o fim | — | Sem rolagem horizontal; menu acessível no celular; botão "Área Restrita" alcançável | |
| CT-UI-05 | WCAG 2.1.1 e 2.4.7 | Landing aberta | Usar só a tecla Tab (e Enter) para agendar | Dados válidos | Todos os campos e botões são alcançados em ordem lógica, com foco visível | |
| CT-UI-06 | WCAG 1.4.3 | — | Medir contraste das cores com uma ferramenta (ex.: WebAIM Contrast Checker) | Cores do `:root` | Texto ≥ 4,5:1; texto grande e bordas ≥ 3:1 | |
| CT-UI-07 | WCAG 4.1.3 | Leitor de tela (NVDA no Windows) | Enviar o formulário | Dados válidos | O leitor anuncia a mensagem de sucesso/erro | |
| CT-UI-08 | Movimento reduzido | Windows: Configurações > Acessibilidade > Efeitos visuais > desligar "Efeitos de animação" | Abrir a landing | — | Botão não pulsa, patas não flutuam | |
| CT-UI-09 | LGPD / transparência | Landing aberta | Tentar enviar sem marcar a caixa de ciência | Dados válidos | Envio bloqueado; link do aviso de privacidade abre | |
| CT-UI-10 | Painel / H3 | Logado, agendamento Pendente | Clicar em Cancelar | — | Confirmação mostra tutor, pet e horário; é possível desistir | |
| CT-UI-11 | Tempo de tarefa (eficiência, ISO/IEC 25010) | Cronômetro; ficha de papel igual à do pet shop (AS-IS) | Cada integrante registra 5 agendamentos no papel e 5 no sistema | Mesmos 5 casos fictícios | Tempo médio de cada forma (só registrar; **não** prometer redução antes de medir) | |

> CT-UI-11 é o único jeito honesto de manter a frase "redução do tempo de registro". Se não der tempo de fazer, a frase deve sair do Resumo.

## 3. Mapeamento para a ISO/IEC 25010 (característica Usabilidade, versão 2011)

| Subcaracterística (ISO/IEC 25010:2011) | Testes / evidências |
|---|---|
| Reconhecimento da adequação (*appropriateness recognizability*) | Landing explica serviços, preços e passos; CT-UI-01 |
| Apreensibilidade (*learnability*) | Avaliação heurística (05-frontend, seção 3) |
| Operabilidade (*operability*) | CT-UI-04, CT-UI-05, CT-UI-10 |
| Proteção contra erro do usuário (*user error protection*) | CT-03 (só horários livres), CT-10, CT-UI-02 |
| Estética da interface (*user interface aesthetics*) | 05-frontend, seção 8 (consistência) |
| Acessibilidade (*accessibility*) | CT-UI-05 a CT-UI-08 e tabela de contraste |

Observação: a revisão ISO/IEC 25010:2023 reorganizou o modelo e passou a chamar essa característica de *interaction capability* (capacidade de interação). [VERIFICAR na norma antes de citar.] Como o TCC cita a versão de 2011, o mapeamento acima segue a de 2011.
