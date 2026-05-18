package com.dbaagent.api.services.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiScriptSchemaValidatorTest {

    private final AiScriptSchemaValidator validator = new AiScriptSchemaValidator();
    private final AiScriptFormatter formatter = new AiScriptFormatter();

    @Test
    void removesIndexWithInventedColumn() {
        String ddl = "CREATE TABLE GER_HISTORICO_TENTATIVA_ACESSO (HTA_ID int, HTA_USUARIO varchar(50));";
        DdlSchemaIndex schema = DdlSchemaIndex.parse(ddl);

        String script = """
                IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_BAD' AND object_id = OBJECT_ID('GER_HISTORICO_TENTATIVA_ACESSO'))
                CREATE NONCLUSTERED INDEX IX_BAD ON GER_HISTORICO_TENTATIVA_ACESSO (HTA_DATA, HTA_USUARIO);
                IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_OK' AND object_id = OBJECT_ID('GER_HISTORICO_TENTATIVA_ACESSO'))
                CREATE NONCLUSTERED INDEX IX_OK ON GER_HISTORICO_TENTATIVA_ACESSO (HTA_USUARIO);
                """;

        var result = validator.sanitize(script, schema);
        assertFalse(result.script().contains("HTA_DATA"));
        assertTrue(result.script().contains("HTA_USUARIO"));
        assertFalse(result.warnings().isEmpty());
    }

    @Test
    void removesInventedColumnFromOneLineScriptLikeProduction() {
        String ddl = """
                CREATE TABLE GER_HISTORICO_TENTATIVA_ACESSO (HTA_ID int, HTA_USUARIO varchar(50));
                CREATE TABLE GER_FUNCIONARIO (FUN_DEM_DATA datetime, TPD_COD int, DES_COD int);
                """;
        DdlSchemaIndex schema = DdlSchemaIndex.parse(ddl);

        String oneLine = """
                IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_GER_HISTORICO_TENTATIVA_ACESSO_DATA_USUARIO' AND object_id = OBJECT_ID('GER_HISTORICO_TENTATIVA_ACESSO')) CREATE NONCLUSTERED INDEX IX_GER_HISTORICO_TENTATIVA_ACESSO_DATA_USUARIO ON GER_HISTORICO_TENTATIVA_ACESSO (HTA_DATA, HTA_USUARIO);IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_GER_FUNCIONARIO_DEM_DATA_INCLUDES' AND object_id = OBJECT_ID('GER_FUNCIONARIO')) CREATE NONCLUSTERED INDEX IX_GER_FUNCIONARIO_DEM_DATA_INCLUDES ON GER_FUNCIONARIO (FUN_DEM_DATA) INCLUDE (TPD_COD, DES_COD);
                """;

        var result = validator.sanitize(oneLine, schema);
        assertFalse(result.script().contains("HTA_DATA"));
        assertTrue(result.script().contains("FUN_DEM_DATA"));
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("HTA_DATA")));
    }

    @Test
    void formatterAddsLineBreaks() {
        String raw = "/* alerta */IF NOT EXISTS (SELECT 1) CREATE INDEX IX_A ON T (A);ALTER INDEX ALL ON T REORGANIZE;";
        String formatted = formatter.format(raw);
        assertTrue(formatted.contains("*/\n\nIF"));
        assertTrue(formatted.contains(";\n\nALTER"));
    }
}
