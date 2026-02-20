# Melhores práticas para aplicações com Java + Banco de Dados Postgres

## Backend e Banco de Dados

- **Tabelas do Banco de Dados**
- criar tabelas bem estruturadas utilizando o conceito de tabelas das entidades e tabelas eventos;
- você deve considerar utilizar um microserviço que foi criado como sendo o Auth Service. Este microserviço já foi criado e está no repositório remoto cujo nome é: auth-service;
- o microserviço possui 03 endpoints. Um para criação de usuário, sendo estes apenas do tipo Role BASIC. O microserviço já contempla a criação de um usuário ADMIN que fará o gerenciamento da aplicação;
- cada empresa contratada será tratada como usuário com perfil BASIC,  a qual terá obrigação de apresentar e postar documentos comprobatórios de suas obrigações legais junto ao governo federal, estadual e municipal. Então, deve haver uma tabela que organize esses documentos postados referentes a cada empresa contratada;
- cada empresa contratada poderá incluir funcionários, os quais também devem conter documentos de obrigações legais relacionados a eles, sendo estes funcionários identificados por seu CPF e Nome Completo. Ou seja, cada empresa (conhecida como usuário com role tipo BASIC) terá vinculada a ela um funcionário que é empregado da empresa e os arquivos postados são relacionados ao funcionário;
- A empresa contratada ou seja, o usuário com role BASIC terá um farol de reporte de seus documentos, se eles foram avaliados e aprovados. Assim como os documentos da empresa, seus funcionários também terão documentos.
- Cada documento postado, seja de empresa contratada, seja de funcionário da empresa contratada, deve-se registrar a data e hora que tais documentos foram postados;

-**Backend e Controllers**

- o backend deverá criar uma API REST que possibilite a integração das necessidades da aplicação;
- a empresa contratante é o usuário (UserAdmin) de role ADMIN, com privilegio de acesso às páginas da aplicação;
- a empresa contratada é o usuário (UserBasic) de role BASIC, com acessos restritos às páginas da aplicação;
- com base na verificação de usuário com role BASIC, criar métodos para captura e cadastro das empresas contratadas(usuários role BASIC) que possuam além de seus dados, tais como, CNPJ, Razão Social, Nome Fantasia, Endereço, Telefone, E-mail, devem possuir um número de contrato e um número de pedido, os quais estes dois últimos serão formalizados junto à contratante após o processo de negociação;
- criar a classe UserAdmin e seus métodos getters e setters;
- criar a classe UserBasic e seus métodos getters e setters, lembrando que tem os outros dois atributos a ela relacionados, o número de contrato e número de pedido. Além disso, relacionado à empresa contratada, serão postados e armazenados os documentos comprobatórios de atendimento legal, tais como: CNDT, CNAT, CNRF, GFIP;
- vinculado a uma empresa contratada (UserBasic), deverá ser criada uma classe (EmployeeBasic) funcionário que possui o CPF e nome completo como atributos. Além disso, relacionado aos funcionários da empresa contratada, deverão ser armazenados os documentos comprobatórios de vínculo com a empresa contratada, tais como: NR-01, NR-10, NR-35, NR-33, NR-34, ASO, FGTS, CTPS, Contrato de Trabalho, Ficha Registro Funcionário, Holerite, INSS. Alguns documentos são de vencimento mensal, ou seja, mensalmente o UserBasic deve enviar documentos relacionados aos funcionários, tais como: Holerite, FGTS, INSS. Todo evento de POST de documento relacionado ao EmployeeBasic deve carregar em si a data e hora que foi postado.
- cada documento avaliado receberá um status que deverá ser armazenado para visualização no Frontend. Assim, deverá haver a possibilidade de mensurar a quantidade total de documentos postados, uma flag de documentos que foram analisados e a quantidade destes analisados, quantidade de documentos aprovados e a quqntidade de documento reprovados. Todas essas ações devem gerar log com registro de data e horário que foram avaliados e aprovados ou reprovados;
- o backend deve retornar a quantidade de documentos analisados, postados, aprovados e reprovados para que sejam apresentados cards de report rápido no frontend da aplicação;
- o backend deverá obter por queries uma lista dos últimos 10 documentos postados, ou seja, os 10 documentos postados recentemente deverão ser reportados para uma apresentação em tabela no frontend;
- o backend deverá trazer uma lista de documentos postados de forma paginada para que seja montada uma tabela de report dos documentos postados, tendo a possibilidade de criar filtros por nome de empresa, nome de funcionário, por data de postagem.
- além disso, para facilitar o vínculo de mensageria, deverá ser retornado pelo backend a quantidade de documento postados recentemente e que por ventura ainda não tenham sido analisados, de maneira que seja apresentado em um Badge com a quantidade de documentos postados e ainda não visualizados;
- conforme foi estruturado o projeto, o backend foi feito em container Docker. Por isso, há um arquivo DockerFile do backend e um docker-compose da aplicação;
- criar as classes os repositories e controllers seguindo o padrão de pacotes e classes focado em boas práticas de programação (Design patterns)
