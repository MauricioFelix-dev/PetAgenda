# 15. Exploração do site como usuário real (madrugada de 18/11/2026)

Depois do polimento, o site foi usado como **tutor**, **atendente** e **administrador**, no computador (1280 px) e no celular (360 px), procurando situações fora do caminho feliz: campos longos, clique duplo, recarregar a página, voltar depois de sair, sessão encerrada em outra aba, dados inválidos no painel. A navegação foi feita por um navegador automático (Playwright), clicando como uma pessoa faria.

## Resultado

**24 situações verificadas: 1 bug encontrado e corrigido, 23 sem problema.**

### Bug encontrado (corrigido)

| Situação | Antes | Depois |
|---|---|---|
| Nome do tutor com mais de 100 letras, ou nome do pet ou raça com mais de 50 | O banco recusava e o tutor via "⚠️ Falha ao gravar no banco de dados. Nada foi salvo." Graças à transação nada ficava gravado pela metade, mas a mensagem não dizia o que corrigir | O formulário não deixa digitar além do limite (`maxlength`) e o servidor responde 400 com "Texto longo demais: nome do tutor até 100 caracteres; nome do pet e raça até 50." Novo teste `sv09` |

Print do antes: `prints/exploracao/antes-nome-longo-erro-generico.png`.

### Sem problema

| Quem | Situação | O que aconteceu |
|---|---|---|
| Tutor | Data no passado | O calendário não deixa escolher (`min` = hoje) e o servidor recusa |
| Tutor | Clique duplo em "Solicitar agendamento" | Só 1 pedido enviado (o botão é desativado no primeiro clique) |
| Tutor | Recarregar depois da confirmação | O formulário volta vazio, sem reenviar |
| Tutor | Código com espaços e minúsculas, telefone curto | Mensagem clara "Não encontramos agendamento..." |
| Funcionário | Senha errada | Mensagem "Login ou senha incorretos." |
| Admin | Preço com letras | O campo só aceita número |
| Admin | Serviço com nome `<img src=x onerror=alert(1)>` | Aparece como texto no painel e na página inicial; nenhum alerta (proteção contra XSS) |
| Admin | Desligar a Ana, que é responsável por serviços | Recusado, com orientação para trocar o responsável antes |
| Admin | Tirar o próprio perfil de administrador | Recusado |
| Admin | Bloqueio com fim antes do início | "O fim precisa ser depois do início." |
| Admin | Bloquear um período que já tem agendamento | Recusado, avisando quantos atendimentos existem (print `bloqueio-sobre-agendamento.png`) |
| Admin | Sessão encerrada em outra aba e depois clicar no painel | Volta para o login |
| Admin | Botão Voltar do navegador depois de Sair | Volta para o login, não mostra dados |
| Celular 360 px | Abas Serviços, Funcionários, Bloqueios, Histórico; Meus agendamentos; Privacidade | Nenhuma rolagem lateral (tabelas rolam dentro do próprio quadro) |

**Console do navegador:** nenhum erro de JavaScript. As únicas linhas vermelhas são as respostas 400, 401, 404 e 409 provocadas de propósito pelos testes, e todas foram tratadas na tela com mensagem.

## Como citar no TCC

Pode entrar como "teste exploratório" na seção de testes, junto com os casos CT-01 a CT-19. Vale deixar claro que foi feito por automação, e não substitui um teste com usuários reais, que continua como limitação ou trabalho futuro.
