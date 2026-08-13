/**
 * Gerador da base de documentos de demonstracao do Portal Partners.
 *
 * Produz 10 PDFs de uma pagina cada, com marca d'agua "DOCUMENTO FICTICIO".
 * Sao amostras para demonstrar o fluxo do portal — nao possuem valor legal
 * e nao reproduzem os leiautes oficiais dos orgaos emissores.
 *
 * Uso:  npm install pdfkit && node gerar-documentos.js
 * Saida: pasta ./documentos
 */

const PDFDocument = require('pdfkit');
const fs = require('fs');
const path = require('path');

const SAIDA = path.join(__dirname, 'documentos');
if (!fs.existsSync(SAIDA)) fs.mkdirSync(SAIDA, { recursive: true });

// ---------------------------------------------------------------- dados base
const EMPRESA = {
  razao: 'EMPRESA 01 SERVICOS E MANUTENCAO LTDA',
  fantasia: 'EMPRESA 01',
  cnpj: '02.512.646/0001-83',
  ie: '110.045.678.113',
  im: '3.456.789-0',
  abertura: '14/03/2005',
  endereco: 'Rua das Industrias, 1200 - Galpao 3',
  bairro: 'Distrito Industrial',
  municipio: 'Porto Velho',
  uf: 'RO',
  cep: '76.821-080',
  cnae: '43.99-1-03 - Obras de alvenaria',
  natureza: '206-2 - Sociedade Empresaria Limitada',
  porte: 'EPP - Empresa de Pequeno Porte',
};

const FUNCIONARIO = {
  nome: 'ARIQUEMES CUNHA',
  cpf: '077.187.230-54',
  nascimento: '22/09/1987',
  mae: 'Marlene Cunha',
  pis: '160.87654.32-1',
  ctps: '0034512 / serie 0021-RO',
  cargo: 'Pedreiro',
  cbo: '7152-10',
  admissao: '05/02/2024',
  salario: 2500.0,
  setor: 'Manutencao Predial',
};

const brl = (v) =>
  'R$ ' + v.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

// ------------------------------------------------------------------ desenho
const AZUL = '#1f3864';
const CINZA = '#555555';
const LINHA = '#cccccc';

function novoDoc(arquivo) {
  const doc = new PDFDocument({ size: 'A4', margin: 50 });
  doc.pipe(fs.createWriteStream(path.join(SAIDA, arquivo)));
  return doc;
}

/** Faixa superior com o orgao/entidade emissora. */
function cabecalho(doc, orgao, subOrgao) {
  doc.rect(0, 0, doc.page.width, 68).fill(AZUL);
  doc.fillColor('#ffffff').font('Helvetica-Bold').fontSize(13)
     .text(orgao, 50, 22, { width: doc.page.width - 100 });
  if (subOrgao) {
    doc.font('Helvetica').fontSize(9).text(subOrgao, 50, 42, { width: doc.page.width - 100 });
  }
  doc.fillColor('#000000');
  doc.y = 100;
}

function titulo(doc, texto, subtitulo) {
  doc.font('Helvetica-Bold').fontSize(15).fillColor(AZUL)
     .text(texto, { align: 'center' });
  if (subtitulo) {
    doc.moveDown(0.2);
    doc.font('Helvetica').fontSize(9).fillColor(CINZA)
       .text(subtitulo, { align: 'center' });
  }
  doc.fillColor('#000000').moveDown(1.2);
}

/** Titulo de secao com linha divisoria. */
function secao(doc, texto) {
  doc.moveDown(0.6);
  doc.font('Helvetica-Bold').fontSize(10).fillColor(AZUL).text(texto.toUpperCase());
  doc.moveTo(50, doc.y + 2).lineTo(doc.page.width - 50, doc.y + 2)
     .strokeColor(LINHA).lineWidth(1).stroke();
  doc.fillColor('#000000').moveDown(0.5);
}

/** Grade de rotulo/valor em duas colunas. */
function campos(doc, pares, colunas = 2) {
  const largura = (doc.page.width - 100) / colunas;
  let coluna = 0;
  let yLinha = doc.y;

  pares.forEach(([rotulo, valor]) => {
    const x = 50 + coluna * largura;
    doc.font('Helvetica').fontSize(7).fillColor(CINZA)
       .text(rotulo.toUpperCase(), x, yLinha, { width: largura - 12 });
    doc.font('Helvetica-Bold').fontSize(9.5).fillColor('#000000')
       .text(String(valor), x, yLinha + 10, { width: largura - 12 });
    coluna++;
    if (coluna === colunas) {
      coluna = 0;
      yLinha += 30;
    }
  });

  doc.y = coluna === 0 ? yLinha : yLinha + 30;
  doc.x = 50;
}

function paragrafo(doc, texto) {
  doc.font('Helvetica').fontSize(9.5).fillColor('#000000')
     .text(texto, 50, doc.y, { align: 'justify', width: doc.page.width - 100, lineGap: 2 });
  doc.moveDown(0.6);
}

/** Tabela simples com cabecalho destacado. */
function tabela(doc, colunas, linhas) {
  const largura = doc.page.width - 100;
  const larguras = colunas.map((c) => (c.peso / colunas.reduce((s, x) => s + x.peso, 0)) * largura);
  let y = doc.y;

  doc.rect(50, y, largura, 18).fill('#eef1f7');
  doc.fillColor(AZUL).font('Helvetica-Bold').fontSize(8);
  let x = 50;
  colunas.forEach((c, i) => {
    doc.text(c.titulo, x + 5, y + 5, { width: larguras[i] - 10, align: c.align || 'left' });
    x += larguras[i];
  });
  y += 18;

  doc.font('Helvetica').fontSize(8.5).fillColor('#000000');
  linhas.forEach((linha, idx) => {
    if (idx % 2 === 1) doc.rect(50, y, largura, 16).fill('#fafafa').fillColor('#000000');
    x = 50;
    linha.forEach((celula, i) => {
      doc.fillColor('#000000')
         .text(String(celula), x + 5, y + 4, { width: larguras[i] - 10, align: colunas[i].align || 'left' });
      x += larguras[i];
    });
    y += 16;
  });

  doc.moveTo(50, y).lineTo(50 + largura, y).strokeColor(LINHA).stroke();
  doc.y = y + 10;
  doc.x = 50;
}

function assinatura(doc, linhas) {
  doc.moveDown(2);
  const largura = 220;
  const x = (doc.page.width - largura) / 2;
  doc.moveTo(x, doc.y).lineTo(x + largura, doc.y).strokeColor('#000000').lineWidth(0.8).stroke();
  doc.moveDown(0.4);
  linhas.forEach((l, i) => {
    doc.font(i === 0 ? 'Helvetica-Bold' : 'Helvetica').fontSize(8.5).fillColor('#000000')
       .text(l, 50, doc.y, { align: 'center', width: doc.page.width - 100 });
  });
}

/** Marca d'agua diagonal + rodape de aviso. Sempre a ultima chamada. */
function finalizar(doc, rodapeExtra) {
  doc.save();
  doc.rotate(-38, { origin: [doc.page.width / 2, doc.page.height / 2] });
  doc.font('Helvetica-Bold').fontSize(52).fillColor('#d0d0d0').fillOpacity(0.35)
     .text('DOCUMENTO FICTICIO', 0, doc.page.height / 2 - 40, {
       align: 'center', width: doc.page.width,
     });
  doc.fontSize(18).text('AMOSTRA PARA DEMONSTRACAO', 0, doc.page.height / 2 + 20, {
    align: 'center', width: doc.page.width,
  });
  doc.fillOpacity(1).restore();

  const yRodape = doc.page.height - 62;
  doc.moveTo(50, yRodape).lineTo(doc.page.width - 50, yRodape)
     .strokeColor(LINHA).lineWidth(1).stroke();
  doc.font('Helvetica').fontSize(7).fillColor(CINZA)
     .text(
       'Documento ficticio gerado para demonstracao do Portal Partners. Nao possui validade legal, '
       + 'nao foi emitido por orgao publico e nao reproduz o leiaute oficial. '
       + (rodapeExtra || ''),
       50, yRodape + 6, { width: doc.page.width - 100, align: 'justify' }
     );
  doc.end();
}

// =========================================================== os 10 documentos
const documentos = [];

// 1 ------------------------------------------------------------ Cartao CNPJ
documentos.push(['01-cartao-cnpj-empresa01.pdf', (doc) => {
  cabecalho(doc, 'REPUBLICA FEDERATIVA DO BRASIL', 'Cadastro Nacional da Pessoa Juridica');
  titulo(doc, 'COMPROVANTE DE INSCRICAO E DE SITUACAO CADASTRAL');
  campos(doc, [
    ['Numero de inscricao', EMPRESA.cnpj + '  (MATRIZ)'],
    ['Data de abertura', EMPRESA.abertura],
  ]);
  secao(doc, 'Identificacao');
  campos(doc, [
    ['Nome empresarial', EMPRESA.razao],
    ['Titulo do estabelecimento', EMPRESA.fantasia],
    ['Porte', EMPRESA.porte],
    ['Natureza juridica', EMPRESA.natureza],
  ]);
  secao(doc, 'Atividade economica');
  campos(doc, [['Codigo e descricao da atividade economica principal', EMPRESA.cnae]], 1);
  campos(doc, [
    ['Atividades secundarias', '43.30-4-04 - Servicos de pintura de edificios'],
    ['', '81.21-4-00 - Limpeza em predios e domicilios'],
  ], 1);
  secao(doc, 'Endereco');
  campos(doc, [
    ['Logradouro', EMPRESA.endereco],
    ['Bairro / Distrito', EMPRESA.bairro],
    ['Municipio', EMPRESA.municipio],
    ['UF', EMPRESA.uf],
    ['CEP', EMPRESA.cep],
    ['Telefone', '(69) 3221-4500'],
  ]);
  secao(doc, 'Situacao cadastral');
  campos(doc, [
    ['Situacao cadastral', 'ATIVA'],
    ['Data da situacao cadastral', EMPRESA.abertura],
    ['Motivo de situacao cadastral', '—'],
    ['Situacao especial', '—'],
  ]);
  finalizar(doc, 'Emitido em 13/08/2026 as 09:14:22.');
}]);

// 2 ------------------------------------------------------------- CND Federal
documentos.push(['02-cnd-federal-empresa01.pdf', (doc) => {
  cabecalho(doc, 'MINISTERIO DA FAZENDA', 'Secretaria Especial da Receita Federal do Brasil / Procuradoria-Geral da Fazenda Nacional');
  titulo(doc, 'CERTIDAO NEGATIVA DE DEBITOS',
    'Relativos aos Creditos Tributarios Federais e a Divida Ativa da Uniao');
  campos(doc, [['Nome empresarial', EMPRESA.razao], ['CNPJ', EMPRESA.cnpj]], 1);
  doc.moveDown(0.5);
  paragrafo(doc,
    'Ressalvado o direito de a Fazenda Nacional cobrar e inscrever quaisquer dividas de '
    + 'responsabilidade do sujeito passivo acima identificado que vierem a ser apuradas, e certificado '
    + 'que nao constam pendencias em seu nome, relativas a creditos tributarios administrados pela '
    + 'Secretaria Especial da Receita Federal do Brasil e a inscricoes em Divida Ativa da Uniao junto '
    + 'a Procuradoria-Geral da Fazenda Nacional.');
  paragrafo(doc,
    'Esta certidao e valida para o estabelecimento matriz e suas filiais e refere-se a situacao do '
    + 'sujeito passivo no ambito da RFB e da PGFN, abrangendo inclusive as contribuicoes sociais '
    + 'previstas nas alineas a a d do paragrafo unico do art. 11 da Lei no 8.212, de 24 de julho de 1991.');
  secao(doc, 'Validade e autenticacao');
  campos(doc, [
    ['Data de emissao', '13/08/2026, as 09:16:41'],
    ['Valida ate', '09/02/2027'],
    ['Codigo de controle da certidao', 'A1B2.C3D4.E5F6.7890'],
    ['Qualificacao', 'Certidao emitida gratuitamente'],
  ]);
  finalizar(doc, 'A autenticidade de certidoes reais deve ser conferida no sitio oficial do orgao emissor.');
}]);

// 3 ----------------------------------------------------------- CND Estadual
documentos.push(['03-cnd-estadual-empresa01.pdf', (doc) => {
  cabecalho(doc, 'GOVERNO DO ESTADO DE RONDONIA', 'Secretaria de Estado de Financas - Coordenadoria da Receita Estadual');
  titulo(doc, 'CERTIDAO NEGATIVA DE DEBITOS ESTADUAIS', 'Tributos estaduais e Divida Ativa do Estado');
  campos(doc, [
    ['Razao social', EMPRESA.razao],
    ['CNPJ', EMPRESA.cnpj],
    ['Inscricao estadual', EMPRESA.ie],
    ['Municipio', EMPRESA.municipio + ' / ' + EMPRESA.uf],
  ]);
  doc.moveDown(0.5);
  paragrafo(doc,
    'Certificamos que, ate a presente data, nao constam debitos inscritos ou nao inscritos em Divida '
    + 'Ativa Estadual em nome do contribuinte acima identificado, relativos a tributos administrados '
    + 'pela Coordenadoria da Receita Estadual.');
  paragrafo(doc,
    'A presente certidao e expedida sem prejuizo de posterior apuracao de debitos, ficando '
    + 'ressalvado o direito de a Fazenda Publica Estadual cobrar quaisquer valores que venham a ser '
    + 'constatados.');
  secao(doc, 'Validade e autenticacao');
  campos(doc, [
    ['Data de emissao', '13/08/2026'],
    ['Valida ate', '11/11/2026'],
    ['Numero da certidao', '2026/0004512-7'],
    ['Codigo de autenticidade', 'RO-9F3K-2M8T-5QX1'],
  ]);
  finalizar(doc);
}]);

// 4 ---------------------------------------------------------- CND Municipal
documentos.push(['04-cnd-municipal-empresa01.pdf', (doc) => {
  cabecalho(doc, 'PREFEITURA MUNICIPAL DE PORTO VELHO', 'Secretaria Municipal de Fazenda - Departamento de Tributacao');
  titulo(doc, 'CERTIDAO NEGATIVA DE DEBITOS MUNICIPAIS', 'Tributos mobiliarios e imobiliarios');
  campos(doc, [
    ['Razao social', EMPRESA.razao],
    ['CNPJ', EMPRESA.cnpj],
    ['Inscricao municipal', EMPRESA.im],
    ['Endereco', EMPRESA.endereco + ' - ' + EMPRESA.bairro],
  ]);
  doc.moveDown(0.5);
  paragrafo(doc,
    'Certificamos, para os devidos fins, que nao constam debitos vencidos e nao pagos referentes a '
    + 'tributos municipais em nome do contribuinte acima qualificado, ate a data de emissao desta '
    + 'certidao, conforme registros da Secretaria Municipal de Fazenda.');
  paragrafo(doc,
    'Ficam ressalvados o direito da Fazenda Publica Municipal de cobrar debitos que venham a ser '
    + 'apurados e a possibilidade de revisao de lancamentos, nos termos da legislacao tributaria vigente.');
  secao(doc, 'Validade e autenticacao');
  campos(doc, [
    ['Data de emissao', '13/08/2026'],
    ['Valida ate', '11/10/2026'],
    ['Numero do protocolo', 'PVH-2026-118432'],
    ['Codigo verificador', '7T2P-QW49-KK10'],
  ]);
  finalizar(doc);
}]);

// 5 ------------------------------------------------------- Ordem de Servico
documentos.push(['05-ordem-de-servico-empresa01.pdf', (doc) => {
  cabecalho(doc, EMPRESA.razao, 'Servico Especializado em Seguranca e Medicina do Trabalho');
  titulo(doc, 'ORDEM DE SERVICO DE SEGURANCA DO TRABALHO',
    'Emitida nos termos da NR-01, item 1.4.1, alinea "b" - Portaria SEPRT no 6.730/2020');
  campos(doc, [
    ['Empresa', EMPRESA.fantasia],
    ['CNPJ', EMPRESA.cnpj],
    ['Funcionario', FUNCIONARIO.nome],
    ['CPF', FUNCIONARIO.cpf],
    ['Funcao', FUNCIONARIO.cargo + ' (CBO ' + FUNCIONARIO.cbo + ')'],
    ['Setor', FUNCIONARIO.setor],
    ['Data de emissao', '13/08/2026'],
    ['Numero da OS', 'OS-2026-0147'],
  ]);
  secao(doc, 'Riscos ocupacionais identificados na funcao');
  tabela(doc,
    [{ titulo: 'Agente', peso: 2 }, { titulo: 'Tipo', peso: 1.2 }, { titulo: 'Medida de controle', peso: 3 }],
    [
      ['Ruido continuo', 'Fisico', 'Protetor auricular tipo concha (CA 12.567)'],
      ['Poeira mineral', 'Quimico', 'Respirador PFF-2 e umidificacao da area'],
      ['Trabalho em altura', 'Acidente', 'Cinto paraquedista, talabarte e linha de vida'],
      ['Postura inadequada', 'Ergonomico', 'Pausas programadas e rodizio de tarefas'],
      ['Queda de materiais', 'Acidente', 'Capacete com jugular e area isolada'],
    ]);
  secao(doc, 'Obrigacoes do trabalhador');
  paragrafo(doc,
    'Cumprir as disposicoes legais e regulamentares sobre seguranca e saude no trabalho, inclusive as '
    + 'ordens de servico expedidas pelo empregador; usar o Equipamento de Protecao Individual fornecido '
    + 'pela empresa; comunicar ao superior imediato qualquer condicao insegura ou avaria no EPI; e '
    + 'colaborar com a empresa na aplicacao das Normas Regulamentadoras.');
  paragrafo(doc,
    'O descumprimento das determinacoes desta Ordem de Servico constitui ato faltoso, nos termos do '
    + 'item 1.4.2 da NR-01 e do art. 158, paragrafo unico, da CLT.');
  secao(doc, 'Ciencia do trabalhador');
  paragrafo(doc,
    'Declaro ter recebido, lido e compreendido as orientacoes contidas nesta Ordem de Servico, bem como '
    + 'ter sido treinado quanto aos riscos da minha funcao e ao uso correto dos EPIs.');
  assinatura(doc, [FUNCIONARIO.nome, 'CPF ' + FUNCIONARIO.cpf]);
  finalizar(doc);
}]);

// 6 ------------------------------------------------------------ CTPS Digital
documentos.push(['06-ctps-digital-ariquemes-cunha.pdf', (doc) => {
  cabecalho(doc, 'MINISTERIO DO TRABALHO E EMPREGO', 'Carteira de Trabalho e Previdencia Social - CTPS Digital');
  titulo(doc, 'EXTRATO DE CONTRATO DE TRABALHO', 'Documento gerado a partir da CTPS Digital');
  secao(doc, 'Dados pessoais');
  campos(doc, [
    ['Nome', FUNCIONARIO.nome],
    ['CPF', FUNCIONARIO.cpf],
    ['Data de nascimento', FUNCIONARIO.nascimento],
    ['Nome da mae', FUNCIONARIO.mae],
    ['PIS/PASEP', FUNCIONARIO.pis],
    ['CTPS (numero/serie)', FUNCIONARIO.ctps],
  ]);
  secao(doc, 'Contrato de trabalho vigente');
  campos(doc, [
    ['Empregador', EMPRESA.razao],
    ['CNPJ do empregador', EMPRESA.cnpj],
    ['Data de admissao', FUNCIONARIO.admissao],
    ['Data de desligamento', 'Contrato em vigor'],
    ['Cargo', FUNCIONARIO.cargo],
    ['CBO', FUNCIONARIO.cbo],
    ['Tipo de contrato', 'Prazo indeterminado'],
    ['Jornada semanal', '44 horas'],
    ['Remuneracao', brl(FUNCIONARIO.salario) + ' mensais'],
    ['Regime', 'CLT'],
  ]);
  secao(doc, 'Historico de anotacoes');
  tabela(doc,
    [{ titulo: 'Data', peso: 1 }, { titulo: 'Ocorrencia', peso: 2.6 }, { titulo: 'Detalhe', peso: 2.4 }],
    [
      ['05/02/2024', 'Admissao', 'Registro inicial do contrato'],
      ['01/03/2025', 'Alteracao salarial', 'Reajuste de convencao coletiva'],
      ['05/02/2026', 'Ferias', 'Periodo aquisitivo 2024/2025 - 30 dias'],
    ]);
  finalizar(doc, 'A CTPS Digital autentica e obtida exclusivamente pelos canais oficiais do Governo Federal.');
}]);

// 7 -------------------------------------------------------------------- ASO
documentos.push(['07-aso-ariquemes-cunha.pdf', (doc) => {
  cabecalho(doc, 'CLINICA OCUPACIONAL SAUDE NO TRABALHO LTDA', 'CNPJ 11.222.333/0001-44 - Responsavel tecnico: Dr. Helio Barreto - CRM/RO 5.412');
  titulo(doc, 'ATESTADO DE SAUDE OCUPACIONAL - ASO',
    'Emitido conforme NR-07 - Programa de Controle Medico de Saude Ocupacional');
  campos(doc, [
    ['Empresa', EMPRESA.razao],
    ['CNPJ', EMPRESA.cnpj],
    ['Funcionario', FUNCIONARIO.nome],
    ['CPF', FUNCIONARIO.cpf],
    ['Data de nascimento', FUNCIONARIO.nascimento],
    ['Funcao', FUNCIONARIO.cargo],
    ['Setor', FUNCIONARIO.setor],
    ['Tipo de exame', 'Periodico'],
  ]);
  secao(doc, 'Riscos ocupacionais a que esta exposto');
  paragrafo(doc,
    'Fisicos: ruido continuo acima de 80 dB(A). Quimicos: poeira mineral. Ergonomicos: levantamento '
    + 'manual de cargas e postura em pe prolongada. Acidentes: trabalho em altura e queda de materiais.');
  secao(doc, 'Exames realizados');
  tabela(doc,
    [{ titulo: 'Exame', peso: 3 }, { titulo: 'Data', peso: 1.2 }, { titulo: 'Resultado', peso: 1.8 }],
    [
      ['Avaliacao clinica ocupacional', '10/08/2026', 'Sem alteracoes'],
      ['Audiometria tonal', '10/08/2026', 'Dentro dos limites'],
      ['Espirometria', '10/08/2026', 'Normal'],
      ['Acuidade visual', '10/08/2026', 'Normal'],
      ['Avaliacao para trabalho em altura', '10/08/2026', 'Apto'],
    ]);
  secao(doc, 'Conclusao');
  doc.font('Helvetica-Bold').fontSize(13).fillColor('#1a7f37')
     .text('APTO PARA A FUNCAO', { align: 'center' });
  doc.fillColor('#000000').moveDown(0.4);
  doc.font('Helvetica').fontSize(9).fillColor(CINZA)
     .text('Proximo exame periodico previsto para 10/08/2027.', { align: 'center' });
  doc.fillColor('#000000');
  assinatura(doc, ['Dr. Helio Barreto', 'Medico do Trabalho - CRM/RO 5.412']);
  finalizar(doc);
}]);

// 8 ------------------------------------------------------------ Ficha de EPI
documentos.push(['08-ficha-epi-ariquemes-cunha.pdf', (doc) => {
  cabecalho(doc, EMPRESA.razao, 'Controle de Entrega de Equipamentos de Protecao Individual');
  titulo(doc, 'FICHA DE CONTROLE DE ENTREGA DE EPI',
    'Emitida conforme NR-06, item 6.6.1, alinea "h"');
  campos(doc, [
    ['Funcionario', FUNCIONARIO.nome],
    ['CPF', FUNCIONARIO.cpf],
    ['Funcao', FUNCIONARIO.cargo],
    ['Setor', FUNCIONARIO.setor],
    ['Admissao', FUNCIONARIO.admissao],
    ['Ficha no', 'EPI-2026-0233'],
  ]);
  secao(doc, 'Equipamentos entregues');
  tabela(doc,
    [
      { titulo: 'Equipamento', peso: 3 },
      { titulo: 'CA', peso: 1 },
      { titulo: 'Qtd', peso: 0.6, align: 'center' },
      { titulo: 'Entrega', peso: 1.1 },
      { titulo: 'Validade', peso: 1.1 },
    ],
    [
      ['Capacete de seguranca com jugular', '31.469', '1', '05/02/2026', '05/02/2027'],
      ['Protetor auricular tipo concha', '12.567', '1', '05/02/2026', '05/08/2026'],
      ['Oculos de protecao incolor', '25.716', '2', '05/02/2026', '05/02/2027'],
      ['Luva de raspa de couro', '38.902', '4', '05/02/2026', '05/05/2026'],
      ['Bota de seguranca com biqueira', '41.150', '1', '05/02/2026', '05/02/2027'],
      ['Respirador semifacial PFF-2', '38.511', '6', '05/02/2026', '05/08/2026'],
      ['Cinto de seguranca paraquedista', '35.024', '1', '05/02/2026', '05/02/2027'],
      ['Talabarte duplo em Y com absorvedor', '35.115', '1', '05/02/2026', '05/02/2027'],
    ]);
  secao(doc, 'Declaracao do trabalhador');
  paragrafo(doc,
    'Declaro ter recebido gratuitamente da empresa os Equipamentos de Protecao Individual acima '
    + 'relacionados, todos em perfeito estado de conservacao e com Certificado de Aprovacao valido. '
    + 'Declaro ainda ter sido treinado quanto ao uso, guarda e conservacao, comprometendo-me a utiliza-los '
    + 'durante toda a jornada, a comunicar qualquer alteracao que os torne improprios e a devolve-los '
    + 'ao termino do contrato, ciente de que o nao uso constitui ato faltoso nos termos do art. 158 da CLT.');
  assinatura(doc, [FUNCIONARIO.nome, 'CPF ' + FUNCIONARIO.cpf]);
  finalizar(doc);
}]);

// 9 --------------------------------------------------------------- Holerite
documentos.push(['09-holerite-ariquemes-cunha.pdf', (doc) => {
  const base = FUNCIONARIO.salario;
  const inss = 105.9 + (base - 1412) * 0.09; // faixas progressivas
  const fgts = base * 0.08;
  const liquido = base - inss;

  cabecalho(doc, EMPRESA.razao, 'CNPJ ' + EMPRESA.cnpj + ' - ' + EMPRESA.endereco);
  titulo(doc, 'RECIBO DE PAGAMENTO DE SALARIO', 'Competencia: JULHO / 2026');
  campos(doc, [
    ['Funcionario', FUNCIONARIO.nome],
    ['CPF', FUNCIONARIO.cpf],
    ['Cargo', FUNCIONARIO.cargo],
    ['Admissao', FUNCIONARIO.admissao],
    ['CBO', FUNCIONARIO.cbo],
    ['Setor', FUNCIONARIO.setor],
  ]);
  secao(doc, 'Demonstrativo');
  tabela(doc,
    [
      { titulo: 'Cod', peso: 0.6 },
      { titulo: 'Descricao', peso: 3.4 },
      { titulo: 'Referencia', peso: 1.2, align: 'center' },
      { titulo: 'Proventos', peso: 1.4, align: 'right' },
      { titulo: 'Descontos', peso: 1.4, align: 'right' },
    ],
    [
      ['001', 'Salario base', '30 dias', brl(base), ''],
      ['201', 'INSS sobre salario', '9,00%', '', brl(inss)],
      ['202', 'IRRF sobre salario', 'Isento', '', brl(0)],
    ]);
  doc.moveDown(0.3);
  campos(doc, [
    ['Total de proventos', brl(base)],
    ['Total de descontos', brl(inss)],
  ]);
  doc.rect(50, doc.y, doc.page.width - 100, 30).fill('#eef1f7');
  doc.fillColor(AZUL).font('Helvetica-Bold').fontSize(12)
     .text('VALOR LIQUIDO A RECEBER: ' + brl(liquido), 50, doc.y + 9,
       { width: doc.page.width - 100, align: 'center' });
  doc.fillColor('#000000');
  doc.y += 40;
  secao(doc, 'Informacoes complementares');
  campos(doc, [
    ['Base de calculo do INSS', brl(base)],
    ['Base de calculo do FGTS', brl(base)],
    ['FGTS do mes (deposito)', brl(fgts)],
    ['Base de calculo do IRRF', brl(base - inss)],
    ['Salario familia', brl(0)],
    ['Data de pagamento', '05/08/2026'],
  ]);
  assinatura(doc, [FUNCIONARIO.nome, 'Declaro ter recebido a importancia liquida discriminada']);
  finalizar(doc, 'Valores calculados com aliquotas ilustrativas.');
}]);

// 10 ------------------------------------------------------- Certificado NR-35
documentos.push(['10-certificado-nr35-ariquemes-cunha.pdf', (doc) => {
  cabecalho(doc, 'CENTRO DE TREINAMENTO SEGURANCA TOTAL', 'CNPJ 44.555.666/0001-77 - Credenciado para treinamentos em Normas Regulamentadoras');
  doc.moveDown(1);
  titulo(doc, 'CERTIFICADO DE TREINAMENTO',
    'NR-35 - TRABALHO EM ALTURA - Capacitacao Inicial');
  doc.moveDown(1);
  doc.font('Helvetica').fontSize(11).fillColor('#000000')
     .text('Certificamos que', { align: 'center' });
  doc.moveDown(0.5);
  doc.font('Helvetica-Bold').fontSize(18).fillColor(AZUL)
     .text(FUNCIONARIO.nome, { align: 'center' });
  doc.moveDown(0.3);
  doc.font('Helvetica').fontSize(10).fillColor(CINZA)
     .text('CPF ' + FUNCIONARIO.cpf, { align: 'center' });
  doc.moveDown(0.8);
  doc.fillColor('#000000');
  paragrafo(doc,
    'concluiu com aproveitamento o treinamento de capacitacao para Trabalho em Altura, com carga '
    + 'horaria de 8 (oito) horas, atendendo aos requisitos do item 35.3 da Norma Regulamentadora no 35, '
    + 'aprovada pela Portaria MTE no 313/2012 e alteracoes posteriores, estando considerado CAPACITADO '
    + 'para a execucao de atividades em altura acima de 2,00 metros do nivel inferior, onde haja risco de queda.');
  secao(doc, 'Conteudo programatico');
  paragrafo(doc,
    'Normas e regulamentos aplicaveis ao trabalho em altura; analise de risco e condicoes impeditivas; '
    + 'riscos potenciais inerentes ao trabalho em altura e medidas de prevencao; sistemas, equipamentos '
    + 'e procedimentos de protecao coletiva; equipamentos de protecao individual para trabalho em altura '
    + '(selecao, inspecao, conservacao e limitacao de uso); acidentes tipicos; e condutas em situacoes '
    + 'de emergencia, incluindo nocoes de tecnicas de resgate e primeiros socorros.');
  secao(doc, 'Dados do treinamento');
  campos(doc, [
    ['Empresa contratante', EMPRESA.razao],
    ['CNPJ', EMPRESA.cnpj],
    ['Periodo de realizacao', '04/08/2026 a 04/08/2026'],
    ['Carga horaria', '8 horas'],
    ['Data de emissao', '05/08/2026'],
    ['Validade (reciclagem bienal)', '04/08/2028'],
    ['Certificado no', 'NR35-2026-00814'],
    ['Aproveitamento', 'Aprovado'],
  ]);
  assinatura(doc, ['Eng. Marta Fontenele', 'Engenheira de Seguranca do Trabalho - CREA/RO 12.884/D']);
  finalizar(doc);
}]);

// ------------------------------------------------------------------ execucao
documentos.forEach(([arquivo, montar]) => {
  const doc = novoDoc(arquivo);
  montar(doc);
  console.log('gerado: documentos/' + arquivo);
});
console.log('\nTotal: ' + documentos.length + ' documentos em ' + SAIDA);
