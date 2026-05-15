-- Script para limpar todo o banco de dados e prepará-lo para uma nova inicialização (onde o Flyway recriará as tabelas e o InitialSetupConfig recriará o tenant/usuário master).

-- ATENÇÃO: ISSO APAGARÁ TODOS OS DADOS DO BANCO.

DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
