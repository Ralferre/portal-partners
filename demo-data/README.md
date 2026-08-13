# Base de documentos para demonstração

Dez PDFs fictícios para popular o Portal Partners em apresentações, cobrindo
o ciclo documental de uma contratada e de um funcionário.

> **Nenhum destes documentos tem validade legal.** Todos trazem marca d'água
> diagonal *"DOCUMENTO FICTÍCIO — AMOSTRA PARA DEMONSTRAÇÃO"* e rodapé
> informando que não foram emitidos por órgão público e que não reproduzem o
> leiaute oficial. O CNPJ e o CPF utilizados foram gerados em ferramenta de
> dados de teste — servem para validar máscaras e dígitos verificadores dos
> formulários, não pertencem a empresa ou pessoa real.

## Conteúdo

### Documentos da empresa — Empresa 01 · CNPJ 02.512.646/0001-83

| Arquivo | Documento |
|---|---|
| `01-cartao-cnpj-empresa01.pdf` | Comprovante de Inscrição e Situação Cadastral (CNPJ) |
| `02-cnd-federal-empresa01.pdf` | Certidão Negativa de Débitos Federais e Dívida Ativa da União |
| `03-cnd-estadual-empresa01.pdf` | Certidão Negativa de Débitos Estaduais |
| `04-cnd-municipal-empresa01.pdf` | Certidão Negativa de Débitos Municipais |
| `05-ordem-de-servico-empresa01.pdf` | Ordem de Serviço de Segurança do Trabalho (NR-01) |

### Documentos do funcionário — Ariquemes Cunha · CPF 077.187.230-54

| Arquivo | Documento |
|---|---|
| `06-ctps-digital-ariquemes-cunha.pdf` | Extrato de contrato de trabalho (CTPS Digital) |
| `07-aso-ariquemes-cunha.pdf` | Atestado de Saúde Ocupacional (NR-07) — conclusão APTO |
| `08-ficha-epi-ariquemes-cunha.pdf` | Ficha de controle de entrega de EPI (NR-06) |
| `09-holerite-ariquemes-cunha.pdf` | Recibo de pagamento de salário — base R$ 2.500,00 |
| `10-certificado-nr35-ariquemes-cunha.pdf` | Certificado de treinamento NR-35 (Trabalho em Altura) |

## Regenerar ou ampliar a base

Os dados da empresa e do funcionário ficam em duas constantes no topo de
`gerar-documentos.js` (`EMPRESA` e `FUNCIONARIO`). Alterar ali e rodar de novo
produz o conjunto inteiro com os novos dados — útil para criar mais
contratadas e funcionários na demonstração.

```bash
cd demo-data
npm install
npm run gerar
```

Só com Docker, sem instalar Node:

```bash
cd demo-data
docker run --rm -v "$PWD:/app" -w /app node:20-alpine \
  sh -c "npm install --silent && node gerar-documentos.js"
```

A saída vai para `documentos/`.

## Notas sobre o conteúdo

- **Holerite**: INSS calculado pelas faixas progressivas (7,5% até o primeiro
  limite, 9% sobre o excedente), resultando em R$ 203,82 de desconto e
  R$ 2.296,18 líquidos. IRRF isento para essa base. FGTS de R$ 200,00 aparece
  como informativo, sem descontar do líquido. As alíquotas são ilustrativas.
- **Ordem de Serviço**: interpretada como a OS de Segurança do Trabalho
  prevista na NR-01, item 1.4.1 — que é o documento de compliance pertinente
  ao portal, e não uma ordem de serviço comercial.
- **Ficha de EPI**: os números de CA são fictícios e não correspondem a
  certificados reais do catálogo do Ministério do Trabalho.
